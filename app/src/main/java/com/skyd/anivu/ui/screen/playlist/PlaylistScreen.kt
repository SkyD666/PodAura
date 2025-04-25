package com.skyd.anivu.ui.screen.playlist

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.skyd.anivu.ui.mvi.MviEventListener
import com.skyd.anivu.ui.mvi.getDispatcher
import com.skyd.anivu.ext.isCompact
import com.skyd.anivu.ext.rememberUpdateSemaphore
import com.skyd.anivu.ext.safeItemKey
import com.skyd.anivu.ext.thenIf
import com.skyd.anivu.ext.vThenP
import com.skyd.anivu.model.bean.playlist.PlaylistViewBean
import com.skyd.anivu.model.preference.behavior.playlist.BasePlaylistSortByPreference
import com.skyd.anivu.model.preference.behavior.playlist.PlaylistSortAscPreference
import com.skyd.anivu.model.preference.behavior.playlist.PlaylistSortByPreference
import com.skyd.anivu.ui.component.CircularProgressPlaceholder
import com.skyd.anivu.ui.component.ErrorPlaceholder
import com.skyd.anivu.ui.component.PagingRefreshStateIndicator
import com.skyd.anivu.ui.component.PodAuraFloatingActionButton
import com.skyd.anivu.ui.component.PodAuraIconButton
import com.skyd.anivu.ui.component.PodAuraTopBar
import com.skyd.anivu.ui.component.dialog.DeleteWarningDialog
import com.skyd.anivu.ui.component.dialog.SortDialog
import com.skyd.anivu.ui.component.dialog.TextFieldDialog
import com.skyd.anivu.ui.component.dialog.WaitingDialog
import com.skyd.anivu.ui.local.LocalNavController
import com.skyd.anivu.ui.local.LocalWindowSizeClass
import com.skyd.anivu.ui.screen.playlist.medialist.PlaylistMediaListRoute
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.add
import podaura.shared.generated.resources.playlist_screen_add_a_playlist
import podaura.shared.generated.resources.playlist_screen_delete_playlist_warning
import podaura.shared.generated.resources.playlist_screen_name
import podaura.shared.generated.resources.rename
import podaura.shared.generated.resources.sort
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyStaggeredGridState


@Serializable
data object PlaylistRoute

