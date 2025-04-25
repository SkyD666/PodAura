package com.skyd.anivu.ui.screen.settings.data

import android.os.Parcelable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoDelete
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PermMedia
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skyd.anivu.ui.mvi.MviEventListener
import com.skyd.anivu.ui.mvi.getDispatcher
import com.skyd.anivu.model.preference.data.medialib.MediaLibLocationPreference
import com.skyd.anivu.ui.component.BackIcon
import com.skyd.anivu.ui.component.BaseSettingsItem
import com.skyd.anivu.ui.component.CategorySettingsItem
import com.skyd.anivu.ui.component.DefaultBackClick
import com.skyd.anivu.ui.component.PodAuraIconButton
import com.skyd.anivu.ui.component.PodAuraTopBar
import com.skyd.anivu.ui.component.PodAuraTopBarStyle
import com.skyd.anivu.ui.component.dialog.DeleteWarningDialog
import com.skyd.anivu.ui.component.dialog.WaitingDialog
import com.skyd.anivu.ui.local.LocalNavController
import com.skyd.anivu.ui.screen.filepicker.FilePickerRoute
import com.skyd.anivu.ui.screen.filepicker.ListenToFilePicker
import com.skyd.anivu.ui.screen.settings.data.autodelete.AutoDeleteRoute
import com.skyd.anivu.ui.screen.settings.data.deleteconstraint.DeleteConstraintRoute
import com.skyd.anivu.ui.screen.settings.data.importexport.ImportExportRoute
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.auto_delete_article_screen_description
import podaura.shared.generated.resources.auto_delete_screen_name
import podaura.shared.generated.resources.cancel
import podaura.shared.generated.resources.data_screen_change_lib_location
import podaura.shared.generated.resources.data_screen_clear_cache
import podaura.shared.generated.resources.data_screen_clear_cache_description
import podaura.shared.generated.resources.data_screen_clear_cache_warning
import podaura.shared.generated.resources.data_screen_clear_play_history
import podaura.shared.generated.resources.data_screen_clear_play_history_description
import podaura.shared.generated.resources.data_screen_clear_play_history_success
import podaura.shared.generated.resources.data_screen_clear_play_history_warning
import podaura.shared.generated.resources.data_screen_clear_up_category
import podaura.shared.generated.resources.data_screen_delete_article_before
import podaura.shared.generated.resources.data_screen_delete_article_before_description
import podaura.shared.generated.resources.data_screen_media_lib_category
import podaura.shared.generated.resources.data_screen_name
import podaura.shared.generated.resources.data_screen_sync_category
import podaura.shared.generated.resources.delete
import podaura.shared.generated.resources.delete_constraint_screen_name
import podaura.shared.generated.resources.delete_constraint_screen_name_description
import podaura.shared.generated.resources.import_export_screen_description
import podaura.shared.generated.resources.import_export_screen_name


@Serializable
@Parcelize
data object DataRoute : Parcelable

