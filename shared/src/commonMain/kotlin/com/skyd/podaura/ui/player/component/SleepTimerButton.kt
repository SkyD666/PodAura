package com.skyd.podaura.ui.player.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.skyd.fundation.util.Platform
import com.skyd.fundation.util.platform
import com.skyd.podaura.model.preference.player.BackgroundPlayPreference
import com.skyd.podaura.model.preference.player.PauseOnScreenOffPreference
import com.skyd.podaura.ui.player.SleepTimerOption
import com.skyd.podaura.ui.player.SleepTimerState
import com.skyd.podaura.ui.player.component.dialog.BasicPlayerDialog
import com.skyd.podaura.ui.player.land.controller.bar.toDurationString
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.back
import podaura.shared.generated.resources.player_config_screen_background_play
import podaura.shared.generated.resources.player_config_screen_pause_on_screen_off
import podaura.shared.generated.resources.sleep_timer
import podaura.shared.generated.resources.sleep_timer_after_minutes
import podaura.shared.generated.resources.sleep_timer_background_hint
import podaura.shared.generated.resources.sleep_timer_end
import podaura.shared.generated.resources.sleep_timer_end_short
import podaura.shared.generated.resources.sleep_timer_off
import podaura.shared.generated.resources.sleep_timer_playback_settings
import podaura.shared.generated.resources.sleep_timer_screen_off_hint
import kotlin.time.Duration.Companion.seconds

@Composable
internal fun SleepTimerButton(
    sleepTimerState: () -> SleepTimerState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    activeOnly: Boolean = false,
) {
    val timer = sleepTimerState()
    val active = timer.option != SleepTimerOption.Off
    if (activeOnly && !active) return
    val title = stringResource(Res.string.sleep_timer)
    val status = sleepTimerStatus(timer)
    val description = if (active) "$title, $status" else title
    val label = if (timer.option == SleepTimerOption.EndOfMedia) {
        stringResource(Res.string.sleep_timer_end_short)
    } else status
    if (activeOnly) {
        Row(
            modifier = modifier
                .semantics { contentDescription = description }
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Bedtime,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    } else if (active) {
        TextButton(
            modifier = modifier.semantics { contentDescription = description },
            enabled = enabled,
            onClick = onClick,
            colors = ButtonDefaults.textButtonColors(contentColor = LocalContentColor.current),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.Bedtime,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    } else {
        IconButton(onClick = onClick, enabled = enabled, modifier = modifier) {
            Icon(Icons.Outlined.Bedtime, contentDescription = title)
        }
    }
}

@Composable
private fun sleepTimerStatus(state: SleepTimerState): String = when (state.option) {
    SleepTimerOption.Off -> stringResource(Res.string.sleep_timer_off)
    SleepTimerOption.EndOfMedia -> stringResource(Res.string.sleep_timer_end)
    is SleepTimerOption.AfterMinutes -> {
        var remainingSeconds by remember(state) { mutableLongStateOf(state.remainingSeconds()) }
        val lifecycleOwner = LocalLifecycleOwner.current
        LaunchedEffect(state, lifecycleOwner) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    remainingSeconds = state.remainingSeconds()
                    if (remainingSeconds == 0L) break
                    delay(1.seconds)
                }
            }
        }
        remainingSeconds.toDurationString()
    }
}

@Composable
internal fun SleepTimerDialog(
    sleepTimerState: () -> SleepTimerState,
    onSelect: (SleepTimerOption) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val state = sleepTimerState()
    var showSettings by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val screenOffPauses = (platform == Platform.Android || platform == Platform.iOS) &&
            PauseOnScreenOffPreference.current
    val windowHeight =
        with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }
    val maxHeight = (windowHeight - 48.dp).coerceIn(120.dp, 560.dp)
    BasicPlayerDialog(onDismissRequest = onDismissRequest) {
        Column(
            Modifier.heightIn(max = maxHeight).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                stringResource(
                    if (showSettings) Res.string.sleep_timer_playback_settings else Res.string.sleep_timer
                ),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            if (showSettings) {
                if (platform == Platform.Android || platform == Platform.iOS) {
                    SleepPlaybackSetting(
                        title = stringResource(Res.string.player_config_screen_pause_on_screen_off),
                        checked = PauseOnScreenOffPreference.current,
                        onCheckedChange = { PauseOnScreenOffPreference.put(scope, it) },
                    )
                }
                SleepPlaybackSetting(
                    title = stringResource(Res.string.player_config_screen_background_play),
                    checked = BackgroundPlayPreference.current,
                    onCheckedChange = { BackgroundPlayPreference.put(scope, it) },
                )
                TextButton(onClick = {
                    showSettings = false
                }) { Text(stringResource(Res.string.back)) }
            } else {
                if (state.option != SleepTimerOption.Off) {
                    Text(
                        sleepTimerStatus(state),
                        style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                if (screenOffPauses || !BackgroundPlayPreference.current) {
                    Text(
                        stringResource(
                            if (screenOffPauses) Res.string.sleep_timer_screen_off_hint
                            else Res.string.sleep_timer_background_hint
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    TextButton(onClick = { showSettings = true }) {
                        Text(stringResource(Res.string.sleep_timer_playback_settings))
                    }
                }
                val options = remember {
                    listOf(SleepTimerOption.Off) + SleepTimerOption.PRESETS.map {
                        SleepTimerOption.AfterMinutes(it)
                    } + SleepTimerOption.EndOfMedia
                }
                options.forEach { option ->
                    val label = when (option) {
                        SleepTimerOption.Off -> stringResource(Res.string.sleep_timer_off)
                        SleepTimerOption.EndOfMedia -> stringResource(Res.string.sleep_timer_end)
                        is SleepTimerOption.AfterMinutes -> stringResource(
                            Res.string.sleep_timer_after_minutes, option.minutes,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .selectable(
                                selected = state.option == option,
                                role = Role.RadioButton,
                                onClick = {
                                    onSelect(option)
                                    onDismissRequest()
                                },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        RadioButton(selected = state.option == option, onClick = null)
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun SleepPlaybackSetting(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = Modifier.semantics {
            contentDescription = title
        })
    }
}
