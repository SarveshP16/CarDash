package com.cardash.link

import android.app.Application
import com.cardash.link.media.PhoneMedia
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class LinkApp : Application() {
    override fun onCreate() {
        super.onCreate()
        LinkService.createChannel(this)
        PhoneMedia.start(this)
        LinkServer.init(this)
        // The link (and its notification) only runs while the hotspot is on; the car can't connect otherwise.
        // The process itself stays alive cheaply because Android keeps the music listener bound.
        Hotspot.watch(this)
        MainScope().launch {
            Hotspot.on.collect { on -> if (on) LinkService.start(this@LinkApp) else LinkService.stop(this@LinkApp) }
        }
    }
}
