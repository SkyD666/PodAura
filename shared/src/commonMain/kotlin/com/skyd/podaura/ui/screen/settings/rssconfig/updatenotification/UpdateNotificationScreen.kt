package com.skyd.podaura.ui.screen.settings.rssconfig.updatenotification

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Pattern
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.skyd.compone.component.ComponeFloatingActionButton
import com.skyd.compone.component.ComponeScaffold
import com.skyd.compone.component.ComponeTopBar
import com.skyd.compone.component.ComponeTopBarStyle
import com.skyd.compone.component.dialog.ComponeDialog
import com.skyd.compone.component.dialog.WaitingDialog
import com.skyd.compone.ext.plus
import com.skyd.mvi.MviEventListener
import com.skyd.mvi.getDispatcher
import com.skyd.podaura.model.bean.ArticleNotificationRuleBean
import com.skyd.podaura.model.bean.group.GroupVo
import com.skyd.podaura.ui.component.ClipboardTextField
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.add
import podaura.shared.generated.resources.cancel
import podaura.shared.generated.resources.default_feed_group
import podaura.shared.generated.resources.delete
import podaura.shared.generated.resources.edit
import podaura.shared.generated.resources.info
import podaura.shared.generated.resources.more
import podaura.shared.generated.resources.notification_content_hint
import podaura.shared.generated.resources.notification_custom_label
import podaura.shared.generated.resources.notification_delete_question
import podaura.shared.generated.resources.notification_delete_warning
import podaura.shared.generated.resources.notification_empty_rule
import podaura.shared.generated.resources.notification_feeds
import podaura.shared.generated.resources.notification_groups
import podaura.shared.generated.resources.notification_invalid_regex
import podaura.shared.generated.resources.notification_managed_delete_warning
import podaura.shared.generated.resources.notification_managed_help
import podaura.shared.generated.resources.notification_managed_label
import podaura.shared.generated.resources.notification_managed_scope
import podaura.shared.generated.resources.notification_match_help
import podaura.shared.generated.resources.notification_regex
import podaura.shared.generated.resources.notification_search
import podaura.shared.generated.resources.notification_unrestricted
import podaura.shared.generated.resources.ok
import podaura.shared.generated.resources.update_notification_rule_name
import podaura.shared.generated.resources.update_notification_screen_name


@Serializable
data object UpdateNotificationRoute : NavKey

@Composable
fun UpdateNotificationScreen(
    viewModel: UpdateNotificationViewModel = koinViewModel(),
    windowInsets: WindowInsets = WindowInsets.safeDrawing
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }

    val uiState by viewModel.viewState.collectAsStateWithLifecycle()
    val dispatcher = viewModel.getDispatcher(startWith = UpdateNotificationIntent.Init)

    var fabHeight by remember { mutableStateOf(0.dp) }
    var openAddDialog by rememberSaveable { mutableStateOf(false) }
    var editingRuleId by rememberSaveable { mutableStateOf<Int?>(null) }

    ComponeScaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            ComponeTopBar(
                style = ComponeTopBarStyle.Small,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(Res.string.update_notification_screen_name)) },
                windowInsets = windowInsets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
        },
        floatingActionButton = {
            ComponeFloatingActionButton(
                onClick = {
                    editingRuleId = null
                    openAddDialog = true
                },
                onSizeWithSinglePaddingChanged = { _, height -> fabHeight = height },
                contentDescription = stringResource(Res.string.add),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(Res.string.add),
                )
            }
        },
        contentWindowInsets = windowInsets
    ) { innerPadding ->
        when (val ruleListState = uiState.ruleListState) {
            is RuleListState.Failed,
            RuleListState.Init -> Unit

            is RuleListState.Success -> {
                RuleList(
                    contentPadding = innerPadding + PaddingValues(bottom = fabHeight),
                    ruleListState = ruleListState,
                    dispatcher = dispatcher,
                    onEdit = {
                        editingRuleId = it.id
                        openAddDialog = true
                    },
                )
                if (openAddDialog) {
                    AddRuleDialog(
                        rule = ruleListState.rules.firstOrNull { it.id == editingRuleId },
                        ruleListState = ruleListState,
                        onDismissRequest = { openAddDialog = false },
                        onAdd = { rule -> dispatcher(UpdateNotificationIntent.Add(rule = rule)) }
                    )
                }
            }
        }

        MviEventListener(viewModel.singleEvent) { event ->
            when (event) {
                is UpdateNotificationEvent.RuleListResultEvent.Failed ->
                    snackbarHostState.showSnackbar(event.msg)

                is UpdateNotificationEvent.AddResultEvent.Failed ->
                    snackbarHostState.showSnackbar(event.msg)

                is UpdateNotificationEvent.RemoveResultEvent.Failed ->
                    snackbarHostState.showSnackbar(event.msg)

            }
        }

        WaitingDialog(visible = uiState.loadingDialog)
    }
}

