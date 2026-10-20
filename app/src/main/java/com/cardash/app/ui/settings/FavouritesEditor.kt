package com.cardash.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cardash.app.nav.Destination
import com.cardash.app.settings.Favourites
import com.cardash.app.settings.Settings

/** Favourites list with remove, plus a name + address form to add one by hand. */
@Composable
fun FavouritesEditor(s: Settings) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    SectionTitle("Favourites")
    if (s.favourites.isEmpty()) {
        Text("None yet. Tap ☆ next to a suggestion in Where to?, or add one below.", color = muted, fontSize = 13.sp)
    }
    s.favourites.forEach { place ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(place.label, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(place.address, fontSize = 13.sp, color = muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = { Favourites.remove(place) }) { Icon(Icons.Filled.Delete, "Remove ${place.label}", tint = muted) }
        }
    }

    var name by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    OutlinedTextField(
        value = name, onValueChange = { name = it }, label = { Text("Name (e.g. Gym)") }, singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next), modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = address, onValueChange = { address = it }, label = { Text("Address or coordinates") }, singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), modifier = Modifier.fillMaxWidth(),
    )
    OutlinedButton(
        onClick = {
            if (Favourites.add(Destination(name.trim().ifBlank { address.trim() }, address.trim()))) {
                name = ""
                address = ""
            }
        },
        enabled = address.isNotBlank(),
        modifier = Modifier.padding(top = 4.dp),
    ) { Text("Add favourite") }
}
