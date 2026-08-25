package com.skyd.podaura.ui.player.land.controller

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.skyd.podaura.ui.player.collectPlayerProgress
import com.skyd.podaura.ui.player.service.PlayerState
import kotlinx.coroutines.flow.StateFlow

@Composable
internal fun ProgressIndicator(
    playerStateFlow: StateFlow<PlayerState>,
    modifier: Modifier = Modifier,
) {
    val progress by collectPlayerProgress(playerStateFlow)
    val animatedProgress by animateFloatAsState(
        targetValue = if (progress.duration == 0L) 0f
        else progress.position.toFloat() / progress.duration,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "playerProgressIndicatorAnimate"
    )
    LinearProgressIndicator(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier),
        progress = { animatedProgress },
    )
}
