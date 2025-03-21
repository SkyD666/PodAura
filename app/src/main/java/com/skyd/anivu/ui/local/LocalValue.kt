package com.skyd.anivu.ui.local

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.compositionLocalOf
import androidx.navigation.NavHostController
import com.skyd.anivu.model.preference.IgnoreUpdateVersionPreference
import com.skyd.anivu.model.preference.appearance.AmoledDarkModePreference
import com.skyd.anivu.model.preference.appearance.DarkModePreference
import com.skyd.anivu.model.preference.appearance.DateStylePreference
import com.skyd.anivu.model.preference.appearance.NavigationBarLabelPreference
import com.skyd.anivu.model.preference.appearance.TextFieldStylePreference
import com.skyd.anivu.model.preference.appearance.ThemePreference
import com.skyd.anivu.model.preference.appearance.article.ArticleItemMinWidthPreference
import com.skyd.anivu.model.preference.appearance.article.ArticleItemTonalElevationPreference
import com.skyd.anivu.model.preference.appearance.article.ArticleListTonalElevationPreference
import com.skyd.anivu.model.preference.appearance.article.ArticleTopBarTonalElevationPreference
import com.skyd.anivu.model.preference.appearance.article.ShowArticlePullRefreshPreference
import com.skyd.anivu.model.preference.appearance.article.ShowArticleTopBarRefreshPreference
import com.skyd.anivu.model.preference.appearance.feed.FeedDefaultGroupExpandPreference
import com.skyd.anivu.model.preference.appearance.feed.FeedListTonalElevationPreference
import com.skyd.anivu.model.preference.appearance.feed.FeedNumberBadgePreference
import com.skyd.anivu.model.preference.appearance.feed.FeedTopBarTonalElevationPreference
import com.skyd.anivu.model.preference.appearance.media.MediaFileFilterPreference
import com.skyd.anivu.model.preference.appearance.media.MediaShowGroupTabPreference
import com.skyd.anivu.model.preference.appearance.media.MediaShowThumbnailPreference
import com.skyd.anivu.model.preference.appearance.read.ReadContentTonalElevationPreference
import com.skyd.anivu.model.preference.appearance.read.ReadTextSizePreference
import com.skyd.anivu.model.preference.appearance.read.ReadTopBarTonalElevationPreference
import com.skyd.anivu.model.preference.appearance.search.SearchItemMinWidthPreference
import com.skyd.anivu.model.preference.appearance.search.SearchListTonalElevationPreference
import com.skyd.anivu.model.preference.appearance.search.SearchTopBarTonalElevationPreference
import com.skyd.anivu.model.preference.behavior.PickImageMethodPreference
import com.skyd.anivu.model.preference.behavior.article.ArticleSwipeLeftActionPreference
import com.skyd.anivu.model.preference.behavior.article.ArticleSwipeRightActionPreference
import com.skyd.anivu.model.preference.behavior.article.ArticleTapActionPreference
import com.skyd.anivu.model.preference.behavior.article.DeduplicateTitleInDescPreference
import com.skyd.anivu.model.preference.behavior.feed.HideEmptyDefaultPreference
import com.skyd.anivu.model.preference.behavior.feed.HideMutedFeedPreference
import com.skyd.anivu.model.preference.behavior.media.MediaListSortAscPreference
import com.skyd.anivu.model.preference.behavior.media.MediaListSortByPreference
import com.skyd.anivu.model.preference.behavior.media.MediaSubListSortAscPreference
import com.skyd.anivu.model.preference.behavior.media.MediaSubListSortByPreference
import com.skyd.anivu.model.preference.behavior.playlist.PlaylistMediaSortAscPreference
import com.skyd.anivu.model.preference.behavior.playlist.PlaylistMediaSortByPreference
import com.skyd.anivu.model.preference.behavior.playlist.PlaylistSortAscPreference
import com.skyd.anivu.model.preference.behavior.playlist.PlaylistSortByPreference
import com.skyd.anivu.model.preference.data.OpmlExportDirPreference
import com.skyd.anivu.model.preference.data.delete.KeepFavoriteArticlesPreference
import com.skyd.anivu.model.preference.data.delete.KeepPlaylistArticlesPreference
import com.skyd.anivu.model.preference.data.delete.KeepUnreadArticlesPreference
import com.skyd.anivu.model.preference.data.delete.autodelete.AutoDeleteArticleBeforePreference
import com.skyd.anivu.model.preference.data.delete.autodelete.AutoDeleteArticleFrequencyPreference
import com.skyd.anivu.model.preference.data.delete.autodelete.AutoDeleteArticleKeepFavoritePreference
import com.skyd.anivu.model.preference.data.delete.autodelete.AutoDeleteArticleKeepPlaylistPreference
import com.skyd.anivu.model.preference.data.delete.autodelete.AutoDeleteArticleKeepUnreadPreference
import com.skyd.anivu.model.preference.data.delete.autodelete.AutoDeleteArticleMaxCountPreference
import com.skyd.anivu.model.preference.data.delete.autodelete.AutoDeleteArticleUseBeforePreference
import com.skyd.anivu.model.preference.data.delete.autodelete.AutoDeleteArticleUseMaxCountPreference
import com.skyd.anivu.model.preference.data.delete.autodelete.UseAutoDeletePreference
import com.skyd.anivu.model.preference.data.medialib.MediaLibLocationPreference
import com.skyd.anivu.model.preference.player.BackgroundPlayPreference
import com.skyd.anivu.model.preference.player.HardwareDecodePreference
import com.skyd.anivu.model.preference.player.PlayerAutoPipPreference
import com.skyd.anivu.model.preference.player.PlayerDoubleTapPreference
import com.skyd.anivu.model.preference.player.PlayerMaxBackCacheSizePreference
import com.skyd.anivu.model.preference.player.PlayerMaxCacheSizePreference
import com.skyd.anivu.model.preference.player.PlayerSeekOptionPreference
import com.skyd.anivu.model.preference.player.PlayerShow85sButtonPreference
import com.skyd.anivu.model.preference.player.PlayerShowProgressIndicatorPreference
import com.skyd.anivu.model.preference.player.PlayerShowScreenshotButtonPreference
import com.skyd.anivu.model.preference.proxy.ProxyHostnamePreference
import com.skyd.anivu.model.preference.proxy.ProxyModePreference
import com.skyd.anivu.model.preference.proxy.ProxyPasswordPreference
import com.skyd.anivu.model.preference.proxy.ProxyPortPreference
import com.skyd.anivu.model.preference.proxy.ProxyTypePreference
import com.skyd.anivu.model.preference.proxy.ProxyUsernamePreference
import com.skyd.anivu.model.preference.proxy.UseProxyPreference
import com.skyd.anivu.model.preference.rss.ParseLinkTagAsEnclosurePreference
import com.skyd.anivu.model.preference.rss.RssSyncBatteryNotLowConstraintPreference
import com.skyd.anivu.model.preference.rss.RssSyncChargingConstraintPreference
import com.skyd.anivu.model.preference.rss.RssSyncFrequencyPreference
import com.skyd.anivu.model.preference.rss.RssSyncWifiConstraintPreference
import com.skyd.anivu.model.preference.transmission.SeedingWhenCompletePreference
import com.skyd.anivu.model.preference.transmission.TorrentDhtBootstrapsPreference
import com.skyd.anivu.model.preference.transmission.TorrentTrackersPreference

