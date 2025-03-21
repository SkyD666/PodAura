package com.skyd.anivu.ui.screen.settings.appearance.search

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tonality
import androidx.compose.material.icons.outlined.WidthNormal
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
import com.skyd.anivu.model.preference.appearance.search.SearchItemMinWidthPreference
import com.skyd.anivu.model.preference.appearance.search.SearchListTonalElevationPreference
import com.skyd.anivu.model.preference.appearance.search.SearchTopBarTonalElevationPreference
import com.skyd.anivu.ui.component.PodAuraTopBar
import com.skyd.anivu.ui.component.PodAuraTopBarStyle
import com.skyd.anivu.ui.component.BaseSettingsItem
import com.skyd.anivu.ui.component.CategorySettingsItem
import com.skyd.anivu.ui.screen.settings.appearance.article.ItemMinWidthDialog
import com.skyd.anivu.ui.screen.settings.appearance.feed.TonalElevationDialog
import com.skyd.anivu.ui.local.LocalSearchItemMinWidth
import com.skyd.anivu.ui.local.LocalSearchListTonalElevation
import com.skyd.anivu.ui.local.LocalSearchTopBarTonalElevation


const val SEARCH_STYLE_SCREEN_ROUTE = "searchStyleScreen"

@Composable
fun SearchStyleScreen() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            PodAuraTopBar(
                style = PodAuraTopBarStyle.Large,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(R.string.search_style_screen_name)) },
            )
        }
    ) { paddingValues ->
        var openTopBarTonalElevationDialog by rememberSaveable { mutableStateOf(false) }
        var openSearchListTonalElevationDialog by rememberSaveable { mutableStateOf(false) }
        var openSearchItemMinWidthDialog by rememberSaveable { mutableStateOf(false) }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = paddingValues,
        ) {
            item {
                CategorySettingsItem(text = stringResource(id = R.string.search_style_screen_top_bar_category))
            }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(Icons.Outlined.Tonality),
                    text = stringResource(id = R.string.tonal_elevation),
                    descriptionText = TonalElevationPreferenceUtil.toDisplay(
                        LocalSearchTopBarTonalElevation.current
                    ),
                    onClick = { openTopBarTonalElevationDialog = true }
                )
            }
            item {
                CategorySettingsItem(text = stringResource(id = R.string.search_style_screen_search_list_category))
            }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(Icons.Outlined.Tonality),
                    text = stringResource(id = R.string.tonal_elevation),
                    descriptionText = TonalElevationPreferenceUtil.toDisplay(
                        LocalSearchListTonalElevation.current
                    ),
                    onClick = { openSearchListTonalElevationDialog = true }
                )
            }
            item {
                CategorySettingsItem(text = stringResource(id = R.string.search_style_screen_search_item_category))
            }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(Icons.Outlined.WidthNormal),
                    text = stringResource(id = R.string.min_width_dp),
                    descriptionText = "${LocalSearchItemMinWidth.current} dp",
                    onClick = { openSearchItemMinWidthDialog = true }
                )
            }
        }

        if (openTopBarTonalElevationDialog) {
            TonalElevationDialog(
                onDismissRequest = { openTopBarTonalElevationDialog = false },
                initValue = LocalSearchTopBarTonalElevation.current,
                defaultValue = { SearchTopBarTonalElevationPreference.default },
                onConfirm = {
                    SearchTopBarTonalElevationPreference.put(
                        context = context,
                        scope = scope,
                        value = it,
                    )
                    openTopBarTonalElevationDialog = false
                }
            )
        }
        if (openSearchListTonalElevationDialog) {
            TonalElevationDialog(
                onDismissRequest = { openSearchListTonalElevationDialog = false },
                initValue = LocalSearchListTonalElevation.current,
                defaultValue = { SearchListTonalElevationPreference.default },
                onConfirm = {
                    SearchListTonalElevationPreference.put(
                        context = context,
                        scope = scope,
                        value = it,
                    )
                    openSearchListTonalElevationDialog = false
                }
            )
        }
        if (openSearchItemMinWidthDialog) {
            ItemMinWidthDialog(
                onDismissRequest = { openSearchItemMinWidthDialog = false },
                initValue = LocalSearchItemMinWidth.current,
                defaultValue = { SearchItemMinWidthPreference.default },
                onConfirm = {
                    SearchItemMinWidthPreference.put(
                        context = context,
                        scope = scope,
                        value = it,
                    )
                    openSearchItemMinWidthDialog = false
                }
            )
        }
    }
}