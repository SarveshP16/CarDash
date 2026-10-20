package com.cardash.link

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/** "Share → Send to car": routes the shared place on the car, or queues it until the car connects (hotspot on). */
class ShareActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
        lifecycleScope.launch {
            val place = MapsLinks.placeFromShare(text, subject)
            val message = when {
                place == null -> "Couldn't find a place in what was shared"
                LinkServer.navigate(place) -> "Sent ${place.name} to the car"
                else -> "The car isn't connected. ${place.name} will be sent when it connects."
            }
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
            finish()
        }
    }
}
