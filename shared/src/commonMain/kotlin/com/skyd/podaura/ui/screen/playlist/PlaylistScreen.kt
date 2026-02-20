package com.skyd.podaura.ui.screen.playlist

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.skyd.compone.component.ComponeFloatingActionButton
import com.skyd.compone.component.ComponeIconButton
import com.skyd.compone.component.ComponeTopBar
import com.skyd.compone.component.dialog.DeleteWarningDialog
import com.skyd.compone.component.dialog.WaitingDialog
import com.skyd.compone.component.navigation.LocalNavBackStack
import com.skyd.compone.ext.thenIf
import com.skyd.mvi.MviEventListener
import com.skyd.mvi.getDispatcher
import com.skyd.podaura.ext.isCompact
import com.skyd.podaura.ext.rememberUpdateSemaphore
import com.skyd.podaura.ext.safeItemKey
import com.skyd.podaura.ext.vThenP
import com.skyd.podaura.model.bean.playlist.PlaylistViewBean
import com.skyd.podaura.model.preference.behavior.playlist.BasePlaylistSortByPreference
import com.skyd.podaura.model.preference.behavior.playlist.PlaylistSortAscPreference
import com.skyd.podaura.model.preference.behavior.playlist.PlaylistSortByPreference
import com.skyd.podaura.ui.component.CircularProgressPlaceholder
import com.skyd.podaura.ui.component.ErrorPlaceholder
import com.skyd.podaura.ui.component.PagingRefreshStateIndicator
import com.skyd.podaura.ui.component.dialog.SortDialog
import com.skyd.podaura.ui.component.dialog.TextFieldDialog
import com.skyd.podaura.ui.local.LocalWindowSizeClass
import com.skyd.podaura.ui.screen.playlist.medialist.PlaylistMediaListRoute
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
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val windowSizeClass = LocalWindowSizeClass.current
    val navBackStack = LocalNavBackStack.current
    val scope = rememberCoroutineScope()

    val lazyListState = rememberLazyStaggeredGridState()
    var fabHeight by remember { mutableStateOf(0.dp) }
    var openAddDialog by rememberSaveable { mutableStateOf(false) }
    var addDialogText by rememberSaveable { mutableStateOf("") }
    var openRenameDialog by rememberSaveable { mutableStateOf<String?>(null) }
    var renameDialogText by rememberSaveable { mutableStateOf("") }
    var openDeleteWarningDialog by rememberSaveable { mutableStateOf<String?>(null) }
    var showSortDialog by rememberSaveable { mutableStateOf(false) }

    val dispatch = viewModel.getDispatcher(startWith = PlaylistIntent.Init)
    val uiState by viewModel.viewState.collectAsStateWithLifecycle()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            ComponeTopBar(
                title = { Text(text = stringResource(Res.string.playlist_screen_name)) },
                scrollBehavior = scrollBehavior,
                navigationIcon = {},
                actions = {
                    ComponeIconButton(
                        onClick = { showSortDialog = true },
                        imageVector = Icons.AutoMirrored.Outlined.Sort,
                        contentDescription = stringResource(Res.string.sort),
                    )
                },
                windowInsets =
                    if (windowSizeClass.isCompact)
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                    else
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.End)
            )
        },
        floatingActionButton = {
            ComponeFloatingActionButton(
                onClick = { openAddDialog = true },
                onSizeWithSinglePaddingChanged = { _, height -> fabHeight = height },
                contentDescription = stringResource(Res.string.add),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(Res.string.add),
                )
            }
        },
        contentWindowInsets =
            if (windowSizeClass.isCompact)
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            else
                WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
    ) { innerPadding ->
        when (val listState = uiState.listState) {
            is ListState.Failed -> ErrorPlaceholder(listState.msg, innerPadding)
            ListState.Init -> CircularProgressPlaceholder(innerPadding)
            is ListState.Success -> {
                val lazyPagingItems = listState.playlistPagingDataFlow.collectAsLazyPagingItems()
                val reorderSemaphore = remember { Channel<Unit>(Channel.UNLIMITED) }
                val reorderPagingItemsSemaphore = lazyPagingItems.rememberUpdateSemaphore(
                    default = null,
                    sendData = { reorderSemaphore.tryReceive().getOrNull() },
                )
                PlayList(
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
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
                        navBackStack.add(PlaylistMediaListRoute(playlistId = it.playlist.playlistId))
                    },
                    onRename = {
                        renameDialogText = it.playlist.name
                        openRenameDialog = it.playlist.playlistId
                    },
                    onDelete = { openDeleteWarningDialog = it.playlist.playlistId },
                    contentPadding = innerPadding
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
                            playlistId = openRenameDialog!!,
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
                    dispatch(PlaylistIntent.Delete(openDeleteWarningDialog!!))
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