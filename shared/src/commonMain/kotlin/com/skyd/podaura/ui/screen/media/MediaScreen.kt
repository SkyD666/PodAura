package com.skyd.podaura.ui.screen.media

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Workspaces
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skyd.compone.component.ComponeFloatingActionButton
import com.skyd.compone.component.ComponeIconButton
import com.skyd.compone.component.ComponeTopBar
import com.skyd.compone.component.ComponeTopBarStyle
import com.skyd.compone.component.dialog.WaitingDialog
import com.skyd.compone.local.LocalNavController
import com.skyd.mvi.MviEventListener
import com.skyd.mvi.getDispatcher
import com.skyd.podaura.ext.isCompact
import com.skyd.podaura.model.bean.MediaGroupBean
import com.skyd.podaura.model.preference.appearance.media.MediaShowGroupTabPreference
import com.skyd.podaura.model.preference.behavior.media.BaseMediaListSortByPreference
import com.skyd.podaura.model.preference.behavior.media.MediaListSortAscPreference
import com.skyd.podaura.model.preference.behavior.media.MediaListSortByPreference
import com.skyd.podaura.model.preference.data.medialib.MediaLibLocationPreference
import com.skyd.podaura.ui.component.LongClickListener
import com.skyd.podaura.ui.component.dialog.SortDialog
import com.skyd.podaura.ui.component.dialog.TextFieldDialog
import com.skyd.podaura.ui.local.LocalWindowSizeClass
import com.skyd.podaura.ui.player.jumper.PlayDataMode
import com.skyd.podaura.ui.player.jumper.rememberPlayerJumper
import com.skyd.podaura.ui.player.resolveToPlayer
import com.skyd.podaura.ui.screen.filepicker.FilePickerRoute
import com.skyd.podaura.ui.screen.filepicker.ListenToFilePicker
import com.skyd.podaura.ui.screen.media.list.GroupInfo
import com.skyd.podaura.ui.screen.media.list.MediaList
import com.skyd.podaura.ui.screen.media.search.MediaSearchRoute
import com.skyd.podaura.ui.screen.settings.appearance.media.MediaStyleRoute
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.data_screen_change_lib_location
import podaura.shared.generated.resources.media_group
import podaura.shared.generated.resources.media_screen_add_group
import podaura.shared.generated.resources.media_screen_name
import podaura.shared.generated.resources.media_screen_refresh_group
import podaura.shared.generated.resources.media_screen_search_hint
import podaura.shared.generated.resources.media_screen_style
import podaura.shared.generated.resources.more
import podaura.shared.generated.resources.open_file
import podaura.shared.generated.resources.sort
import kotlin.math.min


@Serializable
data object MediaRoute

