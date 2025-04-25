package com.skyd.anivu.ui.screen.settings.data.importexport.opml.exportopml

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.outlined.Folder
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skyd.anivu.ui.mvi.MviEventListener
import com.skyd.anivu.ui.mvi.getDispatcher
import com.skyd.anivu.ext.plus
import com.skyd.anivu.ext.safeLaunch
import com.skyd.anivu.ext.showSnackbar
import com.skyd.anivu.model.preference.data.OpmlExportDirPreference
import com.skyd.anivu.ui.component.BaseSettingsItem
import com.skyd.anivu.ui.component.PodAuraExtendedFloatingActionButton
import com.skyd.anivu.ui.component.PodAuraTopBar
import com.skyd.anivu.ui.component.PodAuraTopBarStyle
import com.skyd.anivu.ui.component.dialog.WaitingDialog
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.export_opml_screen_dir_not_selected
import podaura.shared.generated.resources.export_opml_screen_export
import podaura.shared.generated.resources.export_opml_screen_name
import podaura.shared.generated.resources.export_opml_screen_select_dir
import podaura.shared.generated.resources.success_time_msg


@Serializable
data object ExportOpmlRoute

@Composable
fun ExportOpmlScreen(viewModel: ExportOpmlViewModel = koinViewModel()) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val uiState by viewModel.viewState.collectAsStateWithLifecycle()
    val dispatch = viewModel.getDispatcher(startWith = ExportOpmlIntent.Init)

    val exportDir = OpmlExportDirPreference.current
    val pickExportDirLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            OpmlExportDirPreference.put(scope, uri.toString())
        }
    }
    val lazyListState = rememberLazyListState()
    var fabHeight by remember { mutableStateOf(0.dp) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            PodAuraTopBar(
                style = PodAuraTopBarStyle.Large,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(Res.string.export_opml_screen_name)) },
            )
        },
        floatingActionButton = {
            PodAuraExtendedFloatingActionButton(
                text = { Text(text = stringResource(Res.string.export_opml_screen_export)) },
                icon = { Icon(imageVector = Icons.Default.Done, contentDescription = null) },
                onClick = {
                    if (exportDir.isBlank()) {
                        snackbarHostState.showSnackbar(
                            scope = scope,
                            message = Res.string.export_opml_screen_dir_not_selected,
                        )
                    } else {
                        dispatch(ExportOpmlIntent.ExportOpml(outputDir = exportDir.toUri()))
                    }
                },
                onSizeWithSinglePaddingChanged = { _, height -> fabHeight = height },
                contentDescription = stringResource(Res.string.export_opml_screen_export)
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = paddingValues + PaddingValues(bottom = fabHeight),
            state = lazyListState,
        ) {
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(image = Icons.Outlined.Folder),
                    text = stringResource(Res.string.export_opml_screen_select_dir),
                    descriptionText = exportDir.ifBlank { null },
                    onClick = { pickExportDirLauncher.safeLaunch(exportDir.toUri()) }
                )
            }
        }
    }

    WaitingDialog(visible = uiState.loadingDialog)

    MviEventListener(viewModel.singleEvent) { event ->
        when (event) {
            is ExportOpmlEvent.ExportOpmlResultEvent.Failed -> snackbarHostState.showSnackbar(event.msg)
            is ExportOpmlEvent.ExportOpmlResultEvent.Success -> snackbarHostState.showSnackbar(
                getString(Res.string.success_time_msg, event.time / 1000f),
            )
        }
    }
}
