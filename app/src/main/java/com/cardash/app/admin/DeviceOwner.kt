package com.cardash.app.admin

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.cardash.app.MainActivity

/**
 * Everything that needs Device Owner (or plain device admin, for [lockNow]).
 * All calls are no-ops when the app isn't owner, so the app still runs as a normal launcher.
 */
object DeviceOwner {
    private const val TAG = "DeviceOwner"

    private val runtimePermissions = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    private fun dpm(context: Context) = context.getSystemService(DevicePolicyManager::class.java)

    fun isOwner(context: Context): Boolean = dpm(context).isDeviceOwnerApp(context.packageName)

    fun isAdmin(context: Context): Boolean = dpm(context).isAdminActive(AdminReceiver.component(context))

    fun applyPolicies(context: Context) {
        if (!isOwner(context)) return
        val dpm = dpm(context)
        val admin = AdminReceiver.component(context)
        runCatching {
            // No lock screen when the car turns on (only works while no PIN/pattern is set).
            dpm.setKeyguardDisabled(admin, true)
            runtimePermissions.forEach {
                dpm.setPermissionGrantState(
                    admin, context.packageName, it, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED,
                )
            }
            val home = IntentFilter(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addCategory(Intent.CATEGORY_DEFAULT)
            }
            dpm.addPersistentPreferredActivity(admin, home, ComponentName(context, MainActivity::class.java))
        }.onFailure { Log.w(TAG, "Applying owner policies failed", it) }
    }

    /** Turns the screen off. Works for any active admin; the owner always is one. */
    fun lockNow(context: Context) {
        if (isAdmin(context)) runCatching { dpm(context).lockNow() }
    }

    /** Writes a Settings.System value the owner is allowed to set (brightness, brightness mode, timeout). */
    fun setSystemSetting(context: Context, key: String, value: String): Boolean {
        if (!isOwner(context)) return false
        return runCatching { dpm(context).setSystemSetting(AdminReceiver.component(context), key, value) }.isSuccess
    }

    /**
     * Escape hatch: undoes the owner policies and gives up Device Owner, so the app can be
     * uninstalled or the phone used normally again without a factory reset.
     */
    fun release(context: Context) {
        if (!isOwner(context)) return
        val dpm = dpm(context)
        val admin = AdminReceiver.component(context)
        runCatching {
            dpm.setKeyguardDisabled(admin, false)
            dpm.clearPackagePersistentPreferredActivities(admin, context.packageName)
            @Suppress("DEPRECATION")
            dpm.clearDeviceOwnerApp(context.packageName)
        }.onFailure { Log.w(TAG, "Releasing device owner failed", it) }
    }
}
