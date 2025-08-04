package com.skyd.podaura.ui.player.land.controller.button

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skyd.podaura.ext.toSignedString
import com.skyd.podaura.ui.player.land.controller.ControllerLabelGray
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.player_forward_seconds


@Composable
internal fun ForwardOrReplaySeconds(
    modifier: Modifier = Modifier,
    seconds: Int,
    onClick: () -> Unit,
) {
    Text(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color = ControllerLabelGray)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        text = stringResource(
            Res.string.player_forward_seconds,
            seconds.toSignedString()
        ),
        style = MaterialTheme.typography.labelLarge,
        fontSize = 18.sp,
        color = Color.White,
    )
}