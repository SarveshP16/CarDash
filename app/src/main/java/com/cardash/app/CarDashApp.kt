package com.cardash.app

import android.app.Application
import com.cardash.app.admin.DeviceOwner
import com.cardash.app.car.CarService
import com.cardash.app.media.NowPlayingRepository
import com.cardash.app.notifications.Notifications
import com.cardash.app.settings.SettingsStore

class CarDashApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SettingsStore.init(this)
        Notifications.createChannels(this)
        DeviceOwner.applyPolicies(this)
        NowPlayingRepository.start(this)
        CarService.start(this)
    }
}
