package com.skyd.podaura.ui.screen.about.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skyd.compone.component.dialog.ComponeDialog
import com.skyd.compone.component.dialog.WaitingDialog
import com.skyd.mvi.MviEventListener
import com.skyd.mvi.getDispatcher
import com.skyd.podaura.config.Const
import com.skyd.podaura.ext.httpDomain
import com.skyd.podaura.ext.safeOpenUri
import com.skyd.podaura.model.bean.UpdateBean
import com.skyd.podaura.model.preference.IgnoreUpdateVersionPreference
import com.skyd.podaura.ui.component.webview.PodAuraWebView
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.cancel
import podaura.shared.generated.resources.download_update
import podaura.shared.generated.resources.ok
import podaura.shared.generated.resources.update_check
import podaura.shared.generated.resources.update_ignore
import podaura.shared.generated.resources.update_newer
import podaura.shared.generated.resources.update_newer_text
import podaura.shared.generated.resources.update_no_update


@Composable
fun UpdateDialog(
    silence: Boolean = false,
    isRetry: Boolean = false,
    onSuccess: () -> Unit = {},
    onClosed: () -> Unit = {},
    onError: (String) -> Unit = {},
    viewModel: UpdateViewModel = koinViewModel()
) {
    val uiState by viewModel.viewState.collectAsStateWithLifecycle()

    val dispatch = viewModel.getDispatcher(startWith = UpdateIntent.CheckUpdate(isRetry = false))

    LaunchedEffect(Unit) {
        if (isRetry) {
            dispatch(UpdateIntent.CheckUpdate(isRetry = true))
        }
    }

    WaitingDialog(visible = uiState.loadingDialog && !silence)

    when (val updateUiState = uiState.updateUiState) {
        UpdateUiState.Init -> Unit
        is UpdateUiState.OpenNewerDialog -> {
            val uriHandler = LocalUriHandler.current
            NewerDialog(
                updateBean = updateUiState.data,
                silence = silence,
                onDismissRequest = {
                    onClosed()
                    dispatch(UpdateIntent.CloseDialog)
                },
                onDownloadClick = { uriHandler.safeOpenUri(it?.htmlUrl ?: Const.GITHUB_REPO) },
            )
        }

        UpdateUiState.OpenNoUpdateDialog -> {
            NoUpdateDialog(
                visible = !silence,
                onDismissRequest = {
                    onClosed()
                    dispatch(UpdateIntent.CloseDialog)
                }
            )
        }
    }

    MviEventListener(viewModel.singleEvent) { event ->
        when (event) {
            is UpdateEvent.CheckError -> onError(event.msg)
            is UpdateEvent.CheckSuccess -> onSuccess()
        }
    }
}

@Composable
private fun NewerDialog(
    updateBean: UpdateBean?,
    silence: Boolean,
    onDismissRequest: () -> Unit,
    onDownloadClick: (UpdateBean?) -> Unit,
) {
    val ignoreUpdateVersion = IgnoreUpdateVersionPreference.current
    val scope = rememberCoroutineScope()

    val visible = updateBean != null &&
            (!silence || ignoreUpdateVersion < (updateBean.tagName.toLongOrNull() ?: 0L))

    if (!visible) {
        onDismissRequest()
    }

    ComponeDialog(
        onDismissRequest = onDismissRequest,
        visible = visible,
        icon = { Icon(imageVector = Icons.Outlined.Update, contentDescription = null) },
        title = { Text(text = stringResource(Res.string.update_newer)) },
        selectable = false,
        scrollable = false,
        text = {
            Column {
                Column(
                    modifier = Modifier
                        .weight(weight = 1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    SelectionContainer {
                        Text(
                            text = stringResource(
                                Res.string.update_newer_text,
                                updateBean!!.name,
                                updateBean.publishedAt,
                                updateBean.assets.sumOf { it.downloadCount ?: 0 }.toString(),
                            )
                        )
                    }
                    PodAuraWebView(
                        content = updateBean!!.body,
                        refererDomain = Const.GITHUB_REPO.httpDomain(),
                    )
                }
                val checked = ignoreUpdateVersion == (updateBean!!.tagName.toLongOrNull() ?: 0L)
                Spacer(modifier = Modifier.height(5.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = checked,
                                onValueChange = {
                                    IgnoreUpdateVersionPreference.put(
                                        scope = scope,
                                        value = if (it) {
                                            onDismissRequest()
                                            updateBean.tagName.toLongOrNull() ?: 0L
                                        } else {
                                            0L
                                        }
                                    )
                                },
                                role = Role.Checkbox
                            )
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = null,
                        )
                        Text(
                            modifier = Modifier
                                .padding(start = 16.dp)
                                .padding(vertical = 6.dp),
                            text = stringResource(Res.string.update_ignore),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onDownloadClick(updateBean) }) {
                Text(text = stringResource(Res.string.download_update))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(Res.string.cancel))
            }
        }
    )
}

@Composable
private fun NoUpdateDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
) {
    if (!visible) {
        onDismissRequest()
    }

    ComponeDialog(
        onDismissRequest = onDismissRequest,
        visible = visible,
        icon = { Icon(imageVector = Icons.Outlined.Update, contentDescription = null) },
        title = { Text(text = stringResource(Res.string.update_check)) },
        text = { Text(text = stringResource(Res.string.update_no_update)) },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(Res.string.ok))
            }
        }
    )
}