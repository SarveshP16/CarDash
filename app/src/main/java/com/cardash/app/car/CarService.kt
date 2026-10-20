package com.cardash.app.car

import android.app.Service
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.cardash.app.battery.BatteryRepository
import com.cardash.app.battery.ChargeLimiter
import com.cardash.app.battery.HeatLevel
import com.cardash.app.battery.HeatRules
import com.cardash.app.link.LinkClient
import com.cardash.app.notifications.Notifications
import com.cardash.app.settings.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Foreground service that watches the charger and the car's Bluetooth, decides car on/off
 * and runs the routines. Power broadcasts can't be declared in the manifest since Android 8,
 * so this must stay running; it also watches battery temperature.
 */
class CarService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var offJob: Job? = null
    private var heatLevel = HeatLevel.NORMAL

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_POWER_CONNECTED -> updateSignals { it.copy(powered = true) }
                // While the charge limiter has charging paused, USB reads as unplugged: that's not the car.
                Intent.ACTION_POWER_DISCONNECTED ->
                    if (!ChargeLimiter.holding.value) updateSignals { it.copy(powered = false) }
                BluetoothDevice.ACTION_ACL_CONNECTED, BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    // The typed overload needs API 33; the phone is on 29.
                    @Suppress("DEPRECATION")
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    if (device != null && isCarDevice(device)) {
                        val connected = intent.action == BluetoothDevice.ACTION_ACL_CONNECTED
                        updateSignals { it.copy(carBluetoothConnected = connected) }
                    }
                }
                Intent.ACTION_BATTERY_CHANGED -> onBattery(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        ServiceCompat.startForeground(
            this,
            Notifications.STATUS_ID,
            Notifications.status(this, CarRepository.state.value),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        // ACTION_BATTERY_CHANGED is sticky, so this returns the current battery state immediately.
        // Exported: the Bluetooth broadcasts come from the Bluetooth app, not the system UID.
        // They're protected broadcasts, so no other app can fake them.
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_EXPORTED)?.let { sticky ->
            onBattery(sticky)
            updateSignals { it.copy(powered = BatteryRepository.info.value.plugged) }
        }
        checkCarBluetoothConnected()
        // CarDash Link: connects to the main phone whenever this phone is on its hotspot.
        LinkClient.start(this)

        // A hold left over from before a restart: turn charging back on, the limiter re-pauses if needed.
        scope.launch { ChargeLimiter.release(this@CarService) }
        scope.launch {
            SettingsStore.settings.map { it.chargeLimitEnabled }.distinctUntilChanged().collect { enabled ->
                if (!enabled) ChargeLimiter.release(this@CarService)
            }
        }

        // Re-evaluate when the car device or the "charger always on" setting changes.
        scope.launch {
            SettingsStore.settings
                .map { it.carBluetoothAddress to it.chargerAlwaysOn }
                .distinctUntilChanged()
                .collect {
                    checkCarBluetoothConnected()
                    evaluate()
                }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_FORCE_ON -> updateSignals(manual = true) { it.copy(manualOverride = CarState.ON) }
            ACTION_FORCE_OFF -> updateSignals(manual = true) { it.copy(manualOverride = CarState.OFF) }
            ACTION_AUTO -> updateSignals(manual = true) { it.copy(manualOverride = null) }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        unregisterReceiver(receiver)
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun isCarDevice(device: BluetoothDevice): Boolean =
        device.address.equals(SettingsStore.current.carBluetoothAddress, ignoreCase = true)

    /** ACL broadcasts only report changes, so ask the hands-free profile who is connected right now. */
    @Suppress("MissingPermission")
    private fun checkCarBluetoothConnected() {
        val address = SettingsStore.current.carBluetoothAddress
        val adapter = getSystemService(BluetoothManager::class.java)?.adapter
        if (address == null || adapter == null || !adapter.isEnabled) {
            updateSignals { it.copy(carBluetoothConnected = false) }
            return
        }
        adapter.getProfileProxy(this, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                val connected = proxy.connectedDevices.any { it.address.equals(address, ignoreCase = true) }
                adapter.closeProfileProxy(profile, proxy)
                scope.launch { updateSignals { it.copy(carBluetoothConnected = connected) } }
            }
            override fun onServiceDisconnected(profile: Int) = Unit
        }, BluetoothProfile.HEADSET)
    }

    /** A real charger/Bluetooth change cancels any manual "Car mode" override, so automation takes back over. */
    private fun updateSignals(manual: Boolean = false, transform: (CarSignals) -> CarSignals) {
        val previous = CarRepository.signals.value
        var next = transform(previous)
        if (!manual && next.copy(manualOverride = null) != previous.copy(manualOverride = null)) {
            next = next.copy(manualOverride = null)
        }
        if (next == previous) return
        CarRepository.setSignals(next)
        evaluate()
    }

    private fun evaluate() {
        val signals = CarRepository.signals.value
        val target = CarStateLogic.resolve(signals, SettingsStore.current.chargerAlwaysOn)
        val current = CarRepository.state.value
        when {
            target == CarState.ON -> {
                offJob?.cancel()
                offJob = null
                if (current != CarState.ON) transition(CarState.ON)
            }
            current == CarState.ON && offJob == null -> {
                // A manual "off" is immediate; a lost signal waits out the debounce.
                val wait = if (signals.manualOverride == CarState.OFF) 0L else CarStateLogic.OFF_DEBOUNCE_MS
                offJob = scope.launch {
                    delay(wait)
                    offJob = null
                    transition(CarState.OFF)
                }
            }
        }
    }

    private fun transition(state: CarState) {
        CarRepository.setState(state)
        Notifications.updateStatus(this, state)
        val settings = SettingsStore.current
        if (state == CarState.ON) CarRoutines.onCarOn(this, settings) else CarRoutines.onCarOff(this, settings)
        // Parked: charge normally again so the next start is detected by the charger as usual.
        if (state == CarState.OFF) scope.launch { ChargeLimiter.release(this@CarService) }
    }

    private fun onBattery(intent: Intent) {
        val info = BatteryRepository.update(intent)
        val s = SettingsStore.current
        scope.launch { ChargeLimiter.onBattery(this@CarService, info, s.chargeLimitEnabled, s.chargeLimit) }
        val level = HeatRules.level(info.tempC, SettingsStore.current.heatWarnC)
        if (level != heatLevel) {
            heatLevel = level
            Notifications.showHeat(this, level, info.tempC)
        }
    }

    companion object {
        const val ACTION_FORCE_ON = "com.cardash.app.FORCE_ON"
        const val ACTION_FORCE_OFF = "com.cardash.app.FORCE_OFF"
        const val ACTION_AUTO = "com.cardash.app.AUTO"

        fun start(context: Context, action: String? = null) {
            context.startForegroundService(Intent(context, CarService::class.java).setAction(action))
        }
    }
}
