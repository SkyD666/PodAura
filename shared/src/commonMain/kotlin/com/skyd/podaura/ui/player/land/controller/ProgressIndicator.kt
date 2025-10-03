package com.skyd.podaura.ui.player.land.controller

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.skyd.podaura.ui.player.component.state.PlayState

@Composable
internal fun ProgressIndicator(modifier: Modifier = Modifier, playState: () -> PlayState) {
    val animatedProgress by animateFloatAsState(
        targetValue = playState().run {
            if (duration == 0L) 0f else position.toFloat() / duration
        },
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