package com.cardash.app.ui.settings

import android.content.Intent
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.sp
import com.cardash.app.admin.DeviceOwner
import com.cardash.app.system.SystemToggles

/** Shows what's granted, a way into Android settings, and the Device Owner escape hatch. */
@Composable
fun SystemSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var owner by remember { mutableStateOf(DeviceOwner.isOwner(context)) }
    var confirmRelease by remember { mutableStateOf(false) }

    Column(modifier) {
        SectionTitle("System")
        Status("Device Owner", owner)
        Status("Do Not Disturb access", SystemToggles.hasDndAccess(context))
        OutlinedButton(onClick = {
            context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }) { Text("Open Android settings") }
        if (owner) {
            OutlinedButton(onClick = { confirmRelease = true }) { Text("Release Device Owner…") }
        }
    }

    if (confirmRelease) {
        AlertDialog(
            onDismissRequest = { confirmRelease = false },
            title = { Text("Release Device Owner?") },
            text = {
                Text(
                    "The lock screen comes back and the app can be uninstalled. " +
                        "Making it owner again needs setup.sh on a phone with no accounts.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    DeviceOwner.release(context)
                    owner = DeviceOwner.isOwner(context)
                    confirmRelease = false
                }) { Text("Release") }
            },
            dismissButton = { TextButton(onClick = { confirmRelease = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Status(label: String, ok: Boolean) {
    Text(
        "$label: ${if (ok) "yes" else "no"}",
        color = if (ok) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.error,
        fontSize = 16.sp,
    )
}
