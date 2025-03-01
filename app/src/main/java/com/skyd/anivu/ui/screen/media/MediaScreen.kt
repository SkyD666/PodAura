package com.skyd.anivu.ui.screen.media

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Workspaces
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skyd.anivu.R
import com.skyd.anivu.base.mvi.MviEventListener
import com.skyd.anivu.base.mvi.getDispatcher
import com.skyd.anivu.ext.activity
import com.skyd.anivu.ext.isCompact
import com.skyd.anivu.model.bean.MediaGroupBean
import com.skyd.anivu.model.preference.behavior.media.BaseMediaListSortByPreference
import com.skyd.anivu.model.preference.behavior.media.MediaListSortAscPreference
import com.skyd.anivu.model.preference.behavior.media.MediaListSortByPreference
import com.skyd.anivu.model.preference.data.medialib.MediaLibLocationPreference
import com.skyd.anivu.ui.activity.player.PlayActivity
import com.skyd.anivu.ui.component.PodAuraFloatingActionButton
import com.skyd.anivu.ui.component.PodAuraIconButton
import com.skyd.anivu.ui.component.PodAuraTopBar
import com.skyd.anivu.ui.component.PodAuraTopBarStyle
import com.skyd.anivu.ui.component.dialog.SortDialog
import com.skyd.anivu.ui.component.dialog.TextFieldDialog
import com.skyd.anivu.ui.component.dialog.WaitingDialog
import com.skyd.anivu.ui.local.LocalMediaListSortAsc
import com.skyd.anivu.ui.local.LocalMediaListSortBy
import com.skyd.anivu.ui.local.LocalMediaShowGroupTab
import com.skyd.anivu.ui.local.LocalNavController
import com.skyd.anivu.ui.local.LocalWindowSizeClass
import com.skyd.anivu.ui.mpv.resolveUri
import com.skyd.anivu.ui.screen.filepicker.ListenToFilePicker
import com.skyd.anivu.ui.screen.filepicker.openFilePicker
import com.skyd.anivu.ui.screen.media.list.GroupInfo
import com.skyd.anivu.ui.screen.media.list.MediaList
import com.skyd.anivu.ui.screen.settings.appearance.media.MEDIA_STYLE_SCREEN_ROUTE
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.min

const val MEDIA_SCREEN_ROUTE = "mediaScreen"

