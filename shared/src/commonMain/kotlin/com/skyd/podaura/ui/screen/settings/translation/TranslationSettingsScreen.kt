package com.skyd.podaura.ui.screen.settings.translation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.skyd.compone.component.BackIcon
import com.skyd.compone.component.ComponeIconButton
import com.skyd.compone.component.ComponeScaffold
import com.skyd.compone.component.ComponeTopBar
import com.skyd.compone.component.ComponeTopBarStyle
import com.skyd.compone.component.DefaultBackClick
import com.skyd.compone.component.dialog.WaitingDialog
import com.skyd.compone.component.pointerOnBack
import com.skyd.podaura.model.bean.translation.TranslationProfile
import com.skyd.podaura.model.bean.translation.TranslationProviderConfig
import com.skyd.podaura.model.bean.translation.TranslationProviderType
import com.skyd.podaura.model.repository.translation.DeepLTranslationProvider
import com.skyd.podaura.model.repository.translation.GoogleTranslationProvider
import com.skyd.podaura.ui.component.AnimatedDismissModalBottomSheet
import com.skyd.podaura.ui.screen.translation.translationErrorText
import com.skyd.settings.BaseSettingsItem
import com.skyd.settings.SettingsLazyColumn
import com.skyd.settings.SwitchBaseSettingsItem
import com.skyd.settings.dsl.SettingsBaseItemScope
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.add
import podaura.shared.generated.resources.cancel
import podaura.shared.generated.resources.copy
import podaura.shared.generated.resources.delete
import podaura.shared.generated.resources.edit
import podaura.shared.generated.resources.password_visibility_off
import podaura.shared.generated.resources.password_visibility_on
import podaura.shared.generated.resources.translation_action_clear_cache
import podaura.shared.generated.resources.translation_action_make_default
import podaura.shared.generated.resources.translation_action_save
import podaura.shared.generated.resources.translation_action_test
import podaura.shared.generated.resources.translation_api_key
import podaura.shared.generated.resources.translation_api_key_keep
import podaura.shared.generated.resources.translation_cache_category
import podaura.shared.generated.resources.translation_cache_cleared
import podaura.shared.generated.resources.translation_cache_description
import podaura.shared.generated.resources.translation_clear_profile_cache
import podaura.shared.generated.resources.translation_connection_succeeded
import podaura.shared.generated.resources.translation_delete_question
import podaura.shared.generated.resources.translation_empty_profiles
import podaura.shared.generated.resources.translation_empty_profiles_description
import podaura.shared.generated.resources.translation_enabled
import podaura.shared.generated.resources.translation_endpoint_free
import podaura.shared.generated.resources.translation_endpoint_pro
import podaura.shared.generated.resources.translation_endpoint_type
import podaura.shared.generated.resources.translation_is_default
import podaura.shared.generated.resources.translation_notice_copied
import podaura.shared.generated.resources.translation_notice_deleted
import podaura.shared.generated.resources.translation_notice_saved
import podaura.shared.generated.resources.translation_profile_copy_name
import podaura.shared.generated.resources.translation_profile_name
import podaura.shared.generated.resources.translation_profiles_category
import podaura.shared.generated.resources.translation_provider
import podaura.shared.generated.resources.translation_screen_name
import podaura.shared.generated.resources.translation_target_language
import podaura.shared.generated.resources.translation_timeout
import kotlin.uuid.Uuid

@Serializable
data object TranslationSettingsRoute : NavKey

