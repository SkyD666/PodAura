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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.core.os.BundleCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.util.Consumer
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.skyd.anivu.R
import com.skyd.anivu.base.BaseComposeActivity
import com.skyd.anivu.ext.toDecodedUrl
import com.skyd.anivu.ext.type
import com.skyd.anivu.model.bean.MediaBean
import com.skyd.anivu.ui.activity.intenthandler.ImportOpmlIntentHandler
import com.skyd.anivu.ui.activity.intenthandler.OpenDownloadIntentHandler
import com.skyd.anivu.ui.activity.intenthandler.UrlDownloadIntentHandler
import com.skyd.anivu.ui.local.LocalNavController
import com.skyd.anivu.ui.screen.MAIN_SCREEN_ROUTE
import com.skyd.anivu.ui.screen.MainScreen
import com.skyd.anivu.ui.screen.about.ABOUT_SCREEN_ROUTE
import com.skyd.anivu.ui.screen.about.AboutScreen
import com.skyd.anivu.ui.screen.about.license.LICENSE_SCREEN_ROUTE
import com.skyd.anivu.ui.screen.about.license.LicenseScreen
import com.skyd.anivu.ui.screen.about.update.UpdateDialog
import com.skyd.anivu.ui.screen.article.ARTICLE_IDS_JSON_KEY
import com.skyd.anivu.ui.screen.article.ARTICLE_IDS_KEY
import com.skyd.anivu.ui.screen.article.ARTICLE_SCREEN_DEEP_LINK
import com.skyd.anivu.ui.screen.article.ARTICLE_SCREEN_ROUTE
import com.skyd.anivu.ui.screen.article.ArticleScreen
import com.skyd.anivu.ui.screen.article.FEED_URLS_JSON_KEY
import com.skyd.anivu.ui.screen.article.FEED_URLS_KEY
import com.skyd.anivu.ui.screen.download.DOWNLOAD_LINK_KEY
import com.skyd.anivu.ui.screen.download.DOWNLOAD_SCREEN_DEEP_LINK_DATA
import com.skyd.anivu.ui.screen.download.DOWNLOAD_SCREEN_ROUTE
import com.skyd.anivu.ui.screen.download.DownloadScreen
import com.skyd.anivu.ui.screen.download.openDownloadScreen
import com.skyd.anivu.ui.screen.feed.mute.MUTE_FEED_SCREEN_ROUTE
import com.skyd.anivu.ui.screen.feed.mute.MuteFeedScreen
import com.skyd.anivu.ui.screen.feed.reorder.REORDER_GROUP_SCREEN_ROUTE
import com.skyd.anivu.ui.screen.feed.reorder.ReorderGroupScreen
import com.skyd.anivu.ui.screen.feed.requestheaders.FEED_URL_KEY
import com.skyd.anivu.ui.screen.feed.requestheaders.REQUEST_HEADERS_SCREEN_ROUTE
import com.skyd.anivu.ui.screen.feed.requestheaders.RequestHeadersScreen
import com.skyd.anivu.ui.screen.filepicker.EXTENSION_NAME_KEY
import com.skyd.anivu.ui.screen.filepicker.FILE_PICKER_ID_KEY
import com.skyd.anivu.ui.screen.filepicker.FILE_PICKER_SCREEN_ROUTE
import com.skyd.anivu.ui.screen.filepicker.FilePickerScreen
import com.skyd.anivu.ui.screen.filepicker.PATH_KEY
import com.skyd.anivu.ui.screen.filepicker.PICK_FOLDER_KEY
import com.skyd.anivu.ui.screen.history.HISTORY_SCREEN_ROUTE
import com.skyd.anivu.ui.screen.history.HistoryScreen
import com.skyd.anivu.ui.screen.media.sub.SUB_MEDIA_SCREEN_MEDIA_KEY
import com.skyd.anivu.ui.screen.media.sub.SUB_MEDIA_SCREEN_ROUTE
import com.skyd.anivu.ui.screen.media.sub.SubMediaScreenRoute
import com.skyd.anivu.ui.screen.read.ARTICLE_ID_KEY
import com.skyd.anivu.ui.screen.read.READ_SCREEN_ROUTE
import com.skyd.anivu.ui.screen.read.ReadScreen
import com.skyd.anivu.ui.screen.search.SEARCH_DOMAIN_KEY
import com.skyd.anivu.ui.screen.search.SEARCH_SCREEN_ROUTE
import com.skyd.anivu.ui.screen.search.SearchDomain
import com.skyd.anivu.ui.screen.search.SearchScreen
import com.skyd.anivu.ui.screen.settings.SETTINGS_SCREEN_ROUTE
import com.skyd.anivu.ui.screen.settings.SettingsScreen
import com.skyd.anivu.ui.screen.settings.appearance.APPEARANCE_SCREEN_ROUTE
import com.skyd.anivu.ui.screen.settings.appearance.AppearanceScreen
import com.skyd.anivu.ui.screen.settings.appearance.article.ARTICLE_STYLE_SCREEN_ROUTE
import com.skyd.anivu.ui.screen.settings.appearance.article.ArticleStyleScreen
import com.skyd.anivu.ui.screen.settings.appearance.feed.FEED_STYLE_SCREEN_ROUTE
import com.skyd.anivu.ui.screen.settings.appearance.feed.FeedStyleScreen
import com.skyd.anivu.ui.screen.settings.appearance.media.MEDIA_STYLE_SCREEN_ROUTE
import com.skyd.anivu.ui.screen.settings.appearance.media.MediaStyleScreen
import com.skyd.anivu.ui.screen.settings.appearance.read.READ_STYLE_SCREEN_ROUTE
import com.skyd.anivu.ui.screen.settings.appearance.read.ReadStyleScreen
import com.skyd.anivu.ui.screen.settings.appearance.search.SEARCH_STYLE_SCREEN_ROUTE
import com.skyd.anivu.ui.screen.settings.appearance.search.SearchStyleScreen
import com.skyd.anivu.ui.screen.settings.behavior.BEHAVIOR_SCREEN_ROUTE
import com.skyd.anivu.ui.screen.settings.behavior.BehaviorScreen
import com.skyd.anivu.ui.screen.settings.data.DATA_SCREEN_ROUTE
import com.skyd.anivu.ui.screen.settings.data.DataScreen
import com.skyd.anivu.ui.screen.settings.data.autodelete.AUTO_DELETE_SCREEN_ROUTE
import com.skyd.anivu.ui.screen.settings.data.autodelete.AutoDeleteScreen
import com.skyd.anivu.ui.screen.settings.data.importexport.IMPORT_EXPORT_SCREEN_ROUTE
import com.skyd.anivu.ui.screen.settings.data.importexport.ImportExportScreen
import com.skyd.anivu.ui.screen.settings.data.importexport.exportopml.EXPORT_OPML_SCREEN_ROUTE
import com.skyd.anivu.ui.screen.settings.data.importexport.exportopml.ExportOpmlScreen
import com.skyd.anivu.ui.screen.settings.data.importexport.importopml.IMPORT_OPML_SCREEN_ROUTE
import com.skyd.anivu.ui.screen.settings.data.importexport.importopml.ImportOpmlScreen
import com.skyd.anivu.ui.screen.settings.data.importexport.importopml.OPML_URL_KEY
import com.skyd.anivu.ui.screen.settings.data.importexport.importopml.openImportOpmlScreen
import com.skyd.anivu.ui.screen.settings.playerconfig.PLAYER_CONFIG_SCREEN_ROUTE
import com.skyd.anivu.ui.screen.settings.playerconfig.PlayerConfigScreen
import com.skyd.anivu.ui.screen.settings.playerconfig.advanced.PLAYER_CONFIG_ADVANCED_SCREEN_ROUTE
import com.skyd.anivu.ui.screen.settings.playerconfig.advanced.PlayerConfigAdvancedScreen
import com.skyd.anivu.ui.screen.settings.rssconfig.RSS_CONFIG_SCREEN_ROUTE
import com.skyd.anivu.ui.screen.settings.rssconfig.RssConfigScreen
import com.skyd.anivu.ui.screen.settings.rssconfig.updatenotification.UPDATE_NOTIFICATION_SCREEN_ROUTE
import com.skyd.anivu.ui.screen.settings.rssconfig.updatenotification.UpdateNotificationScreen
import com.skyd.anivu.ui.screen.settings.transmission.TRANSMISSION_SCREEN_ROUTE
import com.skyd.anivu.ui.screen.settings.transmission.TransmissionScreen
import com.skyd.anivu.ui.screen.settings.transmission.proxy.PROXY_SCREEN_ROUTE
import com.skyd.anivu.ui.screen.settings.transmission.proxy.ProxyScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.json.Json

