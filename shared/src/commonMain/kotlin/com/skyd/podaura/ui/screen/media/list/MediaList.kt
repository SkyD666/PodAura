package com.skyd.podaura.ui.screen.media.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.LoadingIndicator
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skyd.compone.component.navigation.LocalNavBackStack
import com.skyd.compone.ext.plus
import com.skyd.mvi.MviEventListener
import com.skyd.mvi.getDispatcher
import com.skyd.podaura.model.bean.MediaBean
import com.skyd.podaura.model.bean.MediaGroupBean
import com.skyd.podaura.model.bean.playlist.MediaUrlWithArticleIdBean
import com.skyd.podaura.model.preference.appearance.media.item.BaseMediaItemTypePreference
import com.skyd.podaura.model.preference.appearance.media.item.MediaListItemTypePreference
import com.skyd.podaura.model.preference.appearance.media.item.MediaSubListItemTypePreference
import com.skyd.podaura.ui.component.CircularProgressPlaceholder
import com.skyd.podaura.ui.component.EmptyPlaceholder
import com.skyd.podaura.ui.player.jumper.PlayDataMode
import com.skyd.podaura.ui.player.jumper.rememberPlayerJumper
import com.skyd.podaura.ui.screen.article.ArticleRoute
import com.skyd.podaura.ui.screen.media.CreateGroupDialog
import com.skyd.podaura.ui.screen.media.list.item.MediaItem
import com.skyd.podaura.ui.screen.media.sub.SubMediaRoute
import com.skyd.podaura.ui.screen.playlist.addto.AddToPlaylistSheet
import com.skyd.podaura.ui.screen.read.ReadRoute
import org.koin.compose.viewmodel.koinViewModel

class GroupInfo(
    val group: MediaGroupBean,
    val version: Long,   // For update, if version changed, MediaList will refresh media list
    val onCreateGroup: (MediaGroupBean) -> Unit,
    val onMoveFileToGroup: (MediaBean, MediaGroupBean) -> Unit,
)

@Composable
internal fun MediaList(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    fabPadding: PaddingValues = PaddingValues(),
    path: String,
    isSubList: Boolean,
    groupInfo: GroupInfo? = null,
    viewModel: MediaListViewModel = koinViewModel(key = path + groupInfo?.group)
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val navBackStack = LocalNavBackStack.current

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        val uiState by viewModel.viewState.collectAsStateWithLifecycle()
        val dispatch = viewModel.getDispatcher(
            path,
            groupInfo?.version,
            startWith = MediaListIntent.Init(
                path = path,
                group = groupInfo?.group,
                isSubList = isSubList,
                version = groupInfo?.version,
            )
        )
        val state = rememberPullToRefreshState()
        Box(
            modifier = Modifier.pullToRefresh(
                isRefreshing = uiState.listState.loading,
                onRefresh = {
                    dispatch(MediaListIntent.Refresh(path = path, group = groupInfo?.group))
                },
                state = state
            )
        ) {
            when (val listState = uiState.listState) {
                is ListState.Failed -> Unit
                is ListState.Init -> CircularProgressPlaceholder(contentPadding = innerPadding + contentPadding)

                is ListState.Success -> {
                    if (listState.list.isEmpty()) {
                        EmptyPlaceholder(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            contentPadding = innerPadding + contentPadding
                        )
                    } else {
                        val listItemType = if (isSubList) MediaSubListItemTypePreference.current
                        else MediaListItemTypePreference.current
                        val playerJumper = rememberPlayerJumper()
                        MediaList(
                            modifier = modifier,
                            list = listState.list,
                            groups = uiState.groups,
                            groupInfo = groupInfo,
                            listItemType = listItemType,
                            onPlay = { media ->
                                playerJumper.jump(
                                    PlayDataMode.MediaLibraryList(
                                        startMediaPath = media.filePath,
                                        mediaList = listState.list.filter { it.isMedia }.map {
                                            PlayDataMode.MediaLibraryList.PlayMediaListItem(
                                                path = it.filePath,
                                                articleId = it.articleId,
                                                title = it.displayName,
                                                thumbnail = it.feedBean?.customIcon
                                                    ?: it.feedBean?.icon,
                                            )
                                        },
                                    )
                                )
                            },
                            onOpenDir = { navBackStack.add(SubMediaRoute(media = it)) },
                            onRename = { oldMedia, newName ->
                                dispatch(MediaListIntent.RenameFile(oldMedia.path, newName))
                            },
                            onSetFileDisplayName = { media, displayName ->
                                dispatch(
                                    MediaListIntent.SetFileDisplayName(
                                        media = media,
                                        displayName = displayName,
                                    )
                                )
                            },
                            onRemove = { dispatch(MediaListIntent.DeleteFile(it.path)) },
                            contentPadding = innerPadding + contentPadding + fabPadding,
                        )
                    }
                }
            }

            LoadingIndicator(
                modifier = Modifier
                    .padding(contentPadding + fabPadding)
                    .align(Alignment.TopCenter),
                isRefreshing = uiState.listState.loading,
                state = state
            )
        }

        MviEventListener(viewModel.singleEvent) { event ->
            when (event) {
                is MediaListEvent.DeleteFileResultEvent.Failed ->
                    snackbarHostState.showSnackbar(event.msg)

                is MediaListEvent.MediaListResultEvent.Failed ->
                    snackbarHostState.showSnackbar(event.msg)
            }
        }
    }
}

