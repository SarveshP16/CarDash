package com.cardash.app.car

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.cardash.app.MainActivity
import com.cardash.app.admin.DeviceOwner
import com.cardash.app.media.NowPlayingRepository
import com.cardash.app.settings.Settings
import com.cardash.app.system.SystemToggles

/** The "Car On" and "Car Off" steps. Each step is best-effort: one failing doesn't stop the rest. */
object CarRoutines {

    fun onCarOn(context: Context, s: Settings) {
        // Bluetooth stays on permanently: it's one of the car-detection signals.
        SystemToggles.setBluetooth(context, true)
        if (s.wifiOnCarOn) SystemToggles.setWifi(context, true)
        if (s.dndWhileDriving) SystemToggles.setDnd(context, true)
        if (s.autoBrightnessOnCarOn) SystemToggles.setAutoBrightness(context, true)
        wakeAndShowDashboard(context)
    }

    fun onCarOff(context: Context, s: Settings) {
        if (s.pauseMediaOnCarOff) NowPlayingRepository.pause()
        if (s.dndWhileDriving) SystemToggles.setDnd(context, false)
        // Radios off and screen off keep a parked phone cooler.
        if (s.wifiOffOnCarOff) SystemToggles.setWifi(context, false)
        if (s.screenOffOnCarOff) DeviceOwner.lockNow(context)
    }

    @Suppress("DEPRECATION")
    private fun wakeAndShowDashboard(context: Context) {
        val pm = context.getSystemService(PowerManager::class.java)
        // ACQUIRE_CAUSES_WAKEUP turns the screen on; the activity's FLAG_KEEP_SCREEN_ON then holds it.
        pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "CarDash:carOn",
        ).acquire(5_000)
        // Device Owner apps are exempt from Android 10's background-activity-start limits.
        context.startActivity(
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
        )
    }
}
