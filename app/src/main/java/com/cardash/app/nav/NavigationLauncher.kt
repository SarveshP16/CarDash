package com.cardash.app.nav

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/** [shortLabel] fits the dashboard tile. */
enum class NavApp(val label: String, val shortLabel: String, val packageName: String) {
    GOOGLE_MAPS("Google Maps", "Maps", "com.google.android.apps.maps"),
    WAZE("Waze", "Waze", "com.waze"),
}

/** Opens Google Maps or Waze already navigating to an address, place name or pasted coordinates. */
object NavigationLauncher {
    fun navigate(context: Context, address: String, app: NavApp): Boolean {
        val coords = Coordinates.parse(address)
        // Coordinates go in as-is; anything else is a search the app resolves itself.
        val query = coords?.toString() ?: Uri.encode(address)
        val primary = when (app) {
            NavApp.GOOGLE_MAPS -> Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$query"))
                .setPackage(app.packageName)
            NavApp.WAZE -> Intent(
                Intent.ACTION_VIEW,
                Uri.parse(if (coords != null) "https://waze.com/ul?ll=$query&navigate=yes" else "https://waze.com/ul?q=$query&navigate=yes"),
            )
        }
        // Fallback: any app that handles a geo: query.
        val fallback = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(if (coords != null) "geo:$query?q=$query" else "geo:0,0?q=$query"),
        )
        return start(context, primary) || start(context, fallback)
    }

    /** Opens the nav app on its own map (no destination). */
    fun open(context: Context, app: NavApp): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(app.packageName) ?: return false
        return start(context, intent)
    }

    private fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
