package com.cardash.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cardash.app.link.LinkClient
import com.cardash.app.link.LinkState
import com.cardash.app.settings.Settings
import com.cardash.app.settings.SettingsStore

/** CarDash Link: pairing with the main phone, and how its music follows the car. */
@Composable
fun LinkSection(s: Settings) {
    val state by LinkClient.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme

    SectionTitle("Phone link")
    Text(
        when (val st = state) {
            is LinkState.Connected -> "Connected to ${st.phoneName}"
            LinkState.Connecting -> "Connecting…"
            is LinkState.Searching -> st.note
            is LinkState.NotPaired -> "Not paired"
        },
        color = if (state is LinkState.Connected) colors.primary else colors.onBackground,
        fontSize = 17.sp,
    )
    (state as? LinkState.NotPaired)?.note?.let { Text(it, color = colors.error, fontSize = 14.sp) }

    if (s.linkToken == null) {
        Text(
            "Install CarDash Link on your main phone and turn on its hotspot. Once this phone has joined " +
                "the hotspot, tap Pair with car in CarDash Link and type the code here.",
            color = colors.onSurfaceVariant,
            fontSize = 14.sp,
        )
        var code by rememberSaveable { mutableStateOf("") }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = code,
                onValueChange = { v -> code = v.filter(Char::isDigit).take(6) },
                label = { Text("Code") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.width(180.dp),
            )
            Button(onClick = { LinkClient.pair(code); code = "" }, enabled = code.length == 6) { Text("Pair") }
        }
    } else {
        Text("Paired with ${s.linkPhoneName ?: "your phone"}", color = colors.onSurfaceVariant, fontSize = 14.sp)
        OutlinedButton(onClick = LinkClient::unpair) { Text("Unpair") }
    }

    SectionTitle("Phone music")
    Text(
        "Music on the phone shows on the dashboard while nothing plays here, and the buttons control it.",
        color = colors.onSurfaceVariant,
        fontSize = 14.sp,
    )
    SwitchRow(
        "Resume on Car On",
        s.resumeMusicOnCarOn,
        "If it was playing when the car turned off. Waits for the car's Bluetooth audio, so it never plays from the phone speaker.",
    ) { v -> SettingsStore.update { it.copy(resumeMusicOnCarOn = v) } }
    SwitchRow("Pause on Car Off", s.pauseMediaOnCarOff) { v -> SettingsStore.update { it.copy(pauseMediaOnCarOff = v) } }
}
