package com.skyd.anivu.model.preference

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.skyd.anivu.ext.dataStore
import com.skyd.anivu.ext.toSettings
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
import com.skyd.anivu.ui.local.LocalAmoledDarkMode
import com.skyd.anivu.ui.local.LocalArticleItemMinWidth
import com.skyd.anivu.ui.local.LocalArticleItemTonalElevation
import com.skyd.anivu.ui.local.LocalArticleListTonalElevation
import com.skyd.anivu.ui.local.LocalArticleSwipeLeftAction
import com.skyd.anivu.ui.local.LocalArticleSwipeRightAction
import com.skyd.anivu.ui.local.LocalArticleTapAction
import com.skyd.anivu.ui.local.LocalArticleTopBarTonalElevation
import com.skyd.anivu.ui.local.LocalAutoDeleteArticleBefore
import com.skyd.anivu.ui.local.LocalAutoDeleteArticleFrequency
import com.skyd.anivu.ui.local.LocalAutoDeleteArticleKeepFavorite
import com.skyd.anivu.ui.local.LocalAutoDeleteArticleKeepUnread
import com.skyd.anivu.ui.local.LocalBackgroundPlay
import com.skyd.anivu.ui.local.LocalDarkMode
import com.skyd.anivu.ui.local.LocalDateStyle
import com.skyd.anivu.ui.local.LocalDeduplicateTitleInDesc
import com.skyd.anivu.ui.local.LocalFeedDefaultGroupExpand
import com.skyd.anivu.ui.local.LocalFeedListTonalElevation
import com.skyd.anivu.ui.local.LocalFeedNumberBadge
import com.skyd.anivu.ui.local.LocalFeedTopBarTonalElevation
import com.skyd.anivu.ui.local.LocalHardwareDecode
import com.skyd.anivu.ui.local.LocalHideEmptyDefault
import com.skyd.anivu.ui.local.LocalHideMutedFeed
import com.skyd.anivu.ui.local.LocalIgnoreUpdateVersion
import com.skyd.anivu.ui.local.LocalMediaFileFilter
import com.skyd.anivu.ui.local.LocalMediaLibLocation
import com.skyd.anivu.ui.local.LocalMediaShowGroupTab
import com.skyd.anivu.ui.local.LocalMediaShowThumbnail
import com.skyd.anivu.ui.local.LocalNavigationBarLabel
import com.skyd.anivu.ui.local.LocalOpmlExportDir
import com.skyd.anivu.ui.local.LocalParseLinkTagAsEnclosure
import com.skyd.anivu.ui.local.LocalPickImageMethod
import com.skyd.anivu.ui.local.LocalPlayerAutoPip
import com.skyd.anivu.ui.local.LocalPlayerDoubleTap
import com.skyd.anivu.ui.local.LocalPlayerMaxBackCacheSize
import com.skyd.anivu.ui.local.LocalPlayerMaxCacheSize
import com.skyd.anivu.ui.local.LocalPlayerSeekOption
import com.skyd.anivu.ui.local.LocalPlayerShow85sButton
import com.skyd.anivu.ui.local.LocalPlayerShowProgressIndicator
import com.skyd.anivu.ui.local.LocalPlayerShowScreenshotButton
import com.skyd.anivu.ui.local.LocalProxyHostname
import com.skyd.anivu.ui.local.LocalProxyMode
import com.skyd.anivu.ui.local.LocalProxyPassword
import com.skyd.anivu.ui.local.LocalProxyPort
import com.skyd.anivu.ui.local.LocalProxyType
import com.skyd.anivu.ui.local.LocalProxyUsername
import com.skyd.anivu.ui.local.LocalReadContentTonalElevation
import com.skyd.anivu.ui.local.LocalReadTextSize
import com.skyd.anivu.ui.local.LocalReadTopBarTonalElevation
import com.skyd.anivu.ui.local.LocalRssSyncBatteryNotLowConstraint
import com.skyd.anivu.ui.local.LocalRssSyncChargingConstraint
import com.skyd.anivu.ui.local.LocalRssSyncFrequency
import com.skyd.anivu.ui.local.LocalRssSyncWifiConstraint
import com.skyd.anivu.ui.local.LocalSearchItemMinWidth
import com.skyd.anivu.ui.local.LocalSearchListTonalElevation
import com.skyd.anivu.ui.local.LocalSearchTopBarTonalElevation
import com.skyd.anivu.ui.local.LocalSeedingWhenComplete
import com.skyd.anivu.ui.local.LocalShowArticlePullRefresh
import com.skyd.anivu.ui.local.LocalShowArticleTopBarRefresh
import com.skyd.anivu.ui.local.LocalTextFieldStyle
import com.skyd.anivu.ui.local.LocalTheme
import com.skyd.anivu.ui.local.LocalUseAutoDelete
import com.skyd.anivu.ui.local.LocalUseProxy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map

