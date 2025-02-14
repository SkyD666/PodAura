package com.skyd.anivu.ext

import androidx.datastore.preferences.core.Preferences
import com.skyd.anivu.model.preference.IgnoreUpdateVersionPreference
import com.skyd.anivu.model.preference.Settings
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
import com.skyd.anivu.model.preference.behavior.feed.HideMutedFeedPreference
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
import com.skyd.anivu.model.preference.data.OpmlExportDirPreference
import com.skyd.anivu.model.preference.data.autodelete.AutoDeleteArticleBeforePreference
import com.skyd.anivu.model.preference.data.autodelete.AutoDeleteArticleFrequencyPreference
import com.skyd.anivu.model.preference.data.autodelete.AutoDeleteArticleKeepFavoritePreference
import com.skyd.anivu.model.preference.data.autodelete.AutoDeleteArticleKeepUnreadPreference
import com.skyd.anivu.model.preference.data.autodelete.UseAutoDeletePreference
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

fun Preferences.toSettings(): Settings {
    return Settings(
        // Appearance
        theme = ThemePreference.fromPreferences(this),
        darkMode = DarkModePreference.fromPreferences(this),
        amoledDarkMode = AmoledDarkModePreference.fromPreferences(this),
        feedDefaultGroupExpand = FeedDefaultGroupExpandPreference.fromPreferences(this),
        textFieldStyle = TextFieldStylePreference.fromPreferences(this),
        dateStyle = DateStylePreference.fromPreferences(this),
        navigationBarLabel = NavigationBarLabelPreference.fromPreferences(this),
        feedListTonalElevation = FeedListTonalElevationPreference.fromPreferences(this),
        feedTopBarTonalElevation = FeedTopBarTonalElevationPreference.fromPreferences(this),
        articleListTonalElevation = ArticleListTonalElevationPreference.fromPreferences(this),
        articleTopBarTonalElevation = ArticleTopBarTonalElevationPreference.fromPreferences(this),
        articleItemTonalElevation = ArticleItemTonalElevationPreference.fromPreferences(this),
        searchListTonalElevation = SearchListTonalElevationPreference.fromPreferences(this),
        searchTopBarTonalElevation = SearchTopBarTonalElevationPreference.fromPreferences(this),
        showArticleTopBarRefresh = ShowArticleTopBarRefreshPreference.fromPreferences(this),
        showArticlePullRefresh = ShowArticlePullRefreshPreference.fromPreferences(this),
        articleItemMinWidth = ArticleItemMinWidthPreference.fromPreferences(this),
        searchItemMinWidth = SearchItemMinWidthPreference.fromPreferences(this),
        mediaShowThumbnail = MediaShowThumbnailPreference.fromPreferences(this),
        mediaShowGroupTab = MediaShowGroupTabPreference.fromPreferences(this),
        readTextSize = ReadTextSizePreference.fromPreferences(this),
        readContentTonalElevation = ReadContentTonalElevationPreference.fromPreferences(this),
        readTopBarTonalElevation = ReadTopBarTonalElevationPreference.fromPreferences(this),
        feedNumberBadge = FeedNumberBadgePreference.fromPreferences(this),

        // Update
        ignoreUpdateVersion = IgnoreUpdateVersionPreference.fromPreferences(this),

        // Behavior
        deduplicateTitleInDesc = DeduplicateTitleInDescPreference.fromPreferences(this),
        articleTapAction = ArticleTapActionPreference.fromPreferences(this),
        articleSwipeLeftAction = ArticleSwipeLeftActionPreference.fromPreferences(this),
        articleSwipeRightAction = ArticleSwipeRightActionPreference.fromPreferences(this),
        hideEmptyDefault = HideEmptyDefaultPreference.fromPreferences(this),
        hideMutedFeed = HideMutedFeedPreference.fromPreferences(this),
        pickImageMethod = PickImageMethodPreference.fromPreferences(this),
        mediaFileFilter = MediaFileFilterPreference.fromPreferences(this),

        // RSS
        rssSyncFrequency = RssSyncFrequencyPreference.fromPreferences(this),
        rssSyncWifiConstraint = RssSyncWifiConstraintPreference.fromPreferences(this),
        rssSyncChargingConstraint = RssSyncChargingConstraintPreference.fromPreferences(this),
        rssSyncBatteryNotLowConstraint = RssSyncBatteryNotLowConstraintPreference.fromPreferences(
            this
        ),
        parseLinkTagAsEnclosure = ParseLinkTagAsEnclosurePreference.fromPreferences(this),

        // Player
        playerDoubleTap = PlayerDoubleTapPreference.fromPreferences(this),
        playerShow85sButton = PlayerShow85sButtonPreference.fromPreferences(this),
        playerShowScreenshotButton = PlayerShowScreenshotButtonPreference.fromPreferences(this),
        playerShowProgressIndicator = PlayerShowProgressIndicatorPreference.fromPreferences(this),
        hardwareDecode = HardwareDecodePreference.fromPreferences(this),
        playerAutoPip = PlayerAutoPipPreference.fromPreferences(this),
        playerMaxCacheSize = PlayerMaxCacheSizePreference.fromPreferences(this),
        playerMaxBackCacheSize = PlayerMaxBackCacheSizePreference.fromPreferences(this),
        playerSeekOption = PlayerSeekOptionPreference.fromPreferences(this),
        backgroundPlay = BackgroundPlayPreference.fromPreferences(this),

        // Data
        useAutoDelete = UseAutoDeletePreference.fromPreferences(this),
        autoDeleteArticleFrequency = AutoDeleteArticleFrequencyPreference.fromPreferences(this),
        autoDeleteArticleBefore = AutoDeleteArticleBeforePreference.fromPreferences(this),
        autoDeleteArticleKeepUnread = AutoDeleteArticleKeepUnreadPreference.fromPreferences(this),
        autoDeleteArticleKeepFavorite = AutoDeleteArticleKeepFavoritePreference.fromPreferences(this),
        opmlExportDir = OpmlExportDirPreference.fromPreferences(this),
        mediaLibLocation = MediaLibLocationPreference.fromPreferences(this),

        // Transmission
        seedingWhenComplete = SeedingWhenCompletePreference.fromPreferences(this),
        useProxy = UseProxyPreference.fromPreferences(this),
        proxyMode = ProxyModePreference.fromPreferences(this),
        proxyType = ProxyTypePreference.fromPreferences(this),
        proxyHostname = ProxyHostnamePreference.fromPreferences(this),
        proxyPort = ProxyPortPreference.fromPreferences(this),
        proxyUsername = ProxyUsernamePreference.fromPreferences(this),
        proxyPassword = ProxyPasswordPreference.fromPreferences(this),
        torrentTrackers = TorrentTrackersPreference.fromPreferences(this),
        torrentDhtBootstraps = TorrentDhtBootstrapsPreference.fromPreferences(this),
    )
}
