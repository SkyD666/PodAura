package com.skyd.podaura.ui.component.playlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.skyd.compone.component.ComponeIconButton
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.add_to_playlist
import podaura.shared.generated.resources.delete

/** Selection actions shared by saved playlists and the current playback queue. */
@Composable
internal fun PlaylistSelectionActions(
    visible: Boolean,
    onDelete: () -> Unit,
    onAddToPlaylist: () -> Unit,
) {
    AnimatedVisibility(visible) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
        ) {
            ComponeIconButton(
                onClick = onDelete,
                imageVector = Icons.Outlined.Delete,
                contentDescription = stringResource(Res.string.delete),
            )
            ComponeIconButton(
                onClick = onAddToPlaylist,
                imageVector = Icons.AutoMirrored.Outlined.PlaylistAdd,
                contentDescription = stringResource(Res.string.add_to_playlist),
            )
        }
    }
}
