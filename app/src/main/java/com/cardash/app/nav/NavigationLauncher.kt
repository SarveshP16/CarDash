package com.cardash.app.nav

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

enum class NavApp(val label: String) { GOOGLE_MAPS("Google Maps"), WAZE("Waze") }

/** Opens Google Maps or Waze already navigating, since in-app turn-by-turn isn't an option. */
object NavigationLauncher {
    private const val MAPS_PACKAGE = "com.google.android.apps.maps"

    fun navigate(context: Context, address: String, app: NavApp): Boolean {
        val query = Uri.encode(address)
        val primary = when (app) {
            NavApp.GOOGLE_MAPS -> Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$query"))
                .setPackage(MAPS_PACKAGE)
            NavApp.WAZE -> Intent(Intent.ACTION_VIEW, Uri.parse("https://waze.com/ul?q=$query&navigate=yes"))
        }
        // Fallback: any app that handles a geo: query.
        val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$query"))
        return start(context, primary) || start(context, fallback)
    }

    private fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
