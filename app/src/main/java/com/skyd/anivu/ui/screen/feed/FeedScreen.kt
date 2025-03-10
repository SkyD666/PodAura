package com.skyd.anivu.ui.screen.feed

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Workspaces
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalAbsoluteTonalElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowSize
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.skyd.anivu.R
import com.skyd.anivu.base.mvi.MviEventListener
import com.skyd.anivu.base.mvi.getDispatcher
import com.skyd.anivu.ext.isCompact
import com.skyd.anivu.ext.lastIndex
import com.skyd.anivu.ext.plus
import com.skyd.anivu.ext.safeItemKey
import com.skyd.anivu.model.bean.feed.FeedViewBean
import com.skyd.anivu.model.bean.group.GroupVo
import com.skyd.anivu.model.bean.group.GroupVo.Companion.DEFAULT_GROUP_ID
import com.skyd.anivu.ui.component.ClipboardTextField
import com.skyd.anivu.ui.component.PagingRefreshStateIndicator
import com.skyd.anivu.ui.component.PodAuraFloatingActionButton
import com.skyd.anivu.ui.component.PodAuraIconButton
import com.skyd.anivu.ui.component.PodAuraTopBar
import com.skyd.anivu.ui.component.PodAuraTopBarStyle
import com.skyd.anivu.ui.component.dialog.PodAuraDialog
import com.skyd.anivu.ui.component.dialog.TextFieldDialog
import com.skyd.anivu.ui.component.dialog.WaitingDialog
import com.skyd.anivu.ui.component.showToast
import com.skyd.anivu.ui.local.LocalFeedListTonalElevation
import com.skyd.anivu.ui.local.LocalFeedTopBarTonalElevation
import com.skyd.anivu.ui.local.LocalNavController
import com.skyd.anivu.ui.local.LocalWindowSizeClass
import com.skyd.anivu.ui.screen.article.ArticleScreen
import com.skyd.anivu.ui.screen.article.openArticleScreen
import com.skyd.anivu.ui.screen.feed.item.Feed1Item
import com.skyd.anivu.ui.screen.feed.item.Feed1ItemPlaceholder
import com.skyd.anivu.ui.screen.feed.item.Group1Item
import com.skyd.anivu.ui.screen.feed.mute.MUTE_FEED_SCREEN_ROUTE
import com.skyd.anivu.ui.screen.feed.reorder.REORDER_GROUP_SCREEN_ROUTE
import com.skyd.anivu.ui.screen.search.SearchDomain
import com.skyd.anivu.ui.screen.search.openSearchScreen
import com.skyd.anivu.ui.screen.settings.appearance.feed.FEED_STYLE_SCREEN_ROUTE
import kotlinx.coroutines.android.awaitFrame
import java.util.UUID

const val FEED_SCREEN_ROUTE = "feedScreen"

@Composable
fun FeedScreen() {
    val navigator = rememberListDetailPaneScaffoldNavigator<Map<String, List<String>>>(
        scaffoldDirective = calculatePaneScaffoldDirective(currentWindowAdaptiveInfo()).copy(
            horizontalPartitionSpacerSize = 0.dp,
        )
    )
    val navController = LocalNavController.current
    val windowSizeClass = LocalWindowSizeClass.current
    val density = LocalDensity.current

    var listPaneSelected by remember { mutableStateOf<Map<String, List<String>>?>(null) }

    val onNavigatorBack: () -> Unit = {
        navigator.navigateBack()
        listPaneSelected = navigator.currentDestination?.content
    }

    BackHandler(navigator.canNavigateBack()) {
        onNavigatorBack()
    }

    val windowWidth = with(density) { currentWindowSize().width.toDp() }
    val feedListWidth by remember(windowWidth) { mutableStateOf(windowWidth * 0.335f) }

    ListDetailPaneScaffold(
        modifier = Modifier.windowInsetsPadding(
            WindowInsets.safeDrawing.only(
                WindowInsetsSides.Right.run {
                    if (windowSizeClass.isCompact) plus(WindowInsetsSides.Left) else this
                }
            )),
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            AnimatedPane(modifier = Modifier.preferredWidth(feedListWidth)) {
                FeedList(
                    listPaneSelectedFeedUrls = listPaneSelected?.get("feedUrls"),
                    listPaneSelectedGroupIds = listPaneSelected?.get("groupIds"),
                    onShowArticleListByFeedUrls = { feedUrls ->
                        if (navigator.scaffoldDirective.maxHorizontalPartitions > 1) {
                            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, buildMap {
                                put("feedUrls", feedUrls)
                            })
                        } else {
                            openArticleScreen(
                                navController = navController,
                                feedUrls = ArrayList(feedUrls),
                            )
                        }
                    },
                    onShowArticleListByGroupId = { groupId ->
                        if (navigator.scaffoldDirective.maxHorizontalPartitions > 1) {
                            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, buildMap {
                                put("groupIds", listOf(groupId))
                            })
                        } else {
                            openArticleScreen(
                                navController = navController,
                                feedUrls = emptyList(),
                                groupIds = listOf(groupId),
                            )
                        }
                    }
                )
            }
        },
        detailPane = {
            AnimatedPane {
                navigator.currentDestination?.content?.let {
                    listPaneSelected = it
                    ArticleScreen(
                        feedUrls = it["feedUrls"].orEmpty(),
                        groupIds = it["groupIds"].orEmpty(),
                        articleIds = emptyList(),
                        onBackClick = onNavigatorBack,
                    )
                }
            }
        },
    )
}