@Composable
fun PlaylistScreen(viewModel: PlaylistViewModel = koinViewModel()) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }
    val windowSizeClass = LocalWindowSizeClass.current
    val navController = LocalNavController.current
    val scope = rememberCoroutineScope()

    val lazyListState = rememberLazyStaggeredGridState()
    var fabHeight by remember { mutableStateOf(0.dp) }
    var openAddDialog by rememberSaveable { mutableStateOf(false) }
    var addDialogText by rememberSaveable { mutableStateOf("") }
    var openRenameDialog by rememberSaveable { mutableStateOf<PlaylistViewBean?>(null) }
    var renameDialogText by rememberSaveable { mutableStateOf("") }
    var openDeleteWarningDialog by rememberSaveable { mutableStateOf<PlaylistViewBean?>(null) }
    var showSortDialog by rememberSaveable { mutableStateOf(false) }

    val dispatch = viewModel.getDispatcher(startWith = PlaylistIntent.Init)
    val uiState by viewModel.viewState.collectAsStateWithLifecycle()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            PodAuraTopBar(
                title = { Text(text = stringResource(Res.string.playlist_screen_name)) },
                scrollBehavior = scrollBehavior,
                navigationIcon = {},
                actions = {
                    PodAuraIconButton(
                        onClick = { showSortDialog = true },
                        imageVector = Icons.AutoMirrored.Outlined.Sort,
                        contentDescription = stringResource(Res.string.sort),
                    )
                },
                windowInsets = WindowInsets.safeDrawing.run {
                    var sides = WindowInsetsSides.Top + WindowInsetsSides.Right
                    if (windowSizeClass.isCompact) sides += WindowInsetsSides.Left
                    only(sides)
                },
            )
        },
        floatingActionButton = {
            PodAuraFloatingActionButton(
                onClick = { openAddDialog = true },
                onSizeWithSinglePaddingChanged = { _, height -> fabHeight = height },
                contentDescription = stringResource(Res.string.add),
            ) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing.run {
            var sides = WindowInsetsSides.Top + WindowInsetsSides.Right
            sides += if (windowSizeClass.isCompact) WindowInsetsSides.Left
            else WindowInsetsSides.Bottom
            only(sides)
        },
    ) { paddingValues ->
        when (val listState = uiState.listState) {
            is ListState.Failed -> ErrorPlaceholder(listState.msg, paddingValues)
            ListState.Init -> CircularProgressPlaceholder(paddingValues)
            is ListState.Success -> {
                val lazyPagingItems = listState.playlistPagingDataFlow.collectAsLazyPagingItems()
                val reorderSemaphore = remember { Channel<Unit>(Channel.UNLIMITED) }
                val reorderPagingItemsSemaphore = lazyPagingItems.rememberUpdateSemaphore(
                    default = null,
                    sendData = { reorderSemaphore.tryReceive().getOrNull() },
                )
                PlayList(
                    playlist = lazyPagingItems,
                    state = lazyListState,
                    draggable = PlaylistSortByPreference.current == BasePlaylistSortByPreference.MANUAL,
                    onMoved = onMoved@{ fromIndex, toIndex ->
                        if (fromIndex == toIndex) return@onMoved
                        reorderSemaphore.vThenP(reorderPagingItemsSemaphore) {
                            dispatch(
                                PlaylistIntent.Reorder(
                                    fromIndex = fromIndex,
                                    toIndex = toIndex
                                )
                            )
                        }
                    },
                    onClick = {
                        navController.navigate(PlaylistMediaListRoute(playlistId = it.playlist.playlistId))
                    },
                    onRename = {
                        renameDialogText = it.playlist.name
                        openRenameDialog = it
                    },
                    onDelete = { openDeleteWarningDialog = it },
                    contentPadding = paddingValues,
                )
            }
        }

        WaitingDialog(visible = uiState.loadingDialog)

        if (openAddDialog) {
            TextFieldDialog(
                value = addDialogText,
                onValueChange = { addDialogText = it },
                titleText = stringResource(Res.string.playlist_screen_add_a_playlist),
                onDismissRequest = {
                    openAddDialog = false
                    addDialogText = ""
                },
                onConfirm = {
                    openAddDialog = false
                    dispatch(PlaylistIntent.CreatePlaylist(addDialogText))
                    addDialogText = ""
                },
            )
        }
        if (openRenameDialog != null) {
            TextFieldDialog(
                value = renameDialogText,
                onValueChange = { renameDialogText = it },
                titleText = stringResource(Res.string.rename),
                onDismissRequest = {
                    openRenameDialog = null
                    renameDialogText = ""
                },
                onConfirm = {
                    dispatch(
                        PlaylistIntent.Rename(
                            playlistId = openRenameDialog!!.playlist.playlistId,
                            newName = renameDialogText,
                        )
                    )
                    openRenameDialog = null
                    renameDialogText = ""
                },
            )
        }

        if (openDeleteWarningDialog != null) {
            DeleteWarningDialog(
                text = stringResource(Res.string.playlist_screen_delete_playlist_warning),
                onDismissRequest = { openDeleteWarningDialog = null },
                onDismiss = { openDeleteWarningDialog = null },
                onConfirm = {
                    dispatch(PlaylistIntent.Delete(openDeleteWarningDialog!!.playlist.playlistId))
                    openDeleteWarningDialog = null
                },
            )
        }

        SortDialog(
            visible = showSortDialog,
            onDismissRequest = { showSortDialog = false },
            sortByValues = PlaylistSortByPreference.values,
            sortBy = PlaylistSortByPreference.current,
            sortAsc = PlaylistSortAscPreference.current,
            enableSortAsc = PlaylistSortByPreference.current != BasePlaylistSortByPreference.MANUAL,
            onSortBy = { PlaylistSortByPreference.put(scope, it) },
            onSortAsc = { PlaylistSortAscPreference.put(scope, it) },
            onSortByDisplayName = { BasePlaylistSortByPreference.toDisplayName(it) },
            onSortByIcon = { BasePlaylistSortByPreference.toIcon(it) },
        )

        MviEventListener(viewModel.singleEvent) { event ->
            when (event) {
                is PlaylistEvent.CreateResultEvent.Failed -> snackbarHostState.showSnackbar(event.msg)
                is PlaylistEvent.DeleteResultEvent.Failed -> snackbarHostState.showSnackbar(event.msg)
                is PlaylistEvent.RenameResultEvent.Failed -> snackbarHostState.showSnackbar(event.msg)
                is PlaylistEvent.ReorderResultEvent.Failed -> snackbarHostState.showSnackbar(event.msg)
            }
        }
    }
}

@Composable
private fun PlayList(
    modifier: Modifier = Modifier,
    playlist: LazyPagingItems<PlaylistViewBean>,
    state: LazyStaggeredGridState,
    draggable: Boolean = false,
    onMoved: suspend (Int, Int) -> Unit,
    onClick: (PlaylistViewBean) -> Unit,
    onRename: (PlaylistViewBean) -> Unit,
    onDelete: (PlaylistViewBean) -> Unit,
    contentPadding: PaddingValues,
) {
    val reorderableState = rememberReorderableLazyStaggeredGridState(state) { from, to ->
        onMoved(from.index, to.index)
    }

    PagingRefreshStateIndicator(
        lazyPagingItems = playlist,
        placeholderPadding = contentPadding,
    ) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(300.dp),
            modifier = modifier.fillMaxSize(),
            state = state,
            contentPadding = contentPadding,
        ) {
            val key = playlist.safeItemKey { it.playlist.playlistId }
            items(
                count = playlist.itemCount,
                key = key,
            ) { index ->
                ReorderableItem(
                    state = reorderableState,
                    key = key(index),
                ) {
                    when (val item = playlist[index]) {
                        is PlaylistViewBean -> PlaylistItem(
                            playlistViewBean = item,
                            enableMenu = true,
                            draggable = draggable,
                            dragIconModifier = Modifier.thenIf(draggable) { draggableHandle() },
                            onClick = onClick,
                            onRename = onRename,
                            onDelete = onDelete,
                        )

                        null -> PlaylistItemPlaceholder()
                    }
                }
            }
        }
    }
}