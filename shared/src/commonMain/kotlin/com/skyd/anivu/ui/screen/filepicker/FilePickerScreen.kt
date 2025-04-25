package com.skyd.anivu.ui.screen.filepicker

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.NavigateNext
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.toRoute
import com.skyd.anivu.config.Const
import com.skyd.anivu.config.DEFAULT_FILE_PICKER_PATH
import com.skyd.anivu.ext.isDirectory
import com.skyd.anivu.ext.onlyHorizontal
import com.skyd.anivu.ext.plus
import com.skyd.anivu.ext.popBackStackWithLifecycle
import com.skyd.anivu.model.preference.data.medialib.MediaLibLocationPreference
import com.skyd.anivu.ui.component.PodAuraIconButton
import com.skyd.anivu.ui.component.PodAuraTopBar
import com.skyd.anivu.ui.component.PodAuraTopBarStyle
import com.skyd.anivu.ui.local.LocalNavController
import com.skyd.anivu.ui.mvi.MviEventListener
import com.skyd.anivu.ui.mvi.getDispatcher
import com.skyd.anivu.util.fileicon.fileIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.io.files.Path
import kotlinx.io.files.SystemPathSeparator
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.close
import podaura.shared.generated.resources.file_picker_screen_internal_storage
import podaura.shared.generated.resources.file_picker_screen_open_file
import podaura.shared.generated.resources.file_picker_screen_open_folder
import podaura.shared.generated.resources.file_picker_screen_pick


const val FILE_PICKER_NEW_PATH_KEY = "newPath"

@Composable
fun ListenToFilePicker(onNewPath: CoroutineScope.(FilePickerResult) -> Unit) {
    val navController = LocalNavController.current
    LaunchedEffect(Unit) {
        navController.currentBackStackEntry?.savedStateHandle?.apply {
            getStateFlow<String?>(FILE_PICKER_NEW_PATH_KEY, null)
                .filterNotNull()
                .collect {
                    onNewPath(Json.decodeFromString(it))
                    remove<FilePickerResult?>(FILE_PICKER_NEW_PATH_KEY)
                }
        }
    }
}

@Serializable
data class FilePickerRoute(
    val path: String,
    val pickFolder: Boolean = true,
    val extensionName: String = "",
    val id: String? = null,
) {
    companion object {
        @Composable
        fun FilePickerLauncher(entry: NavBackStackEntry) {
            val filePickerRoute = entry.toRoute<FilePickerRoute>()
            FilePickerScreen(
                path = filePickerRoute.path.takeIf {
                    filePickerRoute.path != MediaLibLocationPreference.default
                } ?: Const.DEFAULT_FILE_PICKER_PATH,
                pickFolder = filePickerRoute.pickFolder,
                extensionName = filePickerRoute.extensionName,
                id = filePickerRoute.id,
            )
        }
    }
}

@Serializable
data class FilePickerResult(
    val id: String?,
    val path: String,
    val pickFolder: Boolean,
    val extensionName: String?,
    val result: String,
) {
    fun toJson(): String = Json.encodeToString(this)
}

