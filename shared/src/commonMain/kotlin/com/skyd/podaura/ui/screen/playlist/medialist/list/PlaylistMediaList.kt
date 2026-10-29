package com.skyd.podaura.ui.screen.playlist.medialist.list

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.paging.compose.LazyPagingItems
import com.skyd.compone.ext.thenIfNotNull
import com.skyd.compone.ext.withoutTop
import com.skyd.mvi.getDispatcher
import com.skyd.podaura.ext.getOrNull
import com.skyd.podaura.ext.safeItemKey
import com.skyd.podaura.model.bean.playlist.MediaUrlWithArticleIdBean.Companion.toMediaUrlWithArticleIdBean
import com.skyd.podaura.model.bean.playlist.PlaylistMediaWithArticleBean
import com.skyd.podaura.ui.component.PagingRefreshStateIndicator
import com.skyd.podaura.ui.component.playlist.PlaylistDragState
import com.skyd.podaura.ui.component.playlist.PlaylistMediaItem
import com.skyd.podaura.ui.component.playlist.PlaylistMediaItemPlaceholder
import com.skyd.podaura.ui.component.playlist.PlaylistSelectionActions
import com.skyd.podaura.ui.component.playlist.playlistDragHandle
import com.skyd.podaura.ui.screen.playlist.addto.AddToPlaylistSheet
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyStaggeredGridState


@Composable
fun PlaylistMediaList(
    currentPlaylistId: String,
    currentPlay: PlaylistMediaWithArticleBean? = null,
    playlist: List<PlaylistMediaWithArticleBean>,
    state: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    onPlay: (PlaylistMediaWithArticleBean) -> Unit,
    onDelete: (List<PlaylistMediaWithArticleBean>) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: ListViewModel = koinViewModel(),
) {
    var openAddToPlaylistSheet by rememberSaveable { mutableStateOf(false) }
    val dispatch = viewModel.getDispatcher(startWith = ListIntent.Init)
    val uiState by viewModel.viewState.collectAsStateWithLifecycle()

    val navigationEventState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(
        state = navigationEventState,
        isBackEnabled = uiState.selectedItems.isNotEmpty(),
        onBackCompleted = {
            dispatch(ListIntent.ClearSelected)
        }
    )

    PlaylistMediaList(
        uiState = uiState,
        dispatch = dispatch,
        currentPlay = currentPlay,
        count = playlist.size,
        onData = { playlist[it] },
        snapshot = { playlist },
        key = { playlist[it].playlistMediaBean.playlistId to playlist[it].playlistMediaBean.url },
        state = state,
        onMoved = { _, _, _ -> },
        onOpenAddToPlaylistSheet = { openAddToPlaylistSheet = true },
        onPlay = onPlay,
        onDelete = onDelete,
        contentPadding = contentPadding,
    )

    LaunchedEffect(Unit) {
        val index = currentPlay?.let { currentPlay ->
            playlist.indexOfFirst { it.playlistMediaBean.url == currentPlay.playlistMediaBean.url }
        } ?: -1
        if (index in playlist.indices) {
            state.animateScrollToItem(index)
        }
    }

    if (openAddToPlaylistSheet) {
        AddToPlaylistSheet(
            onDismissRequest = { openAddToPlaylistSheet = false },
            currentPlaylistId = currentPlaylistId,
            selectedMediaList = remember(uiState.selectedItems) {
                uiState.selectedItems.map { it.toMediaUrlWithArticleIdBean() }
            },
        )
    }
}


@Composable
fun PlaylistMediaList(
    currentPlaylistId: String,
    currentPlay: PlaylistMediaWithArticleBean? = null,
    playlist: LazyPagingItems<PlaylistMediaWithArticleBean>,
    draggable: Boolean = false,
    onMoved: suspend (String, String, List<String>) -> Unit,
    state: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    header: (@Composable LazyStaggeredGridItemScope.() -> Unit)? = null,
    onPlay: (PlaylistMediaWithArticleBean) -> Unit,
    onDelete: (List<PlaylistMediaWithArticleBean>) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    nestedScrollConnection: NestedScrollConnection? = null,
    viewModel: ListViewModel = koinViewModel(),
) {
    var openAddToPlaylistSheet by rememberSaveable { mutableStateOf(false) }
    val dispatch = viewModel.getDispatcher(startWith = ListIntent.Init)
    val uiState by viewModel.viewState.collectAsStateWithLifecycle()

    val navigationEventState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(
        state = navigationEventState,
        isBackEnabled = uiState.selectedItems.isNotEmpty(),
        onBackCompleted = {
            dispatch(ListIntent.ClearSelected)
        }
    )

    PagingRefreshStateIndicator(
        lazyPagingItems = playlist,
        errorContent = { playlist.loadState.refresh },
        placeholderPadding = contentPadding,
    ) {
        PlaylistMediaList(
            uiState = uiState,
            dispatch = dispatch,
            currentPlay = currentPlay,
            count = playlist.itemCount,
            onData = { playlist.getOrNull(it) },
            snapshot = { playlist.itemSnapshotList },
            key = playlist.safeItemKey { it.playlistMediaBean.playlistId to it.playlistMediaBean.url },
            draggable = draggable,
            onMoved = onMoved,
            state = state,
            header = header,
            onOpenAddToPlaylistSheet = { openAddToPlaylistSheet = true },
            onPlay = onPlay,
            onDelete = onDelete,
            contentPadding = contentPadding,
            nestedScrollConnection = nestedScrollConnection,
        )
    }

    if (openAddToPlaylistSheet) {
        AddToPlaylistSheet(
            onDismissRequest = { openAddToPlaylistSheet = false },
            currentPlaylistId = currentPlaylistId,
            selectedMediaList = remember(uiState.selectedItems) {
                uiState.selectedItems.map { it.toMediaUrlWithArticleIdBean() }
            },
        )
    }
}

