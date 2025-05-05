package com.skyd.podaura.ui.screen.settings.appearance.search

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
import com.skyd.podaura.model.preference.appearance.feed.TonalElevationPreferenceUtil
import com.skyd.podaura.model.preference.appearance.search.SearchItemMinWidthPreference
import com.skyd.podaura.model.preference.appearance.search.SearchListTonalElevationPreference
import com.skyd.podaura.model.preference.appearance.search.SearchTopBarTonalElevationPreference
import com.skyd.podaura.ui.component.BaseSettingsItem
import com.skyd.podaura.ui.component.CategorySettingsItem
import com.skyd.podaura.ui.component.PodAuraTopBar
import com.skyd.podaura.ui.component.PodAuraTopBarStyle
import com.skyd.podaura.ui.component.dialog.ItemMinWidthDialog
import com.skyd.podaura.ui.screen.settings.appearance.feed.TonalElevationDialog
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.min_width_dp
import podaura.shared.generated.resources.search_style_screen_name
import podaura.shared.generated.resources.search_style_screen_search_item_category
import podaura.shared.generated.resources.search_style_screen_search_list_category
import podaura.shared.generated.resources.search_style_screen_top_bar_category
import podaura.shared.generated.resources.tonal_elevation


@Serializable
data object SearchStyleRoute

@Composable
fun SearchStyleScreen() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            PodAuraTopBar(
                style = PodAuraTopBarStyle.Large,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(Res.string.search_style_screen_name)) },
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
                CategorySettingsItem(text = stringResource(Res.string.search_style_screen_top_bar_category))
            }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(Icons.Outlined.Tonality),
                    text = stringResource(Res.string.tonal_elevation),
                    descriptionText = TonalElevationPreferenceUtil.toDisplay(
                        SearchTopBarTonalElevationPreference.current
                    ),
                    onClick = { openTopBarTonalElevationDialog = true }
                )
            }
            item {
                CategorySettingsItem(text = stringResource(Res.string.search_style_screen_search_list_category))
            }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(Icons.Outlined.Tonality),
                    text = stringResource(Res.string.tonal_elevation),
                    descriptionText = TonalElevationPreferenceUtil.toDisplay(
                        SearchListTonalElevationPreference.current
                    ),
                    onClick = { openSearchListTonalElevationDialog = true }
                )
            }
            item {
                CategorySettingsItem(text = stringResource(Res.string.search_style_screen_search_item_category))
            }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(Icons.Outlined.WidthNormal),
                    text = stringResource(Res.string.min_width_dp),
                    descriptionText = "%.2f".format(SearchItemMinWidthPreference.current) + " dp",
                    onClick = { openSearchItemMinWidthDialog = true }
                )
            }
        }

        if (openTopBarTonalElevationDialog) {
            TonalElevationDialog(
                onDismissRequest = { openTopBarTonalElevationDialog = false },
                initValue = SearchTopBarTonalElevationPreference.current,
                defaultValue = { SearchTopBarTonalElevationPreference.default },
                onConfirm = {
                    SearchTopBarTonalElevationPreference.put(scope, it)
                    openTopBarTonalElevationDialog = false
                }
            )
        }
        if (openSearchListTonalElevationDialog) {
            TonalElevationDialog(
                onDismissRequest = { openSearchListTonalElevationDialog = false },
                initValue = SearchListTonalElevationPreference.current,
                defaultValue = { SearchListTonalElevationPreference.default },
                onConfirm = {
                    SearchListTonalElevationPreference.put(scope, it)
                    openSearchListTonalElevationDialog = false
                }
            )
        }
        if (openSearchItemMinWidthDialog) {
            ItemMinWidthDialog(
                onDismissRequest = { openSearchItemMinWidthDialog = false },
                initValue = SearchItemMinWidthPreference.current,
                defaultValue = { SearchItemMinWidthPreference.default },
                onConfirm = {
                    SearchItemMinWidthPreference.put(scope, it)
                    openSearchItemMinWidthDialog = false
                }
            )
        }
    }
}