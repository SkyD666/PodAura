package com.skyd.podaura.ui.screen.feed.autodl

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowDown
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.toRoute
import com.skyd.podaura.ext.takeIfNotBlank
import com.skyd.podaura.ui.component.settings.BannerItem
import com.skyd.podaura.ui.component.settings.BaseSettingsItem
import com.skyd.podaura.ui.component.CircularProgressPlaceholder
import com.skyd.podaura.ui.component.ErrorPlaceholder
import com.skyd.podaura.ui.component.PodAuraIconButton
import com.skyd.podaura.ui.component.PodAuraTopBar
import com.skyd.podaura.ui.component.PodAuraTopBarStyle
import com.skyd.podaura.ui.component.settings.SwitchSettingsItem
import com.skyd.podaura.ui.component.dialog.SliderDialog
import com.skyd.podaura.ui.component.dialog.TextFieldDialog
import com.skyd.podaura.ui.component.dialog.WaitingDialog
import com.skyd.podaura.ui.mvi.MviEventListener
import com.skyd.podaura.ui.mvi.getDispatcher
import com.skyd.podaura.ui.screen.feed.autodl.AutoDownloadRuleState.RuleState
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.auto_download_rule_screen_name
import podaura.shared.generated.resources.auto_download_rule_screen_require_battery_not_low
import podaura.shared.generated.resources.auto_download_rule_screen_require_charging
import podaura.shared.generated.resources.auto_download_rule_screen_require_filter_pattern
import podaura.shared.generated.resources.auto_download_rule_screen_require_max_download_count
import podaura.shared.generated.resources.auto_download_rule_screen_require_wifi
import podaura.shared.generated.resources.enable
import podaura.shared.generated.resources.ok
import podaura.shared.generated.resources.reset
import podaura.shared.generated.resources.unlimited


@Serializable
data class AutoDownloadRuleRoute(@SerialName("feedUrl") val feedUrl: String) {
    companion object {
        @Composable
        fun AutoDownloadRuleLauncher(entry: NavBackStackEntry) {
            AutoDownloadRuleScreen(feedUrl = entry.toRoute<AutoDownloadRuleRoute>().feedUrl)
        }
    }
}

@Composable
fun AutoDownloadRuleScreen(
    feedUrl: String,
    viewModel: AutoDownloadRuleViewModel = koinViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }

    val uiState by viewModel.viewState.collectAsStateWithLifecycle()
    val dispatcher =
        viewModel.getDispatcher(feedUrl, startWith = AutoDownloadRuleIntent.Init(feedUrl))

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            PodAuraTopBar(
                style = PodAuraTopBarStyle.LargeFlexible,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(Res.string.auto_download_rule_screen_name)) },
            )
        }
    ) { paddingValues ->
        when (val ruleState = uiState.ruleState) {
            is RuleState.Failed -> ErrorPlaceholder(
                text = ruleState.msg,
                contentPadding = paddingValues,
            )

            RuleState.Init -> CircularProgressPlaceholder(contentPadding = paddingValues)
            is RuleState.Success -> RuleContent(
                contentPadding = paddingValues,
                connection = scrollBehavior.nestedScrollConnection,
                ruleState = ruleState,
                dispatcher = { intent -> dispatcher(intent) },
            )
        }

        MviEventListener(viewModel.singleEvent) { event ->
            when (event) {
                is AutoDownloadRuleEvent.UpdateResultEvent.Failed ->
                    snackbarHostState.showSnackbar(event.msg)
            }
        }

        WaitingDialog(visible = uiState.loadingDialog)
    }
}

