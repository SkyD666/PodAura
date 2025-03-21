package com.skyd.anivu.ui.screen.settings.appearance.read

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.skyd.anivu.R
import com.skyd.anivu.model.preference.appearance.feed.TonalElevationPreferenceUtil
import com.skyd.anivu.model.preference.appearance.read.ReadContentTonalElevationPreference
import com.skyd.anivu.model.preference.appearance.read.ReadTopBarTonalElevationPreference
import com.skyd.anivu.ui.component.PodAuraTopBar
import com.skyd.anivu.ui.component.PodAuraTopBarStyle
import com.skyd.anivu.ui.component.BaseSettingsItem
import com.skyd.anivu.ui.component.CategorySettingsItem
import com.skyd.anivu.ui.local.LocalReadContentTonalElevation
import com.skyd.anivu.ui.local.LocalReadTopBarTonalElevation
import com.skyd.anivu.ui.screen.settings.appearance.feed.TonalElevationDialog


const val READ_STYLE_SCREEN_ROUTE = "readStyleScreen"

@Composable
fun ReadStyleScreen() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            PodAuraTopBar(
                style = PodAuraTopBarStyle.Large,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(R.string.read_style_screen_name)) },
            )
        }
    ) { paddingValues ->
        var openTopBarTonalElevationDialog by rememberSaveable { mutableStateOf(false) }
        var openReadContentTonalElevationDialog by rememberSaveable { mutableStateOf(false) }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = paddingValues,
        ) {
            item {
                CategorySettingsItem(text = stringResource(id = R.string.read_style_screen_top_bar_category))
            }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(Icons.Outlined.Tonality),
                    text = stringResource(id = R.string.tonal_elevation),
                    descriptionText = TonalElevationPreferenceUtil.toDisplay(
                        LocalReadTopBarTonalElevation.current
                    ),
                    onClick = { openTopBarTonalElevationDialog = true }
                )
            }
            item {
                CategorySettingsItem(text = stringResource(id = R.string.read_style_screen_content_category))
            }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(Icons.Outlined.Tonality),
                    text = stringResource(id = R.string.tonal_elevation),
                    descriptionText = TonalElevationPreferenceUtil.toDisplay(
                        LocalReadContentTonalElevation.current
                    ),
                    onClick = { openReadContentTonalElevationDialog = true }
                )
            }
        }

        if (openTopBarTonalElevationDialog) {
            TonalElevationDialog(
                onDismissRequest = { openTopBarTonalElevationDialog = false },
                initValue = LocalReadTopBarTonalElevation.current,
                defaultValue = { ReadTopBarTonalElevationPreference.default },
                onConfirm = {
                    ReadTopBarTonalElevationPreference.put(
                        context = context,
                        scope = scope,
                        value = it,
                    )
                    openTopBarTonalElevationDialog = false
                }
            )
        }
        if (openReadContentTonalElevationDialog) {
            TonalElevationDialog(
                onDismissRequest = { openReadContentTonalElevationDialog = false },
                initValue = LocalReadContentTonalElevation.current,
                defaultValue = { ReadContentTonalElevationPreference.default },
                onConfirm = {
                    ReadContentTonalElevationPreference.put(
                        context = context,
                        scope = scope,
                        value = it,
                    )
                    openReadContentTonalElevationDialog = false
                }
            )
        }
    }
}