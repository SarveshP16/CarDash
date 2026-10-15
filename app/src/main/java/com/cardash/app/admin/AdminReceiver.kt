package com.cardash.app.admin

import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/** The device-admin component that `setup.sh` makes Device Owner via `dpm set-device-owner`. */
class AdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        DeviceOwner.applyPolicies(context)
    }

    companion object {
        fun component(context: Context) = ComponentName(context, AdminReceiver::class.java)
    }
}
