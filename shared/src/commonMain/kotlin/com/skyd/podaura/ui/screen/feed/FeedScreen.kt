package com.skyd.podaura.ui.screen.feed

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.UnfoldLess
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material.icons.outlined.Workspaces
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalAbsoluteTonalElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.rememberPaneExpansionState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.skyd.compone.component.ComponeFloatingActionButton
import com.skyd.compone.component.ComponeIconButton
import com.skyd.compone.component.ComponeTopBar
import com.skyd.compone.component.ComponeTopBarStyle
import com.skyd.compone.component.dialog.WaitingDialog
import com.skyd.compone.ext.isDetailPaneVisible
import com.skyd.compone.ext.isSinglePane
import com.skyd.compone.ext.plus
import com.skyd.compone.local.LocalGlobalNavController
import com.skyd.compone.local.LocalNavController
import com.skyd.mvi.MviEventListener
import com.skyd.mvi.getDispatcher
import com.skyd.podaura.ext.isCompact
import com.skyd.podaura.ext.lastIndex
import com.skyd.podaura.ext.safeItemKey
import com.skyd.podaura.model.bean.feed.FeedViewBean
import com.skyd.podaura.model.bean.group.GroupVo
import com.skyd.podaura.model.bean.group.GroupVo.Companion.DEFAULT_GROUP_ID
import com.skyd.podaura.model.preference.appearance.feed.FeedListTonalElevationPreference
import com.skyd.podaura.model.preference.appearance.feed.FeedTopBarTonalElevationPreference
import com.skyd.podaura.ui.component.NavigableListDetailPaneScaffold
import com.skyd.podaura.ui.component.PagingRefreshStateIndicator
import com.skyd.podaura.ui.component.PodAuraAnimatedPane
import com.skyd.podaura.ui.component.dialog.TextFieldDialog
import com.skyd.podaura.ui.component.rememberListDetailPaneScaffoldNavigator
import com.skyd.podaura.ui.local.LocalWindowSizeClass
import com.skyd.podaura.ui.screen.article.ArticleRoute
import com.skyd.podaura.ui.screen.calendar.portrait.CalendarRoute
import com.skyd.podaura.ui.screen.feed.item.Feed1Item
import com.skyd.podaura.ui.screen.feed.item.Feed1ItemPlaceholder
import com.skyd.podaura.ui.screen.feed.item.Group1Item
import com.skyd.podaura.ui.screen.feed.mute.MuteFeedRoute
import com.skyd.podaura.ui.screen.feed.reorder.feed.ReorderFeedRoute
import com.skyd.podaura.ui.screen.feed.reorder.group.ReorderGroupRoute
import com.skyd.podaura.ui.screen.search.SearchRoute
import com.skyd.podaura.ui.screen.settings.appearance.feed.FeedStyleRoute
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.add
import podaura.shared.generated.resources.calendar_screen_name
import podaura.shared.generated.resources.collapse_all_groups
import podaura.shared.generated.resources.expand_all_groups
import podaura.shared.generated.resources.feed_group
import podaura.shared.generated.resources.feed_screen_add_group
import podaura.shared.generated.resources.feed_screen_all_articles
import podaura.shared.generated.resources.feed_screen_name
import podaura.shared.generated.resources.feed_screen_rss_url
import podaura.shared.generated.resources.feed_screen_search_feed
import podaura.shared.generated.resources.feed_style_screen_name
import podaura.shared.generated.resources.more
import podaura.shared.generated.resources.mute_feed_screen_name
import podaura.shared.generated.resources.reorder_group_screen_name
import kotlin.uuid.Uuid


@Serializable
data object FeedRoute

