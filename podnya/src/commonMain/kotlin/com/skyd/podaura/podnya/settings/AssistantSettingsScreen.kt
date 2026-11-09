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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.skyd.compone.component.BackIcon
import com.skyd.compone.component.ComponeScaffold
import com.skyd.compone.component.ComponeTopBar
import com.skyd.compone.component.ComponeTopBarStyle
import com.skyd.compone.component.pointerOnBack
import com.skyd.mvi.MviEventListener
import com.skyd.mvi.getDispatcher
import com.skyd.podaura.podnya.core.AssistantError
import com.skyd.podaura.podnya.ui.podnyaErrorText
import com.skyd.podnya.resources.Res
import com.skyd.podnya.resources.podnya_api_key
import com.skyd.podnya.resources.podnya_api_key_keep
import com.skyd.podnya.resources.podnya_api_key_required
import com.skyd.podnya.resources.podnya_base_url
import com.skyd.podnya.resources.podnya_clear_configuration
import com.skyd.podnya.resources.podnya_connection_succeeded
import com.skyd.podnya.resources.podnya_consent
import com.skyd.podnya.resources.podnya_endpoint_preview
import com.skyd.podnya.resources.podnya_model_id
import com.skyd.podnya.resources.podnya_save
import com.skyd.podnya.resources.podnya_saving
import com.skyd.podnya.resources.podnya_session_only
import com.skyd.podnya.resources.podnya_session_only_description
import com.skyd.podnya.resources.podnya_settings_cleared
import com.skyd.podnya.resources.podnya_settings_description
import com.skyd.podnya.resources.podnya_settings_disclosure
import com.skyd.podnya.resources.podnya_settings_failed
import com.skyd.podnya.resources.podnya_settings_saved
import com.skyd.podnya.resources.podnya_settings_title
import com.skyd.podnya.resources.podnya_test_description
import com.skyd.podnya.resources.podnya_test_saved
import com.skyd.podnya.resources.podnya_testing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    val snackbar = remember { SnackbarHostState() }
    val editable = state.loaded && state.operation == null && state.available
    val normalizedBaseUrl = state.normalizedBaseUrl
    val savedText = stringResource(Res.string.podnya_settings_saved)
    val clearedText = stringResource(Res.string.podnya_settings_cleared)
    val connectedText = stringResource(Res.string.podnya_connection_succeeded)
    val failedText = stringResource(Res.string.podnya_settings_failed)
    val errorTexts = AssistantError.entries.associateWith { podnyaErrorText(it) }
    LaunchedEffect(state.savedRevision) {
        apiKey = ""
        dispatch(AssistantSettingsIntent.SetHasNewKey(false))
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
                viewModel.processIntent(AssistantSettingsIntent.SetHasNewKey(false))
            }
        }
    }

    ComponeScaffold(
        modifier = Modifier.pointerOnBack(onBack = onBack).imePadding(),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            ComponeTopBar(
                style = ComponeTopBarStyle.LargeFlexible,
                title = { Text(stringResource(Res.string.podnya_settings_title)) },
                navigationIcon = { BackIcon(onClick = onBack) },
                windowInsets = windowInsets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            )
        },
        contentWindowInsets = windowInsets,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    stringResource(Res.string.podnya_settings_description),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (!state.available) {
                    Text(
                        podnyaErrorText(AssistantError.UNAVAILABLE),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                OutlinedTextField(
                    value = state.baseUrl,
                    onValueChange = { value ->
                        val recipient = runCatching { normalizeAssistantBaseUrl(value) }.getOrNull()
                        if (recipient == null || recipient != normalizedBaseUrl) {
                            apiKey = ""
                            dispatch(AssistantSettingsIntent.SetHasNewKey(false))
                        }
                        dispatch(AssistantSettingsIntent.SetBaseUrl(value))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = editable,
                    singleLine = true,
                    label = { Text(stringResource(Res.string.podnya_base_url)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    isError = state.baseUrl.isNotBlank() && normalizedBaseUrl == null,
                    supportingText = {
                        if (normalizedBaseUrl != null) {
                            Text(
                                stringResource(
                                    Res.string.podnya_endpoint_preview,
                                    normalizedBaseUrl + assistantEndpointPath(
                                        normalizedBaseUrl,
                                        state.modelId
                                    )
                                )
                            )
                        } else if (state.baseUrl.isNotBlank()) {
                            Text(podnyaErrorText(AssistantError.INVALID_ADDRESS))
                        }
                    },
                )
                OutlinedTextField(
                    value = state.modelId,
                    onValueChange = { dispatch(AssistantSettingsIntent.SetModelId(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = editable,
                    singleLine = true,
                    label = { Text(stringResource(Res.string.podnya_model_id)) },
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = {
                        apiKey = it
                        dispatch(AssistantSettingsIntent.SetHasNewKey(it.isNotEmpty()))
                    },
                    modifier = Modifier.fillMaxWidth(),
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
                SettingsCheckbox(
                    checked = state.sessionOnly,
                    enabled = editable,
                    onCheckedChange = { dispatch(AssistantSettingsIntent.SetSessionOnly(it)) },
                    text = stringResource(Res.string.podnya_session_only),
                    description = stringResource(Res.string.podnya_session_only_description),
                )
                if (normalizedBaseUrl != null) {
                    Text(
                        stringResource(Res.string.podnya_settings_disclosure, normalizedBaseUrl),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    SettingsCheckbox(
                        checked = state.consentAccepted,
                        enabled = editable,
                        onCheckedChange = { dispatch(AssistantSettingsIntent.SetConsentAccepted(it)) },
                        text = stringResource(Res.string.podnya_consent),
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { dispatch(AssistantSettingsIntent.Save(apiKey.takeIf(String::isNotBlank))) },
                        enabled = editable && state.dirty,
                    ) { Text(stringResource(Res.string.podnya_save)) }
                    OutlinedButton(
                        onClick = { dispatch(AssistantSettingsIntent.Verify) },
                        enabled = state.available && state.canVerify,
                    ) { Text(stringResource(Res.string.podnya_test_saved)) }
                }
                when (state.operation) {
                    AssistantSettingsOperation.TESTING -> Text(stringResource(Res.string.podnya_testing))
                    AssistantSettingsOperation.SAVING, AssistantSettingsOperation.CLEARING ->
                        Text(stringResource(Res.string.podnya_saving))

                    null -> Unit
                }
                Text(
                    stringResource(Res.string.podnya_test_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.saved.baseUrl.isNotBlank()) {
                    TextButton(
                        onClick = { dispatch(AssistantSettingsIntent.Clear) },
                        enabled = editable
                    ) {
                        Text(stringResource(Res.string.podnya_clear_configuration))
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCheckbox(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    text: String,
    description: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                checked,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(text, style = MaterialTheme.typography.bodyLarge)
            if (description != null) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
