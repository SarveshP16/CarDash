package com.cardash.link.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import com.cardash.link.R
import com.cardash.link.proto.OpenSource

/** Version, the Google Maps notice and the open-source licences. */
@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var showFont by remember { mutableStateOf(false) }
    val version = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    if (showFont) {
        val text = remember { context.resources.openRawResource(R.raw.lexend_ofl).bufferedReader().use { it.readText() } }
        AlertDialog(
            onDismissRequest = { showFont = false },
            title = { Text("Lexend font licence") },
            text = {
                LazyColumn(Modifier.heightIn(max = 480.dp)) {
                    item { Text(text, style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = { TextButton(onClick = { showFont = false }) { Text("Close") } },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("CarDash Link $version") },
        text = {
            LazyColumn(Modifier.heightIn(max = 480.dp)) {
                item {
                    Text(
                        "Place search runs on the car phone through Google Maps Platform, used under the " +
                            "Google Maps Platform Terms of Service.",
                        style = MaterialTheme.typography.bodySmall,
                        color = muted,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    Text("Open-source licences", style = MaterialTheme.typography.titleSmall)
                }
                items(OpenSource.link) { n ->
                    Column(Modifier.padding(vertical = 6.dp)) {
                        Text(n.name, style = MaterialTheme.typography.bodyMedium)
                        Text("${n.licence} · ${n.url}", style = MaterialTheme.typography.bodySmall, color = muted)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        dismissButton = { TextButton(onClick = { showFont = true }) { Text("Lexend font licence") } },
    )
}
