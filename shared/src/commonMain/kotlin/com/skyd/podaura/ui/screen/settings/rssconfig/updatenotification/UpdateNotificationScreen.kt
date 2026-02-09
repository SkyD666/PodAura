package com.skyd.podaura.ui.screen.settings.rssconfig.updatenotification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Pattern
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skyd.compone.component.ComponeFloatingActionButton
import com.skyd.compone.component.ComponeIconButton
import com.skyd.compone.component.ComponeTopBar
import com.skyd.compone.component.ComponeTopBarStyle
import com.skyd.compone.component.dialog.ComponeDialog
import com.skyd.compone.component.dialog.WaitingDialog
import com.skyd.compone.ext.plus
import com.skyd.mvi.MviEventListener
import com.skyd.mvi.getDispatcher
import com.skyd.podaura.model.bean.ArticleNotificationRuleBean
import com.skyd.podaura.ui.component.ClipboardTextField
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.add
import podaura.shared.generated.resources.cancel
import podaura.shared.generated.resources.ok
import podaura.shared.generated.resources.remove
import podaura.shared.generated.resources.request_headers_screen_value
import podaura.shared.generated.resources.update_notification_rule_name
import podaura.shared.generated.resources.update_notification_screen_name


@Serializable
data object UpdateNotificationRoute

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

    Scaffold(
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
                onClick = { openAddDialog = true },
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
                )
                if (openAddDialog) {
                    AddRuleDialog(
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
private fun AddRuleDialog(
    onDismissRequest: () -> Unit,
    onAdd: (ArticleNotificationRuleBean) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var name by rememberSaveable { mutableStateOf("") }
    var regex by rememberSaveable { mutableStateOf("") }

    ComponeDialog(
        onDismissRequest = onDismissRequest,
        icon = { Icon(imageVector = Icons.Outlined.Pattern, contentDescription = null) },
        title = { Text(text = stringResource(Res.string.add)) },
        text = {
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
                    placeholder = stringResource(Res.string.request_headers_screen_value),
                    autoRequestFocus = false,
                    focusManager = focusManager,
                    imeAction = ImeAction.None,
                )
            }
        },
        confirmButton = {
            val confirmButtonEnabled = name.isNotBlank() && regex.isNotBlank()
            TextButton(
                enabled = confirmButtonEnabled,
                onClick = {
                    focusManager.clearFocus()
                    onAdd(ArticleNotificationRuleBean(name = name, regex = regex))
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
) {
    val rules = ruleListState.rules

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = rememberLazyListState(),
        contentPadding = contentPadding + PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(rules.size) { index ->
            val rule = rules[index]
            RuleItem(
                rule = rule,
                onRemove = { id -> dispatcher(UpdateNotificationIntent.Remove(ruleId = id)) },
            )
        }
    }
}

@Composable
private fun RuleItem(
    rule: ArticleNotificationRuleBean,
    onRemove: (Int) -> Unit,
) {
    Card(onClick = { }) {
        Row(
            modifier = Modifier
                .padding(top = 4.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .weight(1f),
                text = rule.name,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            ComponeIconButton(
                onClick = { onRemove(rule.id) },
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(Res.string.remove),
            )
        }

        SelectionContainer {
            Text(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp),
                text = rule.regex,
                maxLines = 10,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}