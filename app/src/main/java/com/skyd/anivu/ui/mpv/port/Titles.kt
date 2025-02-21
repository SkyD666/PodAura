package com.skyd.anivu.ui.mpv.port

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skyd.anivu.ui.mpv.component.state.PlayState

@Composable
internal fun Titles(playState: PlayState) {
    val title = playState.run { title.orEmpty().ifBlank { mediaTitle } }
    if (!title.isNullOrBlank()) {
        Text(
            text = title,
            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
            style = MaterialTheme.typography.titleLarge,
            overflow = TextOverflow.Ellipsis,
        )
    }

    val artist = playState.artist
    if (!artist.isNullOrBlank()) {
        Text(
            text = artist,
            modifier = Modifier
                .padding(top = 3.dp)
                .basicMarquee(iterations = Int.MAX_VALUE),
            style = MaterialTheme.typography.titleMedium,
            overflow = TextOverflow.Ellipsis,
        )
    }
}