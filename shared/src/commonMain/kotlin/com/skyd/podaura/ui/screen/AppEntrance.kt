package com.skyd.podaura.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.savedstate.serialization.SavedStateConfiguration
import com.skyd.compone.component.navigation.LocalGlobalNavBackStack
import com.skyd.compone.component.navigation.LocalNavBackStack
import com.skyd.compone.component.navigation.LocalResultStore
import com.skyd.compone.component.navigation.newNavBackStack
import com.skyd.compone.component.navigation.rememberResultStore
import com.skyd.podaura.ext.flowOf
import com.skyd.podaura.ext.getOrDefault
import com.skyd.podaura.model.preference.AcceptTermsPreference
import com.skyd.podaura.model.preference.appearance.DarkModePreference
import com.skyd.podaura.model.preference.dataStore
import com.skyd.podaura.ui.component.PodAuraNavDisplay
import com.skyd.podaura.ui.component.SettingsProvider
import com.skyd.podaura.ui.component.calculateWindowSizeClass
import com.skyd.podaura.ui.component.navigation.ExternalUrlListener
import com.skyd.podaura.ui.component.navigation.PodAuraSerializersModule
import com.skyd.podaura.ui.component.navigation.deeplink.DeepLinkPattern
import com.skyd.podaura.ui.component.navigation.initialNavKey
import com.skyd.podaura.ui.local.LocalWindowSizeClass
import com.skyd.podaura.ui.screen.about.AboutRoute
import com.skyd.podaura.ui.screen.about.AboutScreen
import com.skyd.podaura.ui.screen.about.TermsOfServiceRoute
import com.skyd.podaura.ui.screen.about.TermsOfServiceScreen
import com.skyd.podaura.ui.screen.about.license.LicenseRoute
import com.skyd.podaura.ui.screen.about.license.LicenseScreen
import com.skyd.podaura.ui.screen.about.update.UpdateDialog
import com.skyd.podaura.ui.screen.article.ArticleRoute
import com.skyd.podaura.ui.screen.article.ArticleRoute.Companion.ArticleLauncher
import com.skyd.podaura.ui.screen.calendar.CalendarRoute
import com.skyd.podaura.ui.screen.calendar.CalendarScreen
import com.skyd.podaura.ui.screen.download.DownloadDeepLinkRoute
import com.skyd.podaura.ui.screen.download.DownloadDeepLinkRoute.Companion.DownloadDeepLinkLauncher
import com.skyd.podaura.ui.screen.download.DownloadRoute
import com.skyd.podaura.ui.screen.download.DownloadRoute.Companion.DownloadLauncher
import com.skyd.podaura.ui.screen.download.deepLinkPatterns
import com.skyd.podaura.ui.screen.feed.autodl.AutoDownloadRuleRoute
import com.skyd.podaura.ui.screen.feed.autodl.AutoDownloadRuleRoute.Companion.AutoDownloadRuleLauncher
import com.skyd.podaura.ui.screen.feed.mute.MuteFeedRoute
import com.skyd.podaura.ui.screen.feed.mute.MuteFeedScreen
import com.skyd.podaura.ui.screen.feed.reorder.feed.ReorderFeedRoute
import com.skyd.podaura.ui.screen.feed.reorder.feed.ReorderFeedRoute.Companion.ReorderFeedLauncher
import com.skyd.podaura.ui.screen.feed.reorder.group.ReorderGroupRoute
import com.skyd.podaura.ui.screen.feed.reorder.group.ReorderGroupScreen
import com.skyd.podaura.ui.screen.feed.requestheaders.RequestHeadersRoute
import com.skyd.podaura.ui.screen.feed.requestheaders.RequestHeadersRoute.Companion.RequestHeadersLauncher
import com.skyd.podaura.ui.screen.filepicker.FilePickerRoute
import com.skyd.podaura.ui.screen.filepicker.FilePickerRoute.Companion.FilePickerLauncher
import com.skyd.podaura.ui.screen.history.HistoryRoute
import com.skyd.podaura.ui.screen.history.HistoryScreen
import com.skyd.podaura.ui.screen.history.search.HistorySearchRoute
import com.skyd.podaura.ui.screen.history.search.HistorySearchScreen
import com.skyd.podaura.ui.screen.media.search.MediaSearchRoute
import com.skyd.podaura.ui.screen.media.search.MediaSearchRoute.Companion.MediaSearchLauncher
import com.skyd.podaura.ui.screen.media.sub.SubMediaRoute
import com.skyd.podaura.ui.screen.media.sub.SubMediaRoute.Companion.SubMediaLauncher
import com.skyd.podaura.ui.screen.playlist.medialist.PlaylistMediaListRoute
import com.skyd.podaura.ui.screen.playlist.medialist.PlaylistMediaListRoute.Companion.PlaylistMediaListLauncher
import com.skyd.podaura.ui.screen.read.ReadRoute
import com.skyd.podaura.ui.screen.read.ReadRoute.Companion.ReadLauncher
import com.skyd.podaura.ui.screen.search.SearchRoute
import com.skyd.podaura.ui.screen.search.SearchRoute.Article.Companion.SearchArticleLauncher
import com.skyd.podaura.ui.screen.search.SearchRoute.Feed.SearchFeedLauncher
import com.skyd.podaura.ui.screen.settings.SettingsRoute
import com.skyd.podaura.ui.screen.settings.SettingsScreen
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
import com.skyd.podaura.ui.screen.settings.data.importexport.importopml.ImportOpmlDeepLinkRoute
import com.skyd.podaura.ui.screen.settings.data.importexport.importopml.ImportOpmlDeepLinkRoute.Companion.ImportOpmlDeepLinkLauncher
import com.skyd.podaura.ui.screen.settings.data.importexport.importopml.ImportOpmlRoute
import com.skyd.podaura.ui.screen.settings.data.importexport.importopml.ImportOpmlRoute.Companion.ImportOpmlLauncher
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
import com.skyd.podaura.ui.theme.PodAuraTheme
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.storage_permission_request_screen_first_tip
import podaura.shared.generated.resources.storage_permission_request_screen_rationale
import podaura.shared.generated.resources.storage_permission_request_screen_request_permission
import podaura.shared.generated.resources.storage_permission_request_screen_title