@Composable
fun FeedScreen() {
    val navigator = rememberListDetailPaneScaffoldNavigator<ArticleRoute>(
        scaffoldDirective = calculatePaneScaffoldDirective(currentWindowAdaptiveInfo()).copy(
            horizontalPartitionSpacerSize = 0.dp,
        )
    )
    val paneExpansionState = rememberPaneExpansionState()
    LaunchedEffect(Unit) {
        paneExpansionState.setFirstPaneProportion(0.335f)
    }

    val scope = rememberCoroutineScope()
    val globalNavController = LocalGlobalNavController.current
    var nestedNavKey by remember { mutableStateOf(Uuid.random()) }
    val navController = key(nestedNavKey) { rememberNavController() }
    val windowSizeClass = LocalWindowSizeClass.current

    var currentRoute by remember { mutableStateOf(ArticleRoute()) }
    val onNavigate: (ArticleRoute) -> Unit = {
        if (navigator.isDetailPaneVisible) {
            // If the detail pane was visible, then use the nestedNavController navigate call directly
            // else recreate the NavHost to avoid invoke navigate on a NavController without graph
            if (navController.currentDestination != null) {
                navController.navigate(it) { popUpTo(currentRoute) { inclusive = true } }
            } else {
                nestedNavKey = Uuid.random()
            }
        } else {
            // Otherwise, recreate the NavHost entirely, and start at the new destination
            nestedNavKey = Uuid.random()
        }
        currentRoute = it
        scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail) }
    }
    LaunchedEffect(navigator.isSinglePane) {
        if (!navigator.isSinglePane) onNavigate(currentRoute)
    }

    NavigableListDetailPaneScaffold(
        navigator = navigator,
        listPane = {
            PodAuraAnimatedPane {
                FeedList(
                    listPaneSelectedFeedUrls = currentRoute.feedUrls.takeIf { !navigator.isSinglePane },
                    listPaneSelectedGroupIds = currentRoute.groupIds.takeIf { !navigator.isSinglePane },
                    onShowArticleListByFeedUrls = { feedUrls ->
                        val route = ArticleRoute(feedUrls = feedUrls)
                        if (navigator.isDetailPaneVisible || !windowSizeClass.isCompact) {
                            onNavigate(route)
                        } else {
                            globalNavController.navigate(route)
                        }
                    },
                    onShowArticleListByGroupId = { groupId ->
                        val route = ArticleRoute(groupIds = listOf(groupId))
                        if (navigator.isDetailPaneVisible || !windowSizeClass.isCompact) {
                            onNavigate(route)
                        } else {
                            globalNavController.navigate(route)
                        }
                    }
                )
            }
        },
        detailPane = {
            PodAuraAnimatedPane {
                // https://issuetracker.google.com/issues/334146670
                key(nestedNavKey) {
                    CompositionLocalProvider(LocalNavController provides navController) {
                        FeedDetailPaneNavHost(
                            navController = navController,
                            startDestination = currentRoute,
                            onPaneBack = if (navigator.isSinglePane) {
                                {
                                    scope.launch { navigator.navigateBack() }
                                }
                            } else null,
                            articleRoute = currentRoute,
                        )
                    }
                }
            }
        },
        paneExpansionState = paneExpansionState,
    )
}

