package com.skyd.podaura.ui.screen.settings

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.skyd.compone.component.pointerOnBack
import com.skyd.podaura.ui.component.PodAuraNavDisplay
import com.skyd.podaura.ui.screen.feed.reorder.group.ReorderGroupRoute
import com.skyd.podaura.ui.screen.feed.reorder.group.ReorderGroupScreen
import com.skyd.podaura.ui.screen.filepicker.FilePickerRoute
import com.skyd.podaura.ui.screen.filepicker.FilePickerRoute.Companion.FilePickerLauncher
import com.skyd.podaura.ui.screen.settings.appearance.AppearanceRoute
import com.skyd.podaura.ui.screen.settings.appearance.AppearanceScreen
import com.skyd.podaura.ui.screen.settings.appearance.article.ArticleStyleRoute
import com.skyd.podaura.ui.screen.settings.appearance.article.ArticleStyleScreen
import com.skyd.podaura.ui.screen.settings.appearance.feed.FeedStyleRoute
import com.skyd.podaura.ui.screen.settings.appearance.feed.FeedStyleScreen
import com.skyd.podaura.ui.screen.settings.appearance.media.MediaStyleRoute
import com.skyd.podaura.ui.screen.settings.appearance.media.MediaStyleScreen
import com.skyd.podaura.ui.screen.settings.appearance.read.ReadStyleRoute
import com.skyd.podaura.ui.screen.settings.appearance.read.ReadStyleScreen
import com.skyd.podaura.ui.screen.settings.appearance.search.SearchStyleRoute
import com.skyd.podaura.ui.screen.settings.appearance.search.SearchStyleScreen
import com.skyd.podaura.ui.screen.settings.behavior.BehaviorRoute
import com.skyd.podaura.ui.screen.settings.behavior.BehaviorScreen
import com.skyd.podaura.ui.screen.settings.data.DataRoute
import com.skyd.podaura.ui.screen.settings.data.DataScreen
import com.skyd.podaura.ui.screen.settings.data.autodelete.AutoDeleteRoute
import com.skyd.podaura.ui.screen.settings.data.autodelete.AutoDeleteScreen
import com.skyd.podaura.ui.screen.settings.data.deleteconstraint.DeleteConstraintRoute
import com.skyd.podaura.ui.screen.settings.data.deleteconstraint.DeleteConstraintScreen
import com.skyd.podaura.ui.screen.settings.data.importexport.ImportExportRoute
import com.skyd.podaura.ui.screen.settings.data.importexport.ImportExportScreen
import com.skyd.podaura.ui.screen.settings.data.importexport.importopml.ImportOpmlRoute
import com.skyd.podaura.ui.screen.settings.data.importexport.importopml.ImportOpmlScreen
import com.skyd.podaura.ui.screen.settings.playerconfig.PlayerConfigRoute
import com.skyd.podaura.ui.screen.settings.playerconfig.PlayerConfigScreen
import com.skyd.podaura.ui.screen.settings.playerconfig.advanced.PlayerConfigAdvancedRoute
import com.skyd.podaura.ui.screen.settings.playerconfig.advanced.PlayerConfigAdvancedScreen
import com.skyd.podaura.ui.screen.settings.rssconfig.RssConfigRoute
import com.skyd.podaura.ui.screen.settings.rssconfig.RssConfigScreen
import com.skyd.podaura.ui.screen.settings.rssconfig.updatenotification.UpdateNotificationRoute
import com.skyd.podaura.ui.screen.settings.rssconfig.updatenotification.UpdateNotificationScreen
import com.skyd.podaura.ui.screen.settings.transmission.TransmissionRoute
import com.skyd.podaura.ui.screen.settings.transmission.TransmissionScreen

