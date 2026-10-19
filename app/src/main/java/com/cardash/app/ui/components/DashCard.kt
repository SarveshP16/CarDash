package com.cardash.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Corner radius shared by every tile, so the dashboard reads as one system. */
val TileShape = RoundedCornerShape(14.dp)

/** A borderless tile one step off the background. */
@Composable
fun DashCard(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Surface(
        modifier = modifier,
        shape = TileShape,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), content = content)
    }
}
