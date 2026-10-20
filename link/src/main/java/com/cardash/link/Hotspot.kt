package com.cardash.link

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Whether this phone's hotspot is on, so the link only runs while the car could reach it.
 * Android announces hotspot changes but (since 16) not the current state, so the state itself is
 * read from the network interfaces: a hotspot is an up interface with a private IPv4 address that
 * isn't one of the phone's own connections (Wi-Fi, mobile data, VPN).
 */
object Hotspot {
    private const val TAG = "Hotspot"
    private const val ACTION_AP_STATE = "android.net.wifi.WIFI_AP_STATE_CHANGED"
    /** The hotspot's address comes up a moment after the broadcast, so look again a few times. */
    private val RECHECK_MS = longArrayOf(1_000, 3_000, 8_000)
    /** Mobile-data and virtual interfaces, some of which apps can't see as networks. */
    private val IGNORED = listOf("rmnet", "ccmni", "dummy", "tun", "ifb", "lo")

    private val _on = MutableStateFlow(false)
    val on: StateFlow<Boolean> = _on.asStateFlow()

    private lateinit var app: Context
    private val main = Handler(Looper.getMainLooper())
    private var watching = false

    fun watch(context: Context) {
        app = context.applicationContext
        if (!watching) {
            watching = true
            // Sent by the system Wi-Fi service; exported so it's delivered.
            ContextCompat.registerReceiver(
                app,
                object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        Log.i(TAG, "Hotspot state broadcast: ${intent.getIntExtra("wifi_state", -1)}")
                        recheck()
                    }
                },
                IntentFilter(ACTION_AP_STATE),
                ContextCompat.RECEIVER_EXPORTED,
            )
        }
        check()
    }

    /** Checks now and again shortly after (the hotspot takes a few seconds to come up or go down). */
    fun recheck() {
        check()
        main.removeCallbacksAndMessages(null)
        RECHECK_MS.forEach { main.postDelayed(::check, it) }
    }

    fun check() {
        if (!::app.isInitialized) return
        val now = runCatching { detect() }.getOrDefault(false)
        if (now != _on.value) Log.i(TAG, "Hotspot ${if (now) "on" else "off"}")
        _on.value = now
    }

    private fun detect(): Boolean {
        val cm = app.getSystemService(ConnectivityManager::class.java)
        // Android 16 lists the hotspot as a network too, but only the phone's own connections
        // (Wi-Fi, mobile data, VPN) have a default route.
        @Suppress("DEPRECATION")
        val own = cm.allNetworks.mapNotNull { cm.getLinkProperties(it) }
            .filter { lp -> lp.routes.any { it.isDefaultRoute } }
            .mapNotNull { it.interfaceName }
            .toSet()
        return NetworkInterface.getNetworkInterfaces().asSequence()
            .filter { it.isUp && !it.isLoopback && it.name !in own && IGNORED.none(it.name::startsWith) }
            .any { ni -> ni.inetAddresses.asSequence().any { it is Inet4Address && it.isSiteLocalAddress } }
    }
}