@Composable
fun MediaScreen(path: String, viewModel: MediaViewModel = hiltViewModel()) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val navController = LocalNavController.current
    val context = LocalContext.current
    val windowSizeClass = LocalWindowSizeClass.current
    val scope = rememberCoroutineScope()

    var fabHeight by remember { mutableStateOf(0.dp) }
    var fabWidth by remember { mutableStateOf(0.dp) }

    val dispatch = viewModel.getDispatcher(key1 = path, startWith = MediaIntent.Init(path = path))
    val uiState by viewModel.viewState.collectAsStateWithLifecycle()

    val pagerState = rememberPagerState(pageCount = { uiState.groups.size })
    var openEditGroupDialog by rememberSaveable { mutableStateOf<MediaGroupBean?>(value = null) }
    var openMoreMenu by rememberSaveable { mutableStateOf(false) }
    var showSortMediaDialog by rememberSaveable { mutableStateOf(false) }

    ListenToFilePicker { result ->
        if (result.pickFolder) {
            MediaLibLocationPreference.put(context, this, result.result)
        } else {
            val url = File(result.result).toUri().resolveUri(context)
            if (url != null) {
                PlayActivity.playMediaList(
                    activity = context.activity,
                    startMediaPath = url,
                    mediaList = listOf(
                        PlayActivity.PlayMediaListItem(
                            path = url,
                            articleId = null,
                            title = null,
                            thumbnail = null,
                        )
                    ),
                )
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            PodAuraTopBar(
                style = PodAuraTopBarStyle.Small,
                title = {
                    val title = stringResource(R.string.media_screen_name)
                    Text(
                        modifier = Modifier.basicMarquee(),
                        text = if (LocalMediaShowGroupTab.current) title else {
                            val groupName = uiState.groups
                                .getOrNull(pagerState.currentPage)?.first?.name
                            if (groupName.isNullOrBlank()) title else "$title - $groupName"
                        },
                        maxLines = 1,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    navigationIconContentColor = TopAppBarDefaults.topAppBarColors().actionIconContentColor
                ),
                navigationIcon = {},
                scrollBehavior = scrollBehavior,
                windowInsets = WindowInsets.safeDrawing.run {
                    var sides = WindowInsetsSides.Top + WindowInsetsSides.Right
                    if (windowSizeClass.isCompact) sides += WindowInsetsSides.Left
                    only(sides)
                },
                actions = {
                    PodAuraIconButton(
                        onClick = {
                            openFilePicker(navController = navController, path = path)
                        },
                        imageVector = Icons.Outlined.MyLocation,
                        contentDescription = stringResource(id = R.string.data_screen_media_lib_location),
                    )
                    PodAuraIconButton(
                        onClick = { showSortMediaDialog = true },
                        imageVector = Icons.AutoMirrored.Outlined.Sort,
                        contentDescription = stringResource(id = R.string.sort),
                    )
                    PodAuraIconButton(
                        onClick = { openMoreMenu = true },
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = stringResource(R.string.more),
                    )
                    MoreMenu(
                        expanded = openMoreMenu,
                        onDismissRequest = { openMoreMenu = false },
                        onRefresh = { dispatch(MediaIntent.RefreshGroup(path)) },
                    )
                }
            )
        },
        floatingActionButton = {
            val density = LocalDensity.current
            Column(
                modifier = Modifier.onSizeChanged {
                    with(density) {
                        fabWidth = it.width.toDp() + 16.dp
                        fabHeight = it.height.toDp() + 16.dp
                    }
                },
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                SmallFloatingActionButton(
                    onClick = {
                        openFilePicker(
                            navController = navController,
                            path = path,
                            pickFolder = false,
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.secondary,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FileOpen,
                        contentDescription = stringResource(id = R.string.open_file),
                    )
                }
                PodAuraFloatingActionButton(
                    onClick = {
                        openEditGroupDialog = uiState.groups[pagerState.currentPage].first
                    },
                    contentDescription = stringResource(R.string.edit),
                ) {
                    Icon(imageVector = Icons.Outlined.Edit, contentDescription = null)
                }
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing.run {
            val leftPadding = windowSizeClass.isCompact
            val bottomPadding = !windowSizeClass.isCompact
            var sides = WindowInsetsSides.Top + WindowInsetsSides.Right
            if (leftPadding) sides += WindowInsetsSides.Left
            if (bottomPadding) sides += WindowInsetsSides.Bottom
            only(sides)
        },
    ) { innerPadding ->
        var openCreateGroupDialog by rememberSaveable { mutableStateOf(false) }
        var createGroupDialogGroup by rememberSaveable { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (uiState.groups.isNotEmpty()) {
                if (LocalMediaShowGroupTab.current) {
                    PrimaryScrollableTabRow(
                        modifier = Modifier.fillMaxWidth(),
                        selectedTabIndex = min(uiState.groups.size - 1, pagerState.currentPage),
                        edgePadding = 0.dp,
                        divider = {},
                    ) {
                        uiState.groups.forEachIndexed { index, group ->
                            Tab(
                                selected = pagerState.currentPage == index,
                                onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                                text = {
                                    Text(
                                        modifier = Modifier
                                            .widthIn(max = 220.dp)
                                            .basicMarquee(iterations = Int.MAX_VALUE),
                                        text = group.first.name,
                                        maxLines = 1,
                                    )
                                },
                            )
                        }
                    }
                    HorizontalDivider()
                }

                HorizontalPager(state = pagerState) { index ->
                    MediaList(
                        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                        fabPadding = PaddingValues(bottom = fabHeight),
                        path = path,
                        isSubList = false,
                        groupInfo = GroupInfo(
                            group = uiState.groups[index].first,
                            version = uiState.groups[index].second,
                            onCreateGroup = { dispatch(MediaIntent.CreateGroup(path, it)) },
                            onMoveFileToGroup = { video, newGroup ->
                                dispatch(MediaIntent.ChangeMediaGroup(path, video, newGroup))
                            },
                        ),
                    )
                }
            }
        }

        if (openEditGroupDialog != null) {
            EditMediaGroupSheet(
                onDismissRequest = { openEditGroupDialog = null },
                group = openEditGroupDialog!!,
                groups = remember(uiState.groups) { uiState.groups.map { it.first } },
                onDelete = {
                    dispatch(MediaIntent.DeleteGroup(path, it))
                    openEditGroupDialog = null
                },
                onNameChange = {
                    dispatch(MediaIntent.RenameGroup(path, openEditGroupDialog!!, it))
                },
                onMoveTo = {
                    dispatch(MediaIntent.MoveFilesToGroup(path, openEditGroupDialog!!, it))
                },
                openCreateGroupDialog = { openCreateGroupDialog = true },
            )
        }

        CreateGroupDialog(
            visible = openCreateGroupDialog,
            value = createGroupDialogGroup,
            onValueChange = { text -> createGroupDialogGroup = text },
            onCreateGroup = {
                dispatch(MediaIntent.CreateGroup(path, it))
                openCreateGroupDialog = false
                createGroupDialogGroup = ""
            },
            onDismissRequest = {
                openCreateGroupDialog = false
                createGroupDialogGroup = ""
            }
        )

        MviEventListener(viewModel.singleEvent) { event ->
            when (event) {
                is MediaEvent.CreateGroupResultEvent.Failed ->
                    snackbarHostState.showSnackbar(event.msg)

                is MediaEvent.DeleteGroupResultEvent.Failed ->
                    snackbarHostState.showSnackbar(event.msg)

                is MediaEvent.EditGroupResultEvent.Failed ->
                    snackbarHostState.showSnackbar(event.msg)

                is MediaEvent.MoveFilesToGroupResultEvent.Failed ->
                    snackbarHostState.showSnackbar(event.msg)

                is MediaEvent.ChangeFileGroupResultEvent.Failed ->
                    snackbarHostState.showSnackbar(event.msg)

                is MediaEvent.EditGroupResultEvent.Success -> {
                    dispatch(MediaIntent.RefreshGroup(path = path))
                    if (openEditGroupDialog != null) openEditGroupDialog = event.group
                }

                is MediaEvent.CreateGroupResultEvent.Success,
                is MediaEvent.DeleteGroupResultEvent.Success,
                is MediaEvent.ChangeFileGroupResultEvent.Success,
                is MediaEvent.MoveFilesToGroupResultEvent.Success ->
                    dispatch(MediaIntent.RefreshGroup(path = path))
            }
        }
    }

    SortDialog(
        visible = showSortMediaDialog,
        onDismissRequest = { showSortMediaDialog = false },
        sortByValues = MediaListSortByPreference.values,
        sortBy = LocalMediaListSortBy.current,
        sortAsc = LocalMediaListSortAsc.current,
        onSortBy = { MediaListSortByPreference.put(context, scope, it) },
        onSortAsc = { MediaListSortAscPreference.put(context, scope, it) },
        onSortByDisplayName = { BaseMediaListSortByPreference.toDisplayName(context, it) },
        onSortByIcon = { BaseMediaListSortByPreference.toIcon(it) },
    )

    WaitingDialog(visible = uiState.loadingDialog)
}

@Composable
internal fun CreateGroupDialog(
    visible: Boolean,
    value: String,
    onValueChange: (String) -> Unit,
    onCreateGroup: (MediaGroupBean) -> Unit,
    onDismissRequest: () -> Unit,
) {
    TextFieldDialog(
        visible = visible,
        icon = { Icon(imageVector = Icons.Outlined.Workspaces, contentDescription = null) },
        titleText = stringResource(id = R.string.media_screen_add_group),
        placeholder = stringResource(id = R.string.media_group),
        maxLines = 1,
        value = value,
        onValueChange = onValueChange,
        onConfirm = { text -> onCreateGroup(MediaGroupBean(name = text)) },
        onDismissRequest = onDismissRequest,
    )
}

@Composable
private fun MoreMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onRefresh: () -> Unit,
) {
    val navController = LocalNavController.current
    DropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest) {
        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.media_screen_refresh_group)) },
            leadingIcon = {
                Icon(imageVector = Icons.Outlined.Refresh, contentDescription = null)
            },
            onClick = {
                onRefresh()
                onDismissRequest()
            },
        )
        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.media_screen_style)) },
            leadingIcon = {
                Icon(imageVector = Icons.Outlined.Palette, contentDescription = null)
            },
            onClick = {
                onDismissRequest()
                navController.navigate(MEDIA_STYLE_SCREEN_ROUTE)
            },
        )
    }
}