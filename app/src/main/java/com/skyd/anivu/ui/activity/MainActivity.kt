package com.skyd.anivu.ui.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.util.Consumer
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.skyd.anivu.R
import com.skyd.anivu.base.BaseComposeActivity
import com.skyd.anivu.config.Const
import com.skyd.anivu.ext.listType
import com.skyd.anivu.ext.safeLaunch
import com.skyd.anivu.ext.serializableType
import com.skyd.anivu.ext.type
import com.skyd.anivu.model.bean.MediaBean
import com.skyd.anivu.model.preference.data.medialib.MediaLibLocationPreference
import com.skyd.anivu.ui.local.LocalNavController
import com.skyd.anivu.ui.screen.MainRoute
import com.skyd.anivu.ui.screen.MainScreen
import com.skyd.anivu.ui.screen.about.AboutRoute
import com.skyd.anivu.ui.screen.about.AboutScreen
import com.skyd.anivu.ui.screen.about.license.LicenseRoute
import com.skyd.anivu.ui.screen.about.license.LicenseScreen
import com.skyd.anivu.ui.screen.about.update.UpdateDialog
import com.skyd.anivu.ui.screen.article.ArticleRoute
import com.skyd.anivu.ui.screen.article.ArticleScreen
import com.skyd.anivu.ui.screen.download.DownloadDeepLinkRoute
import com.skyd.anivu.ui.screen.download.DownloadRoute
import com.skyd.anivu.ui.screen.download.DownloadScreen
import com.skyd.anivu.ui.screen.feed.mute.MuteFeedRoute
import com.skyd.anivu.ui.screen.feed.mute.MuteFeedScreen
import com.skyd.anivu.ui.screen.feed.reorder.ReorderGroupRoute
import com.skyd.anivu.ui.screen.feed.reorder.ReorderGroupScreen
import com.skyd.anivu.ui.screen.feed.requestheaders.RequestHeadersRoute
import com.skyd.anivu.ui.screen.feed.requestheaders.RequestHeadersScreen
import com.skyd.anivu.ui.screen.filepicker.FilePickerRoute
import com.skyd.anivu.ui.screen.filepicker.FilePickerScreen
import com.skyd.anivu.ui.screen.history.HistoryRoute
import com.skyd.anivu.ui.screen.history.HistoryScreen
import com.skyd.anivu.ui.screen.media.search.MediaSearchRoute
import com.skyd.anivu.ui.screen.media.search.MediaSearchScreen
import com.skyd.anivu.ui.screen.media.sub.SubMediaRoute
import com.skyd.anivu.ui.screen.media.sub.SubMediaScreenRoute
import com.skyd.anivu.ui.screen.playlist.medialist.PlaylistMediaListRoute
import com.skyd.anivu.ui.screen.playlist.medialist.PlaylistMediaListScreen
import com.skyd.anivu.ui.screen.read.ReadRoute
import com.skyd.anivu.ui.screen.read.ReadScreen
import com.skyd.anivu.ui.screen.search.SearchRoute
import com.skyd.anivu.ui.screen.search.SearchScreen
import com.skyd.anivu.ui.screen.settings.SettingsRoute
import com.skyd.anivu.ui.screen.settings.SettingsScreen
import com.skyd.anivu.ui.screen.settings.appearance.AppearanceRoute
import com.skyd.anivu.ui.screen.settings.appearance.AppearanceScreen
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
import com.skyd.anivu.ui.screen.settings.behavior.BehaviorScreen
import com.skyd.anivu.ui.screen.settings.data.DataRoute
import com.skyd.anivu.ui.screen.settings.data.DataScreen
import com.skyd.anivu.ui.screen.settings.data.autodelete.AutoDeleteRoute
import com.skyd.anivu.ui.screen.settings.data.autodelete.AutoDeleteScreen
import com.skyd.anivu.ui.screen.settings.data.deleteconstraint.DeleteConstraintRoute
import com.skyd.anivu.ui.screen.settings.data.deleteconstraint.DeleteConstraintScreen
import com.skyd.anivu.ui.screen.settings.data.importexport.ImportExportRoute
import com.skyd.anivu.ui.screen.settings.data.importexport.ImportExportScreen
import com.skyd.anivu.ui.screen.settings.data.importexport.exportopml.ExportOpmlRoute
import com.skyd.anivu.ui.screen.settings.data.importexport.exportopml.ExportOpmlScreen
import com.skyd.anivu.ui.screen.settings.data.importexport.importopml.ImportOpmlDeepLinkRoute
import com.skyd.anivu.ui.screen.settings.data.importexport.importopml.ImportOpmlRoute
import com.skyd.anivu.ui.screen.settings.data.importexport.importopml.ImportOpmlScreen
import com.skyd.anivu.ui.screen.settings.playerconfig.PlayerConfigRoute
import com.skyd.anivu.ui.screen.settings.playerconfig.PlayerConfigScreen
import com.skyd.anivu.ui.screen.settings.playerconfig.advanced.PlayerConfigAdvancedRoute
import com.skyd.anivu.ui.screen.settings.playerconfig.advanced.PlayerConfigAdvancedScreen
import com.skyd.anivu.ui.screen.settings.rssconfig.RssConfigRoute
import com.skyd.anivu.ui.screen.settings.rssconfig.RssConfigScreen
import com.skyd.anivu.ui.screen.settings.rssconfig.updatenotification.UpdateNotificationRoute
import com.skyd.anivu.ui.screen.settings.rssconfig.updatenotification.UpdateNotificationScreen
import com.skyd.anivu.ui.screen.settings.transmission.TransmissionRoute
import com.skyd.anivu.ui.screen.settings.transmission.TransmissionScreen
import com.skyd.anivu.ui.screen.settings.transmission.proxy.ProxyRoute
import com.skyd.anivu.ui.screen.settings.transmission.proxy.ProxyScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlin.reflect.typeOf

