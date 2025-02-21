package com.skyd.anivu.ui.mpv.port.controller

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.skyd.anivu.R
import com.skyd.anivu.ext.activity
import com.skyd.anivu.ext.landOrientation
import com.skyd.anivu.ui.component.PodAuraIconButton
import com.skyd.anivu.ui.mpv.component.state.PlayState
import com.skyd.anivu.ui.mpv.component.state.dialog.OnDialogVisibilityChanged
import com.skyd.anivu.ui.mpv.land.controller.bar.BarIconButton
import java.util.Locale


@Composable
internal fun SmallController(
    enabled: Boolean = true,
    playState: PlayState,
    onDialogVisibilityChanged: OnDialogVisibilityChanged,
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Speed button
        Text(
            modifier = Modifier
                .padding(horizontal = 3.dp)
                .clip(CircleShape)
                .height(45.dp)
                .clickable(enabled = enabled, onClick = {
                    onDialogVisibilityChanged.onSpeedDialog(true)
                })
                .padding(horizontal = 6.dp)
                .animateContentSize()
                // For vertical centering
                .wrapContentHeight(),
            text = "${String.format(Locale.getDefault(), "%.2f", playState.speed)}x",
            style = MaterialTheme.typography.labelLarge,
            fontSize = TextUnit(14f, TextUnitType.Sp),
            textAlign = TextAlign.Center,
        )
        // Audio track button
        BarIconButton(
            enabled = enabled,
            onClick = { onDialogVisibilityChanged.onAudioTrackDialog(true) },
            imageVector = Icons.Outlined.MusicNote,
            contentDescription = stringResource(R.string.player_audio_track),
        )
        // Subtitle track button
        BarIconButton(
            enabled = enabled,
            onClick = { onDialogVisibilityChanged.onSubtitleTrackDialog(true) },
            imageVector = Icons.Outlined.ClosedCaption,
            contentDescription = stringResource(R.string.player_subtitle_track),
        )
        Spacer(modifier = Modifier.weight(1f))
        PodAuraIconButton(
            enabled = enabled,
            onClick = { context.activity.landOrientation() },
            imageVector = Icons.Outlined.Fullscreen,
            contentDescription = stringResource(R.string.fullscreen),
        )
    }
}