@Composable
fun TranslationSettingsScreen(
    onBack: (() -> Unit)? = DefaultBackClick,
    viewModel: TranslationSettingsViewModel = koinViewModel(),
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    var editedProfileId by remember { mutableStateOf<String?>(null) }
    var showNewProfileEditor by remember { mutableStateOf(false) }
    var deleteProfile by remember { mutableStateOf<TranslationProfile?>(null) }
    var closeEditorAfterSave by remember { mutableStateOf(false) }

    val noticeText = when (val notice = state.notice) {
        TranslationSettingsNotice.Saved -> stringResource(Res.string.translation_notice_saved)
        TranslationSettingsNotice.Deleted -> stringResource(Res.string.translation_notice_deleted)
        TranslationSettingsNotice.Copied -> stringResource(Res.string.translation_notice_copied)
        TranslationSettingsNotice.CacheCleared -> stringResource(Res.string.translation_cache_cleared)
        TranslationSettingsNotice.ConnectionSucceeded ->
            stringResource(Res.string.translation_connection_succeeded)

        is TranslationSettingsNotice.Failed -> translationErrorText(notice.error)
        null -> null
    }
    LaunchedEffect(noticeText) {
        if (noticeText != null) {
            when (state.notice) {
                TranslationSettingsNotice.Saved -> if (closeEditorAfterSave) {
                    editedProfileId = null
                    showNewProfileEditor = false
                    closeEditorAfterSave = false
                }

                is TranslationSettingsNotice.Failed -> closeEditorAfterSave = false
                else -> Unit
            }
            snackbarHostState.showSnackbar(noticeText)
            viewModel.consumeNotice()
        }
    }

    ComponeScaffold(
        modifier = Modifier.pointerOnBack(onBack = onBack),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ComponeTopBar(
                style = ComponeTopBarStyle.LargeFlexible,
                scrollBehavior = scrollBehavior,
                title = { Text(stringResource(Res.string.translation_screen_name)) },
                navigationIcon = { if (onBack != null) BackIcon(onClick = onBack) },
                actions = {
                    ComponeIconButton(
                        onClick = { showNewProfileEditor = true },
                        imageVector = Icons.Outlined.Add,
                        contentDescription = stringResource(Res.string.add),
                    )
                },
                windowInsets = windowInsets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            )
        },
        contentWindowInsets = windowInsets,
    ) { innerPadding ->
        SettingsLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = innerPadding,
        ) {
            group(text = { getString(Res.string.translation_profiles_category) }) {
                if (state.profiles.isEmpty()) {
                    item {
                        BaseSettingsItem(
                            icon = rememberVectorPainter(Icons.Outlined.Add),
                            text = stringResource(Res.string.translation_empty_profiles),
                            descriptionText = stringResource(
                                Res.string.translation_empty_profiles_description
                            ),
                            enabled = !state.working,
                            onClick = { showNewProfileEditor = true },
                        )
                    }
                } else {
                    state.profiles.forEach { profile ->
                        item(key = profile.id) {
                            TranslationProfileItem(
                                profile = profile,
                                working = state.working,
                                onEdit = { editedProfileId = profile.id },
                                onEnabledChange = { enabled ->
                                    viewModel.save(
                                        profile.copy(
                                            enabled = enabled,
                                            isDefault = profile.isDefault && enabled,
                                        ),
                                        credential = null,
                                    )
                                },
                            )
                        }
                    }
                }
            }
            group(text = { getString(Res.string.translation_cache_category) }) {
                item {
                    BaseSettingsItem(
                        icon = rememberVectorPainter(Icons.Outlined.DeleteSweep),
                        text = stringResource(Res.string.translation_action_clear_cache),
                        descriptionText = stringResource(Res.string.translation_cache_description),
                        enabled = !state.working,
                        onClick = viewModel::clearCache,
                    )
                }
            }
        }
    }

    if (showNewProfileEditor || editedProfileId != null) {
        val profile = editedProfileId?.let { id ->
            state.profiles.firstOrNull { it.id == id }
        }
        val copyName = profile?.let {
            stringResource(Res.string.translation_profile_copy_name, it.name)
        }
        TranslationProfileEditor(
            profile = profile,
            onDismissRequest = {
                editedProfileId = null
                showNewProfileEditor = false
                closeEditorAfterSave = false
            },
            onSave = { profile, credential ->
                closeEditorAfterSave = true
                viewModel.save(profile, credential)
            },
            onTest = viewModel::verify,
            onCopy = profile?.let {
                { viewModel.copy(it, checkNotNull(copyName)) }
            },
            onMakeDefault = profile?.takeUnless { it.isDefault }?.let {
                { viewModel.setDefault(it.id) }
            },
            onDelete = profile?.let {
                {
                    editedProfileId = null
                    closeEditorAfterSave = false
                    deleteProfile = it
                }
            },
        )
    }

    deleteProfile?.let { profile ->
        var clearProfileCache by remember(profile.id) { mutableStateOf(true) }
        AlertDialog(
            onDismissRequest = { deleteProfile = null },
            title = { Text(stringResource(Res.string.delete)) },
            text = {
                Column {
                    Text(stringResource(Res.string.translation_delete_question, profile.name))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { clearProfileCache = !clearProfileCache },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = clearProfileCache,
                            onCheckedChange = { clearProfileCache = it },
                        )
                        Text(stringResource(Res.string.translation_clear_profile_cache))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(profile.id, clearCachedTranslations = clearProfileCache)
                    deleteProfile = null
                }) { Text(stringResource(Res.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteProfile = null }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }

    WaitingDialog(visible = state.working)
}

@Composable
private fun SettingsBaseItemScope.TranslationProfileItem(
    profile: TranslationProfile,
    working: Boolean,
    onEdit: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
) {
    val providerName = TRANSLATION_PROVIDER_EDITOR_OPTIONS
        .firstOrNull { it.type == profile.providerType }
        ?.displayName
        ?: profile.providerType.name
    val endpoint = (profile.config as? TranslationProviderConfig.DeepL)?.let {
        stringResource(
            if (it.useFreeEndpoint) {
                Res.string.translation_endpoint_free
            } else {
                Res.string.translation_endpoint_pro
            }
        )
    }
    SwitchBaseSettingsItem(
        imageVector = Icons.Outlined.Translate,
        text = profile.name,
        description = listOfNotNull(
            listOfNotNull(providerName, endpoint).joinToString(" "),
            profile.targetLanguage,
            stringResource(Res.string.translation_is_default).takeIf { profile.isDefault },
        ).joinToString(" · "),
        checked = profile.enabled,
        enabled = !working,
        onCheckedChange = onEnabledChange,
        onClick = onEdit,
    )
}

@Composable
private fun TranslationProfileEditor(
    profile: TranslationProfile?,
    onDismissRequest: () -> Unit,
    onSave: (TranslationProfile, String?) -> Unit,
    onTest: (TranslationProfile, String?) -> Unit,
    onCopy: (() -> Unit)?,
    onMakeDefault: (() -> Unit)?,
    onDelete: (() -> Unit)?,
) {
    var name by remember(profile?.id) { mutableStateOf(profile?.name.orEmpty()) }
    var apiKey by remember(profile?.id) { mutableStateOf("") }
    var apiKeyVisible by remember(profile?.id) { mutableStateOf(false) }
    var providerType by remember(profile?.id) {
        mutableStateOf(
            TRANSLATION_PROVIDER_EDITOR_OPTIONS
                .firstOrNull { it.type == profile?.providerType }
                ?.type
                ?: TRANSLATION_PROVIDER_EDITOR_OPTIONS.first().type
        )
    }
    val providerOption = TRANSLATION_PROVIDER_EDITOR_OPTIONS.first { it.type == providerType }
    var targetLanguage by remember(profile?.id) {
        mutableStateOf(
            profile?.targetLanguage
                ?.takeIf { it in providerOption.targetLanguages }
                ?: providerOption.defaultTargetLanguage
        )
    }
    var timeoutSeconds by remember(profile?.id) {
        mutableStateOf(((profile?.requestTimeoutMillis ?: 60_000L) / 1000L).toString())
    }
    var enabled by remember(profile?.id, profile?.enabled) {
        mutableStateOf(profile?.enabled ?: true)
    }
    var useFreeEndpoint by remember(profile?.id) {
        mutableStateOf(
            (profile?.config as? TranslationProviderConfig.DeepL)?.useFreeEndpoint ?: true
        )
    }
    val draftId = remember(profile?.id) { profile?.id ?: Uuid.random().toString() }
    val timeout = timeoutSeconds.toLongOrNull()
    val providerConfig = buildProviderConfig(
        providerType = providerType,
        existingProfile = profile,
        useFreeEndpoint = useFreeEndpoint,
    )
    val hasCredential = apiKey.isNotBlank() ||
            (profile?.providerType == providerType && profile.credentialId != null)
    val valid = name.isNotBlank() && hasCredential &&
            timeout != null && timeout in 5L .. 120L && providerConfig != null
    val draftProfile = timeout?.takeIf { valid }?.let { validTimeout ->
        TranslationProfile(
            id = draftId,
            name = name.trim(),
            providerType = providerType,
            endpoint = profile?.endpoint.takeIf { profile?.providerType == providerType },
            credentialId = profile?.credentialId.takeIf {
                profile?.providerType == providerType
            },
            requestTimeoutMillis = validTimeout * 1000L,
            enabled = enabled,
            isDefault = profile?.isDefault == true && enabled,
            targetLanguage = targetLanguage,
            config = checkNotNull(providerConfig),
        )
    }

    AnimatedDismissModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
        sheetGesturesEnabled = false,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(
                    if (profile == null) Res.string.add else Res.string.edit
                ),
                style = MaterialTheme.typography.titleLarge,
            )
            if (profile != null) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    OutlinedButton(
                        enabled = draftProfile != null,
                        onClick = {
                            draftProfile?.let {
                                onTest(it, apiKey.trim().ifBlank { null })
                            }
                        },
                    ) {
                        Icon(Icons.Outlined.Translate, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(Res.string.translation_action_test))
                    }
                    onCopy?.let { action ->
                        OutlinedButton(onClick = action) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(Res.string.copy))
                        }
                    }
                    onMakeDefault?.let { action ->
                        OutlinedButton(onClick = {
                            enabled = true
                            action()
                        }) {
                            Icon(Icons.Outlined.Star, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(Res.string.translation_action_make_default))
                        }
                    }
                    onDelete?.let { action ->
                        TextButton(
                            onClick = action,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                        ) {
                            Icon(Icons.Outlined.Delete, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(Res.string.delete))
                        }
                    }
                }
            }
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(Res.string.translation_profile_name)) },
                singleLine = true,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                CompactDropdownField(
                    modifier = Modifier.weight(0.4f),
                    value = providerOption,
                    options = TRANSLATION_PROVIDER_EDITOR_OPTIONS,
                    optionLabel = TranslationProviderEditorOption::displayName,
                    label = stringResource(Res.string.translation_provider),
                    onValueChange = { option ->
                        if (option.type != providerType) {
                            providerType = option.type
                            apiKey = ""
                            if (targetLanguage !in option.targetLanguages) {
                                targetLanguage = option.defaultTargetLanguage
                            }
                        }
                    },
                )
                OutlinedTextField(
                    modifier = Modifier.weight(0.6f),
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = {
                        Text(
                            text = stringResource(Res.string.translation_api_key),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    placeholder = if (profile == null) null else {
                        {
                            Text(
                                text = stringResource(Res.string.translation_api_key_keep),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    visualTransformation = if (apiKeyVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                            Icon(
                                imageVector = if (apiKeyVisible) {
                                    Icons.Outlined.VisibilityOff
                                } else {
                                    Icons.Outlined.Visibility
                                },
                                contentDescription = stringResource(
                                    if (apiKeyVisible) {
                                        Res.string.password_visibility_off
                                    } else {
                                        Res.string.password_visibility_on
                                    }
                                ),
                            )
                        }
                    },
                    singleLine = true,
                )
            }
            if (providerType == TranslationProviderType.DeepL) {
                Text(
                    text = stringResource(Res.string.translation_endpoint_type),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf(true, false).forEachIndexed { index, free ->
                        SegmentedButton(
                            selected = useFreeEndpoint == free,
                            onClick = { useFreeEndpoint = free },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                            label = {
                                Text(
                                    stringResource(
                                        if (free) {
                                            Res.string.translation_endpoint_free
                                        } else {
                                            Res.string.translation_endpoint_pro
                                        }
                                    )
                                )
                            },
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                CompactDropdownField(
                    modifier = Modifier.weight(0.58f),
                    value = targetLanguage,
                    options = providerOption.targetLanguages,
                    optionLabel = { it },
                    label = stringResource(Res.string.translation_target_language),
                    onValueChange = { targetLanguage = it },
                )
                OutlinedTextField(
                    modifier = Modifier.weight(0.42f),
                    value = timeoutSeconds,
                    onValueChange = { timeoutSeconds = it.filter(Char::isDigit) },
                    label = {
                        Text(
                            text = stringResource(Res.string.translation_timeout),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    suffix = { Text("s") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
            }
            ToggleRow(
                label = stringResource(Res.string.translation_enabled),
                checked = enabled,
                onCheckedChange = {
                    enabled = it
                },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onDismissRequest,
                ) {
                    Text(stringResource(Res.string.cancel))
                }
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = draftProfile != null,
                    onClick = {
                        draftProfile?.let {
                            onSave(it, apiKey.trim().ifBlank { null })
                        }
                    },
                ) { Text(stringResource(Res.string.translation_action_save)) }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun <T> CompactDropdownField(
    value: T,
    options: List<T>,
    optionLabel: (T) -> String,
    label: String,
    onValueChange: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = optionLabel(value),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = {
                Text(
                    text = label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            trailingIcon = {
                Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
            },
        )
        Spacer(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = true },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    trailingIcon = if (option == value) {
                        { Icon(Icons.Outlined.Check, contentDescription = null) }
                    } else {
                        null
                    },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
fun TargetLanguagePicker(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    options: List<String> = DEEPL_TARGET_LANGUAGES,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth()) {
        ListItem(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            leadingContent = { Icon(Icons.Outlined.Language, contentDescription = null) },
            headlineContent = { Text(stringResource(Res.string.translation_target_language)) },
            supportingContent = { Text(value) },
            trailingContent = {
                Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
            },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { language ->
                DropdownMenuItem(
                    text = { Text(language) },
                    trailingIcon = if (language == value) {
                        { Icon(Icons.Outlined.Check, contentDescription = null) }
                    } else {
                        null
                    },
                    onClick = {
                        onValueChange(language)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        headlineContent = { Text(label) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        },
    )
}

val DEEPL_TARGET_LANGUAGES = listOf(
    "AR", "BG", "CS", "DA", "DE", "EL", "EN", "EN-GB", "EN-US", "ES", "ES-419",
    "ET", "FI", "FR", "HE", "HU", "ID", "IT", "JA", "KO", "LT", "LV", "NB", "NL",
    "PL", "PT", "PT-BR", "PT-PT", "RO", "RU", "SK", "SL", "SV", "TH", "TR", "UK",
    "VI", "ZH", "ZH-HANS", "ZH-HANT",
)

val GOOGLE_TARGET_LANGUAGES = listOf(
    "AF", "SQ", "AM", "AR", "HY", "AZ", "EU", "BE", "BN", "BS", "BG", "CA",
    "CEB", "ZH-CN", "ZH-TW", "CO", "HR", "CS", "DA", "NL", "EN", "EO", "ET",
    "FI", "FR", "FY", "GL", "KA", "DE", "EL", "GU", "HT", "HA", "HAW", "HE",
    "HI", "HMN", "HU", "IS", "IG", "ID", "GA", "IT", "JA", "JW", "KN", "KK",
    "KM", "KO", "KU", "KY", "LO", "LA", "LV", "LT", "LB", "MK", "MG", "MS",
    "ML", "MT", "MI", "MR", "MN", "MY", "NE", "NO", "NY", "OR", "PS", "FA",
    "PL", "PT", "PA", "RO", "RU", "SM", "GD", "SR", "ST", "SN", "SD", "SI",
    "SK", "SL", "SO", "ES", "SU", "SW", "SV", "TL", "TG", "TA", "TT", "TE",
    "TH", "TR", "TK", "UK", "UR", "UG", "UZ", "VI", "CY", "XH", "YI", "YO",
    "ZU",
)

fun translationTargetLanguages(providerType: TranslationProviderType): List<String> =
    TRANSLATION_PROVIDER_EDITOR_OPTIONS
        .firstOrNull { it.type == providerType }
        ?.targetLanguages
        .orEmpty()

fun translationMaxTextRequestBytes(providerType: TranslationProviderType): Long =
    TRANSLATION_PROVIDER_EDITOR_OPTIONS
        .firstOrNull { it.type == providerType }
        ?.maxTextRequestBytes
        ?: Long.MAX_VALUE

private data class TranslationProviderEditorOption(
    val type: TranslationProviderType,
    val displayName: String,
    val targetLanguages: List<String>,
    val defaultTargetLanguage: String,
    val maxTextRequestBytes: Long,
)

private val TRANSLATION_PROVIDER_EDITOR_OPTIONS = listOf(
    TranslationProviderEditorOption(
        type = TranslationProviderType.DeepL,
        displayName = "DeepL",
        targetLanguages = DEEPL_TARGET_LANGUAGES,
        defaultTargetLanguage = "EN",
        maxTextRequestBytes = DeepLTranslationProvider.MAX_TEXT_REQUEST_BYTES,
    ),
    TranslationProviderEditorOption(
        type = TranslationProviderType.Google,
        displayName = "Google",
        targetLanguages = GOOGLE_TARGET_LANGUAGES,
        defaultTargetLanguage = "EN",
        maxTextRequestBytes = GoogleTranslationProvider.MAX_TEXT_REQUEST_BYTES,
    ),
)

private fun buildProviderConfig(
    providerType: TranslationProviderType,
    existingProfile: TranslationProfile?,
    useFreeEndpoint: Boolean,
): TranslationProviderConfig? = when (providerType) {
    TranslationProviderType.DeepL -> TranslationProviderConfig.DeepL(
        useFreeEndpoint = useFreeEndpoint,
    )

    TranslationProviderType.Google -> TranslationProviderConfig.Google()

    TranslationProviderType.Azure ->
        existingProfile
            ?.takeIf { it.providerType == providerType }
            ?.config as? TranslationProviderConfig.Azure

    TranslationProviderType.CustomPodAura ->
        existingProfile
            ?.takeIf { it.providerType == providerType }
            ?.config as? TranslationProviderConfig.CustomPodAura

    TranslationProviderType.ChatCompletionsCompatible ->
        existingProfile
            ?.takeIf { it.providerType == providerType }
            ?.config as? TranslationProviderConfig.ChatCompletions
}
