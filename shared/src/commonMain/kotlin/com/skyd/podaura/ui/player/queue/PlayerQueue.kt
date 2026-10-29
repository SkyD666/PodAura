package com.skyd.podaura.ui.player.queue

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.skyd.podaura.model.bean.playlist.MediaUrlWithArticleIdBean.Companion.toMediaUrlWithArticleIdBean
import com.skyd.podaura.ui.component.playlist.PlaylistDragState
import com.skyd.podaura.ui.component.playlist.PlaylistMediaItem
import com.skyd.podaura.ui.component.playlist.PlaylistSelectionActions
import com.skyd.podaura.ui.component.playlist.playlistDragHandle
import com.skyd.podaura.ui.player.component.state.PlayState
import com.skyd.podaura.ui.player.component.state.PlayStateCallback
import com.skyd.podaura.ui.screen.playlist.addto.AddToPlaylistSheet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.player_current_queue
import podaura.shared.generated.resources.player_queue_empty
import podaura.shared.generated.resources.player_queue_follow
import podaura.shared.generated.resources.player_queue_unfollow
import podaura.shared.generated.resources.playlist_reorder_failed
import podaura.shared.generated.resources.playlist_reorder_shuffle
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/** A queue presentation owns selection and scroll state; playback stays in PlayerCoordinator. */
@Composable
internal fun PlayerQueue(
    playState: PlayState,
    playStateCallback: PlayStateCallback,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    queueState: PlayerQueueState = LocalPlayerQueueState.current
        ?: remember(playState.queueId, state) { PlayerQueueState(state) },
) {
    val listState = queueState.listState
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val drag = remember(playState.queueId) { PlaylistDragState() }
    val playlistId = playState.playlistId
    val queueId = playState.queueId
    val playlist = playState.playlist
    var selectedUrls by rememberSaveable(queueId) { mutableStateOf(emptyList<String>()) }
    var openAddToPlaylist by rememberSaveable(queueId) { mutableStateOf(false) }
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
    LaunchedEffect(queueId, currentUrl, currentIndex, queueState.following) {
        if (currentIndex >= 0 && (!queueState.positioned || (queueState.following && queueState.lastFollowedUrl != currentUrl))) {
            listState.requestScrollToItem(currentIndex)
            queueState.positioned = true
            queueState.lastFollowedUrl = currentUrl
        }
    }
    val userScroll = remember(queueState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available != Offset.Zero) {
                    queueState.following = false
                    queueState.positioned = true
                }
                return Offset.Zero
            }
        }
    }
    val reorder = rememberReorderableLazyListState(listState) { from, to ->
        drag.move(from.index, to.index)
    }
    val shownPlaylist = drag.preview?.filterNotNull() ?: playlist

    val commitDrag: () -> Unit = {
        scope.launch {
            try {
                drag.finish { from, to, original ->
                    playStateCallback.onReorderPlaylist(
                        playlistId, from, to,
                        original.filterNotNull().map { it.playlistMediaBean.stableUrl },
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                snackbar.showSnackbar(getString(Res.string.playlist_reorder_failed))
            }
        }
    }

    Box(modifier) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.player_current_queue),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                FilledIconToggleButton(
                    checked = queueState.following,
                    enabled = currentIndex >= 0,
                    onCheckedChange = {
                        queueState.following = it
                        if (it) {
                            listState.requestScrollToItem(currentIndex)
                            queueState.positioned = true
                        }
                    },
                ) {
                    Icon(
                        Icons.Outlined.MyLocation, stringResource(
                            if (queueState.following) Res.string.player_queue_unfollow else Res.string.player_queue_follow,
                        )
                    )
                }
            }
            if (playState.shuffle) {
                Text(
                    text = stringResource(Res.string.playlist_reorder_shuffle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }
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
                LazyColumn(
                    modifier = Modifier.weight(1f).nestedScroll(userScroll).onPreviewKeyEvent {
                        if (it.key in listOf(
                                Key.DirectionUp,
                                Key.DirectionDown,
                                Key.PageUp,
                                Key.PageDown,
                                Key.MoveHome,
                                Key.MoveEnd
                            )
                        ) {
                            queueState.following = false
                            queueState.positioned = true
                        }
                        false
                    },
                    state = listState,
                ) {
                    items(shownPlaylist, key = { it.playlistMediaBean.url }) { item ->
                        ReorderableItem(reorder, key = item.playlistMediaBean.url) {
                            val url = item.playlistMediaBean.url
                            val selected = url in selectedUrls
                            PlaylistMediaItem(
                                playing = url == currentUrl,
                                selected = selected,
                                data = item,
                                draggable = !selectMode && !playState.shuffle,
                                dragIconModifier = playlistDragHandle(
                                    enabled = !selectMode && !playState.shuffle && !drag.saving,
                                    onStart = {
                                        queueState.following = false
                                        drag.start(playlist, item.playlistMediaBean.stableUrl)
                                    },
                                    onCancel = drag::cancel,
                                    onDrop = commitDrag,
                                    onMoveUp = {
                                        queueState.following = false
                                        if (drag.step(
                                                playlist,
                                                item.playlistMediaBean.stableUrl,
                                                -1
                                            )
                                        ) commitDrag()
                                    }.takeIf { item != playlist.firstOrNull() },
                                    onMoveDown = {
                                        queueState.following = false
                                        if (drag.step(
                                                playlist,
                                                item.playlistMediaBean.stableUrl,
                                                1
                                            )
                                        ) commitDrag()
                                    }.takeIf { item != playlist.lastOrNull() },
                                ),
                                onClick = {
                                    if (selectMode) {
                                        selectedUrls =
                                            if (selected) selectedUrls - url else selectedUrls + url
                                    } else playStateCallback.onPlayFileInPlaylist(url)
                                },
                                onLongClick = {
                                    queueState.following = false
                                    if (!selected) selectedUrls = selectedUrls + url
                                },
                            )
                        }
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
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
