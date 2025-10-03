package com.skyd.podaura.ui.player.land.controller.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skyd.podaura.ui.player.component.ControllerLabelGray
import com.skyd.podaura.ui.player.land.controller.bar.toDurationString

@Composable
internal fun BoxScope.SeekTimePreview(
    value: () -> Long,
    duration: () -> Long,
) {
    Row(
        modifier = Modifier
            .align(Alignment.Center)
            .clip(RoundedCornerShape(6.dp))
            .background(color = ControllerLabelGray)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            text = value()
                .coerceIn(0..duration())
                .toDurationString(),
            style = MaterialTheme.typography.labelLarge,
            fontSize = 18.sp,
            color = Color.White,
        )
        Text(
            modifier = Modifier.padding(horizontal = 6.dp),
            text = "/",
            style = MaterialTheme.typography.labelLarge,
            fontSize = 18.sp,
            color = Color.White,
        )
        Text(
            text = duration().toDurationString(),
            style = MaterialTheme.typography.labelLarge,
            fontSize = 18.sp,
            color = Color.White,
        )
    }
}