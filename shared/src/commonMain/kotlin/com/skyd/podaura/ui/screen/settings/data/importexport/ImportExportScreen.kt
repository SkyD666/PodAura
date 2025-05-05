package com.skyd.podaura.ui.screen.settings.data.importexport

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skyd.podaura.BuildKonfig
import com.skyd.podaura.ext.currentTimeMillis
import com.skyd.podaura.ext.toAbsoluteDateTimeString
import com.skyd.podaura.ext.validateFileName
import com.skyd.podaura.ui.component.BaseSettingsItem
import com.skyd.podaura.ui.component.CategorySettingsItem
import com.skyd.podaura.ui.component.PodAuraTopBar
import com.skyd.podaura.ui.component.PodAuraTopBarStyle
import com.skyd.podaura.ui.component.blockString
import com.skyd.podaura.ui.component.dialog.WaitingDialog
import com.skyd.podaura.ui.local.LocalNavController
import com.skyd.podaura.ui.mvi.MviEventListener
import com.skyd.podaura.ui.mvi.getDispatcher
import com.skyd.podaura.ui.screen.settings.data.importexport.importopml.ImportOpmlRoute
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.app_name
import podaura.shared.generated.resources.export_opml_screen_name
import podaura.shared.generated.resources.import_export_screen_feed_category
import podaura.shared.generated.resources.import_export_screen_name
import podaura.shared.generated.resources.import_export_screen_prefer_category
import podaura.shared.generated.resources.import_opml_screen_export_prefer
import podaura.shared.generated.resources.import_opml_screen_export_prefer_description
import podaura.shared.generated.resources.import_opml_screen_import_prefer
import podaura.shared.generated.resources.import_opml_screen_import_prefer_description
import podaura.shared.generated.resources.import_opml_screen_name
import podaura.shared.generated.resources.success_time_msg
import kotlin.random.Random


@Serializable
data object ImportExportRoute

@Composable
fun ImportExportScreen(viewModel: ImportExportViewModel = koinViewModel()) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val navController = LocalNavController.current
    val snackbarHostState = remember { SnackbarHostState() }
    val uiState by viewModel.viewState.collectAsStateWithLifecycle()
    val dispatch = viewModel.getDispatcher(startWith = ImportExportIntent.Init)

    val opmlSaverLauncher = rememberFileSaverLauncher { file ->
        if (file != null) {
            dispatch(ImportExportIntent.ExportOpml(file))
        }
    }

    val jsonPreferencePickerLauncher = rememberFilePickerLauncher(
        type = FileKitType.File(extension = "json"),
        mode = FileKitMode.Single,
    ) { file ->
        if (file != null) {
            dispatch(ImportExportIntent.ImportPrefer(file))
        }
    }
    val jsonPreferenceSaverLauncher = rememberFileSaverLauncher { file ->
        if (file != null) {
            dispatch(ImportExportIntent.ExportPrefer(file))
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            PodAuraTopBar(
                style = PodAuraTopBarStyle.Large,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(Res.string.import_export_screen_name)) },
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = paddingValues,
        ) {
            item { CategorySettingsItem(stringResource(Res.string.import_export_screen_feed_category)) }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(Icons.Outlined.FileDownload),
                    text = stringResource(Res.string.import_opml_screen_name),
                    descriptionText = null,
                    onClick = { navController.navigate(ImportOpmlRoute()) }
                )
            }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(Icons.Outlined.FileUpload),
                    text = stringResource(Res.string.export_opml_screen_name),
                    descriptionText = null,
                    onClick = {
                        val appName = blockString(Res.string.app_name)
                        val time = Clock.currentTimeMillis().toAbsoluteDateTimeString()
                        val random = Random.nextInt(0, Int.MAX_VALUE)
                        opmlSaverLauncher.launch(
                            suggestedName = "${appName}_${time}_${random}".validateFileName(),
                            extension = "opml",
                        )
                    }
                )
            }
            item { CategorySettingsItem(stringResource(Res.string.import_export_screen_prefer_category)) }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(Icons.Outlined.FileDownload),
                    text = stringResource(Res.string.import_opml_screen_import_prefer),
                    descriptionText = stringResource(Res.string.import_opml_screen_import_prefer_description),
                    onClick = { jsonPreferencePickerLauncher.launch() },
                )
            }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(Icons.Outlined.FileUpload),
                    text = stringResource(Res.string.import_opml_screen_export_prefer),
                    descriptionText = stringResource(Res.string.import_opml_screen_export_prefer_description),
                    onClick = {
                        val appName = blockString(Res.string.app_name)
                        val time = Clock.currentTimeMillis().toAbsoluteDateTimeString()
                        jsonPreferenceSaverLauncher.launch(
                            suggestedName = "${appName}_${BuildKonfig.versionName}_preferences_${time}".validateFileName(),
                            extension = "json"
                        )
                    }
                )
            }
        }
    }

    MviEventListener(viewModel.singleEvent) { event ->
        when (event) {
            is ImportExportEvent.ExportResultEvent.Failed -> snackbarHostState.showSnackbar(event.msg)
            is ImportExportEvent.ImportResultEvent.Failed -> snackbarHostState.showSnackbar(event.msg)
            is ImportExportEvent.ExportOpmlResultEvent.Failed ->
                snackbarHostState.showSnackbar(event.msg)

            is ImportExportEvent.ExportResultEvent.Success -> snackbarHostState.showSnackbar(
                getString(Res.string.success_time_msg, event.time / 1000f),
            )

            is ImportExportEvent.ImportResultEvent.Success -> snackbarHostState.showSnackbar(
                getString(Res.string.success_time_msg, event.time / 1000f),
            )

            is ImportExportEvent.ExportOpmlResultEvent.Success -> snackbarHostState.showSnackbar(
                getString(Res.string.success_time_msg, event.time / 1000f),
            )
        }
    }

    WaitingDialog(visible = uiState.loadingDialog)
}
