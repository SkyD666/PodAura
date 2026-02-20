package com.skyd.podaura.ui.screen.settings.appearance.search

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tonality
import androidx.compose.material.icons.outlined.WidthNormal
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
import androidx.navigation3.runtime.NavKey
import com.skyd.compone.component.ComponeScaffold
import com.skyd.compone.component.ComponeTopBar
import com.skyd.compone.component.ComponeTopBarStyle
import com.skyd.fundation.ext.format
import com.skyd.podaura.model.preference.appearance.feed.TonalElevationPreferenceUtil
import com.skyd.podaura.model.preference.appearance.search.SearchItemMinWidthPreference
import com.skyd.podaura.model.preference.appearance.search.SearchListTonalElevationPreference
import com.skyd.podaura.model.preference.appearance.search.SearchTopBarTonalElevationPreference
import com.skyd.podaura.ui.component.dialog.ItemMinWidthDialog
import com.skyd.podaura.ui.screen.settings.appearance.feed.TonalElevationDialog
import com.skyd.settings.BaseSettingsItem
import com.skyd.settings.SettingsLazyColumn
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.min_width_dp
import podaura.shared.generated.resources.search_style_screen_name
import podaura.shared.generated.resources.search_style_screen_search_item_category
import podaura.shared.generated.resources.search_style_screen_search_list_category
import podaura.shared.generated.resources.search_style_screen_top_bar_category
import podaura.shared.generated.resources.tonal_elevation


@Serializable
data object SearchStyleRoute : NavKey

@Composable
fun SearchStyleScreen(
    windowInsets: WindowInsets = WindowInsets.safeDrawing
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scope = rememberCoroutineScope()

    ComponeScaffold(
        topBar = {
            ComponeTopBar(
                style = ComponeTopBarStyle.LargeFlexible,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(Res.string.search_style_screen_name)) },
                windowInsets = windowInsets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
        },
        contentWindowInsets = windowInsets
    ) { innerPadding ->
        var openTopBarTonalElevationDialog by rememberSaveable { mutableStateOf(false) }
        var openSearchListTonalElevationDialog by rememberSaveable { mutableStateOf(false) }
        var openSearchItemMinWidthDialog by rememberSaveable { mutableStateOf(false) }

        SettingsLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = innerPadding,
        ) {
            group(text = { getString(Res.string.search_style_screen_top_bar_category) }) {
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
            }
            group(text = { getString(Res.string.search_style_screen_search_list_category) }) {
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
            }
            group(text = { getString(Res.string.search_style_screen_search_item_category) }) {
                item {
                    BaseSettingsItem(
                        icon = rememberVectorPainter(Icons.Outlined.WidthNormal),
                        text = stringResource(Res.string.min_width_dp),
                        descriptionText = SearchItemMinWidthPreference.current.format(2) + " dp",
                        onClick = { openSearchItemMinWidthDialog = true }
                    )
                }
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