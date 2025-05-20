package com.skyd.podaura.ui.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavUri
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.skyd.podaura.ext.getOrDefault
import com.skyd.podaura.ext.safeLaunch
import com.skyd.podaura.model.preference.AcceptTermsPreference
import com.skyd.podaura.model.preference.dataStore
import com.skyd.podaura.ui.component.PodAuraNavHost
import com.skyd.podaura.ui.component.navigation.ExternalUriHandler
import com.skyd.podaura.ui.local.LocalGlobalNavController
import com.skyd.podaura.ui.local.LocalNavController
import com.skyd.podaura.ui.screen.MainRoute
import com.skyd.podaura.ui.screen.MainScreen
import com.skyd.podaura.ui.screen.about.AboutRoute
import com.skyd.podaura.ui.screen.about.AboutScreen
import com.skyd.podaura.ui.screen.about.TermsOfServiceRoute
import com.skyd.podaura.ui.screen.about.TermsOfServiceScreen
import com.skyd.podaura.ui.screen.about.license.LicenseRoute
import com.skyd.podaura.ui.screen.about.license.LicenseScreen
import com.skyd.podaura.ui.screen.about.update.UpdateDialog
import com.skyd.podaura.ui.screen.article.ArticleRoute
import com.skyd.podaura.ui.screen.article.ArticleRoute.Companion.ArticleLauncher
import com.skyd.podaura.ui.screen.download.DownloadDeepLinkRoute
import com.skyd.podaura.ui.screen.download.DownloadDeepLinkRoute.DownloadDeepLinkLauncher
import com.skyd.podaura.ui.screen.download.DownloadRoute
import com.skyd.podaura.ui.screen.download.DownloadRoute.Companion.DownloadLauncher
import com.skyd.podaura.ui.screen.feed.autodl.AutoDownloadRuleRoute
import com.skyd.podaura.ui.screen.feed.autodl.AutoDownloadRuleRoute.Companion.AutoDownloadRuleLauncher
import com.skyd.podaura.ui.screen.feed.mute.MuteFeedRoute
import com.skyd.podaura.ui.screen.feed.mute.MuteFeedScreen
import com.skyd.podaura.ui.screen.feed.reorder.ReorderGroupRoute
import com.skyd.podaura.ui.screen.feed.reorder.ReorderGroupScreen
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
import com.skyd.podaura.ui.screen.settings.data.importexport.importopml.ImportOpmlDeepLinkLauncher
import com.skyd.podaura.ui.screen.settings.data.importexport.importopml.ImportOpmlDeepLinkRoute
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
import com.skyd.podaura.ui.screen.settings.transmission.proxy.ProxyRoute
import com.skyd.podaura.ui.screen.settings.transmission.proxy.ProxyScreen
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.storage_permission_request_screen_first_tip
import podaura.shared.generated.resources.storage_permission_request_screen_rationale
import podaura.shared.generated.resources.storage_permission_request_screen_request_permission
import podaura.shared.generated.resources.storage_permission_request_screen_title


class MainActivity : BaseComposeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContentBase {
            val navController = rememberNavController()
            CompositionLocalProvider(
                LocalGlobalNavController provides navController,
                LocalNavController provides navController,
            ) {
                if (AcceptTermsPreference.current ||
                    remember { dataStore }.getOrDefault(AcceptTermsPreference)
                ) {
                    MainContent(onHandleIntent = { IntentHandler() })
                } else {
                    TermsOfServiceScreen()
                }
            }
        }
    }

    @Composable
    private fun IntentHandler() {
        val navController = LocalNavController.current

        DisposableEffect(navController) {
            ExternalUriHandler.listener = { uri ->
                navController.navigate(NavUri(uri))
            }
            onDispose {
                // Removes the listener when the composable is no longer active
                ExternalUriHandler.listener = null
            }
        }
    }
}

