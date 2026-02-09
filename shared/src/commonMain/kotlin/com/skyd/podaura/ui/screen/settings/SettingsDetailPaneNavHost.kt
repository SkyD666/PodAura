package com.skyd.podaura.ui.screen.settings

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.skyd.podaura.ui.component.PodAuraNavHost
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
internal fun SettingsDetailPaneNavHost(
    navController: NavHostController,
    startDestination: Any,
    onPaneBack: (() -> Unit)?,
) {
    val windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.End)

    PodAuraNavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable<AppearanceRoute> { AppearanceScreen(onBack = onPaneBack, windowInsets = windowInsets) }
        composable<ArticleStyleRoute> { ArticleStyleScreen(windowInsets = windowInsets) }
        composable<FeedStyleRoute> { FeedStyleScreen(windowInsets = windowInsets) }
        composable<ReadStyleRoute> { ReadStyleScreen(windowInsets = windowInsets) }
        composable<MediaStyleRoute> { MediaStyleScreen(windowInsets = windowInsets) }
        composable<ReorderGroupRoute> { ReorderGroupScreen(windowInsets = windowInsets) }
        composable<SearchStyleRoute> { SearchStyleScreen(windowInsets = windowInsets) }
        composable<BehaviorRoute> { BehaviorScreen(onBack = onPaneBack, windowInsets = windowInsets) }
        composable<AutoDeleteRoute> { AutoDeleteScreen(windowInsets = windowInsets) }
        composable<ImportOpmlRoute> { ImportOpmlScreen(windowInsets = windowInsets) }
        composable<ImportExportRoute> { ImportExportScreen(windowInsets = windowInsets) }
        composable<DataRoute> { DataScreen(onBack = onPaneBack, windowInsets = windowInsets) }
        composable<PlayerConfigRoute> { PlayerConfigScreen(onBack = onPaneBack, windowInsets = windowInsets) }
        composable<PlayerConfigAdvancedRoute> { PlayerConfigAdvancedScreen(windowInsets = windowInsets) }
        composable<RssConfigRoute> { RssConfigScreen(onBack = onPaneBack, windowInsets = windowInsets) }
        composable<TransmissionRoute> { TransmissionScreen(onBack = onPaneBack, windowInsets = windowInsets) }
        composable<UpdateNotificationRoute> { UpdateNotificationScreen(windowInsets = windowInsets) }
        composable<DeleteConstraintRoute> { DeleteConstraintScreen(windowInsets = windowInsets) }
        composable<FilePickerRoute> { FilePickerLauncher(entry = it, windowInsets = windowInsets) }
    }
}