@Composable
internal fun AddRuleDialog(
    rule: ArticleNotificationRuleBean?,
    ruleListState: RuleListState.Success,
    onDismissRequest: () -> Unit,
    onAdd: (ArticleNotificationRuleBean) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var name by rememberSaveable(rule?.id) { mutableStateOf(rule?.name.orEmpty()) }
    var regex by rememberSaveable(rule?.id) { mutableStateOf(rule?.regex.orEmpty()) }
    var feedUrls by rememberSaveable(rule?.id) { mutableStateOf(rule?.feedUrls.orEmpty()) }
    var groupIds by rememberSaveable(rule?.id) { mutableStateOf(rule?.groupIds.orEmpty()) }
    val draft = ArticleNotificationRuleBean(
        id = rule?.id ?: 0, name = name, regex = regex,
        feedUrls = feedUrls, groupIds = groupIds,
    )
    val emptyRule = regex.isBlank() && feedUrls.isEmpty() && groupIds.isEmpty()
    val validRule = remember(regex, feedUrls, groupIds) { draft.isValid() }

    ComponeDialog(
        onDismissRequest = onDismissRequest,
        icon = { Icon(imageVector = Icons.Outlined.Pattern, contentDescription = null) },
        title = { Text(text = stringResource(if (rule == null) Res.string.add else Res.string.edit)) },
        text = {
            // ComponeDialog already provides scrolling for its text content.
            Column {
                ClipboardTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = name,
                    onValueChange = { name = it },
                    maxLines = 1,
                    placeholder = stringResource(Res.string.update_notification_rule_name),
                    focusManager = focusManager,
                )
                Spacer(modifier = Modifier.height(12.dp))
                ClipboardTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = regex,
                    onValueChange = { regex = it },
                    placeholder = stringResource(Res.string.notification_content_hint),
                    autoRequestFocus = false,
                    focusManager = focusManager,
                    imeAction = ImeAction.None,
                )
                TargetPicker(
                    title = stringResource(Res.string.notification_feeds),
                    options = ruleListState.feedNames,
                    selected = feedUrls,
                    onSelected = { feedUrls = it },
                )
                TargetPicker(
                    title = stringResource(Res.string.notification_groups),
                    options = mapOf(
                        GroupVo.DEFAULT_GROUP_ID to stringResource(Res.string.default_feed_group)
                    ) + ruleListState.groupNames,
                    selected = groupIds,
                    onSelected = { groupIds = it },
                )
                Text(
                    stringResource(Res.string.notification_match_help),
                    style = MaterialTheme.typography.bodySmall
                )
                if (!validRule) {
                    Text(
                        stringResource(if (emptyRule) Res.string.notification_empty_rule else Res.string.notification_invalid_regex),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            val confirmButtonEnabled =
                name.isNotBlank() && validRule && rule?.isManaged != true
            TextButton(
                enabled = confirmButtonEnabled,
                onClick = {
                    focusManager.clearFocus()
                    onAdd(draft)
                    onDismissRequest()
                }
            ) {
                Text(
                    text = stringResource(Res.string.ok),
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
                Text(text = stringResource(Res.string.cancel))
            }
        },
    )
}

@Composable
private fun RuleList(
    contentPadding: PaddingValues,
    ruleListState: RuleListState.Success,
    dispatcher: (UpdateNotificationIntent) -> Unit,
    onEdit: (ArticleNotificationRuleBean) -> Unit,
) {
    val rules = ruleListState.rules

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = rememberLazyListState(),
        contentPadding = contentPadding + PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(rules, key = { it.id }) { rule ->
            RuleItem(
                rule = rule,
                ruleListState = ruleListState,
                onEdit = { onEdit(rule) },
                onRemove = { id -> dispatcher(UpdateNotificationIntent.Remove(ruleId = id)) },
            )
        }
    }
}

@Composable
internal fun RuleItem(
    rule: ArticleNotificationRuleBean,
    ruleListState: RuleListState.Success,
    onEdit: () -> Unit,
    onRemove: (Int) -> Unit,
) {
    var showRuleHelp by rememberSaveable(rule.id) { mutableStateOf(false) }
    var showDeleteConfirmation by rememberSaveable(rule.id) { mutableStateOf(false) }
    var showActions by remember { mutableStateOf(false) }
    val title =
        if (rule.isManaged) ruleListState.feedNames[rule.feedUrls.singleOrNull()] ?: rule.name
        else rule.name

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { if (rule.isManaged) showRuleHelp = true else onEdit() },
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    SelectionContainer {
                        Text(text = title, style = MaterialTheme.typography.titleMedium)
                    }
                    NotificationRuleType(
                        isManaged = rule.isManaged,
                        onClick = { showRuleHelp = true },
                    )
                }
                Box {
                    IconButton(onClick = { showActions = true }, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = stringResource(Res.string.more),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DropdownMenu(
                        expanded = showActions,
                        onDismissRequest = { showActions = false }) {
                        if (!rule.isManaged) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.edit)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showActions = false
                                    onEdit()
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(Res.string.delete),
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                showActions = false
                                showDeleteConfirmation = true
                            },
                        )
                    }
                }
            }
            if (!rule.isManaged) {
                val unrestricted = stringResource(Res.string.notification_unrestricted)
                val defaultGroup = stringResource(Res.string.default_feed_group)
                val feeds = rule.feedUrls.joinToString { url ->
                    ruleListState.feedNames[url] ?: url
                }.ifEmpty { unrestricted }
                val groups = rule.groupIds.joinToString { id ->
                    if (id == GroupVo.DEFAULT_GROUP_ID) defaultGroup
                    else ruleListState.groupNames[id] ?: id
                }.ifEmpty { unrestricted }
                Spacer(modifier = Modifier.height(8.dp))
                SelectionContainer(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        RuleCondition(
                            label = stringResource(Res.string.notification_regex),
                            value = rule.regex.ifBlank { unrestricted },
                        )
                        RuleCondition(
                            label = stringResource(Res.string.notification_feeds),
                            value = feeds
                        )
                        RuleCondition(
                            label = stringResource(Res.string.notification_groups),
                            value = groups
                        )
                    }
                }
            }
        }
    }
    if (showDeleteConfirmation) {
        ComponeDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            icon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
            title = { Text(stringResource(Res.string.notification_delete_question, title)) },
            text = {
                Text(
                    stringResource(
                        if (rule.isManaged) Res.string.notification_managed_delete_warning
                        else Res.string.notification_delete_warning,
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                    onRemove(rule.id)
                }) {
                    Text(stringResource(Res.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }
    if (showRuleHelp) {
        ComponeDialog(
            onDismissRequest = { showRuleHelp = false },
            icon = {
                Icon(
                    imageVector = if (rule.isManaged) Icons.Outlined.NotificationsActive else Icons.Outlined.Pattern,
                    contentDescription = null,
                )
            },
            title = {
                Text(stringResource(if (rule.isManaged) Res.string.notification_managed_label else Res.string.notification_custom_label))
            },
            text = {
                Text(stringResource(if (rule.isManaged) Res.string.notification_managed_help else Res.string.notification_match_help))
            },
            confirmButton = {
                TextButton(onClick = { showRuleHelp = false }) {
                    Text(stringResource(Res.string.ok))
                }
            },
        )
    }
}

@Composable
private fun NotificationRuleType(isManaged: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .clickable(
                role = Role.Button,
                onClickLabel = stringResource(Res.string.info),
                onClick = onClick,
            ),
        shape = MaterialTheme.shapes.extraSmall,
        color = if (isManaged) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = if (isManaged) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (isManaged) Icons.Outlined.NotificationsActive else Icons.Outlined.Pattern,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
            )
            Text(
                modifier = Modifier.weight(1f, fill = false),
                text = if (isManaged) {
                    "${stringResource(Res.string.notification_managed_label)} · ${stringResource(Res.string.notification_managed_scope)}"
                } else {
                    stringResource(Res.string.notification_custom_label)
                },
                style = MaterialTheme.typography.labelSmall,
            )
            Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun RuleCondition(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            modifier = Modifier.weight(0.3f),
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            modifier = Modifier.weight(0.7f),
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun TargetPicker(
    title: String,
    options: Map<String, String>,
    selected: List<String>,
    onSelected: (List<String>) -> Unit,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var pendingSelected by rememberSaveable { mutableStateOf(selected) }
    val allOptions = remember(options, selected) {
        options + selected.filterNot { it in options }.associateWith { it }
    }
    val summary = remember(allOptions, selected) {
        selected.joinToString { allOptions.getValue(it) }
    }.ifEmpty { stringResource(Res.string.notification_unrestricted) }
    val filteredOptions = remember(allOptions, query) {
        allOptions.filterValues { it.contains(query, ignoreCase = true) }.toList()
    }
    TextButton(onClick = {
        query = ""
        pendingSelected = selected
        open = true
    }) {
        Text("$title: $summary", maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
    if (open) {
        ComponeDialog(
            onDismissRequest = { open = false },
            title = { Text(title) },
            text = {
                Column {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        label = { Text(stringResource(Res.string.notification_search)) },
                    )
                    TextButton(onClick = { pendingSelected = emptyList() }) {
                        Text(stringResource(Res.string.notification_unrestricted))
                    }
                    LazyColumn(Modifier.heightIn(max = 360.dp)) {
                        items(
                            items = filteredOptions,
                            key = { it.first },
                        ) { (id, label) ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 40.dp)
                                    .toggleable(
                                        value = id in pendingSelected,
                                        role = Role.Checkbox,
                                        onValueChange = { checked ->
                                            pendingSelected = if (checked) {
                                                pendingSelected + id
                                            } else {
                                                pendingSelected - id
                                            }
                                        },
                                    )
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = id in pendingSelected, onCheckedChange = null)
                                Text(label, modifier = Modifier.weight(1f).padding(start = 8.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onSelected(pendingSelected)
                    open = false
                }) { Text(stringResource(Res.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { open = false }) { Text(stringResource(Res.string.cancel)) }
            },
        )
    }
}
