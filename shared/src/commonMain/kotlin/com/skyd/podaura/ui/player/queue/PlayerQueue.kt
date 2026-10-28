package com.skyd.podaura.ui.player.queue

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.skyd.podaura.model.bean.playlist.MediaUrlWithArticleIdBean.Companion.toMediaUrlWithArticleIdBean
import com.skyd.podaura.ui.player.component.state.PlayState
import com.skyd.podaura.ui.player.component.state.PlayStateCallback
import com.skyd.podaura.ui.screen.playlist.addto.AddToPlaylistSheet
import com.skyd.podaura.ui.screen.playlist.medialist.PlaylistMediaItem
import com.skyd.podaura.ui.screen.playlist.medialist.list.PlaylistSelectionActions
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.player_current_queue
import podaura.shared.generated.resources.player_queue_empty

/** A queue presentation owns selection and scroll state; playback stays in PlayerCoordinator. */
@Composable
internal fun PlayerQueue(
    playState: PlayState,
    playStateCallback: PlayStateCallback,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
) {
    val playlistId = playState.playlistId
    val playlist = playState.playlist
    var selectedUrls by rememberSaveable(playlistId) { mutableStateOf(emptyList<String>()) }
    var openAddToPlaylist by rememberSaveable(playlistId) { mutableStateOf(false) }
    // Resolve selected identities against live data: metadata changes must not lose selection,
    // and removed items must never be sent back to the coordinator or the add-to sheet.
    val selectedItems = playlist.filter { it.playlistMediaBean.url in selectedUrls }
    val selectMode = selectedItems.isNotEmpty()
    LaunchedEffect(playlist, selectedUrls) {
        selectedUrls = selectedItems.map { it.playlistMediaBean.url }
        if (selectedItems.isEmpty()) openAddToPlaylist = false
    }

    NavigationBackHandler(
        state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
        isBackEnabled = selectMode && !openAddToPlaylist,
        onBackCompleted = { selectedUrls = emptyList() },
    )

    val currentUrl = playState.path
    val currentIndex = playlist.indexOfFirst { it.playlistMediaBean.url == currentUrl }
    // Index also changes on reorder/removal. Progress and metadata updates do not restart this.
    LaunchedEffect(playlistId, currentUrl, currentIndex) {
        // A suspending scrollToItem can be cancelled by an active UserInput mutation, losing
        // this one-shot effect. Request the next measurement directly instead.
        if (currentIndex >= 0) state.requestScrollToItem(currentIndex)
    }

    Column(modifier) {
        Text(
            text = stringResource(Res.string.player_current_queue),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )
        PlaylistSelectionActions(
            visible = selectMode,
            onDelete = {
                playStateCallback.onRemoveFromPlaylist(selectedItems)
                selectedUrls = emptyList()
            },
            onAddToPlaylist = { openAddToPlaylist = true },
        )
        if (playlist.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(Res.string.player_queue_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(20.dp),
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), state = state) {
                items(playlist, key = { it.playlistMediaBean.url }) { item ->
                    val url = item.playlistMediaBean.url
                    val selected = url in selectedUrls
                    PlaylistMediaItem(
                        playing = url == currentUrl,
                        selected = selected,
                        data = item,
                        onClick = {
                            if (selectMode) {
                                selectedUrls =
                                    if (selected) selectedUrls - url else selectedUrls + url
                            } else playStateCallback.onPlayFileInPlaylist(url)
                        },
                        onLongClick = {
                            if (!selected) selectedUrls = selectedUrls + url
                        },
                    )
                }
            }
        }
    }

    if (openAddToPlaylist && selectMode) {
        AddToPlaylistSheet(
            onDismissRequest = { openAddToPlaylist = false },
            currentPlaylistId = playlistId,
            selectedMediaList = selectedItems.map { it.toMediaUrlWithArticleIdBean() },
            // Saved playlist pages use the default key; their in-flight state stays independent.
            viewModel = koinViewModel(key = "player-queue-add"),
        )
    }
}
