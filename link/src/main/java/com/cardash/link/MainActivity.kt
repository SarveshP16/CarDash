package com.cardash.link

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cardash.link.media.PhoneMedia
import com.cardash.link.ui.LinkScreen
import com.cardash.link.ui.LinkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { LinkTheme { LinkScreen() } }
    }

    override fun onResume() {
        super.onResume()
        // Coming back from the settings screens: pick up newly granted access.
        PhoneMedia.start(this)
        Hotspot.check()
    }
}