@Composable
internal fun SettingsDetailPaneNavDisplay(
    navBackStack: MutableList<NavKey>,
    sceneStrategy: ListDetailSceneStrategy<NavKey>,
    onPaneBack: (() -> Unit)?,
) {
    val windowInsets =
        WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.End)

    PodAuraNavDisplay(
        backStack = navBackStack,
        modifier = Modifier.pointerOnBack(),
        sceneStrategy = sceneStrategy,
        entryProvider = entryProvider {
            entry<SettingsListRoute>(metadata = ListDetailSceneStrategy.listPane()) { _ ->
                SettingsList(onItemSelected = { itemRoute ->
                    navBackStack.removeAll { it !is SettingsListRoute }
                    navBackStack.add(itemRoute)
                })
            }
            entry<AppearanceRoute>(metadata = ListDetailSceneStrategy.detailPane()) {
                AppearanceScreen(onBack = onPaneBack, windowInsets = windowInsets)
            }
            entry<ArticleStyleRoute>(metadata = ListDetailSceneStrategy.detailPane()) {
                ArticleStyleScreen(windowInsets = windowInsets)
            }
            entry<FeedStyleRoute>(metadata = ListDetailSceneStrategy.detailPane()) {
                FeedStyleScreen(windowInsets = windowInsets)
            }
            entry<ReadStyleRoute>(metadata = ListDetailSceneStrategy.detailPane()) {
                ReadStyleScreen(windowInsets = windowInsets)
            }
            entry<MediaStyleRoute>(metadata = ListDetailSceneStrategy.detailPane()) {
                MediaStyleScreen(windowInsets = windowInsets)
            }
            entry<ReorderGroupRoute>(metadata = ListDetailSceneStrategy.detailPane()) {
                ReorderGroupScreen(windowInsets = windowInsets)
            }
            entry<SearchStyleRoute>(metadata = ListDetailSceneStrategy.detailPane()) {
                SearchStyleScreen(windowInsets = windowInsets)
            }
            entry<BehaviorRoute>(metadata = ListDetailSceneStrategy.detailPane()) {
                BehaviorScreen(onBack = onPaneBack, windowInsets = windowInsets)
            }
            entry<AutoDeleteRoute>(metadata = ListDetailSceneStrategy.detailPane()) {
                AutoDeleteScreen(windowInsets = windowInsets)
            }
            entry<ImportOpmlRoute>(metadata = ListDetailSceneStrategy.detailPane()) {
                ImportOpmlScreen(windowInsets = windowInsets)
            }
            entry<ImportExportRoute>(metadata = ListDetailSceneStrategy.detailPane()) {
                ImportExportScreen(windowInsets = windowInsets)
            }
            entry<DataRoute>(metadata = ListDetailSceneStrategy.detailPane()) {
                DataScreen(onBack = onPaneBack, windowInsets = windowInsets)
            }
            entry<PlayerConfigRoute>(metadata = ListDetailSceneStrategy.detailPane()) {
                PlayerConfigScreen(onBack = onPaneBack, windowInsets = windowInsets)
            }
            entry<PlayerConfigAdvancedRoute>(metadata = ListDetailSceneStrategy.detailPane()) {
                PlayerConfigAdvancedScreen(windowInsets = windowInsets)
            }
            entry<RssConfigRoute>(metadata = ListDetailSceneStrategy.detailPane()) {
                RssConfigScreen(onBack = onPaneBack, windowInsets = windowInsets)
            }
            entry<TransmissionRoute>(metadata = ListDetailSceneStrategy.detailPane()) {
                TransmissionScreen(onBack = onPaneBack, windowInsets = windowInsets)
            }
            entry<UpdateNotificationRoute>(metadata = ListDetailSceneStrategy.detailPane()) {
                UpdateNotificationScreen(windowInsets = windowInsets)
            }
            entry<DeleteConstraintRoute>(metadata = ListDetailSceneStrategy.detailPane()) {
                DeleteConstraintScreen(windowInsets = windowInsets)
            }
            entry<FilePickerRoute>(metadata = ListDetailSceneStrategy.detailPane()) {
                FilePickerLauncher(route = it)
            }
        }
    )
}
