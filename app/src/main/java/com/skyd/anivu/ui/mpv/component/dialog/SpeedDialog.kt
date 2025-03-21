package com.skyd.anivu.ui.mpv.component.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.skyd.anivu.R
import com.skyd.anivu.ui.component.PodAuraIconButton
import com.skyd.anivu.ui.mpv.component.state.PlayState
import com.skyd.anivu.ui.mpv.component.state.dialog.SpeedDialogCallback
import com.skyd.anivu.ui.mpv.component.state.dialog.SpeedDialogState
import java.util.Locale


@Composable
internal fun SpeedDialog(
    onDismissRequest: () -> Unit,
    playState: () -> PlayState,
    speedDialogState: () -> SpeedDialogState,
    speedDialogCallback: SpeedDialogCallback,
) {
    val state = speedDialogState()
    if (state.show) {
        val currentPlayState = playState()
        var value by rememberSaveable { mutableFloatStateOf(currentPlayState.speed) }
        BasicPlayerDialog(onDismissRequest = onDismissRequest) {
            Column(modifier = Modifier.padding(PaddingValues(16.dp))) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 6.dp),
                        text = stringResource(id = R.string.player_speed),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        modifier = Modifier.weight(1f),
                        text = String.format(Locale.getDefault(), "%.2f", value),
                    )
                    PodAuraIconButton(
                        onClick = {
                            value = 1f
                            speedDialogCallback.onSpeedChanged(value)
                        },
                        imageVector = Icons.Rounded.RestartAlt,
                        contentDescription = stringResource(id = R.string.reset),
                    )
                }
                Slider(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    value = value,
                    onValueChange = { value = it },
                    onValueChangeFinished = { speedDialogCallback.onSpeedChanged(value) },
                    valueRange = 0.25f..3f,
                )
            }
        }
    }
}