val LocalNavController = compositionLocalOf<NavHostController> {
    error("LocalNavController not initialized!")
}

val LocalWindowSizeClass = compositionLocalOf<WindowSizeClass> {
    error("LocalWindowSizeClass not initialized!")
}

// Appearance
val LocalTheme = compositionLocalOf { ThemePreference.default }
val LocalDarkMode = compositionLocalOf { DarkModePreference.default }
val LocalAmoledDarkMode = compositionLocalOf { AmoledDarkModePreference.default }
val LocalFeedDefaultGroupExpand = compositionLocalOf { FeedDefaultGroupExpandPreference.default }
val LocalTextFieldStyle = compositionLocalOf { TextFieldStylePreference.default }
val LocalDateStyle = compositionLocalOf { DateStylePreference.default }
val LocalNavigationBarLabel = compositionLocalOf { NavigationBarLabelPreference.default }
val LocalFeedListTonalElevation = compositionLocalOf { FeedListTonalElevationPreference.default }
val LocalFeedTopBarTonalElevation =
    compositionLocalOf { FeedTopBarTonalElevationPreference.default }
val LocalArticleListTonalElevation =
    compositionLocalOf { ArticleListTonalElevationPreference.default }
val LocalArticleTopBarTonalElevation =
    compositionLocalOf { ArticleTopBarTonalElevationPreference.default }
