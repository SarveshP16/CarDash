package com.cardash.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A tile button. [highlighted] fills it with the accent (Maps); [active] just brightens the icon
 * (a toggle that's on), so the dashboard isn't a wall of coloured blocks.
 */
@Composable
fun BigIconButton(
    icon: ImageVector,
    label: String?,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    highlighted: Boolean = false,
    container: Color? = null,
    /** Icon beside the label instead of above it, for short tiles. */
    horizontal: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val background = container ?: if (highlighted) colors.primary else colors.surface
    val content = when {
        container != null || highlighted -> if (container == colors.error) colors.onError else colors.onPrimary
        active -> colors.onSurface
        else -> colors.onSurfaceVariant
    }
    val tile = modifier
        .clip(TileShape)
        .background(background)
        .clickable(onClick = onClick)
        .padding(horizontal = 6.dp, vertical = 6.dp)
    if (horizontal && label != null) {
        Row(tile, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, color = content, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        return
    }
    Column(tile, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(icon, contentDescription = label, tint = content, modifier = Modifier.size(28.dp))
        if (label != null) {
            Spacer(Modifier.height(6.dp))
            Text(label, color = content, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
