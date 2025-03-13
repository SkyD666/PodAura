package com.skyd.anivu.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp


@Composable
fun TagText(
    modifier: Modifier = Modifier,
    text: String,
    fontSize: TextUnit = TextUnit.Unspecified,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    contentColor: Color = Color.Unspecified,
) {
    Text(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(containerColor)
            .padding(horizontal = 4.dp, vertical = 0.4.dp),
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = contentColor,
        fontSize = fontSize,
    )
}