@Composable
fun MediaScreen(path: String, viewModel: MediaViewModel = koinViewModel()) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val navController = LocalNavController.current
    val windowSizeClass = LocalWindowSizeClass.current
    val scope = rememberCoroutineScope()

    var fabHeight by remember { mutableStateOf(0.dp) }

    val dispatch = viewModel.getDispatcher(key1 = path, startWith = MediaIntent.Init(path = path))
    val uiState by viewModel.viewState.collectAsStateWithLifecycle()

    val pagerState = rememberPagerState(pageCount = { uiState.groups.size })
    var openMoreMenu by rememberSaveable { mutableStateOf(false) }
    var showSortMediaDialog by rememberSaveable { mutableStateOf(false) }
    val playerJumper = rememberPlayerJumper()

    ListenToFilePicker { result ->
        if (result.pickFolder) {
            MediaLibLocationPreference.put(this, result.result)
        } else {
            val url = PlatformFile(result.result).resolveToPlayer()
            if (url != null) {
                playerJumper.jump(
                    PlayDataMode.MediaLibraryList(
                        startMediaPath = url,
                        mediaList = listOf(
                            PlayDataMode.MediaLibraryList.PlayMediaListItem(
                                path = url,
                                articleId = null,
                                title = null,
                                thumbnail = null,
                            )
                        ),
                    )
                )
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            ComponeTopBar(
                style = ComponeTopBarStyle.Small,
                title = {
                    val title = stringResource(Res.string.media_screen_name)
                    Text(
                        modifier = Modifier.basicMarquee(),
                        text = if (MediaShowGroupTabPreference.current) title else {
                            val groupName = uiState.groups
                                .getOrNull(pagerState.currentPage)?.first?.name
                            if (groupName.isNullOrBlank()) title else groupName
                        },
                        maxLines = 1,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    navigationIconContentColor = TopAppBarDefaults.topAppBarColors().actionIconContentColor
                ),
                navigationIcon = {},
                scrollBehavior = scrollBehavior,
                windowInsets =
                    if (windowSizeClass.isCompact)
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                    else
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.End),
                actions = {
                    ComponeIconButton(
                        onClick = {
                            navController.navigate(MediaSearchRoute(path = path, isSubList = false))
                        },
                        imageVector = Icons.Outlined.Search,
                        contentDescription = stringResource(Res.string.media_screen_search_hint),
                    )
                    ComponeIconButton(
                        onClick = { showSortMediaDialog = true },
                        imageVector = Icons.AutoMirrored.Outlined.Sort,
                        contentDescription = stringResource(Res.string.sort),
                    )
                    ComponeIconButton(
                        onClick = { openMoreMenu = true },
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = stringResource(Res.string.more),
                    )
                    MoreMenu(
                        expanded = openMoreMenu,
                        onDismissRequest = { openMoreMenu = false },
                        onRefresh = { dispatch(MediaIntent.RefreshGroup(path)) },
                        onChangeLibLocation = { navController.navigate(FilePickerRoute(path = path)) }
                    )
                }
            )
        },
        floatingActionButton = {
            ComponeFloatingActionButton(
                onClick = {
                    navController.navigate(FilePickerRoute(path = path, pickFolder = false))
                },
                onSizeWithSinglePaddingChanged = { _, height -> fabHeight = height },
                contentDescription = stringResource(Res.string.open_file),
            ) {
                Icon(imageVector = Icons.Outlined.FileOpen, contentDescription = null)
            }
        },
        contentWindowInsets =
            if (windowSizeClass.isCompact)
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            else
                WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
    ) { innerPadding ->
        var openCreateGroupDialog by rememberSaveable { mutableStateOf(false) }
        var createGroupDialogGroup by rememberSaveable { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (uiState.groups.isNotEmpty()) {
                if (MediaShowGroupTabPreference.current) {
                    PrimaryScrollableTabRow(
                        modifier = Modifier.fillMaxWidth(),
                        selectedTabIndex = min(uiState.groups.size - 1, pagerState.currentPage),
                        edgePadding = 0.dp,
                        divider = {},
                    ) {
                        uiState.groups.forEachIndexed { index, group ->
                            val interactionSource = remember { MutableInteractionSource() }
                            LongClickListener(
                                interactionSource = interactionSource,
                                onLongClick = {
                                    dispatch(MediaIntent.OnEditGroupDialog(uiState.groups[index].first))
                                },
                                onClick = { scope.launch { pagerState.animateScrollToPage(index) } }
                            )
                            Tab(
                                selected = pagerState.currentPage == index,
                                onClick = { },
                                text = {
                                    Text(
                                        modifier = Modifier
                                            .widthIn(max = 220.dp)
                                            .basicMarquee(iterations = Int.MAX_VALUE),
                                        text = group.first.name,
                                        maxLines = 1,
                                    )
                                },
                                interactionSource = interactionSource,
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

        uiState.editGroupDialogBean?.let { group ->
            EditMediaGroupSheet(
                onDismissRequest = { dispatch(MediaIntent.OnEditGroupDialog(null)) },
                group = group,
                groups = remember(uiState.groups) { uiState.groups.map { it.first } },
                onDelete = { dispatch(MediaIntent.DeleteGroup(path, it)) },
                onNameChange = { dispatch(MediaIntent.RenameGroup(path, group, it)) },
                onMoveTo = { dispatch(MediaIntent.MoveFilesToGroup(path, group, it)) },
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

                is MediaEvent.EditGroupResultEvent.Success ->
                    dispatch(MediaIntent.RefreshGroup(path = path))

                is MediaEvent.CreateGroupResultEvent.Success,
                is MediaEvent.DeleteGroupResultEvent.Success,
                is MediaEvent.ChangeFileGroupResultEvent.Success,
                is MediaEvent.MoveFilesToGroupResultEvent.Success -> Unit
            }
        }
    }

    SortDialog(
        visible = showSortMediaDialog,
        onDismissRequest = { showSortMediaDialog = false },
        sortByValues = MediaListSortByPreference.values,
        sortBy = MediaListSortByPreference.current,
        sortAsc = MediaListSortAscPreference.current,
        onSortBy = { MediaListSortByPreference.put(scope, it) },
        onSortAsc = { MediaListSortAscPreference.put(scope, it) },
        onSortByDisplayName = { BaseMediaListSortByPreference.toDisplayName(it) },
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
        titleText = stringResource(Res.string.media_screen_add_group),
        placeholder = stringResource(Res.string.media_group),
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
    onChangeLibLocation: () -> Unit,
) {
    val navController = LocalNavController.current
    DropdownMenuPopup(expanded = expanded, onDismissRequest = onDismissRequest) {
        val texts = listOf(
            stringResource(Res.string.data_screen_change_lib_location),
            stringResource(Res.string.media_screen_refresh_group),
            stringResource(Res.string.media_screen_style),
        )
        val leadingIcons = listOf(
            Icons.Outlined.MyLocation,
            Icons.Outlined.Refresh,
            Icons.Outlined.Palette,
        )
        val onClicks = listOf(
            {
                onDismissRequest()
                onChangeLibLocation()
            },
            {
                onDismissRequest()
                onRefresh()
            },
            {
                onDismissRequest()
                navController.navigate(MediaStyleRoute)
            },
        )
        DropdownMenuGroup(shapes = MenuDefaults.groupShape(0, 1)) {
            texts.forEachIndexed { index, text ->
                DropdownMenuItem(
                    text = { Text(text = text) },
                    shape = MenuDefaults.itemShape(index, texts.size).shape,
                    leadingIcon = {
                        Icon(imageVector = leadingIcons[index], contentDescription = null)
                    },
                    onClick = onClicks[index],
                )
            }
        }
    }
}