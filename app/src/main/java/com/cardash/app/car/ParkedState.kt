package com.cardash.app.car

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.cardash.app.admin.DeviceOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The "parked" screen shown for a few seconds after Car Off, before the screen turns off.
 * [screenOffAt] is non-null while it's showing (epoch ms when the screen will go off).
 */
object ParkedState {
    private val handler = Handler(Looper.getMainLooper())
    private var pending: Runnable? = null

    private val _screenOffAt = MutableStateFlow<Long?>(null)
    val screenOffAt: StateFlow<Long?> = _screenOffAt.asStateFlow()

    fun begin(context: Context, seconds: Int) {
        cancel()
        val app = context.applicationContext
        val turnOff = Runnable {
            pending = null
            _screenOffAt.value = null
            DeviceOwner.lockNow(app)
        }
        pending = turnOff
        _screenOffAt.value = System.currentTimeMillis() + seconds * 1_000L
        handler.postDelayed(turnOff, seconds * 1_000L)
    }

    fun turnOffNow(context: Context) {
        cancel()
        DeviceOwner.lockNow(context.applicationContext)
    }

    /** "Keep screen on" or Car On: hide the parked screen and don't turn the screen off. */
    fun cancel() {
        pending?.let(handler::removeCallbacks)
        pending = null
        _screenOffAt.value = null
    }
}
