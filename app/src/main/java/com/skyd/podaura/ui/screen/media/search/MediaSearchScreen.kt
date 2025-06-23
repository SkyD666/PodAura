package com.skyd.podaura.ui.screen.media.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.toRoute
import com.skyd.compone.component.BackIcon
import com.skyd.compone.component.ComponeFloatingActionButton
import com.skyd.compone.component.SearchBarInputField
import com.skyd.compone.component.dialog.WaitingDialog
import com.skyd.podaura.ext.activity
import com.skyd.podaura.ext.plus
import com.skyd.podaura.model.preference.appearance.media.item.MediaListItemTypePreference
import com.skyd.podaura.model.preference.appearance.media.item.MediaSubListItemTypePreference
import com.skyd.podaura.model.repository.player.PlayDataMode
import com.skyd.podaura.ui.activity.player.PlayActivity
import com.skyd.podaura.ui.component.CircularProgressPlaceholder
import com.skyd.podaura.ui.component.ErrorPlaceholder
import com.skyd.podaura.ui.local.LocalNavController
import com.skyd.podaura.ui.mvi.MviEventListener
import com.skyd.podaura.ui.mvi.getDispatcher
import com.skyd.podaura.ui.screen.media.list.MediaList
import com.skyd.podaura.ui.screen.media.sub.SubMediaRoute
import com.skyd.podaura.ui.screen.search.TrailingIcon
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.media_screen_search_hint
import podaura.shared.generated.resources.to_top


@Serializable
data class MediaSearchRoute(val path: String, val isSubList: Boolean) {
    companion object {
        @Composable
        fun MediaSearchLauncher(entry: NavBackStackEntry) {
            val route = entry.toRoute<MediaSearchRoute>()
            MediaSearchScreen(path = route.path, isSubList = route.isSubList)
        }
    }
}

@Composable
fun MediaSearchScreen(
    path: String,
    isSubList: Boolean,
    viewModel: MediaSearchViewModel = koinViewModel(),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val navController = LocalNavController.current
    val uiState by viewModel.viewState.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current

    val searchResultListState = rememberLazyGridState()
    var fabHeight by remember { mutableStateOf(0.dp) }
    var fabWidth by remember { mutableStateOf(0.dp) }

    var searchFieldValueState by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(text = "", selection = TextRange(0)))
    }

    val dispatch = viewModel.getDispatcher(startWith = MediaSearchIntent.Init(path))

    Scaffold(
        modifier = Modifier.imePadding(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            AnimatedVisibility(
                visible = remember {
                    derivedStateOf { searchResultListState.firstVisibleItemIndex > 2 }
                }.value,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                ComponeFloatingActionButton(
                    onClick = { scope.launch { searchResultListState.animateScrollToItem(0) } },
                    onSizeWithSinglePaddingChanged = { width, height ->
                        fabWidth = width
                        fabHeight = height
                    },
                    contentDescription = stringResource(Res.string.to_top),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowUpward,
                        contentDescription = stringResource(Res.string.to_top),
                    )
                }
            }
        },
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .windowInsetsPadding(
                        WindowInsets.systemBars
                            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                    )
            ) {
                SearchBarInputField(
                    onQueryChange = {
                        searchFieldValueState = it
                        dispatch(MediaSearchIntent.UpdateQuery(path = path, query = it.text))
                    },
                    query = searchFieldValueState,
                    onSearch = { keyboardController?.hide() },
                    placeholder = { Text(text = stringResource(Res.string.media_screen_search_hint)) },
                    leadingIcon = { BackIcon() },
                    trailingIcon = {
                        TrailingIcon(showClearButton = searchFieldValueState.text.isNotEmpty()) {
                            searchFieldValueState = TextFieldValue(
                                text = "", selection = TextRange(0)
                            )
                            dispatch(
                                MediaSearchIntent.UpdateQuery(
                                    path = path,
                                    query = searchFieldValueState.text,
                                )
                            )
                        }
                    }
                )
                HorizontalDivider()
            }
        },
    ) { innerPaddings ->
        when (val searchResultState = uiState.searchResultState) {
            is SearchResultState.Failed -> ErrorPlaceholder(
                modifier = Modifier.sizeIn(maxHeight = 200.dp),
                text = searchResultState.msg,
                contentPadding = innerPaddings
            )

            SearchResultState.Init,
            SearchResultState.Loading -> CircularProgressPlaceholder(contentPadding = innerPaddings)

            is SearchResultState.Success -> MediaList(
                list = searchResultState.result,
                groups = emptyList(),
                groupInfo = null,
                listItemType = if (isSubList) MediaSubListItemTypePreference.current
                else MediaListItemTypePreference.current,
                onPlay = { media ->
                    PlayActivity.playMediaList(
                        context.activity,
                        startMediaPath = media.filePath,
                        mediaList = searchResultState.result.filter { it.isMedia }.map {
                            PlayDataMode.MediaLibraryList.PlayMediaListItem(
                                path = it.filePath,
                                articleId = it.articleId,
                                title = it.displayName,
                                thumbnail = it.feedBean?.customIcon
                                    ?: it.feedBean?.icon,
                            )
                        },
                    )
                },
                onOpenDir = { navController.navigate(SubMediaRoute(media = it)) },
                onRename = { oldMedia, newName ->
                    dispatch(MediaSearchIntent.RenameFile(oldMedia.path, newName))
                },
                onSetFileDisplayName = { media, displayName -> },
                onRemove = { dispatch(MediaSearchIntent.DeleteFile(it.path)) },
                contentPadding = innerPaddings + PaddingValues(bottom = fabHeight),
            )
        }

        WaitingDialog(visible = uiState.loadingDialog)

        MviEventListener(viewModel.singleEvent) { event ->
            when (event) {
                is MediaSearchEvent.DeleteFileResultEvent.Failed ->
                    snackbarHostState.showSnackbar(event.msg)

                is MediaSearchEvent.RenameFileResultEvent.Failed ->
                    snackbarHostState.showSnackbar(event.msg)
            }
        }
    }
}