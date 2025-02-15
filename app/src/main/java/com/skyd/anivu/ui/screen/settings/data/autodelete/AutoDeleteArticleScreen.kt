package com.skyd.anivu.ui.screen.settings.data.autodelete

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoDelete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.skyd.anivu.R
import com.skyd.anivu.ext.dataStore
import com.skyd.anivu.ext.getOrDefault
import com.skyd.anivu.model.preference.data.autodelete.AutoDeleteArticleBeforePreference
import com.skyd.anivu.model.preference.data.autodelete.AutoDeleteArticleFrequencyPreference
import com.skyd.anivu.model.preference.data.autodelete.AutoDeleteArticleKeepFavoritePreference
import com.skyd.anivu.model.preference.data.autodelete.AutoDeleteArticleKeepUnreadPreference
import com.skyd.anivu.model.preference.data.autodelete.AutoDeleteArticleMaxCountPreference
import com.skyd.anivu.model.preference.data.autodelete.AutoDeleteArticleUseBeforePreference
import com.skyd.anivu.model.preference.data.autodelete.AutoDeleteArticleUseMaxCountPreference
import com.skyd.anivu.model.preference.data.autodelete.UseAutoDeletePreference
import com.skyd.anivu.ui.component.BannerItem
import com.skyd.anivu.ui.component.BaseSettingsItem
import com.skyd.anivu.ui.component.CategorySettingsItem
import com.skyd.anivu.ui.component.PodAuraTopBar
import com.skyd.anivu.ui.component.PodAuraTopBarStyle
import com.skyd.anivu.ui.component.SwitchBaseSettingsItem
import com.skyd.anivu.ui.component.SwitchSettingsItem
import com.skyd.anivu.ui.component.TipSettingsItem
import com.skyd.anivu.ui.component.dialog.SliderDialog
import com.skyd.anivu.ui.local.LocalAutoDeleteArticleBefore
import com.skyd.anivu.ui.local.LocalAutoDeleteArticleFrequency
import com.skyd.anivu.ui.local.LocalAutoDeleteArticleKeepFavorite
import com.skyd.anivu.ui.local.LocalAutoDeleteArticleKeepUnread
import com.skyd.anivu.ui.local.LocalAutoDeleteArticleMaxCount
import com.skyd.anivu.ui.local.LocalAutoDeleteArticleUseBefore
import com.skyd.anivu.ui.local.LocalAutoDeleteArticleUseMaxCount
import com.skyd.anivu.ui.local.LocalUseAutoDelete
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds


const val AUTO_DELETE_SCREEN_ROUTE = "autoDeleteScreen"