@Composable
private fun MainNavHost() {
    val navController = LocalNavController.current

    PodAuraNavHost(
        navController = navController,
        startDestination = MainRoute,
    ) {
        composable<MainRoute> { MainScreen() }
        composable<ArticleRoute>(
            typeMap = ArticleRoute.typeMap,
            deepLinks = ArticleRoute.deepLinks,
        ) {
            ArticleLauncher(entry = it)
        }
        composable<LicenseRoute> { LicenseScreen() }
        composable<AboutRoute> { AboutScreen() }
        composable<TermsOfServiceRoute> { TermsOfServiceScreen() }
        composable<SettingsRoute> { SettingsScreen() }
        composable<AppearanceRoute> { AppearanceScreen() }
        composable<ArticleStyleRoute> { ArticleStyleScreen() }
        composable<FeedStyleRoute> { FeedStyleScreen() }
        composable<ReadStyleRoute> { ReadStyleScreen() }
        composable<MediaStyleRoute> { MediaStyleScreen() }
        composable<ReorderGroupRoute> { ReorderGroupScreen() }
        composable<SearchStyleRoute> { SearchStyleScreen() }
        composable<BehaviorRoute> { BehaviorScreen() }
        composable<AutoDeleteRoute> { AutoDeleteScreen() }
        composable<HistoryRoute> { HistoryScreen() }
        composable<ImportOpmlRoute> { ImportOpmlLauncher(it) }
        composable<ImportOpmlDeepLinkRoute>(deepLinks = ImportOpmlDeepLinkRoute.deepLinks) {
            ImportOpmlDeepLinkLauncher(it)
        }
        composable<ImportExportRoute> { ImportExportScreen() }
        composable<DataRoute> { DataScreen() }
        composable<PlayerConfigRoute> { PlayerConfigScreen() }
        composable<PlayerConfigAdvancedRoute> { PlayerConfigAdvancedScreen() }
        composable<RssConfigRoute> { RssConfigScreen() }
        composable<ProxyRoute> { ProxyScreen() }
        composable<TransmissionRoute> { TransmissionScreen() }
        composable<UpdateNotificationRoute> { UpdateNotificationScreen() }
        composable<AutoDownloadRuleRoute> { AutoDownloadRuleLauncher(it) }
        composable<MuteFeedRoute> { MuteFeedScreen() }
        composable<DeleteConstraintRoute> { DeleteConstraintScreen() }
        composable<PlaylistMediaListRoute> { PlaylistMediaListLauncher(it) }
        composable<RequestHeadersRoute> { RequestHeadersLauncher(it) }
        composable<FilePickerRoute> { FilePickerLauncher(it) }
        composable<DownloadRoute>(deepLinks = DownloadRoute.deepLinks) { DownloadLauncher(it) }
        composable<DownloadDeepLinkRoute>(deepLinks = DownloadDeepLinkRoute.deepLinks) {
            DownloadDeepLinkLauncher(it)
        }
        composable<ReadRoute>(deepLinks = ReadRoute.deepLinks) { ReadLauncher(it) }
        composable<SearchRoute.Feed>(typeMap = SearchRoute.Feed.typeMap) {
            SearchFeedLauncher(it)
        }
        composable<SearchRoute.Article>(typeMap = SearchRoute.Article.typeMap) {
            SearchArticleLauncher(it)
        }
        composable<MediaSearchRoute> { MediaSearchLauncher(it) }
        composable<SubMediaRoute>(typeMap = SubMediaRoute.typeMap) { SubMediaLauncher(it) }
        composable<HistorySearchRoute> { HistorySearchScreen() }
    }
}

@Composable
private fun MainContent(onHandleIntent: @Composable () -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        var permissionGranted by remember {
            mutableStateOf(Environment.isExternalStorageManager())
        }
        val permissionRequester = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { permissionGranted = Environment.isExternalStorageManager() }

        if (permissionGranted) {
            MainNavHost()
            onHandleIntent()
        } else {
            RequestStoragePermissionScreen(
                shouldShowRationale = false,
                onPermissionRequest = {
                    permissionGranted = Environment.isExternalStorageManager()
                    if (!permissionGranted) {
                        permissionRequester.safeLaunch(
                            Intent(ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        )
                    }
                },
            )
        }
    } else {
        val storagePermissionState = rememberMultiplePermissionsState(
            mutableListOf<String>().apply {
                add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        )
        if (storagePermissionState.allPermissionsGranted) {
            MainNavHost()
            onHandleIntent()
        } else {
            RequestStoragePermissionScreen(
                shouldShowRationale = storagePermissionState.shouldShowRationale,
                onPermissionRequest = {
                    storagePermissionState.launchMultiplePermissionRequest()
                },
            )
        }
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