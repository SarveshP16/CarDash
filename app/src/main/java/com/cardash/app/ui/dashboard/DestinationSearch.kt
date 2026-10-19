package com.cardash.app.ui.dashboard

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cardash.app.location.LocationRepository
import com.cardash.app.nav.Coordinates
import com.cardash.app.nav.PlacesSearch
import com.cardash.app.nav.RouteStarter
import com.cardash.app.ui.components.TileShape
import kotlinx.coroutines.delay

/**
 * "Where to?" pill: tap it to type a destination, or tap the mic to say it. Starts in-app
 * turn-by-turn (or hands off to the Maps app if in-app navigation isn't available).
 */
@Composable
fun DestinationSearch(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var typing by rememberSaveable { mutableStateOf(false) }

    fun go(query: String, picked: PlacesSearch.Suggestion? = null) {
        if (query.isBlank() && picked == null) return
        typing = false
        RouteStarter.go(context, scope, query, picked)
    }

    // Voice goes straight to navigation: fewer taps while driving, and Maps shows where it's going.
    val voice = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (result.resultCode == Activity.RESULT_OK && spoken != null) go(spoken)
    }
    val listen = { startVoice(context) { voice.launch(it) } }

    Surface(
        modifier = modifier,
        shape = TileShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.weight(1f).clickable { typing = true }.padding(start = 18.dp, top = 14.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(10.dp))
                Text("Where to?", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = listen, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Filled.Mic, "Say a destination", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
            }
        }
    }

    if (typing) {
        SearchDialog(onSearch = ::go, onVoice = { typing = false; listen() }, onDismiss = { typing = false })
    }
}

@Composable
private fun SearchDialog(
    onSearch: (String, PlacesSearch.Suggestion?) -> Unit,
    onVoice: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var suggestions by remember { mutableStateOf(emptyList<PlacesSearch.Suggestion>()) }
    // One token per opening: Google groups this search's keystrokes into a single session.
    val session = remember { PlacesSearch.newSession() }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    // Wait for a typing pause before asking Google, so one search costs a handful of lookups.
    LaunchedEffect(query) {
        val q = query.trim()
        if (q.length < 2 || Coordinates.parse(q) != null) {
            suggestions = emptyList()
            return@LaunchedEffect
        }
        delay(SUGGEST_DEBOUNCE_MS)
        PlacesSearch.suggest(context, q, LocationRepository.fix.value, session)?.let { suggestions = it }
    }

    // A full-height panel on the left instead of a centred dialog: a floating keyboard (which apps
    // can't see) sits on the right, and a docked one shrinks the panel via the IME insets.
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            Modifier.fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        ) {
            Column(
                Modifier.fillMaxHeight().fillMaxWidth(0.45f)
                    .background(MaterialTheme.colorScheme.background)
                    // Swallow taps so tapping the panel doesn't close it.
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.statusBars))
                    .padding(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(52.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Close", modifier = Modifier.size(28.dp))
                    }
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Place, address or coordinates") },
                        singleLine = true,
                        shape = TileShape,
                        trailingIcon = {
                            IconButton(onClick = onVoice) { Icon(Icons.Filled.Mic, "Say a destination") }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { onSearch(query, null) }),
                        modifier = Modifier.weight(1f).focusRequester(focus),
                    )
                    IconButton(
                        onClick = { onSearch(query, null) },
                        enabled = query.isNotBlank(),
                        modifier = Modifier.size(52.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward, "Go",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                }
                LazyColumn(Modifier.fillMaxWidth().weight(1f).padding(top = 8.dp)) {
                    items(suggestions, key = { it.placeId }) { s ->
                        SuggestionRow(s) { onSearch(s.name, s) }
                    }
                }
            }
        }
    }
}

private const val SUGGEST_DEBOUNCE_MS = 300L

@Composable
private fun SuggestionRow(s: PlacesSearch.Suggestion, onClick: () -> Unit) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Place, null, tint = muted, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(s.name, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (s.detail.isNotBlank()) {
                Text(s.detail, fontSize = 13.sp, color = muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        s.distanceMeters?.let { Text(formatDistance(it), fontSize = 13.sp, color = muted) }
    }
}

private fun formatDistance(meters: Int): String =
    if (meters < 1_000) "$meters m" else "%.1f km".format(meters / 1_000.0)

private fun startVoice(context: Context, launch: (Intent) -> Unit) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        .putExtra(RecognizerIntent.EXTRA_PROMPT, "Where to?")
    try {
        launch(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "Voice input isn't available (needs the Google app)", Toast.LENGTH_LONG).show()
    }
}
