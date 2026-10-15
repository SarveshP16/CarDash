package com.cardash.app.system

import android.app.NotificationManager
import android.bluetooth.BluetoothManager
import android.content.Context
import android.net.wifi.WifiManager
import android.provider.Settings
import com.cardash.app.admin.DeviceOwner

/**
 * System switches used by the Car On/Off routines and the dashboard's quick toggles.
 * Wi-Fi and Bluetooth rely on targetSdk 28; DND needs notification-policy access and
 * brightness needs Device Owner or WRITE_SETTINGS, both granted by `setup.sh`.
 */
object SystemToggles {

    // --- Wi-Fi ---
    fun isWifiOn(context: Context): Boolean = wifi(context)?.isWifiEnabled == true

    @Suppress("DEPRECATION")
    fun setWifi(context: Context, on: Boolean): Boolean =
        runCatching { wifi(context)?.setWifiEnabled(on) == true }.getOrDefault(false)

    private fun wifi(context: Context) = context.applicationContext.getSystemService(WifiManager::class.java)

    // --- Bluetooth ---
    fun isBluetoothOn(context: Context): Boolean = bluetooth(context)?.isEnabled == true

    @Suppress("DEPRECATION", "MissingPermission")
    fun setBluetooth(context: Context, on: Boolean): Boolean = runCatching {
        val adapter = bluetooth(context) ?: return false
        if (on) adapter.enable() else adapter.disable()
    }.getOrDefault(false)

    private fun bluetooth(context: Context) = context.getSystemService(BluetoothManager::class.java)?.adapter

    // --- Do Not Disturb ---
    fun hasDndAccess(context: Context): Boolean = notifications(context).isNotificationPolicyAccessGranted

    fun isDndOn(context: Context): Boolean =
        notifications(context).currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL

    fun setDnd(context: Context, on: Boolean): Boolean {
        if (!hasDndAccess(context)) return false
        notifications(context).setInterruptionFilter(
            if (on) NotificationManager.INTERRUPTION_FILTER_PRIORITY else NotificationManager.INTERRUPTION_FILTER_ALL,
        )
        return true
    }

    private fun notifications(context: Context) = context.getSystemService(NotificationManager::class.java)

    // --- Brightness ---
    fun isAutoBrightness(context: Context): Boolean = Settings.System.getInt(
        context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, 0,
    ) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC

    fun setAutoBrightness(context: Context, on: Boolean): Boolean = writeSystem(
        context,
        Settings.System.SCREEN_BRIGHTNESS_MODE,
        if (on) Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC else Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
    )

    /** Manual brightness, 0–255. */
    fun setBrightness(context: Context, level: Int): Boolean =
        setAutoBrightness(context, false) &&
            writeSystem(context, Settings.System.SCREEN_BRIGHTNESS, level.coerceIn(1, 255))

    fun brightness(context: Context): Int =
        Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)

    private fun writeSystem(context: Context, key: String, value: Int): Boolean {
        if (DeviceOwner.setSystemSetting(context, key, value.toString())) return true
        if (!Settings.System.canWrite(context)) return false
        return Settings.System.putInt(context.contentResolver, key, value)
    }
}
