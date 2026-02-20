package com.skyd.podaura.ui.screen.settings.data.autodelete

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.AutoDelete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.skyd.compone.component.ComponeScaffold
import com.skyd.compone.component.ComponeTopBar
import com.skyd.compone.component.ComponeTopBarStyle
import com.skyd.compone.component.dialog.SliderDialog
import com.skyd.podaura.ext.getOrDefault
import com.skyd.podaura.model.preference.data.delete.autodelete.AutoDeleteArticleBeforePreference
import com.skyd.podaura.model.preference.data.delete.autodelete.AutoDeleteArticleFrequencyPreference
import com.skyd.podaura.model.preference.data.delete.autodelete.AutoDeleteArticleKeepFavoritePreference
import com.skyd.podaura.model.preference.data.delete.autodelete.AutoDeleteArticleKeepPlaylistPreference
import com.skyd.podaura.model.preference.data.delete.autodelete.AutoDeleteArticleKeepUnreadPreference
import com.skyd.podaura.model.preference.data.delete.autodelete.AutoDeleteArticleMaxCountPreference
import com.skyd.podaura.model.preference.data.delete.autodelete.AutoDeleteArticleUseBeforePreference
import com.skyd.podaura.model.preference.data.delete.autodelete.AutoDeleteArticleUseMaxCountPreference
import com.skyd.podaura.model.preference.data.delete.autodelete.UseAutoDeletePreference
import com.skyd.podaura.model.preference.dataStore
import com.skyd.settings.BannerItem
import com.skyd.settings.BaseSettingsItem
import com.skyd.settings.SettingsLazyColumn
import com.skyd.settings.SwitchBaseSettingsItem
import com.skyd.settings.SwitchSettingsItem
import com.skyd.settings.TipSettingsItem
import com.skyd.settings.suspendString
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.auto_delete_article_screen_delete_before
import podaura.shared.generated.resources.auto_delete_article_screen_delete_frequency
import podaura.shared.generated.resources.auto_delete_article_screen_delete_max_count
import podaura.shared.generated.resources.auto_delete_article_screen_delete_max_count_description
import podaura.shared.generated.resources.auto_delete_article_screen_keep_favorite
import podaura.shared.generated.resources.auto_delete_article_screen_keep_favorite_description
import podaura.shared.generated.resources.auto_delete_article_screen_keep_playlist
import podaura.shared.generated.resources.auto_delete_article_screen_keep_playlist_description
import podaura.shared.generated.resources.auto_delete_article_screen_keep_unread
import podaura.shared.generated.resources.auto_delete_article_screen_keep_unread_description
import podaura.shared.generated.resources.auto_delete_article_screen_no_strategy_tip
import podaura.shared.generated.resources.auto_delete_article_screen_option_category
import podaura.shared.generated.resources.auto_delete_article_screen_options_tip
import podaura.shared.generated.resources.auto_delete_article_screen_strategy_category
import podaura.shared.generated.resources.auto_delete_article_screen_strategy_tip
import podaura.shared.generated.resources.auto_delete_screen_name
import podaura.shared.generated.resources.cancel
import podaura.shared.generated.resources.enable
import podaura.shared.generated.resources.ic_calendar_clock_24
import podaura.shared.generated.resources.ok
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds


@Serializable
data object AutoDeleteRoute : NavKey

