package com.skyd.podaura.ui.player.port

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skyd.podaura.ui.player.component.state.PlayState

@Composable
/*internal*/ fun Titles(playState: PlayState, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
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