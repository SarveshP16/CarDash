package com.cardash.app.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.cardash.app.MainActivity
import com.cardash.app.R
import com.cardash.app.battery.HeatLevel
import com.cardash.app.car.CarState

object Notifications {
    private const val CHANNEL_STATUS = "status"
    private const val CHANNEL_ALERTS = "alerts"
    private const val CHANNEL_RECORDING = "recording"
    const val STATUS_ID = 1
    private const val HEAT_ID = 2
    const val RECORDING_ID = 3

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_STATUS, context.getString(R.string.channel_status), NotificationManager.IMPORTANCE_MIN),
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERTS, context.getString(R.string.channel_alerts), NotificationManager.IMPORTANCE_HIGH),
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_RECORDING, context.getString(R.string.channel_recording), NotificationManager.IMPORTANCE_LOW),
        )
    }

    /** The foreground-service notification that keeps car detection alive. */
    fun status(context: Context, state: CarState): Notification =
        Notification.Builder(context, CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(if (state == CarState.ON) "Car on" else "Car off")
            .setContentText("Watching the charger and car Bluetooth")
            .setContentIntent(openApp(context))
            .setOngoing(true)
            .build()

    fun updateStatus(context: Context, state: CarState) {
        context.getSystemService(NotificationManager::class.java).notify(STATUS_ID, status(context, state))
    }

    fun showHeat(context: Context, level: HeatLevel, tempC: Float) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (level == HeatLevel.NORMAL) {
            nm.cancel(HEAT_ID)
            return
        }
        val title = if (level == HeatLevel.CRITICAL) "Phone is dangerously hot" else "Phone is getting hot"
        nm.notify(
            HEAT_ID,
            Notification.Builder(context, CHANNEL_ALERTS)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText("Battery at %.1f°C. Shade it, or unplug the charger.".format(tempC))
                .setContentIntent(openApp(context))
                .setOnlyAlertOnce(true)
                .build(),
        )
    }

    /** Shown while the camera service records (Android requires it for background camera use). */
    fun recording(context: Context, what: String): Notification =
        Notification.Builder(context, CHANNEL_RECORDING)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(what)
            .setContentText("Saving to Movies/CarDash")
            .setContentIntent(openApp(context))
            .setOngoing(true)
            .build()

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
    )
}
