package com.skyd.podaura.podnya.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.skyd.compone.component.BackIcon
import com.skyd.compone.component.ComponeIconButton
import com.skyd.compone.component.ComponeTopBar
import com.skyd.compone.component.ComponeTopBarStyle
import com.skyd.compone.component.pointerOnBack
import com.skyd.mvi.MviEventListener
import com.skyd.mvi.getDispatcher
import com.skyd.podaura.podnya.core.AssistantError
import com.skyd.podaura.podnya.core.AssistantProtocol
import com.skyd.podaura.podnya.core.assistantEndpointPath
import com.skyd.podaura.podnya.ui.podnyaErrorText
import com.skyd.podnya.resources.Res
import com.skyd.podnya.resources.podnya_add_profile
import com.skyd.podnya.resources.podnya_clear_configuration
import com.skyd.podnya.resources.podnya_config_corrupt
import com.skyd.podnya.resources.podnya_config_newer
import com.skyd.podnya.resources.podnya_config_storage
import com.skyd.podnya.resources.podnya_config_unknown_protocol
import com.skyd.podnya.resources.podnya_connection_succeeded
import com.skyd.podnya.resources.podnya_delete_replacement
import com.skyd.podnya.resources.podnya_edit_named_profile
import com.skyd.podnya.resources.podnya_edit_profile
import com.skyd.podnya.resources.podnya_endpoint_preview
import com.skyd.podnya.resources.podnya_no_profiles
import com.skyd.podnya.resources.podnya_ok
import com.skyd.podnya.resources.podnya_openai_compatible
import com.skyd.podnya.resources.podnya_profiles
import com.skyd.podnya.resources.podnya_reload
import com.skyd.podnya.resources.podnya_settings_cleared
import com.skyd.podnya.resources.podnya_settings_description
import com.skyd.podnya.resources.podnya_settings_disclosure
import com.skyd.podnya.resources.podnya_settings_failed
import com.skyd.podnya.resources.podnya_settings_info
import com.skyd.podnya.resources.podnya_settings_saved
import com.skyd.podnya.resources.podnya_test_description
import com.skyd.podnya.resources.podnya_test_saved
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
    var showInfo by remember(state.editing, state.saved.configId) { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val normalizedBaseUrl = state.normalizedBaseUrl
    val savedText = stringResource(Res.string.podnya_settings_saved)
    val clearedText = stringResource(Res.string.podnya_settings_cleared)
    val connectedText = stringResource(Res.string.podnya_connection_succeeded)
    val failedText = stringResource(Res.string.podnya_settings_failed)
    val errorTexts = AssistantError.entries.associateWith { podnyaErrorText(it) }
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
            viewModel.viewModelScope.launch(Dispatchers.Main.immediate) {
                viewModel.processIntent(
                    AssistantSettingsIntent.SetHasNewKey(
                        value = false,
                        keyRevision = state.keyRevision
                    )
                )
            }
        }
    }

    fun back() {
        if (state.editing) dispatch(AssistantSettingsIntent.CloseEditor) else onBack()
    }
    if (state.editing) {
        NavigationBackHandler(
            state = rememberNavigationEventState(NavigationEventInfo.None),
            isBackEnabled = true,
            onBackCompleted = ::back,
        )
    }
    Scaffold(
        modifier = Modifier
            .testTag("podnya_settings_screen")
            .pointerOnBack(onBack = ::back)
            .imePadding(),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            ComponeTopBar(
                style = ComponeTopBarStyle.LargeFlexible,
                title = {
                    val title = when {
                        !state.editing -> Res.string.podnya_profiles
                        state.saved.configId == null -> Res.string.podnya_add_profile
                        else -> Res.string.podnya_edit_profile
                    }
                    Crossfade(
                        targetState = title,
                        label = "Profile title"
                    ) { Text(stringResource(it)) }
                },
                navigationIcon = { BackIcon(onClick = ::back) },
                actions = {
                    if ((state.editing && state.saved.configId != null) || state.saved.readError != null) {
                        val deleteId = state.saved.configId
                        key(deleteId, state.profiles.activeProfileId) {
                            var chooseReplacement by remember { mutableStateOf(false) }
                            Box {
                                ComponeIconButton(
                                    onClick = {
                                        if (state.saved.configId == state.profiles.activeProfileId && state.profiles.profiles.size > 1) {
                                            chooseReplacement = true
                                        } else {
                                            dispatch(AssistantSettingsIntent.Clear(profileId = deleteId))
                                        }
                                    },
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = stringResource(Res.string.podnya_clear_configuration),
                                    enabled = state.loaded && state.operation == null && state.available,
                                    colors = IconButtonDefaults.iconButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error,
                                    ),
                                )
                                DropdownMenu(
                                    expanded = chooseReplacement,
                                    onDismissRequest = { chooseReplacement = false },
                                ) {
                                    Text(
                                        text = stringResource(Res.string.podnya_delete_replacement),
                                        modifier = Modifier.padding(12.dp),
                                    )
                                    state.profiles.profiles.filter { it.configId != state.saved.configId }
                                        .forEach { profile ->
                                            DropdownMenuItem(
                                                text = { Text(state.profiles.label(profile)) },
                                                onClick = {
                                                    chooseReplacement = false
                                                    dispatch(
                                                        AssistantSettingsIntent.Clear(
                                                            profileId = deleteId,
                                                            replacementId = profile.configId
                                                        )
                                                    )
                                                },
                                            )
                                        }
                                }
                            }
                        }
                    }
                    if (state.editing) {
                        ComponeIconButton(
                            onClick = { showInfo = true },
                            imageVector = Icons.Outlined.Info,
                            contentDescription = stringResource(Res.string.podnya_settings_info),
                        )
                    }
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
            val listScroll = rememberScrollState()
            AnimatedContent(
                targetState = state,
                contentKey = { it.editing },
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxSize()
                    .clipToBounds(),
                transitionSpec = {
                    val direction =
                        if (targetState.editing) SlideDirection.Start else SlideDirection.End
                    val slide = tween<IntOffset>(durationMillis = 260)
                    slideIntoContainer(towards = direction, animationSpec = slide) togetherWith
                            slideOutOfContainer(towards = direction, animationSpec = slide)
                },
                label = "Profile editor navigation",
            ) { page ->
                val send: (AssistantSettingsIntent) -> Unit = { intent ->
                    if (page.editing == state.editing) dispatch(intent)
                }
                Column(
                    modifier = Modifier
                        .testTag(if (page.editing) "podnya_settings_form" else "podnya_profiles_list")
                        .fillMaxWidth()
                        .verticalScroll(if (page.editing) rememberScrollState() else listScroll)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (!page.available) {
                        Text(
                            text = podnyaErrorText(AssistantError.UNAVAILABLE),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    page.saved.readError?.let { error ->
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
                        OutlinedButton(onClick = { send(AssistantSettingsIntent.Reload) }) {
                            Text(stringResource(Res.string.podnya_reload))
                        }
                    }
                    if (page.loaded && page.saved.readError == null) {
                        if (page.editing) {
                            AssistantProfileEditor(state = page, dispatch = send)
                        } else {
                            ProfileList(
                                profiles = page.profiles,
                                enabled = page.editable,
                                onSelect = { send(AssistantSettingsIntent.SelectProfile(it)) },
                                onEdit = { send(AssistantSettingsIntent.EditProfile(it)) },
                            )
                        }
                    }
                }
            }
        }
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
private fun ProfileList(
    profiles: AssistantProfiles,
    enabled: Boolean,
    onSelect: (String) -> Unit,
    onEdit: (String?) -> Unit,
) {
    if (profiles.profiles.isEmpty()) {
        Text(
            text = stringResource(Res.string.podnya_no_profiles),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
    Button(
        onClick = { onEdit(null) },
        enabled = enabled,
        modifier = Modifier.testTag("podnya_add_profile"),
    ) {
        Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
        Text(
            text = stringResource(Res.string.podnya_add_profile),
            modifier = Modifier.padding(start = 8.dp),
        )
    }
    Column(modifier = Modifier.selectableGroup()) {
        profiles.profiles.forEachIndexed { index, profile ->
            if (index > 0) HorizontalDivider()
            val id = requireNotNull(profile.configId)
            val selected = id == profiles.activeProfileId
            val provider =
                AssistantProviderPresets.find(AssistantProviderPresets.selection(profile))
            val label = profiles.label(profile)
            val subtitle = listOfNotNull(
                provider?.displayName ?: profile.baseUrl.takeIf { label == profile.name },
                profile.modelId.takeUnless { it == profile.name },
            ).joinToString(" · ")
            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selected,
                        enabled = enabled,
                        role = Role.RadioButton,
                        onClick = { onSelect(id) },
                    ),
                leadingContent = {
                    RadioButton(selected = selected, onClick = null, enabled = enabled)
                },
                headlineContent = { Text(label) },
                supportingContent = if (subtitle.isEmpty()) null else {
                    { Text(subtitle) }
                },
                trailingContent = {
                    IconButton(onClick = { onEdit(id) }, enabled = enabled) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = stringResource(
                                Res.string.podnya_edit_named_profile,
                                label
                            ),
                        )
                    }
                },
            )
        }
    }
}

@Composable
internal fun protocolLabel(protocol: AssistantProtocol): String = when (protocol) {
    AssistantProtocol.OPENAI_COMPATIBLE -> stringResource(Res.string.podnya_openai_compatible)
    AssistantProtocol.ANTHROPIC_MESSAGES -> "Claude"
    AssistantProtocol.GEMINI_GENERATE_CONTENT -> "Gemini"
}
