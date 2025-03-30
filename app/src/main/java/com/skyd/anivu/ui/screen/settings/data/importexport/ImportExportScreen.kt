package com.skyd.anivu.ui.screen.settings.data.importexport

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skyd.anivu.R
import com.skyd.anivu.base.mvi.MviEventListener
import com.skyd.anivu.base.mvi.getDispatcher
import com.skyd.anivu.ext.getAppName
import com.skyd.anivu.ext.getAppVersionName
import com.skyd.anivu.ext.safeLaunch
import com.skyd.anivu.ext.toAbsoluteDateTimeString
import com.skyd.anivu.ext.validateFileName
import com.skyd.anivu.ui.component.BaseSettingsItem
import com.skyd.anivu.ui.component.CategorySettingsItem
import com.skyd.anivu.ui.component.PodAuraTopBar
import com.skyd.anivu.ui.component.PodAuraTopBarStyle
import com.skyd.anivu.ui.component.dialog.WaitingDialog
import com.skyd.anivu.ui.local.LocalNavController
import com.skyd.anivu.ui.screen.settings.data.importexport.opml.exportopml.ExportOpmlRoute
import com.skyd.anivu.ui.screen.settings.data.importexport.opml.importopml.ImportOpmlRoute
import kotlinx.serialization.Serializable


@Serializable
data object ImportExportRoute

@Composable
fun ImportExportScreen(viewModel: ImportExportViewModel = hiltViewModel()) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val navController = LocalNavController.current
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val uiState by viewModel.viewState.collectAsStateWithLifecycle()
    val dispatch = viewModel.getDispatcher(startWith = ImportExportIntent.Init)

    val pickImportFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            dispatch(ImportExportIntent.ImportPrefer(inputFile = uri))
        }
    }
    val pickExportFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            dispatch(ImportExportIntent.ExportPrefer(outputFile = uri))
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            PodAuraTopBar(
                style = PodAuraTopBarStyle.Large,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(R.string.import_export_screen_name)) },
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = paddingValues,
        ) {
            item { CategorySettingsItem(stringResource(R.string.import_export_screen_feed_category)) }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(Icons.Outlined.FileDownload),
                    text = stringResource(id = R.string.import_opml_screen_name),
                    descriptionText = null,
                    onClick = { navController.navigate(ImportOpmlRoute()) }
                )
            }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(Icons.Outlined.FileUpload),
                    text = stringResource(id = R.string.export_opml_screen_name),
                    descriptionText = null,
                    onClick = { navController.navigate(ExportOpmlRoute) }
                )
            }
            item { CategorySettingsItem(stringResource(R.string.import_export_screen_prefer_category)) }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(Icons.Outlined.FileDownload),
                    text = stringResource(id = R.string.import_opml_screen_import_prefer),
                    descriptionText = stringResource(R.string.import_opml_screen_import_prefer_description),
                    onClick = { pickImportFileLauncher.safeLaunch("application/json") },
                )
            }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(Icons.Outlined.FileUpload),
                    text = stringResource(id = R.string.import_opml_screen_export_prefer),
                    descriptionText = stringResource(R.string.import_opml_screen_export_prefer_description),
                    onClick = {
                        pickExportFileLauncher.safeLaunch(buildString {
                            append("${context.getAppName()}_")
                            append("${context.getAppVersionName()}_preferences_")
                            append(
                                System.currentTimeMillis().toAbsoluteDateTimeString()
                                    .validateFileName()
                            )
                            append(".json")
                        })
                    }
                )
            }
        }
    }

    MviEventListener(viewModel.singleEvent) { event ->
        when (event) {
            is ImportExportEvent.ExportResultEvent.Failed -> snackbarHostState.showSnackbar(event.msg)
            is ImportExportEvent.ImportResultEvent.Failed -> snackbarHostState.showSnackbar(event.msg)
            is ImportExportEvent.ExportResultEvent.Success -> snackbarHostState.showSnackbar(
                context.getString(R.string.success_time_msg, event.time / 1000f),
            )

            is ImportExportEvent.ImportResultEvent.Success -> snackbarHostState.showSnackbar(
                context.getString(R.string.success_time_msg, event.time / 1000f),
            )

        }
    }

    WaitingDialog(visible = uiState.loadingDialog)
}
