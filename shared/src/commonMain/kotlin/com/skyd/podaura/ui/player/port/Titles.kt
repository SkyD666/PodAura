package com.skyd.podaura.ui.player.port

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skyd.podaura.ui.player.component.state.PlayState
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.playback_failed

@Composable
internal fun Titles(
    playState: PlayState,
    modifier: Modifier = Modifier,
    presentationState: PlayerPresentationState = PlayerPresentationState.Ready,
) {
    Crossfade(
        targetState = presentationState is PlayerPresentationState.Ready,
        modifier = modifier,
        animationSpec = tween(PLAYER_PRESENTATION_CROSSFADE_MILLIS),
        label = "playerTitlesContent",
    ) { ready ->
        if (ready) PlayerTitles(playState) else PlaceholderTitles(presentationState)
    }
}

@Composable
private fun PlayerTitles(playState: PlayState) {
    Column {
        val title = playState.run { title.orEmpty().ifBlank { mediaTitle } }
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                style = MaterialTheme.typography.titleLarge,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }

        val artist = playState.run { artist.orEmpty().ifBlank { mediaArtist } }
        AnimatedVisibility(visible = !artist.isNullOrBlank()) {
            Text(
                text = artist.orEmpty(),
                modifier = Modifier
                    .padding(top = 3.dp)
                    .fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium,
                overflow = TextOverflow.Ellipsis,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun PlaceholderTitles(presentationState: PlayerPresentationState) {
    Column {
        when (presentationState) {
            PlayerPresentationState.Loading -> {
                PlayerSkeleton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp),
                    shape = RoundedCornerShape(4.dp),
                )
                PlayerSkeleton(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .fillMaxWidth(0.62f)
                        .height(22.dp),
                    shape = RoundedCornerShape(4.dp),
                )
            }

            is PlayerPresentationState.Failed -> {
                Text(
                    text = stringResource(Res.string.playback_failed),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                )
                Text(
                    text = presentationState.message,
                    modifier = Modifier
                        .padding(top = 3.dp)
                        .fillMaxWidth(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleMedium,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 2,
                )
            }

            PlayerPresentationState.Ready -> Unit
        }
    }
}