@AndroidEntryPoint
class MainActivity : BaseComposeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContentBase {
            CompositionLocalProvider(
                LocalNavController provides rememberNavController(),
            ) {
                MainContent(onHandleIntent = { IntentHandler() })
            }
        }
    }

    @Composable
    private fun IntentHandler() {
        val navController = LocalNavController.current
        DisposableEffect(navController) {
            val listener = Consumer<Intent> { newIntent ->
                navController.handleDeepLink(newIntent)
            }
            addOnNewIntentListener(listener)
            onDispose { removeOnNewIntentListener(listener) }
        }
    }
}

@Composable
private fun MainNavHost() {
    val navController = LocalNavController.current

    NavHost(
        modifier = Modifier.background(MaterialTheme.colorScheme.background),
        navController = navController,
        startDestination = MainRoute,
        enterTransition = {
            fadeIn(animationSpec = tween(220, delayMillis = 30)) + scaleIn(
                animationSpec = tween(220, delayMillis = 30),
                initialScale = 0.92f,
            )
        },
        exitTransition = { fadeOut(animationSpec = tween(90)) },
        popEnterTransition = {
            fadeIn(animationSpec = tween(220)) + scaleIn(
                animationSpec = tween(220),
                initialScale = 0.92f,
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(220)) + scaleOut(
                animationSpec = tween(220),
                targetScale = 0.92f,
            )
        },
    ) {
        composable<MainRoute> { MainScreen() }
        composable<ArticleRoute>(
            typeMap = mapOf(typeOf<List<String>>() to listType<String>()),
            deepLinks = listOf(navDeepLink<ArticleRoute>(basePath = ArticleRoute.BASE_PATH)),
        ) {
            val route = it.toRoute<ArticleRoute>()
            ArticleScreen(
                feedUrls = route.feedUrls,
                groupIds = route.groupIds,
                articleIds = route.articleIds,
            )
        }
        composable<LicenseRoute> { LicenseScreen() }
        composable<AboutRoute> { AboutScreen() }
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
        composable<ExportOpmlRoute> { ExportOpmlScreen() }
        composable<ImportOpmlRoute> { ImportOpmlScreen(opmlUrl = it.toRoute<ImportOpmlRoute>().opmlUrl) }
        composable<ImportOpmlDeepLinkRoute>(
            deepLinks = listOf("text/xml", "application/xml", "text/x-opml").map { type ->
                navDeepLink {
                    action = Intent.ACTION_SEND
                    mimeType = type
                }
            },
        ) {
            val intent = it.arguments?.getParcelable<Intent>(NavController.KEY_DEEP_LINK_INTENT)
            ImportOpmlScreen(opmlUrl = intent?.clipData?.takeIf { it.itemCount > 0 }
                ?.getItemAt(0)?.uri?.toString())
        }
        composable<ImportExportRoute> { ImportExportScreen() }
        composable<DataRoute> { DataScreen() }
        composable<PlayerConfigRoute> { PlayerConfigScreen() }
        composable<PlayerConfigAdvancedRoute> { PlayerConfigAdvancedScreen() }
        composable<RssConfigRoute> { RssConfigScreen() }
        composable<ProxyRoute> { ProxyScreen() }
        composable<TransmissionRoute> { TransmissionScreen() }
        composable<UpdateNotificationRoute> { UpdateNotificationScreen() }
        composable<MuteFeedRoute> { MuteFeedScreen() }
        composable<DeleteConstraintRoute> { DeleteConstraintScreen() }
        composable<PlaylistMediaListRoute> {
            PlaylistMediaListScreen(playlistId = it.toRoute<PlaylistMediaListRoute>().playlistId)
        }
        composable<RequestHeadersRoute> {
            RequestHeadersScreen(feedUrl = it.toRoute<RequestHeadersRoute>().feedUrl)
        }
        composable<FilePickerRoute> {
            val filePickerRoute = it.toRoute<FilePickerRoute>()
            FilePickerScreen(
                path = filePickerRoute.path.takeIf {
                    filePickerRoute.path != MediaLibLocationPreference.default
                } ?: Const.INTERNAL_STORAGE,
                pickFolder = filePickerRoute.pickFolder,
                extensionName = filePickerRoute.extensionName,
                id = filePickerRoute.id,
            )
        }
        composable<DownloadRoute>(
            deepLinks = listOf(navDeepLink<DownloadRoute>(basePath = DownloadRoute.BASE_PATH)),
        ) {
            val route = it.toRoute<DownloadRoute>()
            DownloadScreen(downloadLink = route.downloadLink, mimetype = route.mimetype)
        }
        composable<DownloadDeepLinkRoute>(
            deepLinks = listOf("magnet:.*", "http://.*", "https://.*", "file://.*").map {
                navDeepLink {
                    action = Intent.ACTION_VIEW
                    uriPattern = it
                }
            },
        ) {
            val intent = it.arguments?.getParcelable<Intent>(NavController.KEY_DEEP_LINK_INTENT)
            DownloadScreen(downloadLink = intent?.data?.toString(), mimetype = intent?.data?.type)
        }
        composable<ReadRoute>(
            deepLinks = listOf(navDeepLink<ReadRoute>(basePath = ReadRoute.BASE_PATH)),
        ) {
            ReadScreen(articleId = it.toRoute<ReadRoute>().articleId)
        }
        composable<SearchRoute.Feed>(
            typeMap = mapOf(typeOf<SearchRoute.Feed>() to serializableType<SearchRoute.Feed>()),
        ) {
            SearchScreen(searchRoute = it.toRoute<SearchRoute.Feed>())
        }
        composable<SearchRoute.Article>(
            typeMap = mapOf(typeOf<SearchRoute.Article>() to serializableType<SearchRoute.Article>())
        ) {
            SearchScreen(searchRoute = it.toRoute<SearchRoute.Article>())
        }
        composable<MediaSearchRoute> { MediaSearchScreen(path = it.toRoute<MediaSearchRoute>().path) }
        composable<SubMediaRoute>(
            typeMap = mapOf(typeOf<MediaBean>() to serializableType<MediaBean>())
        ) {
            SubMediaScreenRoute(media = it.toRoute<SubMediaRoute>().media)
        }
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
                text = stringResource(R.string.storage_permission_request_screen_title),
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
                stringResource(R.string.storage_permission_request_screen_rationale)
            } else {
                // If it's the first time the user lands on this feature, or the user
                // doesn't want to be asked again for this permission, explain that the
                // permission is required
                stringResource(R.string.storage_permission_request_screen_first_tip)
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
                Text(stringResource(R.string.storage_permission_request_screen_request_permission))
            }
        }
    }
}