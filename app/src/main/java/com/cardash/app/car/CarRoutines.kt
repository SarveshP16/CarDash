package com.cardash.app.car

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import com.cardash.app.MainActivity
import com.cardash.app.admin.DeviceOwner
import com.cardash.app.camera.CamSide
import com.cardash.app.camera.CameraRecordService
import com.cardash.app.media.MusicApp
import com.cardash.app.media.NowPlayingRepository
import com.cardash.app.nav.InAppNavigator
import com.cardash.app.settings.Settings
import com.cardash.app.system.SystemToggles

/** The "Car On" and "Car Off" steps. Each step is best-effort: one failing doesn't stop the rest. */
object CarRoutines {
    private const val RESUME_DELAY_MS = 3_000L
    private const val DASHCAM_DELAY_MS = 2_000L

    fun onCarOn(context: Context, s: Settings) {
        // Bluetooth stays on permanently: it's one of the car-detection signals.
        SystemToggles.setBluetooth(context, true)
        if (s.wifiOnCarOn) SystemToggles.setWifi(context, true)
        if (s.dndWhileDriving) SystemToggles.setDnd(context, true)
        if (s.autoBrightnessOnCarOn) SystemToggles.setAutoBrightness(context, true)
        ParkedState.cancel()
        wakeAndShowDashboard(context)
        if (s.dashcamAutoRecord) {
            // Once the dashboard is in front: Android lets a visible app start camera recording.
            Handler(Looper.getMainLooper()).postDelayed(
                { CameraRecordService.start(context, if (s.dashcamBackCamera) CamSide.BACK else CamSide.FRONT) },
                DASHCAM_DELAY_MS,
            )
        }
        if (s.resumeMusicOnCarOn && ResumeState.consume(context)) {
            // Give the audio output (AUX/USB) a moment to come up before playback starts.
            Handler(Looper.getMainLooper()).postDelayed(
                { NowPlayingRepository.resume(context, MusicApp.resolve(context)) },
                RESUME_DELAY_MS,
            )
        }
    }

    fun onCarOff(context: Context, s: Settings) {
        ResumeState.save(context, wasPlaying = NowPlayingRepository.isPlaying)
        if (s.pauseMediaOnCarOff) NowPlayingRepository.pause()
        InAppNavigator.stop()
        CameraRecordService.stopAll()
        if (s.dndWhileDriving) SystemToggles.setDnd(context, false)
        // Radios off and screen off keep a parked phone cooler.
        if (s.wifiOffOnCarOff) SystemToggles.setWifi(context, false)
        if (s.screenOffOnCarOff) {
            if (s.parkedScreen) {
                // Bring the dashboard up so the parked screen shows even if another app was open.
                showDashboard(context)
                ParkedState.begin(context, s.parkedSeconds)
            } else {
                DeviceOwner.lockNow(context)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun wakeAndShowDashboard(context: Context) {
        val pm = context.getSystemService(PowerManager::class.java)
        // ACQUIRE_CAUSES_WAKEUP turns the screen on; the activity's FLAG_KEEP_SCREEN_ON then holds it.
        pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "CarDash:carOn",
        ).acquire(5_000)
        showDashboard(context)
    }

    // Device Owner apps are exempt from Android's background-activity-start limits.
    private fun showDashboard(context: Context) {
        context.startActivity(
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
        )
    }
}