@Composable
private fun FeedList(
    listPaneSelectedFeedUrls: List<String>? = null,
    listPaneSelectedGroupIds: List<String>? = null,
    onShowArticleListByFeedUrls: (List<String>) -> Unit,
    onShowArticleListByGroupId: (String) -> Unit,
    viewModel: FeedViewModel = koinViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val navController = LocalNavController.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val bottomSheetSnackbarHostState = remember { SnackbarHostState() }
    val windowSizeClass = LocalWindowSizeClass.current
    var openMoreMenu by rememberSaveable { mutableStateOf(false) }
    var openAddDialog by rememberSaveable { mutableStateOf(false) }
    var addDialogUrl by rememberSaveable { mutableStateOf("") }

    var openCreateGroupDialog by rememberSaveable { mutableStateOf(false) }
    var createGroupDialogGroup by rememberSaveable { mutableStateOf("") }

    var fabHeight by remember { mutableStateOf(0.dp) }

    val uiState by viewModel.viewState.collectAsStateWithLifecycle()
    val dispatch = viewModel.getDispatcher(startWith = FeedIntent.Init)

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            ComponeTopBar(
                style = ComponeTopBarStyle.Small,
                title = { Text(text = stringResource(Res.string.feed_screen_name)) },
                actions = {
                    ComponeIconButton(
                        onClick = { navController.navigate(SearchRoute.Feed) },
                        imageVector = Icons.Outlined.Search,
                        contentDescription = stringResource(Res.string.feed_screen_search_feed),
                    )
                    ComponeIconButton(
                        onClick = { navController.navigate(CalendarRoute) },
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = stringResource(Res.string.calendar_screen_name),
                    )
                    ComponeIconButton(
                        onClick = { openMoreMenu = true },
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = stringResource(Res.string.more),
                    )
                    MoreMenu(
                        expanded = openMoreMenu,
                        allGroupCollapsed = uiState.allGroupCollapsed,
                        onShowAllArticles = { onShowArticleListByFeedUrls(emptyList()) },
                        onCollapseAllGroup = { dispatch(FeedIntent.CollapseAllGroup(it)) },
                        onDismissRequest = { openMoreMenu = false },
                    )
                },
                navigationIcon = {},
                windowInsets =
                    if (windowSizeClass.isCompact)
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                    else
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Top),
                colors = TopAppBarDefaults.topAppBarColors().copy(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                        FeedTopBarTonalElevationPreference.current.dp
                    ),
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                        FeedTopBarTonalElevationPreference.current.dp + 4.dp
                    ),
                ),
                scrollBehavior = scrollBehavior,
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
                WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical),
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
            LocalAbsoluteTonalElevation.current +
                    FeedListTonalElevationPreference.current.dp
        ),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) { innerPadding ->
        val bottomSheetShowing by remember {
            derivedStateOf { uiState.editGroupDialogBean != null || uiState.editFeedDialogBean != null }
        }
        val currentSnackbarHostState by rememberUpdatedState(
            if (bottomSheetShowing) bottomSheetSnackbarHostState else snackbarHostState
        )
        LaunchedEffect(bottomSheetShowing) {
            if (!bottomSheetShowing) {
                bottomSheetSnackbarHostState.currentSnackbarData?.dismiss()
            }
        }
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
                onEditFeed = { feed -> dispatch(FeedIntent.OnEditFeedDialog(feed)) },
                onEditGroup = { group -> dispatch(FeedIntent.OnEditGroupDialog(group)) },
            )
        }

        MviEventListener(viewModel.singleEvent) { event ->
            when (event) {
                is FeedEvent.AddFeedResultEvent.Failed ->
                    currentSnackbarHostState.showSnackbar(event.msg)

                is FeedEvent.InitFeetListResultEvent.Failed ->
                    currentSnackbarHostState.showSnackbar(event.msg)

                is FeedEvent.CollapseAllGroupResultEvent.Failed ->
                    currentSnackbarHostState.showSnackbar(event.msg)

                is FeedEvent.EditFeedResultEvent.Failed ->
                    currentSnackbarHostState.showSnackbar(event.msg)

                is FeedEvent.RemoveFeedResultEvent.Failed ->
                    currentSnackbarHostState.showSnackbar(event.msg)

                is FeedEvent.RefreshFeedResultEvent.Failed ->
                    currentSnackbarHostState.showSnackbar(event.msg)

                is FeedEvent.CreateGroupResultEvent.Failed ->
                    currentSnackbarHostState.showSnackbar(event.msg)

                is FeedEvent.MoveFeedsToGroupResultEvent.Failed ->
                    currentSnackbarHostState.showSnackbar(event.msg)

                is FeedEvent.DeleteGroupResultEvent.Failed ->
                    currentSnackbarHostState.showSnackbar(event.msg)

                is FeedEvent.EditGroupResultEvent.Failed ->
                    currentSnackbarHostState.showSnackbar(event.msg)

                is FeedEvent.ReadAllResultEvent.Failed ->
                    currentSnackbarHostState.showSnackbar(event.msg)

                is FeedEvent.ClearFeedArticlesResultEvent.Failed ->
                    currentSnackbarHostState.showSnackbar(event.msg)

                is FeedEvent.ClearGroupArticlesResultEvent.Failed ->
                    currentSnackbarHostState.showSnackbar(event.msg)

                is FeedEvent.MuteFeedResultEvent.Failed ->
                    currentSnackbarHostState.showSnackbar(event.msg)

                is FeedEvent.MuteFeedsInGroupResultEvent.Failed ->
                    currentSnackbarHostState.showSnackbar(event.msg)

                else -> Unit
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

        uiState.editFeedDialogBean?.let { feedView ->
            EditFeedSheet(
                onDismissRequest = { dispatch(FeedIntent.OnEditFeedDialog(null)) },
                snackbarHost = { SnackbarHost(hostState = bottomSheetSnackbarHostState) },
                feedView = feedView,
                groups = uiState.groups.collectAsLazyPagingItems(),
                onReadAll = { dispatch(FeedIntent.ReadAllInFeed(it)) },
                onRefresh = { feedUrl, full -> dispatch(FeedIntent.RefreshFeed(feedUrl, full)) },
                onMute = { feedUrl, mute -> dispatch(FeedIntent.MuteFeed(feedUrl, mute)) },
                onClear = { dispatch(FeedIntent.ClearFeedArticles(it)) },
                onDelete = { dispatch(FeedIntent.RemoveFeed(it)) },
                onUrlChange = {
                    dispatch(FeedIntent.EditFeedUrl(oldUrl = feedView.feed.url, newUrl = it))
                },
                onNicknameChange = {
                    dispatch(FeedIntent.EditFeedNickname(url = feedView.feed.url, nickname = it))
                },
                onCustomDescriptionChange = {
                    dispatch(
                        FeedIntent.EditFeedCustomDescription(
                            url = feedView.feed.url, customDescription = it,
                        )
                    )
                },
                onCustomIconChange = {
                    dispatch(
                        FeedIntent.EditFeedCustomIcon(
                            url = feedView.feed.url, customIcon = it,
                        )
                    )
                },
                onSortXmlArticlesOnUpdateChanged = {
                    dispatch(
                        FeedIntent.EditFeedSortXmlArticlesOnUpdate(
                            url = feedView.feed.url, sort = it,
                        )
                    )
                },
                onGroupChange = {
                    dispatch(
                        FeedIntent.EditFeedGroup(url = feedView.feed.url, groupId = it.groupId)
                    )
                },
                openCreateGroupDialog = {
                    openCreateGroupDialog = true
                    createGroupDialogGroup = ""
                },
                onMessage = { scope.launch { currentSnackbarHostState.showSnackbar(it) } }
            )
        }

        uiState.editGroupDialogBean?.let { group ->
            EditGroupSheet(
                onDismissRequest = { dispatch(FeedIntent.OnEditGroupDialog(null)) },
                snackbarHost = { SnackbarHost(hostState = bottomSheetSnackbarHostState) },
                group = group,
                groups = uiState.groups.collectAsLazyPagingItems(),
                onReadAll = { dispatch(FeedIntent.ReadAllInGroup(it)) },
                onRefresh = { groupId, full ->
                    dispatch(FeedIntent.RefreshGroupFeed(groupId, full))
                },
                onMuteAll = { groupId, mute ->
                    dispatch(FeedIntent.MuteFeedsInGroup(groupId, mute))
                },
                onClear = { dispatch(FeedIntent.ClearGroupArticles(it)) },
                onDelete = { dispatch(FeedIntent.DeleteGroup(it)) },
                onNameChange = {
                    dispatch(
                        FeedIntent.RenameGroup(groupId = group.groupId, name = it)
                    )
                },
                onMoveTo = {
                    dispatch(
                        FeedIntent.MoveFeedsToGroup(
                            fromGroupId = group.groupId,
                            toGroupId = it.groupId,
                        )
                    )
                },
                onReorderFeedsInGroup = { navController.navigate(ReorderFeedRoute(groupId = it)) },
                openCreateGroupDialog = {
                    openCreateGroupDialog = true
                    createGroupDialogGroup = ""
                },
                onMessage = { scope.launch { currentSnackbarHostState.showSnackbar(it) } }
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
    TextFieldDialog(
        icon = { Icon(imageVector = Icons.Outlined.RssFeed, contentDescription = null) },
        titleText = stringResource(Res.string.add),
        placeholder = stringResource(Res.string.feed_screen_rss_url),
        value = url,
        onValueChange = onUrlChange,
        onDismissRequest = onDismissRequest,
        onConfirm = onConfirm,
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
        titleText = stringResource(Res.string.feed_screen_add_group),
        placeholder = stringResource(Res.string.feed_group),
        value = value,
        onValueChange = onValueChange,
        onConfirm = { text ->
            onCreateGroup(
                GroupVo(
                    groupId = Uuid.random().toString(),
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
            modifier = modifier
                .fillMaxSize()
                .testTag("FeedLazyColumn"),
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
    allGroupCollapsed: Boolean,
    onShowAllArticles: () -> Unit,
    onCollapseAllGroup: (Boolean) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val navController = LocalNavController.current
    DropdownMenuPopup(expanded = expanded, onDismissRequest = onDismissRequest) {
        val texts = listOf(
            listOf(
                stringResource(Res.string.feed_screen_all_articles),
            ),
            listOf(
                stringResource(
                    if (allGroupCollapsed) Res.string.expand_all_groups
                    else Res.string.collapse_all_groups
                ),
                stringResource(Res.string.reorder_group_screen_name),
                stringResource(Res.string.mute_feed_screen_name),
                stringResource(Res.string.feed_style_screen_name),
            )
        )
        val leadingIcons = listOf(
            listOf(Icons.AutoMirrored.Outlined.Article),
            listOf(
                if (allGroupCollapsed) Icons.Outlined.UnfoldMore else Icons.Outlined.UnfoldLess,
                Icons.AutoMirrored.Outlined.Sort,
                Icons.AutoMirrored.Outlined.VolumeOff,
                null,
            )
        )
        val onClicks = listOf(
            listOf(
                {
                    onDismissRequest()
                    onShowAllArticles()
                }
            ),
            listOf(
                {
                    onDismissRequest()
                    onCollapseAllGroup(!allGroupCollapsed)
                },
                {
                    onDismissRequest()
                    navController.navigate(ReorderGroupRoute)
                },
                {
                    onDismissRequest()
                    navController.navigate(MuteFeedRoute)
                },
                {
                    onDismissRequest()
                    navController.navigate(FeedStyleRoute)
                },
            )
        )

        texts.forEachIndexed { groupIndex, subTexts ->
            DropdownMenuGroup(shapes = MenuDefaults.groupShape(groupIndex, texts.size)) {
                subTexts.forEachIndexed { index, text ->
                    DropdownMenuItem(
                        text = { Text(text = text) },
                        shape = MenuDefaults.itemShape(index, subTexts.size).shape,
                        leadingIcon = leadingIcons[groupIndex][index]?.let { icon ->
                            {
                                Icon(imageVector = icon, contentDescription = null)
                            }
                        },
                        onClick = onClicks[groupIndex][index],
                    )
                }
            }
            Spacer(Modifier.height(MenuDefaults.GroupSpacing))
        }
    }
}