data class Settings(
    // Appearance
    val theme: String = ThemePreference.default,
    val darkMode: Int = DarkModePreference.default,
    val amoledDarkMode: Boolean = AmoledDarkModePreference.default,
    val feedDefaultGroupExpand: Boolean = FeedDefaultGroupExpandPreference.default,
    val textFieldStyle: String = TextFieldStylePreference.default,
    val dateStyle: String = DateStylePreference.default,
    val navigationBarLabel: String = NavigationBarLabelPreference.default,
    val feedListTonalElevation: Float = FeedListTonalElevationPreference.default,
    val feedTopBarTonalElevation: Float = FeedTopBarTonalElevationPreference.default,
    val articleListTonalElevation: Float = ArticleListTonalElevationPreference.default,
    val articleTopBarTonalElevation: Float = ArticleTopBarTonalElevationPreference.default,
    val articleItemTonalElevation: Float = ArticleItemTonalElevationPreference.default,
    val searchListTonalElevation: Float = SearchListTonalElevationPreference.default,
    val searchTopBarTonalElevation: Float = SearchTopBarTonalElevationPreference.default,
    val showArticleTopBarRefresh: Boolean = ShowArticleTopBarRefreshPreference.default,
    val showArticlePullRefresh: Boolean = ShowArticlePullRefreshPreference.default,
    val articleItemMinWidth: Float = ArticleItemMinWidthPreference.default,
    val searchItemMinWidth: Float = SearchItemMinWidthPreference.default,
    val mediaShowThumbnail: Boolean = MediaShowThumbnailPreference.default,
    val mediaShowGroupTab: Boolean = MediaShowGroupTabPreference.default,
    val readTextSize: Float = ReadTextSizePreference.default,
    val readContentTonalElevation: Float = ReadContentTonalElevationPreference.default,
    val readTopBarTonalElevation: Float = ReadTopBarTonalElevationPreference.default,
    val feedNumberBadge: Int = FeedNumberBadgePreference.default,
    // Update
    val ignoreUpdateVersion: Long = IgnoreUpdateVersionPreference.default,
    // Behavior
    val deduplicateTitleInDesc: Boolean = DeduplicateTitleInDescPreference.default,
    val articleTapAction: String = ArticleTapActionPreference.default,
    val articleSwipeLeftAction: String = ArticleSwipeLeftActionPreference.default,
    val articleSwipeRightAction: String = ArticleSwipeRightActionPreference.default,
    val hideEmptyDefault: Boolean = HideEmptyDefaultPreference.default,
    val hideMutedFeed: Boolean = HideMutedFeedPreference.default,
    val pickImageMethod: String = PickImageMethodPreference.default,
    val mediaFileFilter: String = MediaFileFilterPreference.default,
    // RSS
    val rssSyncFrequency: Long = RssSyncFrequencyPreference.default,
    val rssSyncWifiConstraint: Boolean = RssSyncWifiConstraintPreference.default,
    val rssSyncChargingConstraint: Boolean = RssSyncChargingConstraintPreference.default,
    val rssSyncBatteryNotLowConstraint: Boolean = RssSyncBatteryNotLowConstraintPreference.default,
    val parseLinkTagAsEnclosure: Boolean = ParseLinkTagAsEnclosurePreference.default,
    // Player
    val playerDoubleTap: String = PlayerDoubleTapPreference.default,
    val playerShow85sButton: Boolean = PlayerShow85sButtonPreference.default,
    val playerShowScreenshotButton: Boolean = PlayerShowScreenshotButtonPreference.default,
    val playerShowProgressIndicator: Boolean = PlayerShowProgressIndicatorPreference.default,
    val hardwareDecode: Boolean = HardwareDecodePreference.default,
    val playerAutoPip: Boolean = PlayerAutoPipPreference.default,
    val playerMaxCacheSize: Long = PlayerMaxCacheSizePreference.default,
    val playerMaxBackCacheSize: Long = PlayerMaxBackCacheSizePreference.default,
    val playerSeekOption: String = PlayerSeekOptionPreference.default,
    val backgroundPlay: Boolean = BackgroundPlayPreference.default,
    // Data
    val useAutoDelete: Boolean = UseAutoDeletePreference.default,
    val autoDeleteArticleFrequency: Long = AutoDeleteArticleFrequencyPreference.default,
    val autoDeleteArticleBefore: Long = AutoDeleteArticleBeforePreference.default,
    val autoDeleteArticleKeepUnread: Boolean = AutoDeleteArticleKeepUnreadPreference.default,
    val autoDeleteArticleKeepFavorite: Boolean = AutoDeleteArticleKeepFavoritePreference.default,
    val opmlExportDir: String = OpmlExportDirPreference.default,
    val mediaLibLocation: String = MediaLibLocationPreference.default,
    // Transmission
    val seedingWhenComplete: Boolean = SeedingWhenCompletePreference.default,
    val useProxy: Boolean = UseProxyPreference.default,
    val proxyMode: String = ProxyModePreference.default,
    val proxyType: String = ProxyTypePreference.default,
    val proxyHostname: String = ProxyHostnamePreference.default,
    val proxyPort: Int = ProxyPortPreference.default,
    val proxyUsername: String = ProxyUsernamePreference.default,
    val proxyPassword: String = ProxyPasswordPreference.default,
)