@Composable
private fun RuleContent(
    contentPadding: PaddingValues,
    connection: NestedScrollConnection,
    ruleState: RuleState.Success,
    dispatcher: (AutoDownloadRuleIntent) -> Unit,
) {
    val rule = ruleState.autoDownloadRule
    var openMaxDownloadCountDialog by rememberSaveable { mutableStateOf(false) }
    var openFilterPatternDialog by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(connection),
        contentPadding = contentPadding,
    ) {
        item {
            BannerItem {
                SwitchSettingsItem(
                    imageVector = null,
                    text = stringResource(Res.string.enable),
                    checked = rule.enabled,
                    onCheckedChange = {
                        dispatcher(
                            AutoDownloadRuleIntent.Enabled(feedUrl = rule.feedUrl, enabled = it)
                        )
                    }
                )
            }
        }
        item {
            SwitchSettingsItem(
                enabled = rule.enabled,
                imageVector = Icons.Outlined.Wifi,
                text = stringResource(Res.string.auto_download_rule_screen_require_wifi),
                checked = rule.requireWifi,
                onCheckedChange = {
                    dispatcher(
                        AutoDownloadRuleIntent.RequireWifi(feedUrl = rule.feedUrl, requireWifi = it)
                    )
                }
            )
        }
        item {
            SwitchSettingsItem(
                enabled = rule.enabled,
                imageVector = Icons.Outlined.BatteryFull,
                text = stringResource(Res.string.auto_download_rule_screen_require_battery_not_low),
                checked = rule.requireBatteryNotLow,
                onCheckedChange = {
                    dispatcher(
                        AutoDownloadRuleIntent.RequireBatteryNotLow(
                            feedUrl = rule.feedUrl,
                            requireBatteryNotLow = it,
                        )
                    )
                }
            )
        }
        item {
            SwitchSettingsItem(
                enabled = rule.enabled,
                imageVector = Icons.Outlined.Bolt,
                text = stringResource(Res.string.auto_download_rule_screen_require_charging),
                checked = rule.requireCharging,
                onCheckedChange = {
                    dispatcher(
                        AutoDownloadRuleIntent.RequireCharging(
                            feedUrl = rule.feedUrl,
                            requireCharging = it,
                        )
                    )
                }
            )
        }
        item {
            BaseSettingsItem(
                enabled = rule.enabled,
                icon = rememberVectorPainter(Icons.Outlined.KeyboardDoubleArrowDown),
                text = stringResource(Res.string.auto_download_rule_screen_require_max_download_count),
                descriptionText = rule.maxDownloadCount.toString(),
                onClick = { openMaxDownloadCountDialog = true }
            )
        }
        item {
            BaseSettingsItem(
                enabled = rule.enabled,
                icon = rememberVectorPainter(Icons.Outlined.Code),
                text = stringResource(Res.string.auto_download_rule_screen_require_filter_pattern),
                descriptionText = rule.filterPattern.orEmpty().toString(),
                onClick = { openFilterPatternDialog = true }
            )
        }
    }

    if (openMaxDownloadCountDialog) {
        MaxDownloadCountDialog(
            onDismissRequest = { openMaxDownloadCountDialog = false },
            initValue = rule.maxDownloadCount,
            defaultValue = { 2 },
            onConfirm = {
                dispatcher(
                    AutoDownloadRuleIntent.UpdateMaxDownloadCount(
                        feedUrl = rule.feedUrl, maxDownloadCount = it
                    )
                )
                openMaxDownloadCountDialog = false
            },
        )
    }

    if (openFilterPatternDialog) {
        FilterPatternDialog(
            onDismissRequest = { openFilterPatternDialog = false },
            initValue = rule.filterPattern,
            onConfirm = {
                dispatcher(
                    AutoDownloadRuleIntent.UpdateFilterPattern(
                        feedUrl = rule.feedUrl, filterPattern = it
                    )
                )
                openFilterPatternDialog = false
            },
        )
    }
}

@Composable
private fun MaxDownloadCountDialog(
    onDismissRequest: () -> Unit,
    initValue: Int,
    defaultValue: () -> Int,
    onConfirm: (Int) -> Unit,
) {
    var value by rememberSaveable { mutableIntStateOf(initValue) }

    SliderDialog(
        onDismissRequest = onDismissRequest,
        value = value.toFloat(),
        onValueChange = { value = it.toInt() },
        valueRange = 0f..5f,
        valueLabel = {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .animateContentSize(),
                    text = if (value == 0) stringResource(Res.string.unlimited)
                    else value.toInt().toString(),
                    style = MaterialTheme.typography.titleMedium,
                )
                PodAuraIconButton(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    onClick = { value = defaultValue() },
                    imageVector = Icons.Outlined.Restore,
                    contentDescription = stringResource(Res.string.reset),
                )
            }
        },
        title = { Text(text = stringResource(Res.string.auto_download_rule_screen_require_max_download_count)) },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }) {
                Text(text = stringResource(Res.string.ok))
            }
        }
    )
}

@Composable
private fun FilterPatternDialog(
    onDismissRequest: () -> Unit,
    initValue: String?,
    onConfirm: (String?) -> Unit,
) {
    var value by rememberSaveable { mutableStateOf(initValue) }

    TextFieldDialog(
        titleText = stringResource(Res.string.auto_download_rule_screen_require_filter_pattern),
        value = value.orEmpty(),
        onValueChange = { value = it },
        enableConfirm = { true },
        onConfirm = { onConfirm(it.takeIfNotBlank()) },
        onDismissRequest = onDismissRequest,
    )
}