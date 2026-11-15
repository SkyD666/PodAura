package com.skyd.podaura.podnya.ui.screen.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skyd.compone.component.ComponeIconButton
import com.skyd.compone.component.ComponeTooltipBox
import com.skyd.fundation.util.isJvm
import com.skyd.fundation.util.platform
import com.skyd.podaura.podnya.core.AssistantError
import com.skyd.podaura.podnya.core.AssistantLimits
import com.skyd.podaura.podnya.ui.podnyaErrorText
import com.skyd.podnya.resources.Res
import com.skyd.podnya.resources.podnya_composer_hint
import com.skyd.podnya.resources.podnya_model_unavailable
import com.skyd.podnya.resources.podnya_select_current_model
import com.skyd.podnya.resources.podnya_send
import com.skyd.podnya.resources.podnya_send_shortcut
import com.skyd.podnya.resources.podnya_settings_read_failed
import com.skyd.podnya.resources.podnya_settings_title
import com.skyd.podnya.resources.podnya_stop
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PodnyaInputArea(
    uiState: PodnyaState,
    input: TextFieldValue,
    onInput: (TextFieldValue) -> Unit,
    onSend: () -> Unit,
    dispatch: (PodnyaIntent) -> Unit,
) {
    val state = uiState.session
    val settings = uiState.settings
    val ready = uiState.ready
    Column(
        modifier = Modifier
            .widthIn(max = 840.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (uiState.available &&
            (settings != null || uiState.settingsFailed) &&
            (uiState.settingsFailed || !uiState.modelAvailable || settings?.configured != true || !settings.consentAccepted)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = if (uiState.settingsFailed) stringResource(Res.string.podnya_settings_read_failed)
                    else if (!uiState.modelAvailable) stringResource(Res.string.podnya_model_unavailable)
                    else podnyaErrorText(if (settings?.configured == true) AssistantError.DISCLOSURE_REQUIRED else AssistantError.CONFIGURATION_REQUIRED),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ComponeIconButton(
                    onClick = { dispatch(PodnyaIntent.OpenSettings) },
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = stringResource(Res.string.podnya_settings_title)
                )
            }
        }
        if (!uiState.modelAvailable && settings?.configured == true) {
            TextButton(
                onClick = {
                    settings.configId?.let {
                        dispatch(PodnyaIntent.SelectModel(state.id, it))
                    }
                }
            ) {
                Text(
                    stringResource(
                        resource = Res.string.podnya_select_current_model,
                        settings.modelId
                    )
                )
            }
        }
        MessageInputBox(
            input = input,
            onInput = onInput,
            ready = ready,
            running = uiState.running,
            onSend = onSend,
            onStop = { dispatch(PodnyaIntent.Stop(state.id)) },
        )
    }
}

@Composable
private fun MessageInputBox(
    input: TextFieldValue,
    onInput: (TextFieldValue) -> Unit,
    ready: Boolean,
    running: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val hint = stringResource(Res.string.podnya_composer_hint)
    val action = stringResource(if (running) Res.string.podnya_stop else Res.string.podnya_send)
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            width = 1.dp,
            color = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
    ) {
        Column {
            TextField(
                value = input,
                onValueChange = onInput,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("podnya_composer")
                    .semantics { contentDescription = hint }
                    .onFocusChanged { focused = it.isFocused }
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown &&
                            (event.key == Key.Enter || event.key == Key.NumPadEnter) &&
                            !event.isShiftPressed && input.composition == null
                        ) {
                            if (ready && !running && input.text.isNotBlank()) onSend()
                            true
                        } else false
                    },
                placeholder = { Text(hint) }, minLines = 1, maxLines = 5,
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (platform.isJvm) {
                    Text(
                        stringResource(Res.string.podnya_send_shortcut),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                if (input.text.length >= AssistantLimits.INPUT_CHARS * 9 / 10) {
                    Text(
                        "${input.text.length}/${AssistantLimits.INPUT_CHARS}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (input.text.length == AssistantLimits.INPUT_CHARS) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ComponeTooltipBox(
                    tooltip = { PlainTooltip { Text(action) } },
                    state = rememberTooltipState()
                ) {
                    FilledIconButton(
                        onClick = if (running) onStop else onSend,
                        enabled = running || (ready && input.text.isNotBlank()),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag(if (running) "podnya_stop" else "podnya_send"),
                    ) {
                        Icon(
                            imageVector = if (running) Icons.Outlined.Stop else Icons.Outlined.ArrowUpward,
                            contentDescription = action
                        )
                    }
                }
            }
        }
    }
}