@Composable
fun AutoDeleteScreen() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            PodAuraTopBar(
                style = PodAuraTopBarStyle.Large,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(R.string.auto_delete_screen_name)) },
            )
        }
    ) { paddingValues ->
        val useAutoDelete = LocalUseAutoDelete.current
        var openAutoDeleteFrequencyDialog by rememberSaveable { mutableStateOf(false) }
        var openAutoDeleteBeforeDialog by rememberSaveable { mutableStateOf(false) }
        var openAutoDeleteMaxCountDialog by rememberSaveable { mutableStateOf(false) }

        val autoDeleteArticleUseBefore = LocalAutoDeleteArticleUseBefore.current
        val autoDeleteArticleUseMaxCount = LocalAutoDeleteArticleUseMaxCount.current
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = paddingValues,
        ) {
            item {
                BannerItem {
                    SwitchSettingsItem(
                        imageVector = Icons.Outlined.AutoDelete,
                        text = stringResource(id = R.string.enable),
                        checked = useAutoDelete,
                        onCheckedChange = {
                            UseAutoDeletePreference.put(
                                context = context, scope = scope, value = it,
                            )
                        }
                    )
                }
            }
            item {
                val autoDeleteArticleFrequency = LocalAutoDeleteArticleFrequency.current
                BaseSettingsItem(
                    icon = rememberVectorPainter(Icons.Outlined.Timer),
                    text = stringResource(id = R.string.auto_delete_article_screen_delete_frequency),
                    descriptionText = AutoDeleteArticleFrequencyPreference.toDisplayNameMilliseconds(
                        context,
                        autoDeleteArticleFrequency,
                    ),
                    onClick = { openAutoDeleteFrequencyDialog = true },
                    enabled = useAutoDelete,
                )
            }
            item {
                CategorySettingsItem(stringResource(R.string.auto_delete_article_screen_strategy_category))
                Spacer(modifier = Modifier.height(6.dp))
                AnimatedVisibility(
                    visible = !autoDeleteArticleUseBefore && !autoDeleteArticleUseMaxCount,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Text(
                        text = stringResource(R.string.auto_delete_article_screen_no_strategy_tip),
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
                val autoDeleteArticleBefore = LocalAutoDeleteArticleBefore.current
                SwitchBaseSettingsItem(
                    checked = autoDeleteArticleUseBefore,
                    painter = painterResource(id = R.drawable.ic_calendar_clock_24),
                    text = stringResource(id = R.string.auto_delete_article_screen_delete_before),
                    description = AutoDeleteArticleBeforePreference.toDisplayNameMilliseconds(
                        context,
                        autoDeleteArticleBefore,
                    ),
                    onClick = { openAutoDeleteBeforeDialog = true },
                    onCheckedChange = {
                        AutoDeleteArticleUseBeforePreference.put(
                            context = context, scope = scope, value = it,
                        )
                    },
                    enabled = useAutoDelete,
                )
            }
            item {
                val autoDeleteArticleMaxCount = LocalAutoDeleteArticleMaxCount.current
                SwitchBaseSettingsItem(
                    checked = autoDeleteArticleUseMaxCount,
                    imageVector = null,
                    text = stringResource(id = R.string.auto_delete_article_screen_delete_max_count),
                    description = stringResource(
                        R.string.auto_delete_article_screen_delete_max_count_description,
                        autoDeleteArticleMaxCount,
                    ),
                    onClick = { openAutoDeleteMaxCountDialog = true },
                    onCheckedChange = {
                        AutoDeleteArticleUseMaxCountPreference.put(
                            context = context, scope = scope, value = it,
                        )
                    },
                    enabled = useAutoDelete,
                )
            }
            item {
                TipSettingsItem(stringResource(R.string.auto_delete_article_screen_strategy_tip))
            }
            item {
                CategorySettingsItem(stringResource(R.string.auto_delete_article_screen_option_category))
            }
            item {
                SwitchSettingsItem(
                    imageVector = Icons.Outlined.MarkEmailUnread,
                    text = stringResource(id = R.string.auto_delete_article_screen_keep_unread),
                    description = stringResource(id = R.string.auto_delete_article_screen_keep_unread_description),
                    checked = LocalAutoDeleteArticleKeepUnread.current,
                    onCheckedChange = {
                        AutoDeleteArticleKeepUnreadPreference.put(
                            context = context, scope = scope, value = it,
                        )
                    }
                )
            }
            item {
                SwitchSettingsItem(
                    imageVector = Icons.Outlined.FavoriteBorder,
                    text = stringResource(id = R.string.auto_delete_article_screen_keep_favorite),
                    description = stringResource(id = R.string.auto_delete_article_screen_keep_favorite_description),
                    checked = LocalAutoDeleteArticleKeepFavorite.current,
                    onCheckedChange = {
                        AutoDeleteArticleKeepFavoritePreference.put(
                            context = context, scope = scope, value = it,
                        )
                    }
                )
            }
        }

        if (openAutoDeleteFrequencyDialog) {
            AutoDeleteFrequencyDialog(
                onDismissRequest = { openAutoDeleteFrequencyDialog = false },
                onConfirm = {
                    AutoDeleteArticleFrequencyPreference.put(
                        context, scope, it.inWholeMilliseconds,
                    )
                    openAutoDeleteFrequencyDialog = false
                }
            )
        }
        if (openAutoDeleteBeforeDialog) {
            AutoDeleteBeforeDialog(
                onDismissRequest = { openAutoDeleteBeforeDialog = false },
                onConfirm = {
                    AutoDeleteArticleBeforePreference.put(
                        context, scope, it.inWholeMilliseconds,
                    )
                    openAutoDeleteBeforeDialog = false
                }
            )
        }
        if (openAutoDeleteMaxCountDialog) {
            AutoDeleteMaxCountDialog(
                onDismissRequest = { openAutoDeleteMaxCountDialog = false },
                onConfirm = {
                    AutoDeleteArticleMaxCountPreference.put(context, scope, it)
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
    val context = LocalContext.current
    var frequencyDay by rememberSaveable {
        mutableLongStateOf(
            context.dataStore
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
                text = AutoDeleteArticleFrequencyPreference.toDisplayNameDays(
                    context = context,
                    days = frequencyDay,
                ),
                style = MaterialTheme.typography.labelLarge,
            )
        },
        icon = { Icon(imageVector = Icons.Outlined.Timer, contentDescription = null) },
        title = { Text(text = stringResource(id = R.string.auto_delete_article_screen_delete_frequency)) },
        confirmButton = {
            val enabled by remember { derivedStateOf { frequencyDay >= 1 } }
            TextButton(
                onClick = { onConfirm(frequencyDay.days) },
                enabled = enabled,
            ) {
                Text(text = stringResource(id = R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(id = R.string.cancel))
            }
        }
    )
}

@Composable
private fun AutoDeleteBeforeDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (Duration) -> Unit,
) {
    val context = LocalContext.current
    var beforeDay by rememberSaveable {
        mutableLongStateOf(
            context.dataStore
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
                text = AutoDeleteArticleBeforePreference.toDisplayNameDays(
                    context = context,
                    days = beforeDay,
                ),
                style = MaterialTheme.typography.labelLarge,
            )
        },
        icon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_calendar_clock_24),
                contentDescription = null,
            )
        },
        title = { Text(text = stringResource(id = R.string.auto_delete_article_screen_delete_before)) },
        confirmButton = {
            val enabled by remember { derivedStateOf { beforeDay >= 1 } }
            TextButton(
                onClick = { onConfirm(beforeDay.days) },
                enabled = enabled,
            ) {
                Text(text = stringResource(id = R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(id = R.string.cancel))
            }
        }
    )
}

@Composable
private fun AutoDeleteMaxCountDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val context = LocalContext.current
    var maxCount by rememberSaveable {
        mutableIntStateOf(context.dataStore.getOrDefault(AutoDeleteArticleMaxCountPreference))
    }
    SliderDialog(
        onDismissRequest = onDismissRequest,
        value = maxCount.toFloat(),
        onValueChange = { maxCount = it.toInt() },
        valueRange = 10f..3000f,
        valueLabel = {
            Text(
                text = stringResource(
                    R.string.auto_delete_article_screen_delete_max_count_description,
                    maxCount,
                ),
                style = MaterialTheme.typography.labelLarge,
            )
        },
        icon = null,
        title = { Text(text = stringResource(id = R.string.auto_delete_article_screen_delete_before)) },
        confirmButton = {
            val enabled by remember { derivedStateOf { maxCount >= 10 } }
            TextButton(
                onClick = { onConfirm(maxCount) },
                enabled = enabled,
            ) {
                Text(text = stringResource(id = R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(id = R.string.cancel))
            }
        }
    )
}