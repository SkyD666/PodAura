package com.skyd.anivu.ui.screen.settings

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.skyd.anivu.ui.component.PodAuraNavHost
import com.skyd.anivu.ui.screen.feed.reorder.ReorderGroupRoute
import com.skyd.anivu.ui.screen.feed.reorder.ReorderGroupScreen
import com.skyd.anivu.ui.screen.filepicker.FilePickerRoute
import com.skyd.anivu.ui.screen.filepicker.FilePickerRoute.Companion.FilePickerLauncher
import com.skyd.anivu.ui.screen.settings.appearance.AppearanceRoute
import com.skyd.anivu.ui.screen.settings.appearance.AppearanceRoute.AppearanceLauncher
import com.skyd.anivu.ui.screen.settings.appearance.article.ArticleStyleRoute
import com.skyd.anivu.ui.screen.settings.appearance.article.ArticleStyleScreen
import com.skyd.anivu.ui.screen.settings.appearance.feed.FeedStyleRoute
import com.skyd.anivu.ui.screen.settings.appearance.feed.FeedStyleScreen
import com.skyd.anivu.ui.screen.settings.appearance.media.MediaStyleRoute
import com.skyd.anivu.ui.screen.settings.appearance.media.MediaStyleScreen
import com.skyd.anivu.ui.screen.settings.appearance.read.ReadStyleRoute
import com.skyd.anivu.ui.screen.settings.appearance.read.ReadStyleScreen
import com.skyd.anivu.ui.screen.settings.appearance.search.SearchStyleRoute
import com.skyd.anivu.ui.screen.settings.appearance.search.SearchStyleScreen
import com.skyd.anivu.ui.screen.settings.behavior.BehaviorRoute
import com.skyd.anivu.ui.screen.settings.behavior.BehaviorRoute.BehaviorLauncher
import com.skyd.anivu.ui.screen.settings.data.DataRoute
import com.skyd.anivu.ui.screen.settings.data.DataRoute.DataLauncher
import com.skyd.anivu.ui.screen.settings.data.autodelete.AutoDeleteRoute
import com.skyd.anivu.ui.screen.settings.data.autodelete.AutoDeleteScreen
import com.skyd.anivu.ui.screen.settings.data.deleteconstraint.DeleteConstraintRoute
import com.skyd.anivu.ui.screen.settings.data.deleteconstraint.DeleteConstraintScreen
import com.skyd.anivu.ui.screen.settings.data.importexport.ImportExportRoute
import com.skyd.anivu.ui.screen.settings.data.importexport.ImportExportScreen
import com.skyd.anivu.ui.screen.settings.data.importexport.opml.exportopml.ExportOpmlRoute
import com.skyd.anivu.ui.screen.settings.data.importexport.opml.exportopml.ExportOpmlScreen
import com.skyd.anivu.ui.screen.settings.data.importexport.opml.importopml.ImportOpmlRoute
import com.skyd.anivu.ui.screen.settings.data.importexport.opml.importopml.ImportOpmlScreen
import com.skyd.anivu.ui.screen.settings.playerconfig.PlayerConfigRoute
import com.skyd.anivu.ui.screen.settings.playerconfig.PlayerConfigRoute.PlayerConfigLauncher
import com.skyd.anivu.ui.screen.settings.playerconfig.advanced.PlayerConfigAdvancedRoute
import com.skyd.anivu.ui.screen.settings.playerconfig.advanced.PlayerConfigAdvancedScreen
import com.skyd.anivu.ui.screen.settings.rssconfig.RssConfigRoute
import com.skyd.anivu.ui.screen.settings.rssconfig.RssConfigRoute.RssConfigLauncher
import com.skyd.anivu.ui.screen.settings.rssconfig.updatenotification.UpdateNotificationRoute
import com.skyd.anivu.ui.screen.settings.rssconfig.updatenotification.UpdateNotificationScreen
import com.skyd.anivu.ui.screen.settings.transmission.TransmissionRoute
import com.skyd.anivu.ui.screen.settings.transmission.TransmissionRoute.TransmissionLauncher
import com.skyd.anivu.ui.screen.settings.transmission.proxy.ProxyRoute
import com.skyd.anivu.ui.screen.settings.transmission.proxy.ProxyScreen

@Composable
internal fun SettingsPaneNavHost(
    navController: NavHostController,
    startDestination: Any,
    onPaneBack: (() -> Unit)?,
) {
    PodAuraNavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable<AppearanceRoute> { AppearanceLauncher(onPaneBack) }
        composable<ArticleStyleRoute> { ArticleStyleScreen() }
        composable<FeedStyleRoute> { FeedStyleScreen() }
        composable<ReadStyleRoute> { ReadStyleScreen() }
        composable<MediaStyleRoute> { MediaStyleScreen() }
        composable<ReorderGroupRoute> { ReorderGroupScreen() }
        composable<SearchStyleRoute> { SearchStyleScreen() }
        composable<BehaviorRoute> { BehaviorLauncher(onPaneBack) }
        composable<AutoDeleteRoute> { AutoDeleteScreen() }
        composable<ExportOpmlRoute> { ExportOpmlScreen() }
        composable<ImportOpmlRoute> { ImportOpmlScreen() }
        composable<ImportExportRoute> { ImportExportScreen() }
        composable<DataRoute> { DataLauncher(onPaneBack) }
        composable<PlayerConfigRoute> { PlayerConfigLauncher(onPaneBack) }
        composable<PlayerConfigAdvancedRoute> { PlayerConfigAdvancedScreen() }
        composable<RssConfigRoute> { RssConfigLauncher(onPaneBack) }
        composable<ProxyRoute> { ProxyScreen() }
        composable<TransmissionRoute> { TransmissionLauncher(onPaneBack) }
        composable<UpdateNotificationRoute> { UpdateNotificationScreen() }
        composable<DeleteConstraintRoute> { DeleteConstraintScreen() }
        composable<FilePickerRoute> { FilePickerLauncher(it) }
    }
}