val LocalArticleItemTonalElevation =
    compositionLocalOf { ArticleItemTonalElevationPreference.default }
val LocalSearchListTonalElevation =
    compositionLocalOf { SearchListTonalElevationPreference.default }
val LocalSearchTopBarTonalElevation =
    compositionLocalOf { SearchTopBarTonalElevationPreference.default }
val LocalShowArticleTopBarRefresh =
    compositionLocalOf { ShowArticleTopBarRefreshPreference.default }
val LocalShowArticlePullRefresh = compositionLocalOf { ShowArticlePullRefreshPreference.default }
val LocalArticleItemMinWidth = compositionLocalOf { ArticleItemMinWidthPreference.default }
val LocalSearchItemMinWidth = compositionLocalOf { SearchItemMinWidthPreference.default }
val LocalMediaShowThumbnail = compositionLocalOf { MediaShowThumbnailPreference.default }
val LocalMediaShowGroupTab = compositionLocalOf { MediaShowGroupTabPreference.default }
val LocalReadTextSize = compositionLocalOf { ReadTextSizePreference.default }
val LocalReadContentTonalElevation =
    compositionLocalOf { ReadContentTonalElevationPreference.default }
val LocalReadTopBarTonalElevation =
    compositionLocalOf { ReadTopBarTonalElevationPreference.default }
val LocalFeedNumberBadge = compositionLocalOf { FeedNumberBadgePreference.default }

// Update
val LocalIgnoreUpdateVersion = compositionLocalOf { IgnoreUpdateVersionPreference.default }

// Behavior
val LocalDeduplicateTitleInDesc = compositionLocalOf { DeduplicateTitleInDescPreference.default }
val LocalArticleTapAction = compositionLocalOf { ArticleTapActionPreference.default }
val LocalArticleSwipeLeftAction = compositionLocalOf { ArticleSwipeLeftActionPreference.default }
val LocalArticleSwipeRightAction = compositionLocalOf { ArticleSwipeRightActionPreference.default }
val LocalHideEmptyDefault = compositionLocalOf { HideEmptyDefaultPreference.default }
val LocalHideMutedFeed = compositionLocalOf { HideMutedFeedPreference.default }
val LocalPickImageMethod = compositionLocalOf { PickImageMethodPreference.default }
val LocalMediaFileFilter = compositionLocalOf { MediaFileFilterPreference.default }
val LocalMediaListSortAsc = compositionLocalOf { MediaListSortAscPreference.default }
val LocalMediaSubListSortAsc = compositionLocalOf { MediaSubListSortAscPreference.default }
val LocalMediaListSortBy = compositionLocalOf { MediaListSortByPreference.default }
val LocalMediaSubListSortBy = compositionLocalOf { MediaSubListSortByPreference.default }
val LocalPlaylistSortAsc = compositionLocalOf { PlaylistSortAscPreference.default }
val LocalPlaylistMediaSortAsc = compositionLocalOf { PlaylistMediaSortAscPreference.default }
val LocalPlaylistSortBy = compositionLocalOf { PlaylistSortByPreference.default }
val LocalPlaylistMediaSortBy = compositionLocalOf { PlaylistMediaSortByPreference.default }

// RSS
val LocalRssSyncFrequency = compositionLocalOf { RssSyncFrequencyPreference.default }
val LocalRssSyncWifiConstraint = compositionLocalOf { RssSyncWifiConstraintPreference.default }
val LocalRssSyncChargingConstraint =
    compositionLocalOf { RssSyncChargingConstraintPreference.default }
