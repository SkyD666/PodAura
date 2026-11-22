package com.skyd.podaura.ui.screen.download

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.skyd.compone.component.ComponeFloatingActionButton
import com.skyd.compone.component.ComponeScaffold
import com.skyd.compone.component.ComponeTopBar
import com.skyd.compone.component.ComponeTopBarStyle
import com.skyd.compone.ext.plus
import com.skyd.podaura.ui.component.navigation.ExternalUrlHandler
import com.skyd.podaura.ui.component.navigation.deeplink.DeepLinkPattern
import io.ktor.http.Url
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.download_screen_add_download
import podaura.shared.generated.resources.download_screen_name

@Serializable
data class DownloadRoute(
    val downloadLink: String? = null,
    val mimetype: String? = null,
) : NavKey {
    companion object {
        const val BASE_PATH = "podaura://download.screen"

        val deepLinkPattern = DeepLinkPattern(
            serializer(),
            urlPattern = Url(BASE_PATH)
        )

        @Composable
        fun DownloadLauncher(route: DownloadRoute) {
            DownloadScreen(downloadLink = route.downloadLink, mimetype = route.mimetype)
        }
    }
}

@Serializable
data class DownloadDeepLinkRoute(
    @SerialName(ExternalUrlHandler.UrlData.URL_NAME)
    val url: String? = null,
    @SerialName(ExternalUrlHandler.UrlData.MIMETYPE_NAME)
    val mimeType: String? = null,
) : NavKey {
    companion object {
        @Composable
        fun DownloadDeepLinkLauncher(route: DownloadDeepLinkRoute) {
            DownloadScreen(downloadLink = route.url, mimetype = route.mimeType)
        }
    }
}

expect val DownloadDeepLinkRoute.Companion.deepLinkPatterns: List<DeepLinkPattern<DownloadDeepLinkRoute>>

@Composable
fun DownloadScreen(
    downloadLink: String? = null,
    mimetype: String? = null,
    viewModel: DownloadViewModel = koinViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    var link by rememberSaveable(downloadLink) { mutableStateOf(downloadLink) }
    var fabHeight by remember { mutableStateOf(0.dp) }

    ComponeScaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            ComponeTopBar(
                style = ComponeTopBarStyle.CenterAligned,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(Res.string.download_screen_name)) },
            )
        },
        floatingActionButton = {
            ComponeFloatingActionButton(
                onClick = { link = "" },
                contentDescription = stringResource(Res.string.download_screen_add_download),
                onSizeWithSinglePaddingChanged = { _, height -> fabHeight = height },
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(Res.string.download_screen_add_download),
                )
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        DownloadContent(
            viewModel = viewModel,
            snackbarHostState = snackbarHostState,
            contentPadding = innerPadding + PaddingValues(bottom = fabHeight + 16.dp),
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        )
    }
    AddDownloadDialog(
        link = link,
        onLinkChange = { link = it },
        snackbarHostState = snackbarHostState,
        mimetype = mimetype,
    )
}