@Composable
fun AutoDeleteScreen(
    windowInsets: WindowInsets = WindowInsets.safeDrawing
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scope = rememberCoroutineScope()

    ComponeScaffold(
        topBar = {
            ComponeTopBar(
                style = ComponeTopBarStyle.LargeFlexible,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(Res.string.auto_delete_screen_name)) },
                windowInsets = windowInsets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
        },
        contentWindowInsets = windowInsets
    ) { innerPadding ->
        val useAutoDelete = UseAutoDeletePreference.current
        var openAutoDeleteFrequencyDialog by rememberSaveable { mutableStateOf(false) }
        var openAutoDeleteBeforeDialog by rememberSaveable { mutableStateOf(false) }
        var openAutoDeleteMaxCountDialog by rememberSaveable { mutableStateOf(false) }

        val autoDeleteArticleUseBefore = AutoDeleteArticleUseBeforePreference.current
        val autoDeleteArticleUseMaxCount = AutoDeleteArticleUseMaxCountPreference.current
        SettingsLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = innerPadding,
        ) {
            item {
                BannerItem {
                    SwitchSettingsItem(
                        imageVector = Icons.Outlined.AutoDelete,
                        text = stringResource(Res.string.enable),
                        checked = useAutoDelete,
                        onCheckedChange = { UseAutoDeletePreference.put(scope, it) }
                    )
                }
            }
            group {
                item {
                    BaseSettingsItem(
                        icon = rememberVectorPainter(Icons.Outlined.Timer),
                        text = stringResource(Res.string.auto_delete_article_screen_delete_frequency),
                        descriptionText = suspendString(AutoDeleteArticleFrequencyPreference.current) {
                            AutoDeleteArticleFrequencyPreference.toDisplayNameMilliseconds(it)
                        },
                        onClick = { openAutoDeleteFrequencyDialog = true },
                        enabled = useAutoDelete,
                    )
                }
            }
            group(text = { getString(Res.string.auto_delete_article_screen_strategy_category) }) {
                otherItem {
                    AnimatedVisibility(
                        visible = !autoDeleteArticleUseBefore && !autoDeleteArticleUseMaxCount,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        Text(
                            text = stringResource(Res.string.auto_delete_article_screen_no_strategy_tip),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .background(
                                    MaterialTheme.colorScheme.errorContainer,
                                    RoundedCornerShape(6.dp),
                                )
                                .padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
                item {
                    SwitchBaseSettingsItem(
                        checked = autoDeleteArticleUseBefore,
                        painter = painterResource(Res.drawable.ic_calendar_clock_24),
                        text = stringResource(Res.string.auto_delete_article_screen_delete_before),
                        description = suspendString(AutoDeleteArticleBeforePreference.current) {
                            AutoDeleteArticleBeforePreference.toDisplayNameMilliseconds(it)
                        },
                        onClick = { openAutoDeleteBeforeDialog = true },
                        onCheckedChange = { AutoDeleteArticleUseBeforePreference.put(scope, it) },
                        enabled = useAutoDelete,
                    )
                }
                item {
                    SwitchBaseSettingsItem(
                        checked = autoDeleteArticleUseMaxCount,
                        imageVector = null,
                        text = stringResource(Res.string.auto_delete_article_screen_delete_max_count),
                        description = stringResource(
                            Res.string.auto_delete_article_screen_delete_max_count_description,
                            AutoDeleteArticleMaxCountPreference.current,
                        ),
                        onClick = { openAutoDeleteMaxCountDialog = true },
                        onCheckedChange = { AutoDeleteArticleUseMaxCountPreference.put(scope, it) },
                        enabled = useAutoDelete,
                    )
                }
                otherItem {
                    TipSettingsItem(stringResource(Res.string.auto_delete_article_screen_strategy_tip))
                }
            }
            group(text = { getString(Res.string.auto_delete_article_screen_option_category) }) {
                item {
                    SwitchSettingsItem(
                        imageVector = Icons.Outlined.MarkEmailUnread,
                        text = stringResource(Res.string.auto_delete_article_screen_keep_unread),
                        description = stringResource(Res.string.auto_delete_article_screen_keep_unread_description),
                        checked = AutoDeleteArticleKeepUnreadPreference.current,
                        onCheckedChange = { AutoDeleteArticleKeepUnreadPreference.put(scope, it) }
                    )
                }
                item {
                    SwitchSettingsItem(
                        imageVector = Icons.Outlined.FavoriteBorder,
                        text = stringResource(Res.string.auto_delete_article_screen_keep_favorite),
                        description = stringResource(Res.string.auto_delete_article_screen_keep_favorite_description),
                        checked = AutoDeleteArticleKeepFavoritePreference.current,
                        onCheckedChange = { AutoDeleteArticleKeepFavoritePreference.put(scope, it) }
                    )
                }
                item {
                    SwitchSettingsItem(
                        imageVector = Icons.AutoMirrored.Outlined.PlaylistPlay,
                        text = stringResource(Res.string.auto_delete_article_screen_keep_playlist),
                        description = stringResource(Res.string.auto_delete_article_screen_keep_playlist_description),
                        checked = AutoDeleteArticleKeepPlaylistPreference.current,
                        onCheckedChange = { AutoDeleteArticleKeepPlaylistPreference.put(scope, it) }
                    )
                }
                otherItem {
                    TipSettingsItem(stringResource(Res.string.auto_delete_article_screen_options_tip))
                }
            }
        }

        if (openAutoDeleteFrequencyDialog) {
            AutoDeleteFrequencyDialog(
                onDismissRequest = { openAutoDeleteFrequencyDialog = false },
                onConfirm = {
                    AutoDeleteArticleFrequencyPreference.put(scope, it.inWholeMilliseconds)
                    openAutoDeleteFrequencyDialog = false
                }
            )
        }
        if (openAutoDeleteBeforeDialog) {
            AutoDeleteBeforeDialog(
                onDismissRequest = { openAutoDeleteBeforeDialog = false },
                onConfirm = {
                    AutoDeleteArticleBeforePreference.put(scope, it.inWholeMilliseconds)
                    openAutoDeleteBeforeDialog = false
                }
            )
        }
        if (openAutoDeleteMaxCountDialog) {
            AutoDeleteMaxCountDialog(
                onDismissRequest = { openAutoDeleteMaxCountDialog = false },
                onConfirm = {
                    AutoDeleteArticleMaxCountPreference.put(scope, it)
                    openAutoDeleteMaxCountDialog = false
                }
            )
        }
    }
}

@Composable
private fun AutoDeleteFrequencyDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (Duration) -> Unit,
) {
    var frequencyDay by rememberSaveable {
        mutableLongStateOf(
            dataStore
                .getOrDefault(AutoDeleteArticleFrequencyPreference)
                .milliseconds
                .inWholeDays
        )
    }
    SliderDialog(
        onDismissRequest = onDismissRequest,
        value = frequencyDay.toFloat(),
        onValueChange = { frequencyDay = it.toLong() },
        valueRange = 1f..365f,
        valueLabel = {
            Text(
                text = suspendString {
                    AutoDeleteArticleFrequencyPreference.toDisplayNameDays(frequencyDay)
                },
                style = MaterialTheme.typography.labelLarge,
            )
        },
        icon = { Icon(imageVector = Icons.Outlined.Timer, contentDescription = null) },
        title = { Text(text = stringResource(Res.string.auto_delete_article_screen_delete_frequency)) },
        confirmButton = {
            val enabled by remember { derivedStateOf { frequencyDay >= 1 } }
            TextButton(
                onClick = { onConfirm(frequencyDay.days) },
                enabled = enabled,
            ) {
                Text(text = stringResource(Res.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(Res.string.cancel))
            }
        }
    )
}

@Composable
private fun AutoDeleteBeforeDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (Duration) -> Unit,
) {
    var beforeDay by rememberSaveable {
        mutableLongStateOf(
            dataStore
                .getOrDefault(AutoDeleteArticleBeforePreference)
                .milliseconds
                .inWholeDays
        )
    }
    SliderDialog(
        onDismissRequest = onDismissRequest,
        value = beforeDay.toFloat(),
        onValueChange = { beforeDay = it.toLong() },
        valueRange = 1f..365f,
        valueLabel = {
            Text(
                text = suspendString { AutoDeleteArticleBeforePreference.toDisplayNameDays(beforeDay) },
                style = MaterialTheme.typography.labelLarge,
            )
        },
        icon = {
            Icon(
                painter = painterResource(Res.drawable.ic_calendar_clock_24),
                contentDescription = null,
            )
        },
        title = { Text(text = stringResource(Res.string.auto_delete_article_screen_delete_before)) },
        confirmButton = {
            val enabled by remember { derivedStateOf { beforeDay >= 1 } }
            TextButton(
                onClick = { onConfirm(beforeDay.days) },
                enabled = enabled,
            ) {
                Text(text = stringResource(Res.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(Res.string.cancel))
            }
        }
    )
}

@Composable
private fun AutoDeleteMaxCountDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var maxCount by rememberSaveable {
        mutableIntStateOf(dataStore.getOrDefault(AutoDeleteArticleMaxCountPreference))
    }
    SliderDialog(
        onDismissRequest = onDismissRequest,
        value = maxCount.toFloat(),
        onValueChange = { maxCount = it.toInt() },
        valueRange = 10f..3000f,
        valueLabel = {
            Text(
                text = stringResource(
                    Res.string.auto_delete_article_screen_delete_max_count_description,
                    maxCount,
                ),
                style = MaterialTheme.typography.labelLarge,
            )
        },
        icon = null,
        title = { Text(text = stringResource(Res.string.auto_delete_article_screen_delete_before)) },
        confirmButton = {
            val enabled by remember { derivedStateOf { maxCount >= 10 } }
            TextButton(
                onClick = { onConfirm(maxCount) },
                enabled = enabled,
            ) {
                Text(text = stringResource(Res.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(Res.string.cancel))
            }
        }
    )
}