@Composable
private fun PlaylistMediaList(
    uiState: ListState,
    dispatch: (ListIntent) -> Unit,
    currentPlay: PlaylistMediaWithArticleBean? = null,
    count: Int,
    onData: (Int) -> PlaylistMediaWithArticleBean?,
    snapshot: () -> List<PlaylistMediaWithArticleBean?>,
    key: (index: Int) -> Any,
    draggable: Boolean = false,
    onMoved: suspend (String, String, List<String>) -> Unit,
    state: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    header: (@Composable LazyStaggeredGridItemScope.() -> Unit)? = null,
    onOpenAddToPlaylistSheet: () -> Unit,
    onPlay: (PlaylistMediaWithArticleBean) -> Unit,
    onDelete: (List<PlaylistMediaWithArticleBean>) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    nestedScrollConnection: NestedScrollConnection? = null,
) {
    val selectMode = uiState.selectedItems.isNotEmpty()

    val scope = rememberCoroutineScope()
    val drag = remember { PlaylistDragState() }
    val commitDrag: () -> Unit = {
        scope.launch {
            drag.finish { from, to, original ->
                onMoved(from, to, original.filterNotNull().map { it.playlistMediaBean.stableUrl })
            }
        }
    }
    // Keep count, keys and rows on the same snapshot until the drag finishes. A refresh may
    // insert/remove rows or fill placeholders, but must not mix its indices with the preview.
    val preview = drag.preview
    val shownCount = preview?.size ?: count
    val shownKey: (Int) -> Any = { index ->
        if (preview == null) key(index)
        else preview[index]?.playlistMediaBean?.let { it.playlistId to it.url } ?: index
    }
    val reorderableState = rememberReorderableLazyStaggeredGridState(state) { from, to ->
        val offset = if (header == null) 0 else 1
        drag.move(from.index - offset, to.index - offset, snapshot())
    }

    Column(modifier = Modifier.padding(top = contentPadding.calculateTopPadding())) {
        PlaylistSelectionActions(
            visible = selectMode,
            onDelete = {
                onDelete(uiState.selectedItems)
                dispatch(ListIntent.ClearSelected)
            },
            onAddToPlaylist = onOpenAddToPlaylistSheet,
        )
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(300.dp),
            modifier = Modifier
                .fillMaxSize()
                .thenIfNotNull(nestedScrollConnection) { nestedScroll(it) },
            state = state,
            contentPadding = contentPadding.withoutTop(),
        ) {
            if (header != null) {
                item(span = StaggeredGridItemSpan.FullLine) { header() }
            }
            items(
                count = shownCount,
                key = shownKey,
            ) { index ->
                ReorderableItem(
                    state = reorderableState,
                    key = shownKey(index),
                ) {
                    // Keep sending access hints to Paging while showing the frozen preview.
                    val liveItem = onData(index)
                    when (val item = if (preview == null) liveItem else preview[index]) {
                        is PlaylistMediaWithArticleBean -> {
                            val selected = item in uiState.selectedItems
                            PlaylistMediaItem(
                                playing = item.playlistMediaBean.isSamePlaylistMedia(currentPlay?.playlistMediaBean),
                                selected = selected,
                                data = item,
                                dragIconModifier = playlistDragHandle(
                                    enabled = draggable && !selectMode && !drag.saving,
                                    onStart = {
                                        drag.start(
                                            snapshot(),
                                            item.playlistMediaBean.stableUrl
                                        )
                                    },
                                    onCancel = drag::cancel,
                                    onDrop = commitDrag,
                                    onMoveUp = {
                                        if (drag.step(
                                                snapshot(),
                                                item.playlistMediaBean.stableUrl,
                                                -1
                                            )
                                        ) commitDrag()
                                    }.takeIf { index > 0 },
                                    onMoveDown = {
                                        if (drag.step(
                                                snapshot(),
                                                item.playlistMediaBean.stableUrl,
                                                1
                                            )
                                        ) commitDrag()
                                    }.takeIf { index < shownCount - 1 },
                                ),
                                draggable = draggable && !selectMode,
                                onClick = {
                                    if (selectMode) {
                                        if (selected) {
                                            dispatch(ListIntent.RemoveSelected(it))
                                        } else {
                                            dispatch(ListIntent.AddSelected(it))
                                        }
                                    } else onPlay(it)
                                },
                                onLongClick = { dispatch(ListIntent.AddSelected(it)) },
                            )
                        }

                        null -> PlaylistMediaItemPlaceholder()
                    }
                }
            }
        }
    }
}
