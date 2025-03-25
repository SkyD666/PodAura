package com.skyd.anivu.ui.screen.filepicker

import android.os.Parcelable
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skyd.anivu.R
import com.skyd.anivu.base.mvi.MviEventListener
import com.skyd.anivu.base.mvi.getDispatcher
import com.skyd.anivu.config.Const
import com.skyd.anivu.ext.getMimeType
import com.skyd.anivu.ext.onlyHorizontal
import com.skyd.anivu.ext.plus
import com.skyd.anivu.ext.popBackStackWithLifecycle
import com.skyd.anivu.ui.component.PodAuraIconButton
import com.skyd.anivu.ui.component.PodAuraTopBar
import com.skyd.anivu.ui.component.PodAuraTopBarStyle
import com.skyd.anivu.ui.local.LocalNavController
import com.skyd.anivu.util.fileicon.getFileIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.io.File


const val FILE_PICKER_NEW_PATH_KEY = "newPath"

@Composable
fun ListenToFilePicker(onNewPath: CoroutineScope.(FilePickerResult) -> Unit) {
    val navController = LocalNavController.current
    LaunchedEffect(Unit) {
        navController.currentBackStackEntry?.savedStateHandle?.apply {
            getStateFlow<FilePickerResult?>(FILE_PICKER_NEW_PATH_KEY, null)
                .filterNotNull()
                .onEach {
                    onNewPath(it)
                    remove<FilePickerResult?>(FILE_PICKER_NEW_PATH_KEY)
                }
                .collect()
        }
    }
}

@Serializable
data class FilePickerRoute(
    val path: String,
    val pickFolder: Boolean = true,
    val extensionName: String = "",
    val id: String? = null,
)

@Parcelize
data class FilePickerResult(
    val id: String?,
    val path: String,
    val pickFolder: Boolean,
    val extensionName: String?,
    val result: String,
) : Parcelable


@Composable
fun FilePickerScreen(
    path: String,
    pickFolder: Boolean = false,
    extensionName: String? = null,
    id: String?,
    viewModel: FilePickerViewModel = hiltViewModel(),
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
        val current = File(uiState.path)
        val parent = current.parent
        if (!parent.isNullOrBlank() &&
            uiState.path != Const.INTERNAL_STORAGE &&
            uiState.path.startsWith(Const.INTERNAL_STORAGE)
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
                            if (pickFolder) R.string.file_picker_screen_open_folder
                            else R.string.file_picker_screen_open_file
                        )
                    )
                },
                navigationIcon = {
                    PodAuraIconButton(
                        onClick = { navController.popBackStackWithLifecycle() },
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(id = R.string.close),
                    )
                },
                actions = {
                    PodAuraIconButton(
                        onClick = { dispatch(FilePickerIntent.NewLocation(Const.INTERNAL_STORAGE)) },
                        imageVector = Icons.Outlined.PhoneAndroid,
                        contentDescription = stringResource(id = R.string.file_picker_screen_internal_storage),
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
                (uiState.fileListState as? FileListState.Success)?.list?.forEach { file ->
                    item {
                        ListItem(
                            modifier = Modifier.clickable {
                                if (file.isDirectory) {
                                    dispatch(FilePickerIntent.NewLocation(file.path))
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
                                                    result = file.absolutePath,
                                                )
                                            )
                                        navController.popBackStackWithLifecycle()
                                    }
                                }
                            },
                            leadingContent = {
                                Icon(
                                    painter = painterResource(
                                        id = remember(file) {
                                            getFileIcon(file.getMimeType() ?: "*/*").resourceId
                                        }
                                    ),
                                    contentDescription = null,
                                )
                            },
                            headlineContent = { Text(text = file.name) },
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
                                )
                            )
                        navController.popBackStackWithLifecycle()
                    },
                ) {
                    Text(text = stringResource(id = R.string.file_picker_screen_pick))
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
                if (newPath.startsWith(Const.INTERNAL_STORAGE)) {
                    add(Const.INTERNAL_STORAGE)
                    newPath = newPath.removePrefix(Const.INTERNAL_STORAGE)
                }
                newPath.removeSurrounding(File.separator)
                newPath.split(File.separator).forEach {
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
                                .joinToString(File.separator)
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
