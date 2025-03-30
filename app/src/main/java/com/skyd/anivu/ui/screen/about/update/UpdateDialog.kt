package com.skyd.anivu.ui.screen.about.update

import android.text.Html
import android.text.method.LinkMovementMethod
import android.widget.TextView
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
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skyd.anivu.R
import com.skyd.anivu.base.mvi.MviEventListener
import com.skyd.anivu.base.mvi.getDispatcher
import com.skyd.anivu.model.bean.UpdateBean
import com.skyd.anivu.model.preference.IgnoreUpdateVersionPreference
import com.skyd.anivu.ui.component.dialog.PodAuraDialog
import com.skyd.anivu.ui.component.dialog.WaitingDialog
import com.skyd.generated.preference.LocalIgnoreUpdateVersion
import okhttp3.internal.toLongOrDefault


@Composable
fun UpdateDialog(
    silence: Boolean = false,
    isRetry: Boolean = false,
    onSuccess: () -> Unit = {},
    onClosed: () -> Unit = {},
    onError: (String) -> Unit = {},
    viewModel: UpdateViewModel = hiltViewModel()
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
            NewerDialog(
                updateBean = updateUiState.data,
                silence = silence,
                onDismissRequest = {
                    onClosed()
                    dispatch(UpdateIntent.CloseDialog)
                },
                onDownloadClick = { updateBean ->
                    dispatch(
                        UpdateIntent.Update(updateBean?.htmlUrl)
                    )
                }
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
    val context = LocalContext.current
    val ignoreUpdateVersion = LocalIgnoreUpdateVersion.current
    val scope = rememberCoroutineScope()

    val visible = updateBean != null &&
            (!silence || ignoreUpdateVersion < updateBean.tagName.toLongOrDefault(0L))

    if (!visible) {
        onDismissRequest()
    }

    PodAuraDialog(
        onDismissRequest = onDismissRequest,
        visible = visible,
        icon = { Icon(imageVector = Icons.Outlined.Update, contentDescription = null) },
        title = { Text(text = stringResource(R.string.update_newer)) },
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
                                R.string.update_newer_text,
                                updateBean!!.name,
                                updateBean.publishedAt,
                                updateBean.assets.sumOf { it.downloadCount ?: 0 }.toString(),
                            )
                        )
                    }
                    val textColor = LocalContentColor.current
                    AndroidView(
                        factory = { context ->
                            TextView(context).apply {
                                setTextColor(textColor.toArgb())
                                setTextIsSelectable(true)
                                movementMethod = LinkMovementMethod.getInstance()
                                isSingleLine = false
                                text = Html.fromHtml(updateBean!!.body, Html.FROM_HTML_MODE_COMPACT)
                            }
                        }
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
                                        context = context,
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
                            text = stringResource(R.string.update_ignore),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onDownloadClick(updateBean) }) {
                Text(text = stringResource(id = R.string.download_update))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(id = R.string.cancel))
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

    PodAuraDialog(
        onDismissRequest = onDismissRequest,
        visible = visible,
        icon = { Icon(imageVector = Icons.Outlined.Update, contentDescription = null) },
        title = { Text(text = stringResource(R.string.update_check)) },
        text = { Text(text = stringResource(R.string.update_no_update)) },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(id = R.string.ok))
            }
        }
    )
}