@Composable
fun FilePickerScreen(
    path: String,
    pickFolder: Boolean = false,
    extensionName: String? = null,
    id: String?,
    viewModel: FilePickerViewModel = koinViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }
    val navController = LocalNavController.current
    val uiState by viewModel.viewState.collectAsStateWithLifecycle()
    val dispatch = viewModel.getDispatcher(
        startWith = FilePickerIntent.NewLocation(
            path = path,
            extensionName = extensionName,
        )
    )

    BackHandler {
        val current = Path(uiState.path)
        val parent = current.parent?.toString()
        if (!parent.isNullOrBlank() &&
            uiState.path != Const.DEFAULT_FILE_PICKER_PATH &&
            uiState.path.startsWith(Const.DEFAULT_FILE_PICKER_PATH)
        ) {
            dispatch(FilePickerIntent.NewLocation(parent))
        } else {
            navController.popBackStackWithLifecycle()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            PodAuraTopBar(
                style = PodAuraTopBarStyle.Small,
                scrollBehavior = scrollBehavior,
                title = {
                    Text(
                        text = stringResource(
                            if (pickFolder) Res.string.file_picker_screen_open_folder
                            else Res.string.file_picker_screen_open_file
                        )
                    )
                },
                navigationIcon = {
                    PodAuraIconButton(
                        onClick = { navController.popBackStackWithLifecycle() },
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(Res.string.close),
                    )
                },
                actions = {
                    PodAuraIconButton(
                        onClick = { dispatch(FilePickerIntent.NewLocation(Const.DEFAULT_FILE_PICKER_PATH)) },
                        imageVector = Icons.Outlined.PhoneAndroid,
                        contentDescription = stringResource(Res.string.file_picker_screen_internal_storage),
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
            PathLevelIndication(
                path = uiState.path,
                onRouteTo = { dispatch(FilePickerIntent.NewLocation(it)) },
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = paddingValues.onlyHorizontal() + PaddingValues(
                    bottom = if (pickFolder) 0.dp else paddingValues.calculateBottomPadding(),
                ),
            ) {
                (uiState.fileListState as? FileListState.Success)?.list?.forEach { filePath ->
                    item {
                        ListItem(
                            modifier = Modifier.clickable {
                                if (filePath.isDirectory) {
                                    dispatch(FilePickerIntent.NewLocation(filePath.toString()))
                                } else {
                                    if (!pickFolder) {
                                        navController.previousBackStackEntry
                                            ?.savedStateHandle
                                            ?.set(
                                                FILE_PICKER_NEW_PATH_KEY,
                                                FilePickerResult(
                                                    id = id,
                                                    path = path,
                                                    pickFolder = false,
                                                    extensionName = extensionName,
                                                    result = filePath.toString(),
                                                ).toJson()
                                            )
                                        navController.popBackStackWithLifecycle()
                                    }
                                }
                            },
                            leadingContent = {
                                Icon(
                                    painter = painterResource(
                                        remember(filePath) { filePath.fileIcon().resource }
                                    ),
                                    contentDescription = null,
                                )
                            },
                            headlineContent = { Text(text = filePath.name) },
                        )
                    }
                }
            }

            if (pickFolder) {
                Button(
                    modifier = Modifier
                        .padding(
                            top = 12.dp,
                            bottom = 12.dp + paddingValues.calculateBottomPadding(),
                        )
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    onClick = {
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set(
                                FILE_PICKER_NEW_PATH_KEY,
                                FilePickerResult(
                                    id = id,
                                    path = path,
                                    pickFolder = true,
                                    extensionName = extensionName,
                                    result = uiState.path,
                                ).toJson()
                            )
                        navController.popBackStackWithLifecycle()
                    },
                ) {
                    Text(text = stringResource(Res.string.file_picker_screen_pick))
                }
            }
        }

        MviEventListener(viewModel.singleEvent) { event ->
            when (event) {
                is FilePickerEvent.FileListResultEvent.Failed ->
                    snackbarHostState.showSnackbar(event.msg)
            }
        }
    }
}

@Composable
private fun PathLevelIndication(path: String, onRouteTo: (String) -> Unit) {
    val scrollState = rememberScrollState()
    LaunchedEffect(scrollState.maxValue) {
        if (scrollState.canScrollForward) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }
    Row(
        modifier = Modifier
            .horizontalScroll(scrollState)
            .padding(horizontal = 12.dp)
            .animateContentSize(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val items = remember(path) {
            mutableListOf<String>().apply {
                var newPath = path
                if (newPath.startsWith(Const.DEFAULT_FILE_PICKER_PATH)) {
                    add(Const.DEFAULT_FILE_PICKER_PATH)
                    newPath = newPath.removePrefix(Const.DEFAULT_FILE_PICKER_PATH)
                }
                newPath.removeSurrounding(SystemPathSeparator.toString())
                newPath.split(SystemPathSeparator).forEach {
                    if (it.isNotBlank()) add(it)
                }
            }
        }

        items.forEachIndexed { index, item ->
            Text(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .clickable {
                        onRouteTo(
                            items
                                .subList(0, index + 1)
                                .joinToString(SystemPathSeparator.toString())
                        )
                    }
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                text = item,
                style = MaterialTheme.typography.labelLarge,
            )
            if (index != items.size - 1) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.NavigateNext,
                    contentDescription = null,
                )
            }
        }
    }
}
