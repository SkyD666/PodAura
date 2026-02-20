package com.skyd.podaura.ui.screen.playlist.medialist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.paging.compose.collectAsLazyPagingItems
import com.skyd.compone.component.ComponeIconButton
import com.skyd.compone.component.ComponeTopBar
import com.skyd.compone.component.dialog.WaitingDialog
import com.skyd.fundation.ext.toRelativeDateTimeString
import com.skyd.mvi.MviEventListener
import com.skyd.mvi.getDispatcher
import com.skyd.podaura.ext.rememberUpdateSemaphore
import com.skyd.podaura.ext.vThenP
import com.skyd.podaura.model.bean.playlist.MediaUrlWithArticleIdBean.Companion.toMediaUrlWithArticleIdBean
import com.skyd.podaura.model.bean.playlist.PlaylistViewBean
import com.skyd.podaura.model.preference.behavior.playlist.BasePlaylistSortByPreference
import com.skyd.podaura.model.preference.behavior.playlist.PlaylistMediaSortAscPreference
import com.skyd.podaura.model.preference.behavior.playlist.PlaylistMediaSortByPreference
import com.skyd.podaura.ui.component.CircularProgressPlaceholder
import com.skyd.podaura.ui.component.ErrorPlaceholder
import com.skyd.podaura.ui.component.dialog.SortDialog
import com.skyd.podaura.ui.player.jumper.PlayDataMode
import com.skyd.podaura.ui.player.jumper.rememberPlayerJumper
import com.skyd.podaura.ui.screen.playlist.PlaylistThumbnail
import com.skyd.podaura.ui.screen.playlist.medialist.list.PlaylistMediaList
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.created_on
import podaura.shared.generated.resources.play
import podaura.shared.generated.resources.playlist_screen_item_count
import podaura.shared.generated.resources.sort


@Serializable
data class PlaylistMediaListRoute(val playlistId: String) : NavKey {
    companion object {
        @Composable
        fun PlaylistMediaListLauncher(route: PlaylistMediaListRoute) {
            PlaylistMediaListScreen(playlistId = route.playlistId)
        }
    }
}