val LocalRssSyncBatteryNotLowConstraint =
    compositionLocalOf { RssSyncBatteryNotLowConstraintPreference.default }
val LocalParseLinkTagAsEnclosure = compositionLocalOf { ParseLinkTagAsEnclosurePreference.default }

// Player
val LocalPlayerDoubleTap = compositionLocalOf { PlayerDoubleTapPreference.default }
val LocalPlayerShow85sButton = compositionLocalOf { PlayerShow85sButtonPreference.default }
val LocalPlayerShowScreenshotButton =
    compositionLocalOf { PlayerShowScreenshotButtonPreference.default }
val LocalPlayerShowProgressIndicator =
    compositionLocalOf { PlayerShowProgressIndicatorPreference.default }
val LocalHardwareDecode = compositionLocalOf { HardwareDecodePreference.default }
val LocalPlayerAutoPip = compositionLocalOf { PlayerAutoPipPreference.default }
val LocalPlayerMaxCacheSize = compositionLocalOf { PlayerMaxCacheSizePreference.default }
val LocalPlayerMaxBackCacheSize = compositionLocalOf { PlayerMaxBackCacheSizePreference.default }
val LocalPlayerSeekOption = compositionLocalOf { PlayerSeekOptionPreference.default }
val LocalBackgroundPlay = compositionLocalOf { BackgroundPlayPreference.default }

// Data
val LocalUseAutoDelete = compositionLocalOf { UseAutoDeletePreference.default }
val LocalAutoDeleteArticleUseBefore =
    compositionLocalOf { AutoDeleteArticleUseBeforePreference.default }
val LocalAutoDeleteArticleFrequency =
    compositionLocalOf { AutoDeleteArticleFrequencyPreference.default }
val LocalAutoDeleteArticleBefore = compositionLocalOf { AutoDeleteArticleBeforePreference.default }
val LocalAutoDeleteArticleKeepUnread =
    compositionLocalOf { AutoDeleteArticleKeepUnreadPreference.default }
val LocalAutoDeleteArticleKeepFavorite =
    compositionLocalOf { AutoDeleteArticleKeepFavoritePreference.default }
val LocalAutoDeleteArticleKeepPlaylist =
    compositionLocalOf { AutoDeleteArticleKeepPlaylistPreference.default }
val LocalAutoDeleteArticleUseMaxCount =
    compositionLocalOf { AutoDeleteArticleUseMaxCountPreference.default }
val LocalAutoDeleteArticleMaxCount =
    compositionLocalOf { AutoDeleteArticleMaxCountPreference.default }
val LocalKeepPlaylistArticles = compositionLocalOf { KeepPlaylistArticlesPreference.default }
val LocalKeepUnreadArticles = compositionLocalOf { KeepUnreadArticlesPreference.default }
val LocalKeepFavoriteArticles = compositionLocalOf { KeepFavoriteArticlesPreference.default }
val LocalOpmlExportDir = compositionLocalOf { OpmlExportDirPreference.default }
val LocalMediaLibLocation = compositionLocalOf { MediaLibLocationPreference.default }

// Transmission
val LocalSeedingWhenComplete = compositionLocalOf { SeedingWhenCompletePreference.default }
val LocalUseProxy = compositionLocalOf { UseProxyPreference.default }
val LocalProxyMode = compositionLocalOf { ProxyModePreference.default }
val LocalProxyType = compositionLocalOf { ProxyTypePreference.default }
val LocalProxyHostname = compositionLocalOf { ProxyHostnamePreference.default }
val LocalProxyPort = compositionLocalOf { ProxyPortPreference.default }
val LocalProxyUsername = compositionLocalOf { ProxyUsernamePreference.default }
val LocalProxyPassword = compositionLocalOf { ProxyPasswordPreference.default }
val LocalTorrentTrackers = compositionLocalOf { TorrentTrackersPreference.default }
val LocalTorrentDhtBootstraps = compositionLocalOf { TorrentDhtBootstrapsPreference.default }