@Composable
fun SettingsProvider(
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val settings by remember { context.dataStore.data.map { it.toSettings() } }
        .collectAsState(initial = Settings(), context = Dispatchers.Default)

    CompositionLocalProvider(
        // Appearance
        LocalTheme provides settings.theme,
        LocalDarkMode provides settings.darkMode,
        LocalAmoledDarkMode provides settings.amoledDarkMode,
        LocalFeedDefaultGroupExpand provides settings.feedDefaultGroupExpand,
        LocalTextFieldStyle provides settings.textFieldStyle,
        LocalDateStyle provides settings.dateStyle,
        LocalNavigationBarLabel provides settings.navigationBarLabel,
        LocalFeedListTonalElevation provides settings.feedListTonalElevation,
        LocalFeedTopBarTonalElevation provides settings.feedTopBarTonalElevation,
        LocalArticleListTonalElevation provides settings.articleListTonalElevation,
        LocalArticleTopBarTonalElevation provides settings.articleTopBarTonalElevation,
        LocalArticleItemTonalElevation provides settings.articleItemTonalElevation,
        LocalSearchListTonalElevation provides settings.searchListTonalElevation,
        LocalSearchTopBarTonalElevation provides settings.searchTopBarTonalElevation,
        LocalShowArticleTopBarRefresh provides settings.showArticleTopBarRefresh,
        LocalShowArticlePullRefresh provides settings.showArticlePullRefresh,
        LocalArticleItemMinWidth provides settings.articleItemMinWidth,
        LocalSearchItemMinWidth provides settings.searchItemMinWidth,
        LocalMediaShowThumbnail provides settings.mediaShowThumbnail,
        LocalMediaShowGroupTab provides settings.mediaShowGroupTab,
        LocalReadTextSize provides settings.readTextSize,
        LocalReadContentTonalElevation provides settings.readContentTonalElevation,
        LocalReadTopBarTonalElevation provides settings.readTopBarTonalElevation,
        LocalFeedNumberBadge provides settings.feedNumberBadge,
        // Update
        LocalIgnoreUpdateVersion provides settings.ignoreUpdateVersion,
        // Behavior
        LocalDeduplicateTitleInDesc provides settings.deduplicateTitleInDesc,
        LocalArticleTapAction provides settings.articleTapAction,
        LocalArticleSwipeLeftAction provides settings.articleSwipeLeftAction,
        LocalArticleSwipeRightAction provides settings.articleSwipeRightAction,
        LocalHideEmptyDefault provides settings.hideEmptyDefault,
        LocalHideMutedFeed provides settings.hideMutedFeed,
        LocalPickImageMethod provides settings.pickImageMethod,
        LocalMediaFileFilter provides settings.mediaFileFilter,
        // rss
        LocalRssSyncFrequency provides settings.rssSyncFrequency,
        LocalRssSyncWifiConstraint provides settings.rssSyncWifiConstraint,
        LocalRssSyncChargingConstraint provides settings.rssSyncChargingConstraint,
        LocalRssSyncBatteryNotLowConstraint provides settings.rssSyncBatteryNotLowConstraint,
        LocalParseLinkTagAsEnclosure provides settings.parseLinkTagAsEnclosure,
        // Player
        LocalPlayerDoubleTap provides settings.playerDoubleTap,
        LocalPlayerShow85sButton provides settings.playerShow85sButton,
        LocalPlayerShowScreenshotButton provides settings.playerShowScreenshotButton,
        LocalPlayerShowProgressIndicator provides settings.playerShowProgressIndicator,
        LocalHardwareDecode provides settings.hardwareDecode,
        LocalPlayerAutoPip provides settings.playerAutoPip,
        LocalPlayerMaxCacheSize provides settings.playerMaxCacheSize,
        LocalPlayerMaxBackCacheSize provides settings.playerMaxBackCacheSize,
        LocalPlayerSeekOption provides settings.playerSeekOption,
        LocalBackgroundPlay provides settings.backgroundPlay,
        // Data
        LocalUseAutoDelete provides settings.useAutoDelete,
        LocalAutoDeleteArticleFrequency provides settings.autoDeleteArticleFrequency,
        LocalAutoDeleteArticleBefore provides settings.autoDeleteArticleBefore,
        LocalAutoDeleteArticleKeepUnread provides settings.autoDeleteArticleKeepUnread,
        LocalAutoDeleteArticleKeepFavorite provides settings.autoDeleteArticleKeepFavorite,
        LocalOpmlExportDir provides settings.opmlExportDir,
        LocalMediaLibLocation provides settings.mediaLibLocation,
        // Transmission
        LocalSeedingWhenComplete provides settings.seedingWhenComplete,
        LocalUseProxy provides settings.useProxy,
        LocalProxyMode provides settings.proxyMode,
        LocalProxyType provides settings.proxyType,
        LocalProxyHostname provides settings.proxyHostname,
        LocalProxyPort provides settings.proxyPort,
        LocalProxyUsername provides settings.proxyUsername,
        LocalProxyPassword provides settings.proxyPassword,
    ) {
        content()
    }
}