package com.skyd.podaura.ui.screen.settings.data.importexport.importopml

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Segment
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.skyd.compone.component.ComponeExtendedFloatingActionButton
import com.skyd.compone.component.ComponeTopBar
import com.skyd.compone.component.ComponeTopBarStyle
import com.skyd.compone.component.connectedButtonShapes
import com.skyd.compone.component.dialog.WaitingDialog
import com.skyd.podaura.ext.asPlatformFile
import com.skyd.podaura.ext.showSnackbar
import com.skyd.podaura.model.repository.importexport.opml.ImportOpmlConflictStrategy
import com.skyd.podaura.ui.mvi.MviEventListener
import com.skyd.podaura.ui.mvi.getDispatcher
import com.skyd.settings.BaseSettingsItem
import com.skyd.settings.SettingsLazyColumn
import com.skyd.settings.TipSettingsItem
import com.skyd.settings.plus
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
            ComponeTopBar(
                style = ComponeTopBarStyle.LargeFlexible,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(Res.string.import_opml_screen_name)) },
            )
        },
        floatingActionButton = {
            ComponeExtendedFloatingActionButton(
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
        SettingsLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = paddingValues + PaddingValues(bottom = fabHeight),
            state = lazyListState,
        ) {
            group {
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(
                                    space = ButtonGroupDefaults.ConnectedSpaceBetween,
                                    alignment = Alignment.CenterHorizontally,
                                ),
                            ) {
                                ImportOpmlConflictStrategy.strategies.forEachIndexed { index, proxy ->
                                    ToggleButton(
                                        checked = index == selectedImportStrategyIndex,
                                        onCheckedChange = {
                                            if (it) selectedImportStrategyIndex = index
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .semantics { role = Role.RadioButton },
                                        shapes = ButtonGroupDefaults.connectedButtonShapes(
                                            list = ImportOpmlConflictStrategy.strategies,
                                            index = index,
                                        ),
                                    ) {
                                        Text(text = proxy.displayName)
                                    }
                                }
                            }
                        }
                    )
                }
                otherItem {
                    TipSettingsItem(text = stringResource(Res.string.import_opml_screen_desc))
                }
            }
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