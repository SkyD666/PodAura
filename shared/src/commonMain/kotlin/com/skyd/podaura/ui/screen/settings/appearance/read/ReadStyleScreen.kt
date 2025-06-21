package com.skyd.podaura.ui.screen.settings.appearance.read

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tonality
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.skyd.podaura.model.preference.appearance.feed.TonalElevationPreferenceUtil
import com.skyd.podaura.model.preference.appearance.read.ReadContentTonalElevationPreference
import com.skyd.podaura.model.preference.appearance.read.ReadTopBarTonalElevationPreference
import com.skyd.podaura.ui.component.PodAuraTopBar
import com.skyd.podaura.ui.component.PodAuraTopBarStyle
import com.skyd.podaura.ui.screen.settings.appearance.feed.TonalElevationDialog
import com.skyd.settings.BaseSettingsItem
import com.skyd.settings.SettingsLazyColumn
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.read_style_screen_content_category
import podaura.shared.generated.resources.read_style_screen_name
import podaura.shared.generated.resources.read_style_screen_top_bar_category
import podaura.shared.generated.resources.tonal_elevation


@Serializable
data object ReadStyleRoute

@Composable
fun ReadStyleScreen() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            PodAuraTopBar(
                style = PodAuraTopBarStyle.LargeFlexible,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(Res.string.read_style_screen_name)) },
            )
        }
    ) { paddingValues ->
        var openTopBarTonalElevationDialog by rememberSaveable { mutableStateOf(false) }
        var openReadContentTonalElevationDialog by rememberSaveable { mutableStateOf(false) }

        SettingsLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = paddingValues,
        ) {
            group(text = { getString(Res.string.read_style_screen_top_bar_category) }) {
                item {
                    BaseSettingsItem(
                        icon = rememberVectorPainter(Icons.Outlined.Tonality),
                        text = stringResource(Res.string.tonal_elevation),
                        descriptionText = TonalElevationPreferenceUtil.toDisplay(
                            ReadTopBarTonalElevationPreference.current
                        ),
                        onClick = { openTopBarTonalElevationDialog = true }
                    )
                }
            }
            group(text = { getString(Res.string.read_style_screen_content_category) }) {
                item {
                    BaseSettingsItem(
                        icon = rememberVectorPainter(Icons.Outlined.Tonality),
                        text = stringResource(Res.string.tonal_elevation),
                        descriptionText = TonalElevationPreferenceUtil.toDisplay(
                            ReadContentTonalElevationPreference.current
                        ),
                        onClick = { openReadContentTonalElevationDialog = true }
                    )
                }
            }
        }

        if (openTopBarTonalElevationDialog) {
            TonalElevationDialog(
                onDismissRequest = { openTopBarTonalElevationDialog = false },
                initValue = ReadTopBarTonalElevationPreference.current,
                defaultValue = { ReadTopBarTonalElevationPreference.default },
                onConfirm = {
                    ReadTopBarTonalElevationPreference.put(scope, it)
                    openTopBarTonalElevationDialog = false
                }
            )
        }
        if (openReadContentTonalElevationDialog) {
            TonalElevationDialog(
                onDismissRequest = { openReadContentTonalElevationDialog = false },
                initValue = ReadContentTonalElevationPreference.current,
                defaultValue = { ReadContentTonalElevationPreference.default },
                onConfirm = {
                    ReadContentTonalElevationPreference.put(scope, it)
                    openReadContentTonalElevationDialog = false
                }
            )
        }
    }
}