internal val deepLinkPatterns: List<DeepLinkPattern<out NavKey>> = buildList {
    add(ArticleRoute.deepLinkPattern)
    add(ImportOpmlDeepLinkRoute.deepLinkPattern)
    add(DownloadRoute.deepLinkPattern)
    addAll(DownloadDeepLinkRoute.deepLinkPatterns)
    add(ReadRoute.deepLinkPattern)
}

@Composable
fun AppEntrance() {
    SettingsProvider {
        if (AcceptTermsPreference.current) {
            PermissionChecker(
                onMainContent = {
                    val resultStore = rememberResultStore()
                    val navBackStack = newNavBackStack(
                        base = rememberNavBackStack(
                            configuration = SavedStateConfiguration {
                                serializersModule = PodAuraSerializersModule
                            },
                            initialNavKey() ?: MainRoute
                        ),
                        parent = null,
                    )
                    CompositionLocalProvider(
                        LocalNavBackStack provides navBackStack,
                        LocalGlobalNavBackStack provides navBackStack,
                        LocalResultStore provides resultStore,
                    ) {
                        ExternalUrlListener(navBackStack = navBackStack)
                        MainNavHost()
                    }
                    var openUpdateDialog by rememberSaveable { mutableStateOf(true) }
                    if (openUpdateDialog) {
                        UpdateDialog(
                            silence = true,
                            onClosed = { openUpdateDialog = false },
                            onError = { openUpdateDialog = false },
                        )
                    }
                }
            )
        } else {
            TermsOfServiceScreen()
        }
    }
}

@Composable
fun SettingsProvider(content: @Composable () -> Unit) {
    val acceptTerms = rememberSaveable { dataStore.getOrDefault(AcceptTermsPreference) }
    val darkMode = rememberSaveable { dataStore.getOrDefault(DarkModePreference) }
    CompositionLocalProvider(
        LocalWindowSizeClass provides calculateWindowSizeClass(),
        AcceptTermsPreference.local provides dataStore.flowOf(AcceptTermsPreference)
            .collectAsState(initial = acceptTerms).value,
    ) {
        SettingsProvider(dataStore) {
            CompositionLocalProvider(
                DarkModePreference.local provides dataStore.flowOf(DarkModePreference)
                    .collectAsState(initial = darkMode).value,
            ) {
                PodAuraTheme(darkTheme = DarkModePreference.current, content)
            }
        }
    }
}

