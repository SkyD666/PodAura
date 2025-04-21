package com.skyd.anivu.ui.screen.feed.requestheaders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Http
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.toRoute
import com.skyd.anivu.R
import com.skyd.anivu.base.mvi.MviEventListener
import com.skyd.anivu.base.mvi.getDispatcher
import com.skyd.anivu.ext.plus
import com.skyd.anivu.model.bean.feed.FeedBean
import com.skyd.anivu.ui.component.ClipboardTextField
import com.skyd.anivu.ui.component.PodAuraFloatingActionButton
import com.skyd.anivu.ui.component.PodAuraIconButton
import com.skyd.anivu.ui.component.PodAuraTopBar
import com.skyd.anivu.ui.component.PodAuraTopBarStyle
import com.skyd.anivu.ui.component.dialog.PodAuraDialog
import com.skyd.anivu.ui.component.dialog.TextFieldDialog
import com.skyd.anivu.ui.component.dialog.WaitingDialog
import com.skyd.anivu.ui.screen.playlist.medialist.PlaylistMediaListRoute
import kotlinx.serialization.Serializable


@Serializable
data class RequestHeadersRoute(val feedUrl: String) {
    companion object {
        @Composable
        fun RequestHeadersLauncher(entry: NavBackStackEntry) {
            RequestHeadersScreen(feedUrl = entry.toRoute<RequestHeadersRoute>().feedUrl)
        }
    }
}

@Composable
fun RequestHeadersScreen(feedUrl: String, viewModel: RequestHeadersViewModel = hiltViewModel()) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }

    val uiState by viewModel.viewState.collectAsStateWithLifecycle()
    val dispatcher =
        viewModel.getDispatcher(feedUrl, startWith = RequestHeadersIntent.Init(feedUrl))

    var fabHeight by remember { mutableStateOf(0.dp) }
    var openAddDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            PodAuraTopBar(
                style = PodAuraTopBarStyle.Small,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(R.string.request_headers_screen_name)) },
            )
        },
        floatingActionButton = {
            PodAuraFloatingActionButton(
                onClick = { openAddDialog = true },
                onSizeWithSinglePaddingChanged = { _, height -> fabHeight = height },
                contentDescription = stringResource(R.string.add),
            ) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
            }
        }
    ) { paddingValues ->
        when (val headersState = uiState.headersState) {
            is HeadersState.Failed,
            HeadersState.Init -> Unit

            is HeadersState.Success -> {
                HeadersMap(
                    feedUrl = feedUrl,
                    contentPadding = paddingValues + PaddingValues(bottom = fabHeight),
                    headersState = headersState,
                    dispatcher = dispatcher,
                )
                if (openAddDialog) {
                    AddHeaderDialog(
                        onDismissRequest = { openAddDialog = false },
                        onAdd = { k, v ->
                            dispatcher(
                                RequestHeadersIntent.UpdateHeaders(
                                    feedUrl = feedUrl,
                                    headers = FeedBean.RequestHeaders(
                                        headersState.headers.headers
                                            .toMutableMap().apply { set(k, v) }
                                    )
                                )
                            )
                        }
                    )
                }
            }
        }

        MviEventListener(viewModel.singleEvent) { event ->
            when (event) {
                is RequestHeadersEvent.HeadersResultEvent.Failed ->
                    snackbarHostState.showSnackbar(event.msg)

                is RequestHeadersEvent.UpdateHeadersResultEvent.Failed ->
                    snackbarHostState.showSnackbar(event.msg)

            }
        }

        WaitingDialog(visible = uiState.loadingDialog)
    }
}

@Composable
private fun AddHeaderDialog(
    onDismissRequest: () -> Unit,
    onAdd: (String, String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var key by rememberSaveable { mutableStateOf("") }
    var value by rememberSaveable { mutableStateOf("") }

    PodAuraDialog(
        onDismissRequest = onDismissRequest,
        icon = { Icon(imageVector = Icons.Outlined.Http, contentDescription = null) },
        title = { Text(text = stringResource(id = R.string.add)) },
        text = {
            Column {
                ClipboardTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = key,
                    onValueChange = { key = it },
                    maxLines = 1,
                    placeholder = stringResource(id = R.string.request_headers_screen_key),
                    focusManager = focusManager,
                )
                Spacer(modifier = Modifier.height(12.dp))
                ClipboardTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = value,
                    onValueChange = { value = it },
                    placeholder = stringResource(id = R.string.request_headers_screen_value),
                    autoRequestFocus = false,
                    focusManager = focusManager,
                )
            }
        },
        confirmButton = {
            val confirmButtonEnabled = key.isNotBlank() && value.isNotBlank()
            TextButton(
                enabled = confirmButtonEnabled,
                onClick = {
                    focusManager.clearFocus()
                    onAdd(key, value)
                    onDismissRequest()
                }
            ) {
                Text(
                    text = stringResource(id = R.string.ok),
                    color = if (confirmButtonEnabled) {
                        Color.Unspecified
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(id = R.string.cancel))
            }
        },
    )
}

@Composable
private fun HeadersMap(
    feedUrl: String,
    contentPadding: PaddingValues,
    headersState: HeadersState.Success,
    dispatcher: (RequestHeadersIntent) -> Unit,
) {
    val headersMap = headersState.headers.headers
    val headersList = headersMap.toList()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = rememberLazyListState(),
        contentPadding = contentPadding + PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(headersList.size, key = { headersList[it].first }) { index ->
            val (key, value) = headersList[index]
            HeaderItem(
                key = key,
                value = value,
                onRemove = {
                    dispatcher(
                        RequestHeadersIntent.UpdateHeaders(
                            feedUrl = feedUrl,
                            headers = FeedBean.RequestHeaders(
                                headersMap.toMutableMap().apply { remove(it) }
                            )
                        )
                    )
                },
                onEdit = { k, v ->
                    dispatcher(
                        RequestHeadersIntent.UpdateHeaders(
                            feedUrl = feedUrl,
                            headers = FeedBean.RequestHeaders(
                                headersMap.toMutableMap().apply { set(k, v) }
                            )
                        )
                    )
                },
            )
        }
    }
}

@Composable
private fun HeaderItem(
    key: String,
    value: String,
    onRemove: (String) -> Unit,
    onEdit: (String, String) -> Unit,
) {
    var openEditDialog by rememberSaveable { mutableStateOf<String?>(null) }

    Card(onClick = { openEditDialog = value }) {
        Row(
            modifier = Modifier
                .padding(top = 6.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .weight(1f),
                text = key,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            PodAuraIconButton(
                onClick = { onRemove(key) },
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(id = R.string.remove),
            )
        }
        Text(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 3.dp, bottom = 12.dp),
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 10,
        )
    }

    if (openEditDialog != null) {
        TextFieldDialog(
            titleText = key,
            value = openEditDialog!!,
            onValueChange = { openEditDialog = it },
            onConfirm = {
                onEdit(key, openEditDialog!!)
                openEditDialog = null
            },
            onDismissRequest = { openEditDialog = null },
        )
    }
}