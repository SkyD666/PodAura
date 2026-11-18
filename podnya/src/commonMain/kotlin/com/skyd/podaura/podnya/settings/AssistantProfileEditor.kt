package com.skyd.podaura.podnya.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.skyd.podaura.podnya.core.AssistantError
import com.skyd.podaura.podnya.core.AssistantProtocol
import com.skyd.podaura.podnya.core.AssistantReasoningEffort
import com.skyd.podaura.podnya.core.normalizeAssistantBaseUrl
import com.skyd.podaura.podnya.ui.podnyaFailureText
import com.skyd.podnya.resources.Res
import com.skyd.podnya.resources.ic_model_claude
import com.skyd.podnya.resources.ic_model_deepseek
import com.skyd.podnya.resources.ic_model_gemini
import com.skyd.podnya.resources.ic_model_grok
import com.skyd.podnya.resources.ic_model_openai
import com.skyd.podnya.resources.podnya_api_key
import com.skyd.podnya.resources.podnya_api_key_keep
import com.skyd.podnya.resources.podnya_api_key_required
import com.skyd.podnya.resources.podnya_base_url
import com.skyd.podnya.resources.podnya_cancel
import com.skyd.podnya.resources.podnya_choose_provider
import com.skyd.podnya.resources.podnya_create_and_use
import com.skyd.podnya.resources.podnya_create_profile
import com.skyd.podnya.resources.podnya_custom_provider
import com.skyd.podnya.resources.podnya_manual_model
import com.skyd.podnya.resources.podnya_model_id
import com.skyd.podnya.resources.podnya_profile_name
import com.skyd.podnya.resources.podnya_protocol
import com.skyd.podnya.resources.podnya_provider
import com.skyd.podnya.resources.podnya_reasoning_effort
import com.skyd.podnya.resources.podnya_reasoning_reset
import com.skyd.podnya.resources.podnya_reasoning_wait
import com.skyd.podnya.resources.podnya_reload_conflicting_profile
import com.skyd.podnya.resources.podnya_save_and_use
import com.skyd.podnya.resources.podnya_save_changes
import com.skyd.podnya.resources.podnya_saving
import com.skyd.podnya.resources.podnya_settings_failed
import com.skyd.podnya.resources.podnya_test_cost
import com.skyd.podnya.resources.podnya_test_saved
import com.skyd.podnya.resources.podnya_testing
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AssistantProfileEditor(
    state: AssistantSettingsState,
    dispatch: (AssistantSettingsIntent) -> Unit,
) {
    // Never rememberSaveable: neither credentials nor chat belong in saved instance state.
    var apiKey by remember { mutableStateOf("") }
    var keyRevision by remember { mutableStateOf(state.keyRevision) }
    LaunchedEffect(state.keyRevision) {
        if (keyRevision != state.keyRevision) {
            apiKey = ""
            keyRevision = state.keyRevision
        }
    }
    val editable = state.editable
    val normalizedBaseUrl = state.normalizedBaseUrl
    val failedText = stringResource(Res.string.podnya_settings_failed)

    fun save(activate: Boolean) {
        dispatch(
            AssistantSettingsIntent.Save(
                apiKey = apiKey.takeIf { keyRevision == state.keyRevision && it.isNotBlank() },
                editRevision = state.editRevision,
                keyRevision = keyRevision,
                activate = activate,
            )
        )
    }

    OutlinedTextField(
        value = state.name,
        onValueChange = { dispatch(AssistantSettingsIntent.SetName(it)) },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("podnya_profile_name"),
        label = { Text(stringResource(Res.string.podnya_profile_name)) },
        placeholder = { Text(state.modelId) },
        singleLine = true,
        enabled = editable,
    )
    SettingsChoice(
        tag = "podnya_provider",
        label = stringResource(Res.string.podnya_provider),
        selected = AssistantProviderPresets.find(state.providerId)?.displayName
            ?: stringResource(if (state.providerId == null) Res.string.podnya_choose_provider else Res.string.podnya_custom_provider),
        options = AssistantProviderPresets.entries.map { it.id to it.displayName } +
                (AssistantProviderPresets.CUSTOM to stringResource(Res.string.podnya_custom_provider)),
        selectedId = if (state.custom) AssistantProviderPresets.CUSTOM else state.providerId,
        enabled = editable,
        optionIcon = { ProviderIcon(it) },
        onSelect = { dispatch(AssistantSettingsIntent.SelectProvider(it)) },
    )
    if (state.providerId != null && state.saved.readError == null) {
        if (state.custom) {
            OutlinedTextField(
                value = state.baseUrl,
                onValueChange = { value ->
                    val recipient = runCatching {
                        normalizeAssistantBaseUrl(
                            input = value,
                            protocol = state.protocol
                        )
                    }.getOrNull()
                    if (recipient == null || recipient != normalizedBaseUrl) {
                        apiKey = ""
                        dispatch(
                            AssistantSettingsIntent.SetHasNewKey(
                                value = false,
                                keyRevision = keyRevision
                            )
                        )
                    }
                    dispatch(AssistantSettingsIntent.SetBaseUrl(value))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("podnya_address"),
                enabled = editable,
                singleLine = true,
                label = { Text(stringResource(Res.string.podnya_base_url)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                isError = state.baseUrl.isNotBlank() && normalizedBaseUrl == null,
                supportingText = if (state.baseUrl.isNotBlank() && normalizedBaseUrl == null) {
                    { Text(podnyaFailureText(AssistantError.INVALID_ADDRESS)) }
                } else {
                    null
                },
            )
            SettingsChoice(
                tag = "podnya_protocol",
                label = stringResource(Res.string.podnya_protocol),
                selected = protocolLabel(state.protocol),
                options = AssistantProtocol.entries.map { it.id to protocolLabel(it) },
                selectedId = state.protocol.id,
                enabled = editable,
                onSelect = { id ->
                    AssistantProtocol.fromId(id)
                        ?.let { dispatch(AssistantSettingsIntent.SelectProtocol(it)) }
                },
            )
        }
        OutlinedTextField(
            value = if (keyRevision == state.keyRevision) apiKey else "",
            onValueChange = {
                apiKey = it
                keyRevision = state.keyRevision
                dispatch(
                    AssistantSettingsIntent.SetHasNewKey(
                        value = it.isNotEmpty(),
                        keyRevision = keyRevision
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("podnya_key"),
            enabled = editable,
            singleLine = true,
            label = { Text(stringResource(Res.string.podnya_api_key)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            supportingText = {
                Text(
                    stringResource(
                        if (state.sameRecipient && state.saved.hasApiKey) Res.string.podnya_api_key_keep
                        else Res.string.podnya_api_key_required,
                    )
                )
            },
        )
        val preset = AssistantProviderPresets.find(state.providerId)
        if (preset != null) {
            val manual = stringResource(Res.string.podnya_manual_model)
            SettingsChoice(
                tag = "podnya_model_choice",
                label = stringResource(Res.string.podnya_model_id),
                selected = if (state.manualModel) manual else state.modelId,
                options = preset.models.map { it to it } + ("" to manual),
                selectedId = if (state.manualModel) "" else state.modelId,
                enabled = editable,
                onSelect = { id ->
                    dispatch(AssistantSettingsIntent.SetManualModel(id.isEmpty()))
                    if (id.isNotEmpty()) dispatch(AssistantSettingsIntent.SetModelId(id))
                },
            )
        }
        if (preset == null || state.manualModel) {
            OutlinedTextField(
                value = state.modelId,
                onValueChange = { dispatch(AssistantSettingsIntent.SetModelId(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("podnya_model_input"),
                enabled = editable,
                singleLine = true,
                label = { Text(stringResource(Res.string.podnya_model_id)) },
            )
        }
        if (state.supportsReasoningEffort) {
            SettingsChoice(
                tag = "podnya_reasoning_effort",
                label = stringResource(Res.string.podnya_reasoning_effort),
                selected = state.reasoningEffort.id,
                options = AssistantReasoningEffort.entries.map { it.id to it.id },
                selectedId = state.reasoningEffort.id,
                enabled = editable,
                onSelect = { id ->
                    AssistantReasoningEffort.fromId(id)?.let {
                        dispatch(AssistantSettingsIntent.SetReasoningEffort(it))
                    }
                },
            )
            if (state.reasoningEffort != AssistantReasoningEffort.LOW) {
                Text(
                    text = stringResource(Res.string.podnya_reasoning_wait),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (state.reasoningEffortReset) {
            Text(
                text = stringResource(Res.string.podnya_reasoning_reset),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (state.hasEditConflict) {
            OutlinedButton(
                onClick = { dispatch(AssistantSettingsIntent.DiscardConflictingEdits) },
                enabled = editable,
            ) { Text(stringResource(Res.string.podnya_reload_conflicting_profile)) }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = { save(false) },
                enabled = state.canSave,
                modifier = Modifier.testTag("podnya_save_profile"),
            ) {
                val label = when {
                    state.saved.configId != null -> Res.string.podnya_save_changes
                    state.profiles.profiles.isEmpty() -> Res.string.podnya_create_and_use
                    else -> Res.string.podnya_create_profile
                }
                Text(stringResource(label))
            }
            if (state.profiles.activeProfileId != null &&
                state.saved.configId != state.profiles.activeProfileId
            ) {
                OutlinedButton(
                    onClick = { save(true) },
                    enabled = state.canSave,
                ) { Text(stringResource(Res.string.podnya_save_and_use)) }
            }
            if (state.saved.configId != null) {
                OutlinedButton(
                    onClick = { dispatch(AssistantSettingsIntent.Verify) },
                    enabled = state.available && state.canVerify,
                ) { Text(stringResource(Res.string.podnya_test_saved)) }
            }
            TextButton(
                onClick = { dispatch(AssistantSettingsIntent.CloseEditor) },
                enabled = state.operation != AssistantSettingsOperation.SAVING && state.operation != AssistantSettingsOperation.CLEARING,
                modifier = Modifier.testTag("podnya_cancel_editor"),
            ) {
                Text(stringResource(Res.string.podnya_cancel))
            }
        }
        state.failure?.let { failure ->
            Text(
                text = failure.error?.let { podnyaFailureText(it) } ?: failedText,
                color = MaterialTheme.colorScheme.error,
            )
        }
        when (state.operation) {
            AssistantSettingsOperation.TESTING -> Text(stringResource(Res.string.podnya_testing))
            AssistantSettingsOperation.SAVING, AssistantSettingsOperation.CLEARING ->
                Text(stringResource(Res.string.podnya_saving))

            null -> Unit
        }
        if (state.saved.configId != null) {
            Text(
                stringResource(Res.string.podnya_test_cost),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProviderIcon(providerId: String) {
    val icon = when (providerId) {
        "openai" -> Res.drawable.ic_model_openai
        "deepseek" -> Res.drawable.ic_model_deepseek
        "claude" -> Res.drawable.ic_model_claude
        "gemini" -> Res.drawable.ic_model_gemini
        "grok" -> Res.drawable.ic_model_grok
        else -> null
    }
    if (icon != null) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
    } else {
        Icon(
            imageVector = Icons.Outlined.Tune,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
internal fun SettingsChoice(
    tag: String,
    label: String,
    selected: String,
    options: List<Pair<String, String>>,
    selectedId: String?,
    enabled: Boolean,
    optionIcon: (@Composable (String) -> Unit)? = null,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { if (enabled) expanded = it }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            leadingIcon = if (optionIcon != null && selectedId != null) {
                { optionIcon(selectedId) }
            } else {
                null
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(tag)
                .menuAnchor(
                    type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                    enabled = enabled
                ),
        )
        ExposedDropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false }) {
            options.forEach { (id, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    leadingIcon = if (optionIcon != null) {
                        { optionIcon(id) }
                    } else {
                        null
                    },
                    modifier = Modifier.semantics { this.selected = id == selectedId },
                    trailingIcon = {
                        if (id == selectedId) {
                            Icon(imageVector = Icons.Outlined.Check, contentDescription = null)
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelect(id)
                    },
                )
            }
        }
    }
}
