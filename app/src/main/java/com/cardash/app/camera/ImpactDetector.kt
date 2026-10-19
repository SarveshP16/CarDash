package com.cardash.app.camera

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Watches the accelerometer while recording. A jolt well beyond hard braking (default 2.5 g total)
 * counts as an impact, so the current dashcam clip gets locked. One trigger per 10 s at most.
 */
class ImpactDetector(context: Context, private val onImpact: () -> Unit) : SensorEventListener {
    private val sensors = context.getSystemService(SensorManager::class.java)
    private var lastTrigger = 0L

    fun start() {
        sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sensors.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() = sensors.unregisterListener(this)

    override fun onSensorChanged(event: SensorEvent) {
        val (x, y, z) = event.values
        val g = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
        val now = System.currentTimeMillis()
        if (isImpact(g) && now - lastTrigger > 10_000) {
            lastTrigger = now
            onImpact()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        const val THRESHOLD_G = 2.5f

        /** Pure: total acceleration in g (1.0 at rest). */
        fun isImpact(totalG: Float) = totalG >= THRESHOLD_G
    }
}
