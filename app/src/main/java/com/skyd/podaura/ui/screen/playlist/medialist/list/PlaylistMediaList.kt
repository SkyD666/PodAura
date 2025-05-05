package com.skyd.podaura.ui.screen.playlist.medialist.list

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import com.skyd.podaura.ext.getOrNull
import com.skyd.podaura.ext.safeItemKey
import com.skyd.podaura.ext.thenIf
import com.skyd.podaura.ext.thenIfNotNull
import com.skyd.podaura.ext.withoutTop
import com.skyd.podaura.model.bean.playlist.MediaUrlWithArticleIdBean.Companion.toMediaUrlWithArticleIdBean
import com.skyd.podaura.model.bean.playlist.PlaylistMediaWithArticleBean
import com.skyd.podaura.ui.component.PagingRefreshStateIndicator
import com.skyd.podaura.ui.component.PodAuraIconButton
import com.skyd.podaura.ui.mvi.getDispatcher
import com.skyd.podaura.ui.screen.playlist.addto.AddToPlaylistSheet
import com.skyd.podaura.ui.screen.playlist.medialist.PlaylistMediaItem
import com.skyd.podaura.ui.screen.playlist.medialist.PlaylistMediaItemPlaceholder
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.add_to_playlist
import podaura.shared.generated.resources.delete
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

    BackHandler(uiState.selectedItems.isNotEmpty()) {
        dispatch(ListIntent.ClearSelected)
    }

    PlaylistMediaList(
        uiState = uiState,
        dispatch = dispatch,
        currentPlay = currentPlay,
        count = playlist.size,
        onData = { playlist[it] },
        key = { playlist[it].playlistMediaBean.playlistId to playlist[it].playlistMediaBean.url },
        state = state,
        onMoved = { _, _ -> },
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
    onMoved: suspend (Int, Int) -> Unit,
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

    BackHandler(uiState.selectedItems.isNotEmpty()) {
        dispatch(ListIntent.ClearSelected)
    }

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
    key: (index: Int) -> Any,
    draggable: Boolean = false,
    onMoved: suspend (Int, Int) -> Unit,
    state: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    header: (@Composable LazyStaggeredGridItemScope.() -> Unit)? = null,
    onOpenAddToPlaylistSheet: () -> Unit,
    onPlay: (PlaylistMediaWithArticleBean) -> Unit,
    onDelete: (List<PlaylistMediaWithArticleBean>) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    nestedScrollConnection: NestedScrollConnection? = null,
) {
    val selectMode = uiState.selectedItems.isNotEmpty()

    val reorderableState = rememberReorderableLazyStaggeredGridState(state) { from, to ->
        if (header == null) {
            onMoved(from.index, to.index)
        } else {
            onMoved(from.index - 1, to.index - 1)
        }
    }

    Column(modifier = Modifier.padding(top = contentPadding.calculateTopPadding())) {
        AnimatedVisibility(selectMode) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
            ) {
                PodAuraIconButton(
                    onClick = {
                        onDelete(uiState.selectedItems)
                        dispatch(ListIntent.ClearSelected)
                    },
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(Res.string.delete),
                )
                PodAuraIconButton(
                    onClick = onOpenAddToPlaylistSheet,
                    imageVector = Icons.AutoMirrored.Outlined.PlaylistAdd,
                    contentDescription = stringResource(Res.string.add_to_playlist),
                )
            }
        }
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
                count = count,
                key = key,
            ) { index ->
                ReorderableItem(
                    state = reorderableState,
                    key = key(index),
                ) {
                    when (val item = onData(index)) {
                        is PlaylistMediaWithArticleBean -> {
                            val selected = item in uiState.selectedItems
                            PlaylistMediaItem(
                                playing = item.playlistMediaBean.isSamePlaylistMedia(currentPlay?.playlistMediaBean),
                                selected = selected,
                                data = item,
                                dragIconModifier = Modifier.thenIf(draggable) { draggableHandle() },
                                draggable = draggable,
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
