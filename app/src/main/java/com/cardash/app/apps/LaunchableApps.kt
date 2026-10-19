package com.cardash.app.apps

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

data class LaunchableApp(val label: String, val packageName: String, val icon: ImageBitmap)

/** Apps that appear in a launcher, used by the app drawer and the music-app picker. */
object LaunchableApps {
    /** Slow (loads icons): call off the main thread. */
    fun load(context: Context): List<LaunchableApp> {
        val pm = context.packageManager
        return pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .filter { it.activityInfo.packageName != context.packageName }
            .map {
                LaunchableApp(
                    label = it.loadLabel(pm).toString(),
                    packageName = it.activityInfo.packageName,
                    icon = it.loadIcon(pm).toBitmap(144, 144).asImageBitmap(),
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    fun packages(context: Context): List<String> =
        context.packageManager
            .queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { it.activityInfo.packageName }
            .distinct()

    fun label(context: Context, packageName: String): String? = runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    }.getOrNull()

    fun launch(context: Context, packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return true
    }
}
