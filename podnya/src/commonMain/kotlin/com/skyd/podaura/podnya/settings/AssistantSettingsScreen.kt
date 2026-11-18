package com.skyd.podaura.podnya.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.skyd.compone.component.BackIcon
import com.skyd.compone.component.ComponeIconButton
import com.skyd.compone.component.ComponeScaffold
import com.skyd.compone.component.ComponeTopBar
import com.skyd.compone.component.ComponeTopBarStyle
import com.skyd.compone.component.pointerOnBack
import com.skyd.mvi.MviEventListener
import com.skyd.mvi.getDispatcher
import com.skyd.podaura.podnya.core.AssistantError
import com.skyd.podaura.podnya.core.AssistantProtocol
import com.skyd.podaura.podnya.core.AssistantReasoningEffort
import com.skyd.podaura.podnya.core.assistantEndpointPath
import com.skyd.podaura.podnya.core.normalizeAssistantBaseUrl
import com.skyd.podaura.podnya.ui.podnyaErrorText
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
import com.skyd.podnya.resources.podnya_clear_configuration
import com.skyd.podnya.resources.podnya_config_corrupt
import com.skyd.podnya.resources.podnya_config_newer
import com.skyd.podnya.resources.podnya_config_storage
import com.skyd.podnya.resources.podnya_config_unknown_protocol
import com.skyd.podnya.resources.podnya_connection_succeeded
import com.skyd.podnya.resources.podnya_consent
import com.skyd.podnya.resources.podnya_custom_provider
import com.skyd.podnya.resources.podnya_endpoint_preview
import com.skyd.podnya.resources.podnya_manual_model
import com.skyd.podnya.resources.podnya_model_id
import com.skyd.podnya.resources.podnya_ok
import com.skyd.podnya.resources.podnya_openai_compatible
import com.skyd.podnya.resources.podnya_protocol
import com.skyd.podnya.resources.podnya_provider
import com.skyd.podnya.resources.podnya_reasoning_effort
import com.skyd.podnya.resources.podnya_reasoning_reset
import com.skyd.podnya.resources.podnya_reasoning_wait
import com.skyd.podnya.resources.podnya_reload
import com.skyd.podnya.resources.podnya_save
import com.skyd.podnya.resources.podnya_saving
import com.skyd.podnya.resources.podnya_session_only
import com.skyd.podnya.resources.podnya_session_only_description
import com.skyd.podnya.resources.podnya_settings_cleared
import com.skyd.podnya.resources.podnya_settings_description
import com.skyd.podnya.resources.podnya_settings_disclosure
import com.skyd.podnya.resources.podnya_settings_failed
import com.skyd.podnya.resources.podnya_settings_info
import com.skyd.podnya.resources.podnya_settings_saved
import com.skyd.podnya.resources.podnya_settings_title
import com.skyd.podnya.resources.podnya_switch_confirm
import com.skyd.podnya.resources.podnya_switch_message
import com.skyd.podnya.resources.podnya_switch_title
import com.skyd.podnya.resources.podnya_test_cost
import com.skyd.podnya.resources.podnya_test_description
import com.skyd.podnya.resources.podnya_test_saved
import com.skyd.podnya.resources.podnya_testing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun AssistantSettingsScreen(
    onBack: () -> Unit,
    viewModel: AssistantSettingsViewModel,
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    val dispatch = viewModel.getDispatcher(startWith = null)
    // Never rememberSaveable: neither credentials nor chat belong in saved instance state.
    var apiKey by remember { mutableStateOf("") }
    var keyRevision by remember { mutableStateOf(state.keyRevision) }
    var showInfo by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val editable = state.editable
    val normalizedBaseUrl = state.normalizedBaseUrl
    val savedText = stringResource(Res.string.podnya_settings_saved)
    val clearedText = stringResource(Res.string.podnya_settings_cleared)
    val connectedText = stringResource(Res.string.podnya_connection_succeeded)
    val failedText = stringResource(Res.string.podnya_settings_failed)
    val errorTexts = AssistantError.entries.associateWith { podnyaErrorText(it) }
    LaunchedEffect(state.keyRevision) {
        if (keyRevision != state.keyRevision) {
            apiKey = ""
            keyRevision = state.keyRevision
        }
    }
    MviEventListener(viewModel.singleEvent) { event ->
        snackbar.showSnackbar(
            when (event) {
                AssistantSettingsEvent.Saved -> savedText
                AssistantSettingsEvent.Cleared -> clearedText
                AssistantSettingsEvent.ConnectionSucceeded -> connectedText
                is AssistantSettingsEvent.Failed -> event.error?.let(errorTexts::get) ?: failedText
            }
        )
    }
    LifecycleStartEffect(viewModel) {
        onStopOrDispose {
            // The composition dispatcher is cancelled on disposal; cleanup belongs to the VM.
            viewModel.viewModelScope.launch(Dispatchers.Main.immediate) {
                viewModel.processIntent(AssistantSettingsIntent.CancelVerification)
            }
        }
    }
    DisposableEffect(viewModel) {
        onDispose {
            apiKey = ""
            viewModel.viewModelScope.launch(Dispatchers.Main.immediate) {
                viewModel.processIntent(
                    AssistantSettingsIntent.SetHasNewKey(
                        value = false,
                        keyRevision = keyRevision
                    )
                )
            }
        }
    }

    ComponeScaffold(
        modifier = Modifier
            .testTag("podnya_settings_screen")
            .pointerOnBack(onBack = onBack)
            .imePadding(),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            ComponeTopBar(
                style = ComponeTopBarStyle.LargeFlexible,
                title = { Text(stringResource(Res.string.podnya_settings_title)) },
                navigationIcon = { BackIcon(onClick = onBack) },
                actions = {
                    if (state.saved.baseUrl.isNotBlank() || state.saved.readError != null) {
                        ComponeIconButton(
                            onClick = { dispatch(AssistantSettingsIntent.Clear) },
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = stringResource(Res.string.podnya_clear_configuration),
                            enabled = state.loaded && state.operation == null && state.available,
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                        )
                    }
                    ComponeIconButton(
                        onClick = { showInfo = true },
                        imageVector = Icons.Outlined.Info,
                        contentDescription = stringResource(Res.string.podnya_settings_info),
                    )
                },
                windowInsets = windowInsets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            )
        },
        contentWindowInsets = windowInsets,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .testTag("podnya_settings_form")
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (!state.available) {
                    Text(
                        podnyaErrorText(AssistantError.UNAVAILABLE),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                state.saved.readError?.let { error ->
                    Text(
                        text = stringResource(
                            when (error) {
                                AssistantConfigurationReadError.NEWER_FORMAT -> Res.string.podnya_config_newer
                                AssistantConfigurationReadError.UNKNOWN_PROTOCOL -> Res.string.podnya_config_unknown_protocol
                                AssistantConfigurationReadError.CORRUPT -> Res.string.podnya_config_corrupt
                                AssistantConfigurationReadError.STORAGE -> Res.string.podnya_config_storage
                            }
                        ),
                        color = MaterialTheme.colorScheme.error,
                    )
                    OutlinedButton(onClick = { dispatch(AssistantSettingsIntent.Reload) }) {
                        Text(stringResource(Res.string.podnya_reload))
                    }
                }
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
                                { Text(podnyaErrorText(AssistantError.INVALID_ADDRESS)) }
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
                    SettingsCheckbox(
                        checked = state.sessionOnly,
                        enabled = editable,
                        onCheckedChange = { dispatch(AssistantSettingsIntent.SetSessionOnly(it)) },
                        text = stringResource(Res.string.podnya_session_only),
                    )
                    if (normalizedBaseUrl != null) {
                        SettingsCheckbox(
                            checked = state.consentAccepted,
                            enabled = editable,
                            onCheckedChange = {
                                dispatch(
                                    AssistantSettingsIntent.SetConsentAccepted(it)
                                )
                            },
                            text = stringResource(Res.string.podnya_consent),
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = {
                                dispatch(
                                    AssistantSettingsIntent.Save(
                                        apiKey = apiKey.takeIf { keyRevision == state.keyRevision && it.isNotBlank() },
                                        editRevision = state.editRevision,
                                        keyRevision = keyRevision,
                                    )
                                )
                            },
                            enabled = editable && state.dirty,
                        ) { Text(stringResource(Res.string.podnya_save)) }
                        OutlinedButton(
                            onClick = { dispatch(AssistantSettingsIntent.Verify) },
                            enabled = state.available && state.canVerify,
                        ) { Text(stringResource(Res.string.podnya_test_saved)) }
                    }
                    state.failure?.let { failure ->
                        Text(
                            text = failure.error?.let(errorTexts::get) ?: failedText,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    when (state.operation) {
                        AssistantSettingsOperation.TESTING -> Text(stringResource(Res.string.podnya_testing))
                        AssistantSettingsOperation.SAVING, AssistantSettingsOperation.CLEARING ->
                            Text(stringResource(Res.string.podnya_saving))

                        null -> Unit
                    }
                    Text(
                        stringResource(Res.string.podnya_test_cost),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
    state.pendingSwitch?.let { pending ->
        AlertDialog(
            onDismissRequest = { dispatch(AssistantSettingsIntent.CancelSwitch) },
            title = { Text(stringResource(Res.string.podnya_switch_title)) },
            text = { Text(stringResource(Res.string.podnya_switch_message)) },
            confirmButton = {
                TextButton(onClick = { dispatch(AssistantSettingsIntent.ConfirmSwitch(pending.editRevision)) }) {
                    Text(stringResource(Res.string.podnya_switch_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { dispatch(AssistantSettingsIntent.CancelSwitch) }) {
                    Text(stringResource(Res.string.podnya_cancel))
                }
            },
        )
    }
    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            icon = { Icon(imageVector = Icons.Outlined.Info, contentDescription = null) },
            title = { Text(stringResource(Res.string.podnya_settings_info)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Text(stringResource(Res.string.podnya_settings_description))
                    Text(protocolLabel(state.protocol))
                    if (normalizedBaseUrl != null) {
                        Text(
                            stringResource(
                                Res.string.podnya_endpoint_preview,
                                normalizedBaseUrl + assistantEndpointPath(
                                    baseUrl = normalizedBaseUrl,
                                    modelId = state.modelId,
                                    protocol = state.protocol,
                                ),
                            )
                        )
                        Text(
                            stringResource(
                                Res.string.podnya_settings_disclosure,
                                normalizedBaseUrl
                            )
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(Res.string.podnya_session_only),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(stringResource(Res.string.podnya_session_only_description))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(Res.string.podnya_test_saved),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(stringResource(Res.string.podnya_test_description))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfo = false }) {
                    Text(stringResource(Res.string.podnya_ok))
                }
            },
        )
    }
}

@Composable
private fun SettingsCheckbox(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    text: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
        Text(
            text = text,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun protocolLabel(protocol: AssistantProtocol): String = when (protocol) {
    AssistantProtocol.OPENAI_COMPATIBLE -> stringResource(Res.string.podnya_openai_compatible)
    AssistantProtocol.ANTHROPIC_MESSAGES -> "Claude"
    AssistantProtocol.GEMINI_GENERATE_CONTENT -> "Gemini"
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
private fun SettingsChoice(
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