@Composable
private fun FeedList(
    listPaneSelectedFeedUrls: List<String>? = null,
    listPaneSelectedGroupIds: List<String>? = null,
    onShowArticleListByFeedUrls: (List<String>) -> Unit,
    onShowArticleListByGroupId: (String) -> Unit,
    viewModel: FeedViewModel = hiltViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val navController = LocalNavController.current
    val snackbarHostState = remember { SnackbarHostState() }
    val windowSizeClass = LocalWindowSizeClass.current
    var openMoreMenu by rememberSaveable { mutableStateOf(false) }
    var openAddDialog by rememberSaveable { mutableStateOf(false) }
    var addDialogUrl by rememberSaveable { mutableStateOf("") }
    var openEditFeedDialog by rememberSaveable { mutableStateOf<FeedViewBean?>(null) }
    var openEditGroupDialog by rememberSaveable { mutableStateOf<GroupVo?>(value = null) }

    var openCreateGroupDialog by rememberSaveable { mutableStateOf(false) }
    var createGroupDialogGroup by rememberSaveable { mutableStateOf("") }

    var fabHeight by remember { mutableStateOf(0.dp) }
    var fabWidth by remember { mutableStateOf(0.dp) }

    val uiState by viewModel.viewState.collectAsStateWithLifecycle()
    val dispatch = viewModel.getDispatcher(startWith = FeedIntent.Init)

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            PodAuraTopBar(
                style = PodAuraTopBarStyle.Small,
                title = { Text(text = stringResource(id = R.string.feed_screen_name)) },
                actions = {
                    PodAuraIconButton(
                        onClick = { onShowArticleListByFeedUrls(emptyList()) },
                        imageVector = Icons.AutoMirrored.Outlined.Article,
                        contentDescription = stringResource(id = R.string.feed_screen_all_articles),
                    )
                    PodAuraIconButton(
                        onClick = {
                            openSearchScreen(
                                navController = navController,
                                searchDomain = SearchDomain.Feed,
                            )
                        },
                        imageVector = Icons.Outlined.Search,
                        contentDescription = stringResource(id = R.string.feed_screen_search_feed),
                    )
                    PodAuraIconButton(
                        onClick = { openMoreMenu = true },
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = stringResource(id = R.string.more),
                    )
                    MoreMenu(expanded = openMoreMenu, onDismissRequest = { openMoreMenu = false })
                },
                navigationIcon = {},
                windowInsets = WindowInsets.safeDrawing.only(
                    (WindowInsetsSides.Top + WindowInsetsSides.Right).run {
                        if (windowSizeClass.isCompact) plus(WindowInsetsSides.Left) else this
                    }
                ),
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors().copy(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                        LocalFeedTopBarTonalElevation.current.dp
                    ),
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                        LocalFeedTopBarTonalElevation.current.dp + 4.dp
                    ),
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            PodAuraFloatingActionButton(
                onClick = { openAddDialog = true },
                onSizeWithSinglePaddingChanged = { width, height ->
                    fabWidth = width
                    fabHeight = height
                },
                contentDescription = stringResource(R.string.add),
            ) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
            LocalAbsoluteTonalElevation.current +
                    LocalFeedListTonalElevation.current.dp
        ),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) { innerPadding ->
        when (val listState = uiState.listState) {
            is ListState.Failed, ListState.Init, ListState.Loading -> {}
            is ListState.Success -> FeedList(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                lazyPagingItems = listState.dataPagingDataFlow.collectAsLazyPagingItems(),
                contentPadding = innerPadding,
                fabPadding = fabHeight + 16.dp,
                selectedFeedUrls = listPaneSelectedFeedUrls,
                selectedGroupIds = listPaneSelectedGroupIds,
                onShowArticleListByFeedUrls = { feedUrls -> onShowArticleListByFeedUrls(feedUrls) },
                onShowArticleListByGroupId = { groupId -> onShowArticleListByGroupId(groupId) },
                onExpandChanged = { group, expanded ->
                    dispatch(FeedIntent.ChangeGroupExpanded(group, expanded))
                },
                onEditFeed = { feed -> openEditFeedDialog = feed },
                onEditGroup = { group -> openEditGroupDialog = group },
            )
        }

        MviEventListener(viewModel.singleEvent) { event ->
            when (event) {
                is FeedEvent.AddFeedResultEvent.Failed -> snackbarHostState.showSnackbar(event.msg)
                is FeedEvent.InitFeetListResultEvent.Failed ->
                    snackbarHostState.showSnackbar(event.msg)

                is FeedEvent.EditFeedResultEvent.Failed -> event.msg.showToast()
                is FeedEvent.RemoveFeedResultEvent.Failed -> event.msg.showToast()
                is FeedEvent.RefreshFeedResultEvent.Failed -> event.msg.showToast()
                is FeedEvent.CreateGroupResultEvent.Failed -> event.msg.showToast()
                is FeedEvent.MoveFeedsToGroupResultEvent.Failed -> event.msg.showToast()
                is FeedEvent.DeleteGroupResultEvent.Failed -> event.msg.showToast()
                is FeedEvent.EditGroupResultEvent.Failed -> event.msg.showToast()
                is FeedEvent.ReadAllResultEvent.Failed -> event.msg.showToast()
                is FeedEvent.ClearFeedArticlesResultEvent.Failed -> event.msg.showToast()
                is FeedEvent.ClearGroupArticlesResultEvent.Failed -> event.msg.showToast()
                is FeedEvent.MuteFeedResultEvent.Failed -> event.msg.showToast()
                is FeedEvent.MuteFeedsInGroupResultEvent.Failed -> event.msg.showToast()

                is FeedEvent.EditFeedResultEvent.Success -> {
                    if (openEditFeedDialog != null) openEditFeedDialog = event.feed
                }

                is FeedEvent.EditGroupResultEvent.Success -> {
                    if (openEditGroupDialog != null) openEditGroupDialog = event.group
                }

                is FeedEvent.ClearFeedArticlesResultEvent.Success -> {
                    if (openEditFeedDialog != null) openEditFeedDialog = event.feed
                }

                is FeedEvent.MuteFeedResultEvent.Success -> {
                    openEditFeedDialog = openEditFeedDialog?.let { feedView ->
                        feedView.copy(feed = feedView.feed.copy(mute = event.mute))
                    }
                }

                is FeedEvent.ReadAllResultEvent.Success -> if (openEditFeedDialog != null) {
                    val newFeed = event.feeds.firstOrNull {
                        it.feed.url == openEditFeedDialog?.feed?.url
                    }
                    if (newFeed != null) openEditFeedDialog = newFeed
                }

                is FeedEvent.RefreshFeedResultEvent.Success -> if (openEditFeedDialog != null) {
                    val newFeed = event.feeds.firstOrNull {
                        it.feed.url == openEditFeedDialog?.feed?.url
                    }
                    if (newFeed != null) openEditFeedDialog = newFeed
                }

                is FeedEvent.AddFeedResultEvent.Success -> openEditFeedDialog = event.feed

                FeedEvent.RemoveFeedResultEvent.Success,
                FeedEvent.CreateGroupResultEvent.Success,
                FeedEvent.MoveFeedsToGroupResultEvent.Success,
                FeedEvent.ClearGroupArticlesResultEvent.Success,
                FeedEvent.MuteFeedsInGroupResultEvent.Success,
                FeedEvent.DeleteGroupResultEvent.Success -> Unit

            }
        }

        if (openAddDialog) {
            AddFeedDialog(
                url = addDialogUrl,
                onUrlChange = { text -> addDialogUrl = text },
                onConfirm = { newUrl ->
                    if (newUrl.isNotBlank()) {
                        dispatch(FeedIntent.AddFeed(url = newUrl))
                    }
                    addDialogUrl = ""
                    openAddDialog = false
                },
                onDismissRequest = {
                    addDialogUrl = ""
                    openAddDialog = false
                }
            )
        }

        if (openEditFeedDialog != null) {
            EditFeedSheet(
                onDismissRequest = { openEditFeedDialog = null },
                feedView = openEditFeedDialog!!,
                groups = uiState.groups.collectAsLazyPagingItems(),
                onReadAll = { dispatch(FeedIntent.ReadAllInFeed(it)) },
                onRefresh = { dispatch(FeedIntent.RefreshFeed(it)) },
                onMute = { feedUrl, mute -> dispatch(FeedIntent.MuteFeed(feedUrl, mute)) },
                onClear = { dispatch(FeedIntent.ClearFeedArticles(it)) },
                onDelete = { dispatch(FeedIntent.RemoveFeed(it)) },
                onUrlChange = {
                    dispatch(
                        FeedIntent.EditFeedUrl(
                            oldUrl = openEditFeedDialog!!.feed.url,
                            newUrl = it
                        )
                    )
                },
                onNicknameChange = {
                    dispatch(
                        FeedIntent.EditFeedNickname(
                            url = openEditFeedDialog!!.feed.url,
                            nickname = it
                        )
                    )
                },
                onCustomDescriptionChange = {
                    dispatch(
                        FeedIntent.EditFeedCustomDescription(
                            url = openEditFeedDialog!!.feed.url, customDescription = it,
                        )
                    )
                },
                onCustomIconChange = {
                    dispatch(
                        FeedIntent.EditFeedCustomIcon(
                            url = openEditFeedDialog!!.feed.url, customIcon = it,
                        )
                    )
                },
                onSortXmlArticlesOnUpdateChanged = {
                    dispatch(
                        FeedIntent.EditFeedSortXmlArticlesOnUpdate(
                            url = openEditFeedDialog!!.feed.url, sort = it,
                        )
                    )
                },
                onGroupChange = {
                    dispatch(
                        FeedIntent.EditFeedGroup(
                            url = openEditFeedDialog!!.feed.url,
                            groupId = it.groupId
                        )
                    )
                },
                openCreateGroupDialog = {
                    openCreateGroupDialog = true
                    createGroupDialogGroup = ""
                },
            )
        }

        if (openEditGroupDialog != null) {
            EditGroupSheet(
                onDismissRequest = { openEditGroupDialog = null },
                group = openEditGroupDialog!!,
                groups = uiState.groups.collectAsLazyPagingItems(),
                onReadAll = { dispatch(FeedIntent.ReadAllInGroup(it)) },
                onRefresh = { dispatch(FeedIntent.RefreshGroupFeed(it)) },
                onMuteAll = { groupId, mute ->
                    dispatch(FeedIntent.MuteFeedsInGroup(groupId, mute))
                },
                onClear = { dispatch(FeedIntent.ClearGroupArticles(it)) },
                onDelete = { dispatch(FeedIntent.DeleteGroup(it)) },
                onNameChange = {
                    dispatch(
                        FeedIntent.RenameGroup(groupId = openEditGroupDialog!!.groupId, name = it)
                    )
                },
                onMoveTo = {
                    dispatch(
                        FeedIntent.MoveFeedsToGroup(
                            fromGroupId = openEditGroupDialog!!.groupId,
                            toGroupId = it.groupId,
                        )
                    )
                },
                openCreateGroupDialog = {
                    openCreateGroupDialog = true
                    createGroupDialogGroup = ""
                },
            )
        }

        WaitingDialog(visible = uiState.loadingDialog)

        CreateGroupDialog(
            visible = openCreateGroupDialog,
            value = createGroupDialogGroup,
            onValueChange = { text -> createGroupDialogGroup = text },
            onCreateGroup = {
                dispatch(FeedIntent.CreateGroup(it))
                openCreateGroupDialog = false
            },
            onDismissRequest = {
                openCreateGroupDialog = false
            }
        )
    }
}

