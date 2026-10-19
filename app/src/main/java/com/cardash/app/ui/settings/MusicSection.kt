package com.cardash.app.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cardash.app.apps.LaunchableApp
import com.cardash.app.apps.LaunchableApps
import com.cardash.app.media.MusicApp
import com.cardash.app.settings.Settings
import com.cardash.app.settings.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun MusicSection(s: Settings) {
    val context = LocalContext.current
    var picking by remember { mutableStateOf(false) }
    val current = remember(s.musicPackage) {
        MusicApp.resolve(context)?.let { LaunchableApps.label(context, it) ?: it }
    }

    SectionTitle("Music")
    Text(
        when {
            current == null -> "Music app: none found (install YouTube Music)"
            s.musicPackage == null -> "Music app: $current (auto-detected)"
            else -> "Music app: $current"
        },
        color = MaterialTheme.colorScheme.onBackground,
        fontSize = 17.sp,
    )
    OutlinedButton(onClick = { picking = true }) { Text("Change music app") }
    SwitchRow(
        "Resume music on Car On",
        s.resumeMusicOnCarOn,
        "If it was playing when the car turned off",
    ) { v -> SettingsStore.update { it.copy(resumeMusicOnCarOn = v) } }

    if (picking) {
        MusicAppPicker(
            onPick = { pkg ->
                SettingsStore.update { it.copy(musicPackage = pkg) }
                picking = false
            },
            onDismiss = { picking = false },
        )
    }
}

@Composable
private fun MusicAppPicker(onPick: (String?) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val apps by produceState(emptyList<LaunchableApp>()) {
        value = withContext(Dispatchers.Default) { LaunchableApps.load(context) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Music app") },
        text = {
            LazyColumn(Modifier.heightIn(max = 360.dp)) {
                item {
                    Text(
                        "Auto-detect YouTube Music",
                        fontSize = 17.sp,
                        modifier = Modifier.fillMaxWidth().clickable { onPick(null) }.padding(vertical = 12.dp),
                    )
                }
                items(apps, key = { it.packageName }) { app ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onPick(app.packageName) }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(app.icon, null, Modifier.size(36.dp))
                        Text(app.label, fontSize = 17.sp, modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
