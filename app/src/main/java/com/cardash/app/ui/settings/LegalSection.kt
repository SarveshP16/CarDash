package com.cardash.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cardash.app.BuildConfig
import com.cardash.app.R
import com.cardash.link.proto.OpenSource

/** Version, Google Maps notice and the open-source licences. */
@Composable
fun LegalSection() {
    var showLicences by remember { mutableStateOf(false) }
    var showFont by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme

    SectionTitle("About")
    Text("CarDash ${BuildConfig.VERSION_NAME}", color = colors.onBackground, fontSize = 17.sp)
    Text(
        "Maps, navigation and place search by Google Maps Platform, used under the Google Maps Platform Terms of Service.",
        color = colors.onSurfaceVariant,
        fontSize = 13.sp,
    )
    OutlinedButton(onClick = { showLicences = true }) { Text("Open-source licences") }

    if (showLicences) {
        AlertDialog(
            onDismissRequest = { showLicences = false },
            title = { Text("Open-source licences") },
            text = {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(OpenSource.carDash) { n ->
                        Column(Modifier.padding(vertical = 6.dp)) {
                            Text(n.name, fontSize = 16.sp)
                            Text("${n.licence} · ${n.url}", fontSize = 12.sp, color = colors.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showLicences = false }) { Text("Close") } },
            dismissButton = { TextButton(onClick = { showFont = true }) { Text("Lexend font licence") } },
        )
    }
    if (showFont) {
        val context = LocalContext.current
        val text = remember { context.resources.openRawResource(R.raw.lexend_ofl).bufferedReader().use { it.readText() } }
        AlertDialog(
            onDismissRequest = { showFont = false },
            title = { Text("Lexend font licence") },
            text = {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    item { Text(text, fontSize = 12.sp) }
                }
            },
            confirmButton = { TextButton(onClick = { showFont = false }) { Text("Close") } },
        )
    }
}
