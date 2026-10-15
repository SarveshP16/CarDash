package com.cardash.app.ui.apps

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.cardash.app.ui.components.ScreenHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class LaunchableApp(val label: String, val packageName: String, val icon: ImageBitmap)

/** The launcher's app drawer: every launchable app except this one. */
@Composable
fun AppsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val apps by produceState(emptyList<LaunchableApp>()) {
        value = withContext(Dispatchers.Default) { loadApps(context) }
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        ScreenHeader("Apps", onBack)
        LazyVerticalGrid(
            columns = GridCells.Adaptive(110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(apps, key = { it.packageName }) { app ->
                Column(
                    Modifier.clip(RoundedCornerShape(16.dp))
                        .clickable { launch(context, app.packageName) }
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(app.icon, app.label, Modifier.size(64.dp))
                    Text(
                        app.label,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

private fun loadApps(context: Context): List<LaunchableApp> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(intent, 0)
        .filter { it.activityInfo.packageName != context.packageName }
        .map {
            LaunchableApp(
                label = it.loadLabel(pm).toString(),
                packageName = it.activityInfo.packageName,
                icon = it.loadIcon(pm).toImage(),
            )
        }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
}

private fun Drawable.toImage(): ImageBitmap = toBitmap(144, 144).asImageBitmap()

private fun launch(context: Context, packageName: String) {
    context.packageManager.getLaunchIntentForPackage(packageName)?.let(context::startActivity)
}
