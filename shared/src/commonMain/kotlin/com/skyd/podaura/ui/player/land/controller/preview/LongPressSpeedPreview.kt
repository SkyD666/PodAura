package com.skyd.podaura.ui.player.land.controller.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skyd.podaura.ui.player.component.ControllerLabelGray
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.player_long_press_playback_speed


@Composable
internal fun BoxScope.LongPressSpeedPreview(speed: () -> Float) {
    Row(
        modifier = Modifier
            .align(BiasAlignment(0f, -0.6f))
            .clip(RoundedCornerShape(6.dp))
            .background(color = ControllerLabelGray)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(30.dp),
            imageVector = Icons.Rounded.FastForward,
            contentDescription = stringResource(Res.string.player_long_press_playback_speed),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "${speed()}x",
            style = MaterialTheme.typography.labelLarge,
            fontSize = 18.sp,
            color = Color.White,
        )
        Spacer(modifier = Modifier.width(6.dp))
    }
}
