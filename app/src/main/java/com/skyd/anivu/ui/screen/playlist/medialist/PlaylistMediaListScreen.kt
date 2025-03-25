package com.skyd.anivu.ui.screen.playlist.medialist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.skyd.anivu.R
import com.skyd.anivu.base.mvi.MviEventListener
import com.skyd.anivu.base.mvi.getDispatcher
import com.skyd.anivu.ext.activity
import com.skyd.anivu.ext.rememberUpdateSemaphore
import com.skyd.anivu.ext.toRelativeDateTimeString
import com.skyd.anivu.ext.vThenP
import com.skyd.anivu.model.bean.playlist.PlaylistViewBean
import com.skyd.anivu.model.preference.behavior.playlist.BasePlaylistSortByPreference
import com.skyd.anivu.model.preference.behavior.playlist.PlaylistMediaSortAscPreference
import com.skyd.anivu.model.preference.behavior.playlist.PlaylistMediaSortByPreference
import com.skyd.anivu.ui.activity.player.PlayActivity
import com.skyd.anivu.ui.component.CircularProgressPlaceholder
import com.skyd.anivu.ui.component.ErrorPlaceholder
import com.skyd.anivu.ui.component.PodAuraIconButton
import com.skyd.anivu.ui.component.PodAuraTopBar
import com.skyd.anivu.ui.component.dialog.SortDialog
import com.skyd.anivu.ui.component.dialog.WaitingDialog
import com.skyd.anivu.ui.local.LocalPlaylistMediaSortAsc
import com.skyd.anivu.ui.local.LocalPlaylistMediaSortBy
import com.skyd.anivu.ui.screen.playlist.PlaylistThumbnail
import com.skyd.anivu.ui.screen.playlist.medialist.list.PlaylistMediaList
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.Serializable


@Serializable
data class PlaylistMediaListRoute(val playlistId: String)

@Composable
fun PlaylistMediaListScreen(
    playlistId: String,
    viewModel: PlaylistMediaListViewModel = hiltViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lazyGridState = rememberLazyStaggeredGridState()
    val headerVisible by remember { derivedStateOf { lazyGridState.firstVisibleItemIndex == 0 } }

    val dispatch = viewModel.getDispatcher(startWith = PlaylistMediaListIntent.Init(playlistId))
    val uiState by viewModel.viewState.collectAsStateWithLifecycle()

    var showSortDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            PodAuraTopBar(
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
                    PodAuraIconButton(
                        onClick = { showSortDialog = true },
                        imageVector = Icons.AutoMirrored.Outlined.Sort,
                        contentDescription = stringResource(id = R.string.sort),
                    )
                }
            )
        },
    ) { paddingValues ->
        when (val listState = uiState.listState) {
            is ListState.Failed -> ErrorPlaceholder(listState.msg, paddingValues)
            ListState.Init -> CircularProgressPlaceholder(paddingValues)
            is ListState.Success -> Column(modifier = Modifier.padding(paddingValues)) {
                val lazyPagingItems =
                    listState.playlistMediaPagingDataFlow.collectAsLazyPagingItems()
                val reorderSemaphore = remember { Channel<Unit>(Channel.UNLIMITED) }
                val reorderPagingItemsSemaphore = lazyPagingItems.rememberUpdateSemaphore(
                    default = null,
                    sendData = { reorderSemaphore.tryReceive().getOrNull() }
                )
                PlaylistMediaList(
                    currentPlaylistId = playlistId,
                    playlist = lazyPagingItems,
                    draggable = LocalPlaylistMediaSortBy.current == BasePlaylistSortByPreference.MANUAL,
                    state = lazyGridState,
                    header = {
                        PlaylistInfo(
                            playlistViewBean = listState.playlistViewBean,
                            onPlay = {
                                PlayActivity.playPlaylist(
                                    activity = context.activity,
                                    playlistId = listState.playlistViewBean.playlist.playlistId,
                                    mediaUrl = null,
                                )
                            },
                        )
                    },
                    onPlay = {
                        PlayActivity.playPlaylist(
                            activity = context.activity,
                            playlistId = playlistId,
                            mediaUrl = it.playlistMediaBean.url,
                        )
                    },
                    onDelete = { dispatch(PlaylistMediaListIntent.Delete(it)) },
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
            sortBy = LocalPlaylistMediaSortBy.current,
            sortAsc = LocalPlaylistMediaSortAsc.current,
            enableSortAsc = LocalPlaylistMediaSortBy.current != BasePlaylistSortByPreference.MANUAL,
            onSortBy = { PlaylistMediaSortByPreference.put(context, scope, it) },
            onSortAsc = { PlaylistMediaSortAscPreference.put(context, scope, it) },
            onSortByDisplayName = { BasePlaylistSortByPreference.toDisplayName(context, it) },
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
                            R.string.created_on,
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
                            R.plurals.playlist_screen_item_count,
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
                        contentDescription = stringResource(R.string.play),
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
        }
    }
}
