package com.cardash.link

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/** Keeps the link listening for the car, with a quiet ongoing notification showing its state. */
class LinkService : LifecycleService() {
    override fun onCreate() {
        super.onCreate()
        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, notification(this, LinkServer.status.value),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
        )
        LinkServer.start()
        lifecycleScope.launch {
            LinkServer.status.collect {
                getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(this@LinkService, it))
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onDestroy() {
        LinkServer.stop()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "link"
        private const val NOTIFICATION_ID = 1

        fun createChannel(context: Context) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL, context.getString(R.string.channel_link), NotificationManager.IMPORTANCE_MIN),
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LinkService::class.java))
        }

        /** Android may refuse while the app is in the background without unrestricted battery. */
        fun start(context: Context) {
            runCatching { context.startForegroundService(Intent(context, LinkService::class.java)) }
                .onFailure { Log.w("LinkService", "Couldn't start the link service", it) }
        }

        private fun notification(context: Context, status: LinkStatus): Notification =
            Notification.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(
                    when (status) {
                        is LinkStatus.Connected -> "Connected to ${status.carName}"
                        is LinkStatus.Waiting -> "Waiting for ${status.carName}"
                        LinkStatus.NotPaired -> "Not paired with a car"
                    },
                )
                .setContentText(
                    when (status) {
                        is LinkStatus.Connected -> "Sharing places and music with the car"
                        is LinkStatus.Waiting -> "Connects when the car joins your hotspot"
                        LinkStatus.NotPaired -> "Open CarDash Link to pair"
                    },
                )
                .setContentIntent(
                    PendingIntent.getActivity(
                        context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
                    ),
                )
                .setOngoing(true)
                .build()
    }
}

/** Wakes the app after a reboot or an update; it then starts the link if the hotspot is on. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Hotspot.watch(context)
    }
}
