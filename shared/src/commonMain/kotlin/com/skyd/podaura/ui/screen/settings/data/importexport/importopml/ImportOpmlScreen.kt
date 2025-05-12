package com.skyd.podaura.ui.screen.settings.data.importexport.importopml

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Segment
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.skyd.podaura.ext.asPlatformFile
import com.skyd.podaura.ext.plus
import com.skyd.podaura.ext.showSnackbar
import com.skyd.podaura.model.repository.importexport.opml.ImportOpmlConflictStrategy
import com.skyd.podaura.ui.component.BaseSettingsItem
import com.skyd.podaura.ui.component.PodAuraExtendedFloatingActionButton
import com.skyd.podaura.ui.component.PodAuraTopBar
import com.skyd.podaura.ui.component.PodAuraTopBarStyle
import com.skyd.podaura.ui.component.TipSettingsItem
import com.skyd.podaura.ui.component.dialog.WaitingDialog
import com.skyd.podaura.ui.mvi.MviEventListener
import com.skyd.podaura.ui.mvi.getDispatcher
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.path
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.failed_msg
import podaura.shared.generated.resources.import_opml_result
import podaura.shared.generated.resources.import_opml_screen_desc
import podaura.shared.generated.resources.import_opml_screen_import
import podaura.shared.generated.resources.import_opml_screen_name
import podaura.shared.generated.resources.import_opml_screen_on_conflict
import podaura.shared.generated.resources.import_opml_screen_opml_not_selected
import podaura.shared.generated.resources.import_opml_screen_select_file


@Serializable
data class ImportOpmlRoute(val opmlUrl: String? = null) {
    companion object {
        @Composable
        fun ImportOpmlLauncher(entry: NavBackStackEntry) {
            ImportOpmlScreen(opmlUrl = entry.toRoute<ImportOpmlRoute>().opmlUrl)
        }
    }
}

@Serializable
data object ImportOpmlDeepLinkRoute {
    val deepLinks = listOf("text/xml", "application/xml", "text/x-opml").map { type ->
        navDeepLink { mimeType = type }
    }
}

@Composable
expect fun ImportOpmlDeepLinkLauncher(entry: NavBackStackEntry)

@Composable
fun ImportOpmlScreen(
    opmlUrl: String? = null,
    viewModel: ImportOpmlViewModel = koinViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val uiState by viewModel.viewState.collectAsStateWithLifecycle()
    var selectedImportStrategyIndex by rememberSaveable { mutableIntStateOf(0) }

    val dispatch = viewModel.getDispatcher(startWith = ImportOpmlIntent.Init)

    var opmlFilePath by rememberSaveable(opmlUrl) { mutableStateOf(opmlUrl) }
    val filePickerLauncher = rememberFilePickerLauncher(
        type = FileKitType.File(),
        mode = FileKitMode.Single,
    ) { file ->
        if (file != null) {
            opmlFilePath = file.path
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
                title = { Text(text = stringResource(Res.string.import_opml_screen_name)) },
            )
        },
        floatingActionButton = {
            PodAuraExtendedFloatingActionButton(
                text = { Text(text = stringResource(Res.string.import_opml_screen_import)) },
                icon = { Icon(imageVector = Icons.Default.Done, contentDescription = null) },
                onClick = {
                    if (opmlFilePath.isNullOrBlank()) {
                        snackbarHostState.showSnackbar(
                            scope = scope,
                            message = Res.string.import_opml_screen_opml_not_selected,
                        )
                    } else {
                        dispatch(
                            ImportOpmlIntent.ImportOpml(
                                opmlFile = opmlFilePath!!.asPlatformFile(),
                                strategy = ImportOpmlConflictStrategy.strategies[selectedImportStrategyIndex],
                            )
                        )
                    }
                },
                onSizeWithSinglePaddingChanged = { _, height -> fabHeight = height },
                contentDescription = null,
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
                    icon = rememberVectorPainter(image = Icons.AutoMirrored.Outlined.Segment),
                    text = stringResource(Res.string.import_opml_screen_select_file),
                    descriptionText = opmlFilePath?.ifBlank { null },
                    onClick = { filePickerLauncher.launch() }
                )
            }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(image = Icons.AutoMirrored.Outlined.HelpOutline),
                    text = stringResource(Res.string.import_opml_screen_on_conflict),
                    description = {
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            ImportOpmlConflictStrategy.strategies.forEachIndexed { index, proxy ->
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = ImportOpmlConflictStrategy.strategies.size
                                    ),
                                    onClick = { selectedImportStrategyIndex = index },
                                    selected = index == selectedImportStrategyIndex
                                ) {
                                    Text(text = proxy.displayName)
                                }
                            }
                        }
                    }
                )
            }
            item { TipSettingsItem(text = stringResource(Res.string.import_opml_screen_desc)) }
        }
    }

    WaitingDialog(visible = uiState.loadingDialog)

    MviEventListener(viewModel.singleEvent) { event ->
        when (event) {
            is ImportOpmlEvent.ImportOpmlResultEvent.Success -> snackbarHostState.showSnackbar(
                getPluralString(
                    Res.plurals.import_opml_result,
                    event.result.importedFeedCount,
                    event.result.importedFeedCount,
                    event.result.time / 1000f,
                ),
            )

            is ImportOpmlEvent.ImportOpmlResultEvent.Failed ->
                snackbarHostState.showSnackbar(getString(Res.string.failed_msg, event.msg))
        }
    }
}