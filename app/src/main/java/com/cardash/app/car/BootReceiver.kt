package com.cardash.app.car

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Starts the car service after a reboot or an app update, before the launcher is even shown. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Only the two system events it's declared for (other apps can't send these).
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        CarService.start(context)
    }
}