@Composable
fun DataScreen(
    onBack: (() -> Unit)? = DefaultBackClick,
    viewModel: DataViewModel = koinViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val navController = LocalNavController.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val uiState by viewModel.viewState.collectAsStateWithLifecycle()
    val dispatch = viewModel.getDispatcher(startWith = DataIntent.Init)

    ListenToFilePicker { result ->
        MediaLibLocationPreference.put(this, result.result)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            PodAuraTopBar(
                style = PodAuraTopBarStyle.Large,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(Res.string.data_screen_name)) },
                navigationIcon = { if (onBack != null) BackIcon(onClick = onBack) },
            )
        }
    ) { paddingValues ->
        var openDeleteWarningDialog by rememberSaveable { mutableStateOf(false) }
        var openDeletePlayHistoryWarningDialog by rememberSaveable { mutableStateOf(false) }
        var openDeleteBeforeDatePickerDialog by rememberSaveable { mutableStateOf(false) }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = paddingValues,
        ) {
            item {
                CategorySettingsItem(
                    text = stringResource(Res.string.data_screen_media_lib_category),
                )
            }
            item {
                val localMediaLibLocation = MediaLibLocationPreference.current
                BaseSettingsItem(
                    icon = rememberVectorPainter(Icons.Outlined.PermMedia),
                    text = stringResource(Res.string.data_screen_change_lib_location),
                    descriptionText = localMediaLibLocation,
                    onClick = { navController.navigate(FilePickerRoute(path = localMediaLibLocation)) },
                ) {
                    PodAuraIconButton(
                        onClick = {
                            MediaLibLocationPreference.put(
                                scope,
                                MediaLibLocationPreference.default
                            )
                        },
                        imageVector = Icons.Outlined.Replay,
                    )
                }
            }
            item {
                CategorySettingsItem(
                    text = stringResource(Res.string.data_screen_clear_up_category),
                )
            }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(Icons.Outlined.Delete),
                    text = stringResource(Res.string.data_screen_clear_cache),
                    descriptionText = stringResource(Res.string.data_screen_clear_cache_description),
                    onClick = { openDeleteWarningDialog = true }
                )
            }
            item {
                BaseSettingsItem(
                    icon = null,
                    text = stringResource(Res.string.delete_constraint_screen_name),
                    descriptionText = stringResource(Res.string.delete_constraint_screen_name_description),
                    onClick = { navController.navigate(DeleteConstraintRoute) },
                )
            }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(Icons.Outlined.Today),
                    text = stringResource(Res.string.data_screen_delete_article_before),
                    descriptionText = stringResource(Res.string.data_screen_delete_article_before_description),
                    onClick = { openDeleteBeforeDatePickerDialog = true },
                )
            }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(Icons.Outlined.AutoDelete),
                    text = stringResource(Res.string.auto_delete_screen_name),
                    descriptionText = stringResource(Res.string.auto_delete_article_screen_description),
                    onClick = { navController.navigate(AutoDeleteRoute) }
                )
            }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(Icons.Outlined.History),
                    text = stringResource(Res.string.data_screen_clear_play_history),
                    descriptionText = stringResource(Res.string.data_screen_clear_play_history_description),
                    onClick = { openDeletePlayHistoryWarningDialog = true }
                )
            }
            item {
                CategorySettingsItem(
                    text = stringResource(Res.string.data_screen_sync_category),
                )
            }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(Icons.Outlined.SwapVert),
                    text = stringResource(Res.string.import_export_screen_name),
                    descriptionText = stringResource(Res.string.import_export_screen_description),
                    onClick = { navController.navigate(ImportExportRoute) }
                )
            }
        }

        if (openDeleteBeforeDatePickerDialog) {
            DeleteArticleBeforeDatePickerDialog(
                onDismissRequest = { openDeleteBeforeDatePickerDialog = false },
                onConfirm = { dispatch(DataIntent.DeleteArticleBefore(it)) }
            )
        }

        DeleteWarningDialog(
            visible = openDeleteWarningDialog,
            text = stringResource(Res.string.data_screen_clear_cache_warning),
            onDismissRequest = { openDeleteWarningDialog = false },
            onDismiss = { openDeleteWarningDialog = false },
            onConfirm = { dispatch(DataIntent.ClearCache) },
        )

        DeleteWarningDialog(
            visible = openDeletePlayHistoryWarningDialog,
            text = stringResource(Res.string.data_screen_clear_play_history_warning),
            onDismissRequest = { openDeletePlayHistoryWarningDialog = false },
            onDismiss = { openDeletePlayHistoryWarningDialog = false },
            onConfirm = { dispatch(DataIntent.DeletePlayHistory) },
        )

        WaitingDialog(visible = uiState.loadingDialog)

        MviEventListener(viewModel.singleEvent) { event ->
            when (event) {
                is DataEvent.ClearCacheResultEvent.Success ->
                    snackbarHostState.showSnackbar(event.msg)

                is DataEvent.ClearCacheResultEvent.Failed ->
                    snackbarHostState.showSnackbar(event.msg)

                is DataEvent.DeleteArticleBeforeResultEvent.Failed ->
                    snackbarHostState.showSnackbar(event.msg)

                is DataEvent.DeleteArticleBeforeResultEvent.Success ->
                    snackbarHostState.showSnackbar(event.msg)

                is DataEvent.DeletePlayHistoryResultEvent.Failed ->
                    snackbarHostState.showSnackbar(event.msg)

                is DataEvent.DeletePlayHistoryResultEvent.Success ->
                    snackbarHostState.showSnackbar(
                        getString(Res.string.data_screen_clear_play_history_success, event.count)
                    )
            }
        }
    }
}

@Composable
private fun DeleteArticleBeforeDatePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val datePickerState = rememberDatePickerState()
    val confirmEnabled = remember { derivedStateOf { datePickerState.selectedDateMillis != null } }
    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = { onConfirm(datePickerState.selectedDateMillis!!) },
                enabled = confirmEnabled.value,
            ) {
                Text(stringResource(Res.string.delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(Res.string.cancel))
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}