package com.skyd.podaura.ui.screen.settings.appearance.article

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
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
import com.skyd.compone.component.ComponeTopBar
import com.skyd.compone.component.ComponeTopBarStyle
import com.skyd.fundation.ext.format
import com.skyd.podaura.model.preference.appearance.article.ArticleItemMinWidthPreference
import com.skyd.podaura.model.preference.appearance.article.ArticleItemTonalElevationPreference
import com.skyd.podaura.model.preference.appearance.article.ArticleListTonalElevationPreference
import com.skyd.podaura.model.preference.appearance.article.ArticleTopBarTonalElevationPreference
import com.skyd.podaura.model.preference.appearance.article.ShowArticlePullRefreshPreference
import com.skyd.podaura.model.preference.appearance.article.ShowArticleTopBarRefreshPreference
import com.skyd.podaura.model.preference.appearance.feed.TonalElevationPreferenceUtil
import com.skyd.podaura.ui.component.dialog.ItemMinWidthDialog
import com.skyd.podaura.ui.screen.settings.appearance.feed.TonalElevationDialog
import com.skyd.settings.BaseSettingsItem
import com.skyd.settings.SettingsLazyColumn
import com.skyd.settings.SwitchSettingsItem
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.article_style_screen_article_item_category
import podaura.shared.generated.resources.article_style_screen_article_list_category
import podaura.shared.generated.resources.article_style_screen_name
import podaura.shared.generated.resources.article_style_screen_pull_refresh
import podaura.shared.generated.resources.article_style_screen_pull_refresh_description
import podaura.shared.generated.resources.article_style_screen_top_bar_category
import podaura.shared.generated.resources.article_style_screen_top_bar_refresh
import podaura.shared.generated.resources.article_style_screen_top_bar_refresh_description
import podaura.shared.generated.resources.min_width_dp
import podaura.shared.generated.resources.tonal_elevation


@Serializable
data object ArticleStyleRoute

@Composable
fun ArticleStyleScreen(
    windowInsets: WindowInsets = WindowInsets.safeDrawing
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            ComponeTopBar(
                style = ComponeTopBarStyle.LargeFlexible,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(Res.string.article_style_screen_name)) },
                windowInsets = windowInsets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
        },
        contentWindowInsets = windowInsets
    ) { innerPadding ->
        var openTopBarTonalElevationDialog by rememberSaveable { mutableStateOf(false) }
        var openArticleListTonalElevationDialog by rememberSaveable { mutableStateOf(false) }
        var openArticleItemTonalElevationDialog by rememberSaveable { mutableStateOf(false) }
        var openArticleItemMinWidthDialog by rememberSaveable { mutableStateOf(false) }

        SettingsLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = innerPadding,
        ) {
            group(text = { getString(Res.string.article_style_screen_top_bar_category) }) {
                item {
                    BaseSettingsItem(
                        icon = rememberVectorPainter(Icons.Outlined.Tonality),
                        text = stringResource(Res.string.tonal_elevation),
                        descriptionText = TonalElevationPreferenceUtil.toDisplay(
                            ArticleTopBarTonalElevationPreference.current
                        ),
                        onClick = { openTopBarTonalElevationDialog = true }
                    )
                }
                item {
                    SwitchSettingsItem(
                        imageVector = Icons.Outlined.Refresh,
                        text = stringResource(Res.string.article_style_screen_top_bar_refresh),
                        description = stringResource(Res.string.article_style_screen_top_bar_refresh_description),
                        checked = ShowArticleTopBarRefreshPreference.current,
                        onCheckedChange = { ShowArticleTopBarRefreshPreference.put(scope, it) }
                    )
                }
            }
            group(text = { getString(Res.string.article_style_screen_article_list_category) }) {
                item {
                    BaseSettingsItem(
                        icon = rememberVectorPainter(Icons.Outlined.Tonality),
                        text = stringResource(Res.string.tonal_elevation),
                        descriptionText = TonalElevationPreferenceUtil.toDisplay(
                            ArticleListTonalElevationPreference.current
                        ),
                        onClick = { openArticleListTonalElevationDialog = true }
                    )
                }
                item {
                    SwitchSettingsItem(
                        imageVector = Icons.Outlined.Refresh,
                        text = stringResource(Res.string.article_style_screen_pull_refresh),
                        description = stringResource(Res.string.article_style_screen_pull_refresh_description),
                        checked = ShowArticlePullRefreshPreference.current,
                        onCheckedChange = { ShowArticlePullRefreshPreference.put(scope, it) }
                    )
                }
            }
            group(text = { getString(Res.string.article_style_screen_article_item_category) }) {
                item {
                    BaseSettingsItem(
                        icon = rememberVectorPainter(Icons.Outlined.Tonality),
                        text = stringResource(Res.string.tonal_elevation),
                        descriptionText = TonalElevationPreferenceUtil.toDisplay(
                            ArticleItemTonalElevationPreference.current
                        ),
                        onClick = { openArticleItemTonalElevationDialog = true }
                    )
                }
                item {
                    BaseSettingsItem(
                        icon = rememberVectorPainter(Icons.Outlined.WidthNormal),
                        text = stringResource(Res.string.min_width_dp),
                        descriptionText = ArticleItemMinWidthPreference.current.format(2) + " dp",
                        onClick = { openArticleItemMinWidthDialog = true }
                    )
                }
            }
        }

        if (openTopBarTonalElevationDialog) {
            TonalElevationDialog(
                onDismissRequest = { openTopBarTonalElevationDialog = false },
                initValue = ArticleTopBarTonalElevationPreference.current,
                defaultValue = { ArticleTopBarTonalElevationPreference.default },
                onConfirm = {
                    ArticleTopBarTonalElevationPreference.put(scope, it)
                    openTopBarTonalElevationDialog = false
                }
            )
        }
        if (openArticleListTonalElevationDialog) {
            TonalElevationDialog(
                onDismissRequest = { openArticleListTonalElevationDialog = false },
                initValue = ArticleListTonalElevationPreference.current,
                defaultValue = { ArticleListTonalElevationPreference.default },
                onConfirm = {
                    ArticleListTonalElevationPreference.put(scope, it)
                    openArticleListTonalElevationDialog = false
                }
            )
        }
        if (openArticleItemTonalElevationDialog) {
            TonalElevationDialog(
                onDismissRequest = { openArticleItemTonalElevationDialog = false },
                initValue = ArticleItemTonalElevationPreference.current,
                defaultValue = { ArticleItemTonalElevationPreference.default },
                onConfirm = {
                    ArticleItemTonalElevationPreference.put(scope, it)
                    openArticleItemTonalElevationDialog = false
                }
            )
        }
        if (openArticleItemMinWidthDialog) {
            ItemMinWidthDialog(
                onDismissRequest = { openArticleItemMinWidthDialog = false },
                initValue = ArticleItemMinWidthPreference.current,
                defaultValue = { ArticleItemMinWidthPreference.default },
                onConfirm = {
                    ArticleItemMinWidthPreference.put(scope, it)
                    openArticleItemMinWidthDialog = false
                }
            )
        }
    }
}