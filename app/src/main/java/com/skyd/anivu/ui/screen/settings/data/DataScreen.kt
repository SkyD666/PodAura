package com.skyd.anivu.ui.screen.settings.data

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skyd.anivu.R
import com.skyd.anivu.base.mvi.MviEventListener
import com.skyd.anivu.base.mvi.getDispatcher
import com.skyd.anivu.model.preference.data.medialib.MediaLibLocationPreference
import com.skyd.anivu.ui.component.PodAuraIconButton
import com.skyd.anivu.ui.component.PodAuraTopBar
import com.skyd.anivu.ui.component.PodAuraTopBarStyle
import com.skyd.anivu.ui.component.BaseSettingsItem
import com.skyd.anivu.ui.component.CategorySettingsItem
import com.skyd.anivu.ui.component.dialog.DeleteWarningDialog
import com.skyd.anivu.ui.component.dialog.WaitingDialog
import com.skyd.anivu.ui.local.LocalMediaLibLocation
import com.skyd.anivu.ui.local.LocalNavController
import com.skyd.anivu.ui.screen.filepicker.ListenToFilePicker
import com.skyd.anivu.ui.screen.filepicker.openFilePicker
import com.skyd.anivu.ui.screen.settings.data.autodelete.AUTO_DELETE_SCREEN_ROUTE
import com.skyd.anivu.ui.screen.settings.data.importexport.IMPORT_EXPORT_SCREEN_ROUTE


const val DATA_SCREEN_ROUTE = "dataScreen"

@Composable
fun DataScreen(viewModel: DataViewModel = hiltViewModel()) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val navController = LocalNavController.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val uiState by viewModel.viewState.collectAsStateWithLifecycle()
    val dispatch = viewModel.getDispatcher(startWith = DataIntent.Init)

    ListenToFilePicker { result ->
        MediaLibLocationPreference.put(context, this, result.result)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            PodAuraTopBar(
                style = PodAuraTopBarStyle.Large,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(R.string.data_screen_name)) },
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
                    text = stringResource(id = R.string.data_screen_media_lib_category),
                )
            }
            item {
                val localMediaLibLocation = LocalMediaLibLocation.current
                BaseSettingsItem(
                    icon = rememberVectorPainter(Icons.Outlined.PermMedia),
                    text = stringResource(id = R.string.data_screen_change_lib_location),
                    descriptionText = localMediaLibLocation,
                    onClick = {
                        openFilePicker(
                            navController = navController,
                            path = localMediaLibLocation,
                        )
                    }
                ) {
                    PodAuraIconButton(
                        onClick = {
                            MediaLibLocationPreference.put(
                                context,
                                scope,
                                MediaLibLocationPreference.default,
                            )
                        },
                        imageVector = Icons.Outlined.Replay,
                    )
                }
            }
            item {
                CategorySettingsItem(
                    text = stringResource(id = R.string.data_screen_clear_up_category),
                )
            }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(Icons.Outlined.Delete),
                    text = stringResource(id = R.string.data_screen_clear_cache),
                    descriptionText = stringResource(id = R.string.data_screen_clear_cache_description),
                    onClick = { openDeleteWarningDialog = true }
                )
            }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(Icons.Outlined.Today),
                    text = stringResource(id = R.string.data_screen_delete_article_before),
                    descriptionText = stringResource(id = R.string.data_screen_delete_article_before_description),
                    onClick = { openDeleteBeforeDatePickerDialog = true },
                )
            }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(Icons.Outlined.AutoDelete),
                    text = stringResource(id = R.string.auto_delete_screen_name),
                    descriptionText = stringResource(id = R.string.auto_delete_article_screen_description),
                    onClick = { navController.navigate(AUTO_DELETE_SCREEN_ROUTE) }
                )
            }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(Icons.Outlined.History),
                    text = stringResource(id = R.string.data_screen_clear_play_history),
                    descriptionText = stringResource(id = R.string.data_screen_clear_play_history_description),
                    onClick = { openDeletePlayHistoryWarningDialog = true }
                )
            }
            item {
                CategorySettingsItem(
                    text = stringResource(id = R.string.data_screen_sync_category),
                )
            }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(Icons.Outlined.SwapVert),
                    text = stringResource(id = R.string.import_export_screen_name),
                    descriptionText = stringResource(id = R.string.import_export_screen_description),
                    onClick = { navController.navigate(IMPORT_EXPORT_SCREEN_ROUTE) }
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
            text = stringResource(id = R.string.data_screen_clear_cache_warning),
            onDismissRequest = { openDeleteWarningDialog = false },
            onDismiss = { openDeleteWarningDialog = false },
            onConfirm = { dispatch(DataIntent.ClearCache) },
        )

        DeleteWarningDialog(
            visible = openDeletePlayHistoryWarningDialog,
            text = stringResource(id = R.string.data_screen_clear_play_history_warning),
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
                        context.getString(
                            R.string.data_screen_clear_play_history_success,
                            event.count
                        )
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
                Text(stringResource(id = R.string.delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}