@Composable
fun PlaylistMediaListScreen(
    playlistId: String,
    viewModel: PlaylistMediaListViewModel = koinViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val lazyGridState = rememberLazyStaggeredGridState()
    val headerVisible by remember { derivedStateOf { lazyGridState.firstVisibleItemIndex == 0 } }

    val dispatch = viewModel.getDispatcher(startWith = PlaylistMediaListIntent.Init(playlistId))
    val uiState by viewModel.viewState.collectAsStateWithLifecycle()

    var showSortDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            ComponeTopBar(
                title = {
                    AnimatedVisibility(
                        visible = !headerVisible,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        val title = (uiState.listState as? ListState.Success)
                            ?.playlistViewBean?.playlist?.name
                        title?.let { Text(title) }
                    }
                },
                scrollBehavior = scrollBehavior,
                actions = {
                    ComponeIconButton(
                        onClick = { showSortDialog = true },
                        imageVector = Icons.AutoMirrored.Outlined.Sort,
                        contentDescription = stringResource(Res.string.sort),
                    )
                }
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        when (val listState = uiState.listState) {
            is ListState.Failed -> ErrorPlaceholder(listState.msg, innerPadding)
            ListState.Init -> CircularProgressPlaceholder(innerPadding)
            is ListState.Success -> Column(modifier = Modifier.padding(innerPadding)) {
                val lazyPagingItems =
                    listState.playlistMediaPagingDataFlow.collectAsLazyPagingItems()
                val reorderSemaphore = remember { Channel<Unit>(Channel.UNLIMITED) }
                val reorderPagingItemsSemaphore = lazyPagingItems.rememberUpdateSemaphore(
                    default = null,
                    sendData = { reorderSemaphore.tryReceive().getOrNull() }
                )
                val playerJumper = rememberPlayerJumper()
                PlaylistMediaList(
                    currentPlaylistId = playlistId,
                    playlist = lazyPagingItems,
                    draggable = PlaylistMediaSortByPreference.current == BasePlaylistSortByPreference.MANUAL,
                    state = lazyGridState,
                    header = {
                        PlaylistInfo(
                            playlistViewBean = listState.playlistViewBean,
                            onPlay = {
                                playerJumper.jump(
                                    PlayDataMode.Playlist(
                                        playlistId = listState.playlistViewBean.playlist.playlistId,
                                        mediaUrl = null,
                                    )
                                )
                            },
                        )
                    },
                    onPlay = {
                        playerJumper.jump(
                            PlayDataMode.Playlist(
                                playlistId = playlistId,
                                mediaUrl = it.playlistMediaBean.url,
                            )
                        )
                    },
                    onDelete = { beans ->
                        dispatch(
                            PlaylistMediaListIntent.Delete(
                                playlistId = playlistId,
                                deletes = beans.map { it.toMediaUrlWithArticleIdBean() },
                            )
                        )
                    },
                    onMoved = onMoved@{ fromIndex, toIndex ->
                        if (fromIndex == toIndex) return@onMoved
                        reorderSemaphore.vThenP(reorderPagingItemsSemaphore) {
                            dispatch(
                                PlaylistMediaListIntent.Reorder(
                                    playlistId = playlistId,
                                    fromIndex = fromIndex,
                                    toIndex = toIndex,
                                )
                            )
                        }
                    },
                )
            }
        }

        SortDialog(
            visible = showSortDialog,
            onDismissRequest = { showSortDialog = false },
            sortByValues = PlaylistMediaSortByPreference.values,
            sortBy = PlaylistMediaSortByPreference.current,
            sortAsc = PlaylistMediaSortAscPreference.current,
            enableSortAsc = PlaylistMediaSortByPreference.current != BasePlaylistSortByPreference.MANUAL,
            onSortBy = { PlaylistMediaSortByPreference.put(scope, it) },
            onSortAsc = { PlaylistMediaSortAscPreference.put(scope, it) },
            onSortByDisplayName = { BasePlaylistSortByPreference.toDisplayName(it) },
            onSortByIcon = { BasePlaylistSortByPreference.toIcon(it) },
        )

        WaitingDialog(visible = uiState.loadingDialog)

        MviEventListener(viewModel.singleEvent) { event ->
            when (event) {
                is PlaylistMediaListEvent.DeleteResultEvent.Failed ->
                    snackbarHostState.showSnackbar(event.msg)

                is PlaylistMediaListEvent.ReorderResultEvent.Failed ->
                    snackbarHostState.showSnackbar(event.msg)
            }
        }
    }
}

@Composable
private fun PlaylistInfo(
    playlistViewBean: PlaylistViewBean,
    onPlay: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .padding(top = 6.dp, bottom = 14.dp)
            .padding(contentPadding)
            .height(IntrinsicSize.Max),
    ) {
        PlaylistThumbnail(
            modifier = Modifier.size(100.dp),
            thumbnails = playlistViewBean.thumbnails,
            roundedCorner = 6.dp,
            spaceSize = 1.5.dp,
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlistViewBean.playlist.name,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            Res.string.created_on,
                            playlistViewBean.playlist.createTime.toRelativeDateTimeString(),
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = LocalContentColor.current.copy(alpha = 0.7f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = pluralStringResource(
                            Res.plurals.playlist_screen_item_count,
                            playlistViewBean.itemCount,
                            playlistViewBean.itemCount
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = LocalContentColor.current.copy(alpha = 0.7f),
                    )
                }
                FilledIconButton(
                    onClick = onPlay,
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .size(55.dp)
                        .align(Alignment.Bottom),
                    enabled = playlistViewBean.itemCount > 0,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = stringResource(Res.string.play),
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
        }
    }
}