@Composable
private fun MainNavHost() {
    val navBackStack = LocalNavBackStack.current

    PodAuraNavDisplay(
        backStack = navBackStack,
        entryProvider = entryProvider {
            entry<MainRoute> { MainScreen() }
            entry<ArticleRoute> { ArticleLauncher(it) }
            entry<LicenseRoute> { LicenseScreen() }
            entry<AboutRoute> { AboutScreen() }
            entry<TermsOfServiceRoute> { TermsOfServiceScreen() }
            entry<SettingsRoute> { SettingsScreen() }
            entry<AppearanceRoute> { AppearanceScreen() }
            entry<ArticleStyleRoute> { ArticleStyleScreen() }
            entry<FeedStyleRoute> { FeedStyleScreen() }
            entry<ReadStyleRoute> { ReadStyleScreen() }
            entry<MediaStyleRoute> { MediaStyleScreen() }
            entry<ReorderGroupRoute> { ReorderGroupScreen() }
            entry<ReorderFeedRoute> { ReorderFeedLauncher(it) }
            entry<SearchStyleRoute> { SearchStyleScreen() }
            entry<CalendarRoute> { CalendarScreen() }
            entry<BehaviorRoute> { BehaviorScreen() }
            entry<AutoDeleteRoute> { AutoDeleteScreen() }
            entry<HistoryRoute> { HistoryScreen() }
            entry<ImportOpmlRoute> { ImportOpmlLauncher(it) }
            entry<ImportOpmlDeepLinkRoute> { ImportOpmlDeepLinkLauncher(it) }
            entry<ImportExportRoute> { ImportExportScreen() }
            entry<DataRoute> { DataScreen() }
            entry<PlayerConfigRoute> { PlayerConfigScreen() }
            entry<PlayerConfigAdvancedRoute> { PlayerConfigAdvancedScreen() }
            entry<RssConfigRoute> { RssConfigScreen() }
            entry<TransmissionRoute> { TransmissionScreen() }
            entry<UpdateNotificationRoute> { UpdateNotificationScreen() }
            entry<AutoDownloadRuleRoute> { AutoDownloadRuleLauncher(it) }
            entry<MuteFeedRoute> { MuteFeedScreen() }
            entry<DeleteConstraintRoute> { DeleteConstraintScreen() }
            entry<PlaylistMediaListRoute> { PlaylistMediaListLauncher(it) }
            entry<RequestHeadersRoute> { RequestHeadersLauncher(it) }
            entry<FilePickerRoute> { FilePickerLauncher(it) }
            entry<DownloadRoute> { DownloadLauncher(it) }
            entry<DownloadDeepLinkRoute> { DownloadDeepLinkLauncher(it) }
            entry<ReadRoute> { ReadLauncher(it) }
            entry<SearchRoute.Feed> { SearchFeedLauncher(it) }
            entry<SearchRoute.Article> { SearchArticleLauncher(it) }
            entry<MediaSearchRoute> { MediaSearchLauncher(it) }
            entry<SubMediaRoute> { SubMediaLauncher(it) }
            entry<HistorySearchRoute> { HistorySearchScreen() }
        }
    )
}

@Composable
internal expect fun PermissionChecker(onMainContent: @Composable () -> Unit)

@Composable
fun RequestStoragePermissionScreen(
    shouldShowRationale: Boolean,
    onPermissionRequest: () -> Unit
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(50.dp))

            Text(
                text = stringResource(Res.string.storage_permission_request_screen_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )

            Icon(
                modifier = Modifier
                    .padding(30.dp)
                    .size(110.dp),
                imageVector = Icons.Rounded.Storage,
                contentDescription = null,
            )

            val textToShow = if (shouldShowRationale) {
                // If the user has denied the permission but the rationale can be shown,
                // then gently explain why the app requires this permission
                stringResource(Res.string.storage_permission_request_screen_rationale)
            } else {
                // If it's the first time the user lands on this feature, or the user
                // doesn't want to be asked again for this permission, explain that the
                // permission is required
                stringResource(Res.string.storage_permission_request_screen_first_tip)
            }
            Text(
                text = textToShow,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )

            Spacer(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 20.dp)
            )

            Button(
                modifier = Modifier.padding(vertical = 30.dp),
                onClick = onPermissionRequest,
            ) {
                Text(stringResource(Res.string.storage_permission_request_screen_request_permission))
            }
        }
    }
}