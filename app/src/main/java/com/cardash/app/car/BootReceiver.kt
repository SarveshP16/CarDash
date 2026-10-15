package com.cardash.app.car

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Starts the car service after a reboot or an app update, before the launcher is even shown. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        CarService.start(context)
    }
}
