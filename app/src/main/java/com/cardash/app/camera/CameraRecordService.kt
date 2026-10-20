package com.cardash.app.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.cardash.app.notifications.Notifications
import com.cardash.app.settings.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.coroutines.resume

enum class CamSide(val label: String, val selector: CameraSelector) {
    FRONT("Front camera", CameraSelector.DEFAULT_FRONT_CAMERA),
    BACK("Back camera", CameraSelector.DEFAULT_BACK_CAMERA),
}

/**
 * Dashcam: records the front or back camera with no preview, as a camera foreground service
 * (Android only allows preview-less camera use from one). Video only, saved to Movies/CarDash in
 * 3-minute segments, so loop recording can delete the oldest and an impact locks just the
 * segment it happened in. One camera at a time: the OnePlus 5 can't run both together.
 */
class CameraRecordService : LifecycleService() {
    private var provider: ProcessCameraProvider? = null
    private var capture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var side: CamSide? = null
    /** Sound is recorded with this recording (setting on and microphone granted). */
    private var audio = false
    private var rotation: Job? = null
    /** Set by an impact; the current segment is renamed "_locked" when it finishes. */
    private var lockCurrent = false
    private val impacts by lazy { ImpactDetector(this) { onImpact() } }

    override fun onCreate() {
        super.onCreate()
        instance = this
        ServiceCompat.startForeground(
            this, Notifications.RECORDING_ID, Notifications.recording(this, "Starting camera…"),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA,
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val requested = intent?.getStringExtra(EXTRA_SIDE)?.let { runCatching { CamSide.valueOf(it) }.getOrNull() }
        val startOnly = intent?.getBooleanExtra(EXTRA_START_ONLY, false) == true
        if (requested != null) {
            lifecycleScope.launch { if (startOnly) startIfIdle(requested) else toggle(requested) }
        } else {
            stopIfIdle()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopRecording(announce = false)
        instance = null
        _recordingSide.value = null
        super.onDestroy()
    }

    private suspend fun toggle(requested: CamSide) {
        val current = side
        when {
            current == requested -> stopRecording(announce = true)
            current != null -> toast("Stop the ${current.label.lowercase()} first. This phone records one camera at a time.")
            else -> start(requested, announce = true)
        }
    }

    /** Auto-record on Car On: don't toggle off something already recording. */
    private suspend fun startIfIdle(requested: CamSide) {
        if (side == null) start(requested, announce = true) else stopIfIdle()
    }

    private suspend fun start(requested: CamSide, announce: Boolean) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            toast("Camera permission missing. Run setup.sh")
            stopIfIdle()
            return
        }
        val p = provider ?: cameraProvider().also { provider = it }
        if (!p.hasCamera(requested.selector)) {
            toast("No ${requested.label.lowercase()} on this phone")
            stopIfIdle()
            return
        }
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HD, FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)))
            .build()
        val videoCapture = VideoCapture.withOutput(recorder)
        try {
            p.unbindAll()
            p.bindToLifecycle(this, requested.selector, videoCapture)
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't open ${requested.label}", e)
            toast("Couldn't open the ${requested.label.lowercase()}")
            stopIfIdle()
            return
        }
        audio = SettingsStore.current.dashcamAudio &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        // The microphone needs its own foreground-service type while sound is recorded.
        ServiceCompat.startForeground(
            this, Notifications.RECORDING_ID, Notifications.recording(this, "Recording: ${requested.label.lowercase()}"),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or (if (audio) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0),
        )
        capture = videoCapture
        side = requested
        _recordingSide.value = requested
        startSegment()
        impacts.start()
        getSystemService(android.app.NotificationManager::class.java)
            .notify(Notifications.RECORDING_ID, Notifications.recording(this, "Recording: ${requested.label.lowercase()}"))
        if (announce) toast("${requested.label} recording started")
    }

    private fun startSegment() {
        val cam = side ?: return
        val videoCapture = capture ?: return
        val name = "CarDash_${cam.name.lowercase()}_${LocalDateTime.now().format(FILE_TIME)}"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Movies/CarDash")
        }
        val output = MediaStoreOutputOptions.Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(values)
            .build()
        lockCurrent = false
        @SuppressLint("MissingPermission") // Checked in start(): audio is only set with RECORD_AUDIO granted.
        val pending = videoCapture.output.prepareRecording(this, output).apply { if (audio) withAudioEnabled() }
        recording = pending.start(ContextCompat.getMainExecutor(this)) { event ->
            if (event is VideoRecordEvent.Finalize) onSegmentFinished(event)
        }
        rotation?.cancel()
        rotation = lifecycleScope.launch {
            delay(SEGMENT_MS)
            // Stopping finalizes this segment; onSegmentFinished starts the next one.
            recording?.stop()
        }
    }

    private fun onSegmentFinished(event: VideoRecordEvent.Finalize) {
        if (event.hasError() && event.error != VideoRecordEvent.Finalize.ERROR_NO_VALID_DATA) {
            Log.w(TAG, "Segment ended with error ${event.error}", event.cause)
        }
        val uri = event.outputResults.outputUri
        val locked = lockCurrent
        val s = SettingsStore.current
        // Not lifecycleScope: the last segment finishes after the service has stopped itself.
        housekeeping.launch {
            if (locked && uri != android.net.Uri.EMPTY) DashcamStorage.lock(this@CameraRecordService, uri)
            if (s.dashcamLoop) DashcamStorage.cleanup(this@CameraRecordService, s.dashcamLimitGb * GB)
        }
        // Still recording (segment rotation rather than a stop): carry straight on.
        if (side != null) startSegment()
    }

    private fun onImpact() {
        if (side == null || lockCurrent) return
        lockCurrent = true
        toast("Impact detected. This clip is saved and won't be deleted.")
    }

    private fun stopRecording(announce: Boolean) {
        val stopped = side
        // Clear the side first so the final segment's callback doesn't start another one.
        side = null
        _recordingSide.value = null
        rotation?.cancel()
        impacts.stop()
        recording?.stop()
        recording = null
        capture = null
        provider?.unbindAll()
        if (announce && stopped != null) toast("${stopped.label} recording stopped")
        stopIfIdle()
    }

    private fun stopIfIdle() {
        if (side == null) stopSelf()
    }

    private suspend fun cameraProvider(): ProcessCameraProvider = suspendCancellableCoroutine { cont ->
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({ cont.resume(future.get()) }, ContextCompat.getMainExecutor(this))
    }

    private fun toast(message: String) = Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()

    companion object {
        private const val TAG = "CameraRecord"
        private const val EXTRA_SIDE = "side"
        private const val EXTRA_START_ONLY = "startOnly"
        private const val SEGMENT_MS = 3 * 60_000L
        private const val GB = 1_000_000_000L
        private val FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

        private val _recordingSide = MutableStateFlow<CamSide?>(null)
        /** Which camera is recording right now (drives the button's red "recording" state). */
        val recordingSide: StateFlow<CamSide?> = _recordingSide.asStateFlow()

        private var instance: CameraRecordService? = null
        private val housekeeping = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /** Starts recording [side], or stops it if it's the one already recording. Call from the UI. */
        fun toggle(context: Context, side: CamSide) {
            ContextCompat.startForegroundService(
                context, Intent(context, CameraRecordService::class.java).putExtra(EXTRA_SIDE, side.name),
            )
        }

        /** Auto-record on Car On: starts [side] unless something is already recording. */
        fun start(context: Context, side: CamSide) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, CameraRecordService::class.java)
                    .putExtra(EXTRA_SIDE, side.name)
                    .putExtra(EXTRA_START_ONLY, true),
            )
        }

        /** Used by the Car Off routine. */
        fun stopAll() {
            instance?.stopRecording(announce = true)
        }
    }
}
