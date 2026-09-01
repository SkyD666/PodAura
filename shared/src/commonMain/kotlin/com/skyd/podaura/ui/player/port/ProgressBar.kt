package com.skyd.podaura.ui.player.port

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skyd.podaura.ui.player.PlayerProgress
import com.skyd.podaura.ui.player.land.controller.bar.toDurationString
import kotlinx.coroutines.flow.Flow


@Composable
internal fun PlayerProgressBar(
    progress: Flow<PlayerProgress>,
    initialProgress: PlayerProgress,
    isSeeking: Boolean,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val currentProgress by progress.collectAsStateWithLifecycle(initialProgress)
    ProgressBar(
        progress = currentProgress,
        isSeeking = isSeeking,
        onSeekTo = onSeekTo,
        modifier = modifier,
        enabled = enabled,
    )
}

@Composable
private fun ProgressBar(
    progress: PlayerProgress,
    isSeeking: Boolean,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var sliderValue by rememberSaveable {
        mutableFloatStateOf(progress.position.toFloat())
    }
    var valueIsChanging by rememberSaveable { mutableStateOf(false) }
    if (!valueIsChanging && !isSeeking && sliderValue != progress.position.toFloat()) {
        sliderValue = progress.position.toFloat()
    }
    Column(modifier = modifier) {
        Slider(
            modifier = Modifier.fillMaxWidth(1f),
            value = sliderValue,
            onValueChange = {
                valueIsChanging = true
                sliderValue = it
            },
            onValueChangeFinished = {
                onSeekTo(sliderValue.toLong())
                valueIsChanging = false
            },
            colors = SliderDefaults.colors(),
            enabled = enabled,
            valueRange = 0f..progress.duration.toFloat().coerceAtLeast(1f),
        )
        Spacer(modifier = Modifier.height(3.dp))
        Row(modifier = Modifier.padding(horizontal = 3.dp)) {
            Text(
                text = progress.position.toDurationString(),
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = progress.duration.toDurationString(),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