@Composable
private fun AddFeedDialog(
    url: String,
    onUrlChange: (String) -> Unit,
    onConfirm: (url: String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    PodAuraDialog(
        visible = true,
        icon = { Icon(imageVector = Icons.Outlined.RssFeed, contentDescription = null) },
        title = { Text(text = stringResource(id = R.string.add)) },
        onDismissRequest = onDismissRequest,
        selectable = false,
        text = {
            Column {
                val focusManager = LocalFocusManager.current
                val focusRequester = remember { FocusRequester() }
                val keyboard = LocalSoftwareKeyboardController.current
                ClipboardTextField(
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .fillMaxWidth(),
                    value = url,
                    onValueChange = onUrlChange,
                    autoRequestFocus = false,
                    label = stringResource(id = R.string.feed_screen_rss_url),
                    focusManager = focusManager,
                    imeAction = ImeAction.Next,
                    keyboardAction = { _, _ ->
                        focusManager.moveFocus(FocusDirection.Next)
                    }
                )

                LaunchedEffect(focusRequester) {
                    focusRequester.requestFocus()
                    awaitFrame()
                    keyboard?.show()
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = url.isNotBlank(),
                onClick = { onConfirm(url) }
            ) {
                Text(
                    text = stringResource(R.string.ok),
                    color = if (url.isNotBlank()) Color.Unspecified
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun CreateGroupDialog(
    visible: Boolean,
    value: String,
    onValueChange: (String) -> Unit,
    onCreateGroup: (GroupVo) -> Unit,
    onDismissRequest: () -> Unit,
) {
    TextFieldDialog(
        visible = visible,
        icon = { Icon(imageVector = Icons.Outlined.Workspaces, contentDescription = null) },
        titleText = stringResource(id = R.string.feed_screen_add_group),
        placeholder = stringResource(id = R.string.feed_group),
        value = value,
        onValueChange = onValueChange,
        onConfirm = { text ->
            onCreateGroup(
                GroupVo(
                    groupId = UUID.randomUUID().toString(),
                    name = text,
                    isExpanded = true,
                )
            )
        },
        onDismissRequest = onDismissRequest,
    )
}

@Composable
private fun FeedList(
    modifier: Modifier = Modifier,
    lazyPagingItems: LazyPagingItems<Any>,
    contentPadding: PaddingValues = PaddingValues(),
    fabPadding: Dp = 0.dp,
    selectedFeedUrls: List<String>? = null,
    selectedGroupIds: List<String>? = null,
    onShowArticleListByFeedUrls: (List<String>) -> Unit,
    onShowArticleListByGroupId: (String) -> Unit,
    onExpandChanged: (GroupVo, Boolean) -> Unit,
    onEditFeed: (FeedViewBean) -> Unit,
    onEditGroup: (GroupVo) -> Unit,
) {
    PagingRefreshStateIndicator(
        lazyPagingItems = lazyPagingItems,
        placeholderPadding = contentPadding,
    ) {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = contentPadding + PaddingValues(
                bottom = fabPadding,
                start = 16.dp,
                end = 16.dp,
            ),
        ) {
            items(
                count = lazyPagingItems.itemCount,
                key = lazyPagingItems.safeItemKey { item ->
                    when (item) {
                        is GroupVo.DefaultGroup -> item.groupId
                        is GroupVo -> item.groupId
                        is FeedViewBean -> item.feed.url
                        else -> item
                    }
                },
            ) { index ->
                Box(modifier = Modifier.animateItem()) {
                    when (val item = lazyPagingItems[index]) {
                        is GroupVo -> Group1Item(
                            index = index,
                            data = item,
                            onExpandChange = onExpandChanged,
                            isEmpty = { it == lazyPagingItems.lastIndex || lazyPagingItems[it + 1] is GroupVo },
                            onShowAllArticles = { group -> onShowArticleListByGroupId(group.groupId) },
                            onEdit = onEditGroup,
                        )

                        is FeedViewBean -> Feed1Item(
                            data = item,
                            selected = selectedFeedUrls != null && item.feed.url in selectedFeedUrls ||
                                    selectedGroupIds != null &&
                                    (item.feed.groupId ?: DEFAULT_GROUP_ID) in selectedGroupIds,
                            inGroup = true,
                            isEnd = index == lazyPagingItems.lastIndex || lazyPagingItems[index + 1] is GroupVo,
                            onClick = { onShowArticleListByFeedUrls(listOf(it.url)) },
                            onEdit = onEditFeed,
                        )

                        else -> Feed1ItemPlaceholder()
                    }
                }
            }
        }
    }
}

@Composable
private fun MoreMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
) {
    val navController = LocalNavController.current
    DropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest) {
        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.reorder_group_screen_name)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Sort,
                    contentDescription = null,
                )
            },
            onClick = {
                onDismissRequest()
                navController.navigate(REORDER_GROUP_SCREEN_ROUTE)
            },
        )
        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.mute_feed_screen_name)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.VolumeOff,
                    contentDescription = null,
                )
            },
            onClick = {
                onDismissRequest()
                navController.navigate(MUTE_FEED_SCREEN_ROUTE)
            },
        )
        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.feed_style_screen_name)) },
            onClick = {
                onDismissRequest()
                navController.navigate(FEED_STYLE_SCREEN_ROUTE)
            },
        )
    }
}