@Composable
internal fun MediaList(
    modifier: Modifier = Modifier,
    list: List<MediaBean>,
    groups: List<MediaGroupBean>,
    groupInfo: GroupInfo?,
    listItemType: String,
    onPlay: (MediaBean) -> Unit,
    onOpenDir: (MediaBean) -> Unit,
    onRename: (MediaBean, String) -> Unit,
    onSetFileDisplayName: (MediaBean, String?) -> Unit,
    onRemove: (MediaBean) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val navBackStack = LocalNavBackStack.current
    var openEditMediaDialog by rememberSaveable { mutableStateOf<String?>(null) }
    var openCreateGroupDialog by rememberSaveable { mutableStateOf(false) }
    var createGroupDialogGroup by rememberSaveable { mutableStateOf("") }
    var openAddToPlaylistSheet by rememberSaveable { mutableStateOf<Pair<String, String?>?>(null) }

    val onOpenFeed: (MediaBean) -> ((MediaBean) -> Unit)? = { mediaBean: MediaBean ->
        mediaBean.feedBean?.let {
            { it: MediaBean ->
                it.feedUrl?.let { feedUrl ->
                    navBackStack.add(ArticleRoute(feedUrls = listOf(feedUrl)))
                }
            }
        }
    }
    val onOpenArticle: (MediaBean) -> ((MediaBean) -> Unit)? = { mediaBean: MediaBean ->
        mediaBean.articleWithEnclosure?.let {
            { it: MediaBean ->
                it.articleId?.let { navBackStack.add(ReadRoute(articleId = it)) }
            }
        }
    }
    val onOpenAddToPlaylistSheet: (MediaBean) -> ((MediaBean) -> Unit)? = { mediaBean: MediaBean ->
        if (mediaBean.isMedia) {
            { it: MediaBean ->
                openAddToPlaylistSheet = it.filePath to it.articleId
            }
        } else null
    }

    LazyVerticalGrid(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding + PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        columns = GridCells.Adaptive(BaseMediaItemTypePreference.toMinWidth(listItemType).dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(list) { item ->
            MediaItem(
                itemType = listItemType,
                data = item,
                onPlay = onPlay,
                onOpenDir = onOpenDir,
                onRemove = onRemove,
                onOpenFeed = onOpenFeed(item),
                onOpenArticle = onOpenArticle(item),
                onLongClick = if (groupInfo == null) null else {
                    { openEditMediaDialog = it.filePath }
                },
                onOpenAddToPlaylistSheet = onOpenAddToPlaylistSheet(item),
            )
        }
    }
    if (openEditMediaDialog != null) {
        val mediaBean = remember(openEditMediaDialog) {
            list.firstOrNull { it.filePath == openEditMediaDialog }
        }
        if (mediaBean != null) {
            EditMediaSheet(
                onDismissRequest = { openEditMediaDialog = null },
                mediaBean = mediaBean,
                currentGroup = groupInfo!!.group,
                groups = groups,
                onRename = onRename,
                onSetFileDisplayName = onSetFileDisplayName,
                onAddToPlaylistClicked = onOpenAddToPlaylistSheet(mediaBean),
                onDelete = {
                    onRemove(it)
                    openEditMediaDialog = null
                },
                onGroupChange = {
                    groupInfo.onMoveFileToGroup(mediaBean, it)
                    openEditMediaDialog = null
                },
                openCreateGroupDialog = {
                    openCreateGroupDialog = true
                    createGroupDialogGroup = ""
                },
                onOpenFeed = onOpenFeed(mediaBean),
                onOpenArticle = onOpenArticle(mediaBean),
            )
        }
    }

    openAddToPlaylistSheet?.let { (url, articleId) ->
        AddToPlaylistSheet(
            onDismissRequest = { openAddToPlaylistSheet = null },
            currentPlaylistId = null,
            selectedMediaList = listOf(MediaUrlWithArticleIdBean(url = url, articleId = articleId)),
        )
    }

    CreateGroupDialog(
        visible = openCreateGroupDialog,
        value = createGroupDialogGroup,
        onValueChange = { text -> createGroupDialogGroup = text },
        onCreateGroup = {
            groupInfo?.onCreateGroup?.invoke(it)
            openCreateGroupDialog = false
            createGroupDialogGroup = ""
        },
        onDismissRequest = {
            openCreateGroupDialog = false
            createGroupDialogGroup = ""
        }
    )
}