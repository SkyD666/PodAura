package com.skyd.podaura.ui.component.navigation

import androidx.navigation3.runtime.NavKey
import com.skyd.podaura.ui.screen.MainRoute
import com.skyd.podaura.ui.screen.about.AboutRoute
import com.skyd.podaura.ui.screen.about.TermsOfServiceRoute
import com.skyd.podaura.ui.screen.about.license.LicenseRoute
import com.skyd.podaura.ui.screen.article.ArticleRoute
import com.skyd.podaura.ui.screen.calendar.CalendarRoute
import com.skyd.podaura.ui.screen.download.DownloadDeepLinkRoute
import com.skyd.podaura.ui.screen.download.DownloadRoute
import com.skyd.podaura.ui.screen.feed.FeedListRoute
import com.skyd.podaura.ui.screen.feed.FeedRoute
import com.skyd.podaura.ui.screen.feed.autodl.AutoDownloadRuleRoute
import com.skyd.podaura.ui.screen.feed.mute.MuteFeedRoute
import com.skyd.podaura.ui.screen.feed.reorder.feed.ReorderFeedRoute
import com.skyd.podaura.ui.screen.feed.reorder.group.ReorderGroupRoute
import com.skyd.podaura.ui.screen.feed.requestheaders.RequestHeadersRoute
import com.skyd.podaura.ui.screen.filepicker.FilePickerRoute
import com.skyd.podaura.ui.screen.history.HistoryRoute
import com.skyd.podaura.ui.screen.history.search.HistorySearchRoute
import com.skyd.podaura.ui.screen.media.MediaRoute
import com.skyd.podaura.ui.screen.media.search.MediaSearchRoute
import com.skyd.podaura.ui.screen.media.sub.SubMediaRoute
import com.skyd.podaura.ui.screen.more.MoreRoute
import com.skyd.podaura.ui.screen.playlist.PlaylistRoute
import com.skyd.podaura.ui.screen.playlist.medialist.PlaylistMediaListRoute
import com.skyd.podaura.ui.screen.read.ReadRoute
import com.skyd.podaura.ui.screen.search.SearchRoute
import com.skyd.podaura.ui.screen.settings.SettingsListRoute
import com.skyd.podaura.ui.screen.settings.SettingsRoute
import com.skyd.podaura.ui.screen.settings.appearance.AppearanceRoute
import com.skyd.podaura.ui.screen.settings.appearance.article.ArticleStyleRoute
import com.skyd.podaura.ui.screen.settings.appearance.feed.FeedStyleRoute
import com.skyd.podaura.ui.screen.settings.appearance.media.MediaStyleRoute
import com.skyd.podaura.ui.screen.settings.appearance.read.ReadStyleRoute
import com.skyd.podaura.ui.screen.settings.appearance.search.SearchStyleRoute
import com.skyd.podaura.ui.screen.settings.behavior.BehaviorRoute
import com.skyd.podaura.ui.screen.settings.data.DataRoute
import com.skyd.podaura.ui.screen.settings.data.autodelete.AutoDeleteRoute
import com.skyd.podaura.ui.screen.settings.data.deleteconstraint.DeleteConstraintRoute
import com.skyd.podaura.ui.screen.settings.data.importexport.ImportExportRoute
import com.skyd.podaura.ui.screen.settings.data.importexport.importopml.ImportOpmlDeepLinkRoute
import com.skyd.podaura.ui.screen.settings.data.importexport.importopml.ImportOpmlRoute
import com.skyd.podaura.ui.screen.settings.playerconfig.PlayerConfigRoute
import com.skyd.podaura.ui.screen.settings.playerconfig.advanced.PlayerConfigAdvancedRoute
import com.skyd.podaura.ui.screen.settings.rssconfig.RssConfigRoute
import com.skyd.podaura.ui.screen.settings.rssconfig.updatenotification.UpdateNotificationRoute
import com.skyd.podaura.ui.screen.settings.transmission.TransmissionRoute
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

val PodAuraSerializersModule = SerializersModule {
    polymorphic(baseClass = NavKey::class) {
        subclass(MainRoute::class)
        subclass(FeedRoute::class)
        subclass(FeedListRoute::class)
        subclass(ArticleRoute::class)
        subclass(MoreRoute::class)
        subclass(LicenseRoute::class)
        subclass(AboutRoute::class)
        subclass(TermsOfServiceRoute::class)
        subclass(SettingsRoute::class)
        subclass(AppearanceRoute::class)
        subclass(ArticleStyleRoute::class)
        subclass(FeedStyleRoute::class)
        subclass(ReadStyleRoute::class)
        subclass(MediaStyleRoute::class)
        subclass(ReorderGroupRoute::class)
        subclass(ReorderFeedRoute::class)
        subclass(SearchStyleRoute::class)
        subclass(CalendarRoute::class)
        subclass(BehaviorRoute::class)
        subclass(AutoDeleteRoute::class)
        subclass(HistoryRoute::class)
        subclass(ImportOpmlRoute::class)
        subclass(ImportOpmlDeepLinkRoute::class)
        subclass(ImportExportRoute::class)
        subclass(DataRoute::class)
        subclass(PlayerConfigRoute::class)
        subclass(PlayerConfigAdvancedRoute::class)
        subclass(RssConfigRoute::class)
        subclass(TransmissionRoute::class)
        subclass(UpdateNotificationRoute::class)
        subclass(AutoDownloadRuleRoute::class)
        subclass(MuteFeedRoute::class)
        subclass(DeleteConstraintRoute::class)
        subclass(PlaylistRoute::class)
        subclass(PlaylistMediaListRoute::class)
        subclass(RequestHeadersRoute::class)
        subclass(FilePickerRoute::class)
        subclass(DownloadRoute::class)
        subclass(DownloadDeepLinkRoute::class)
        subclass(ReadRoute::class)
        subclass(SearchRoute.Feed::class)
        subclass(SearchRoute.Article::class)
        subclass(MediaRoute::class)
        subclass(MediaSearchRoute::class)
        subclass(SubMediaRoute::class)
        subclass(HistorySearchRoute::class)
        subclass(SettingsListRoute::class)
    }
}