@AndroidEntryPoint
class MainActivity : BaseComposeActivity() {
    private var needHandleOnCreateIntent = true

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
        if (needHandleOnCreateIntent) {
            LaunchedEffect(Unit) {
                needHandleOnCreateIntent = false
                handleIntent(intent = intent, navController = navController)
            }
        }

        DisposableEffect(navController) {
            val listener = Consumer<Intent> { newIntent ->
                handleIntent(intent = newIntent, navController = navController)
            }
            addOnNewIntentListener(listener)
            onDispose { removeOnNewIntentListener(listener) }
        }
    }

    private fun handleIntent(intent: Intent?, navController: NavController) {
        intent ?: return
        val data = intent.data

        listOf(
            UrlDownloadIntentHandler { downloadUrl ->
                openDownloadScreen(
                    navController = navController,
                    downloadLink = downloadUrl,
                    mimetype = data?.type,
                )
            },
            OpenDownloadIntentHandler {
                openDownloadScreen(navController = navController)
            },
            ImportOpmlIntentHandler { opmlUrl ->
                openImportOpmlScreen(
                    navController = navController,
                    opmlUrl = opmlUrl,
                )
            }
        ).forEach { it.handle(intent) }
    }
}

@Composable
private fun MainNavHost() {
    val navController = LocalNavController.current

    NavHost(
        modifier = Modifier.background(MaterialTheme.colorScheme.background),
        navController = navController,
        startDestination = MAIN_SCREEN_ROUTE,
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
        composable(route = MAIN_SCREEN_ROUTE) { MainScreen() }
        composable(
            route = "$ARTICLE_SCREEN_ROUTE?" +
                    "$FEED_URLS_JSON_KEY={$FEED_URLS_JSON_KEY}&" +
                    "$ARTICLE_IDS_JSON_KEY={$ARTICLE_IDS_JSON_KEY}",
            arguments = listOf(
                navArgument(FEED_URLS_JSON_KEY) { nullable = true },
                navArgument(ARTICLE_IDS_JSON_KEY) { nullable = true },
            ),
            deepLinks = listOf(navDeepLink {
                uriPattern = "$ARTICLE_SCREEN_DEEP_LINK?" +
                        "$FEED_URLS_JSON_KEY={$FEED_URLS_JSON_KEY}&" +
                        "$ARTICLE_IDS_JSON_KEY={$ARTICLE_IDS_JSON_KEY}"
            }),
        ) {
            val feedUrls: List<String> = it.arguments?.getStringArrayList(FEED_URLS_KEY)
                ?: runCatching {
                    Json.decodeFromString<List<String>>(
                        it.arguments?.getString(FEED_URLS_JSON_KEY)?.toDecodedUrl().orEmpty()
                    )
                }.getOrDefault(emptyList())
            val articleIds: List<String> = it.arguments?.getStringArrayList(ARTICLE_IDS_KEY)
                ?: runCatching {
                    Json.decodeFromString<List<String>>(
                        it.arguments?.getString(ARTICLE_IDS_JSON_KEY)?.toDecodedUrl().orEmpty()
                    )
                }.getOrDefault(emptyList())
            ArticleScreen(
                feedUrls = feedUrls,
                articleIds = articleIds,
            )
        }
        composable(route = LICENSE_SCREEN_ROUTE) { LicenseScreen() }
        composable(route = ABOUT_SCREEN_ROUTE) { AboutScreen() }
        composable(route = SETTINGS_SCREEN_ROUTE) { SettingsScreen() }
        composable(route = APPEARANCE_SCREEN_ROUTE) { AppearanceScreen() }
        composable(route = ARTICLE_STYLE_SCREEN_ROUTE) { ArticleStyleScreen() }
        composable(route = FEED_STYLE_SCREEN_ROUTE) { FeedStyleScreen() }
        composable(route = READ_STYLE_SCREEN_ROUTE) { ReadStyleScreen() }
        composable(route = MEDIA_STYLE_SCREEN_ROUTE) { MediaStyleScreen() }
        composable(route = REORDER_GROUP_SCREEN_ROUTE) { ReorderGroupScreen() }
        composable(route = SEARCH_STYLE_SCREEN_ROUTE) { SearchStyleScreen() }
        composable(route = BEHAVIOR_SCREEN_ROUTE) { BehaviorScreen() }
        composable(route = AUTO_DELETE_SCREEN_ROUTE) { AutoDeleteScreen() }
        composable(route = HISTORY_SCREEN_ROUTE) { HistoryScreen() }
        composable(route = EXPORT_OPML_SCREEN_ROUTE) { ExportOpmlScreen() }
        composable(
            route = IMPORT_OPML_SCREEN_ROUTE,
            arguments = listOf(navArgument(OPML_URL_KEY) { nullable = true }),
        ) {
            ImportOpmlScreen(opmlUrl = it.arguments?.getString(OPML_URL_KEY))
        }
        composable(route = IMPORT_EXPORT_SCREEN_ROUTE) { ImportExportScreen() }
        composable(route = DATA_SCREEN_ROUTE) { DataScreen() }
        composable(route = PLAYER_CONFIG_SCREEN_ROUTE) { PlayerConfigScreen() }
        composable(route = PLAYER_CONFIG_ADVANCED_SCREEN_ROUTE) { PlayerConfigAdvancedScreen() }
        composable(route = RSS_CONFIG_SCREEN_ROUTE) { RssConfigScreen() }
        composable(route = PROXY_SCREEN_ROUTE) { ProxyScreen() }
        composable(route = TRANSMISSION_SCREEN_ROUTE) { TransmissionScreen() }
        composable(route = UPDATE_NOTIFICATION_SCREEN_ROUTE) { UpdateNotificationScreen() }
        composable(route = MUTE_FEED_SCREEN_ROUTE) { MuteFeedScreen() }
        composable(
            route = "$REQUEST_HEADERS_SCREEN_ROUTE/{$FEED_URL_KEY}",
            arguments = listOf(
                navArgument(FEED_URL_KEY) { type = NavType.StringType },
            ),
        ) {
            val arguments = it.arguments
            if (arguments != null) {
                RequestHeadersScreen(feedUrl = arguments.getString(FEED_URL_KEY)!!.toDecodedUrl())
            }
        }
        composable(
            route = "$FILE_PICKER_SCREEN_ROUTE/{$PATH_KEY}/{$PICK_FOLDER_KEY}",
            arguments = listOf(
                navArgument(PATH_KEY) { type = NavType.StringType },
                navArgument(PICK_FOLDER_KEY) { type = NavType.BoolType },
                navArgument(EXTENSION_NAME_KEY) { nullable = true },
                navArgument(FILE_PICKER_ID_KEY) { nullable = true },
            ),
        ) {
            val arguments = it.arguments
            if (arguments != null) {
                FilePickerScreen(
                    path = arguments.getString(PATH_KEY)!!.toDecodedUrl(),
                    pickFolder = arguments.getBoolean(PICK_FOLDER_KEY),
                    extensionName = arguments.getString(EXTENSION_NAME_KEY),
                    id = arguments.getString(FILE_PICKER_ID_KEY),
                )
            }
        }
        composable(
            route = DOWNLOAD_SCREEN_ROUTE,
            arguments = listOf(navArgument(DOWNLOAD_LINK_KEY) { nullable = true }),
            deepLinks = listOf(navDeepLink {
                uriPattern = DOWNLOAD_SCREEN_DEEP_LINK_DATA.deepLink
            }),
        ) {
            DownloadScreen(downloadLink = it.arguments?.getString(DOWNLOAD_LINK_KEY))
        }
        composable(
            route = "$READ_SCREEN_ROUTE/{$ARTICLE_ID_KEY}",
            arguments = listOf(navArgument(ARTICLE_ID_KEY) { type = NavType.StringType }),
        ) {
            ReadScreen(
                articleId = it.arguments?.getString(ARTICLE_ID_KEY).orEmpty().toDecodedUrl()
            )
        }
        composable(
            route = SEARCH_SCREEN_ROUTE,
            arguments = listOf(navArgument(SEARCH_DOMAIN_KEY) {
                type = NavType.SerializableType(SearchDomain::class.java)
                defaultValue = SearchDomain.Feed
            }),
        ) {
            val searchDomain = it.arguments?.let { arguments ->
                BundleCompat.getSerializable(arguments, SEARCH_DOMAIN_KEY, SearchDomain::class.java)
            } ?: SearchDomain.Feed
            SearchScreen(searchDomain = searchDomain)
        }
        composable(route = SUB_MEDIA_SCREEN_ROUTE) {
            SubMediaScreenRoute(
                media = it.arguments?.let { arguments ->
                    BundleCompat.getSerializable(
                        arguments,
                        SUB_MEDIA_SCREEN_MEDIA_KEY,
                        MediaBean::class.java
                    )
                }
            )
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
                        permissionRequester.launch(
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