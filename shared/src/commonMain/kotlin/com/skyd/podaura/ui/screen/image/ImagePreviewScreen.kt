package com.skyd.podaura.ui.screen.image

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import coil3.compose.LocalPlatformContext
import com.github.panpf.zoomimage.CoilZoomAsyncImage
import com.github.panpf.zoomimage.rememberCoilZoomState
import com.github.panpf.zoomimage.zoom.GestureType
import com.skyd.compone.component.BackIcon
import com.skyd.compone.component.ComponeTopBar
import com.skyd.compone.component.ComponeTopBarStyle
import com.skyd.compone.component.dialog.WaitingDialog
import com.skyd.compone.component.navigation.LocalNavBackStack
import com.skyd.fundation.util.isJvm
import com.skyd.fundation.util.isPhone
import com.skyd.fundation.util.platform
import com.skyd.mvi.MviEventListener
import com.skyd.mvi.getDispatcher
import com.skyd.podaura.ext.isNetworkUrl
import com.skyd.podaura.ext.onRightClickIfSupported
import com.skyd.podaura.ext.safeOpenUri
import com.skyd.podaura.ui.component.AnimatedDismissModalBottomSheet
import com.skyd.podaura.ui.component.imageRequest
import com.skyd.podaura.ui.component.rememberPodAuraImageLoader
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.copy
import podaura.shared.generated.resources.image_preview_description
import podaura.shared.generated.resources.image_preview_load_failed
import podaura.shared.generated.resources.read_screen_download_image
import podaura.shared.generated.resources.read_screen_open_image_in_browser
import podaura.shared.generated.resources.retry
import podaura.shared.generated.resources.share

@Serializable
data class ImagePreviewRoute(val image: String, val title: String? = null) : NavKey {
    companion object {
        @Composable
        fun ImagePreviewLauncher(route: ImagePreviewRoute) {
            val navBackStack = LocalNavBackStack.current
            ImagePreviewScreen(
                image = route.image,
                title = route.title,
                onBack = navBackStack::removeLastOrNull,
            )
        }
    }
}

@Composable
fun ImagePreviewScreen(
    image: String,
    title: String? = null,
    onBack: () -> Unit,
    viewModel: ImagePreviewViewModel = koinViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val uiState by viewModel.viewState.collectAsStateWithLifecycle()
    val dispatch = viewModel.getDispatcher(
        image,
        startWith = ImagePreviewIntent.LoadStarted,
    )
    val imageLoader = rememberPodAuraImageLoader()
    val platformContext = LocalPlatformContext.current
    val imageRequest = remember(image, uiState.retryVersion, platformContext) {
        imageRequest(model = image, context = platformContext)
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboard = LocalClipboard.current
    var showImageActions by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            AnimatedVisibility(
                visible = uiState.showWidgets,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                ComponeTopBar(
                    style = ComponeTopBarStyle.Small,
                    scrollBehavior = scrollBehavior,
                    title = { },
                    navigationIcon = {
                        BackIcon(onClick = onBack)
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black.copy(alpha = 0.45f),
                        navigationIconContentColor = Color.White,
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White,
                    ),
                )
            }
        },
        containerColor = Color.Black,
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            key(image, uiState.retryVersion) {
                val zoomState = rememberCoilZoomState()
                if (!platform.isPhone) {
                    zoomState.zoomable.setDisabledGestureTypes(
                        zoomState.zoomable.disabledGestureTypes or
                                GestureType.MOUSE_WHEEL_SCALE or
                                GestureType.DOUBLE_TAP_SCALE
                    )
                }
                val desktopGestureState = remember(zoomState.zoomable) {
                    DesktopImageGestureState(zoomState.zoomable)
                }
                DesktopMagnificationEffect(desktopGestureState)

                CoilZoomAsyncImage(
                    model = imageRequest,
                    contentDescription = stringResource(Res.string.image_preview_description),
                    imageLoader = imageLoader,
                    zoomState = zoomState,
                    modifier = Modifier
                        .fillMaxSize()
                        .desktopImageGestures(desktopGestureState, platform)
                        .onRightClickIfSupported { showImageActions = true },
                    onLoading = {
                        dispatch(ImagePreviewIntent.LoadStarted)
                    },
                    onSuccess = {
                        dispatch(ImagePreviewIntent.LoadSucceeded)
                    },
                    onError = { error ->
                        val throwable = error.result.throwable
                        dispatch(
                            ImagePreviewIntent.LoadFailed(
                                throwable.message ?: throwable.toString()
                            )
                        )
                    },
                    onTap = {
                        dispatch(ImagePreviewIntent.ToggleWidgets)
                    },
                    onLongPress = { showImageActions = true },
                )
            }

            when (val loadState = uiState.loadState) {
                ImagePreviewLoadState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(40.dp),
                        color = Color.White,
                    )
                }

                ImagePreviewLoadState.Success -> Unit

                is ImagePreviewLoadState.Failed -> {
                    ImageLoadFailed(
                        message = loadState.message,
                        onRetry = { dispatch(ImagePreviewIntent.Retry) },
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        }

        if (showImageActions) {
            ImageActionsBottomSheet(
                image = image,
                onDismissRequest = { showImageActions = false },
                onDownload = {
                    dispatch(ImagePreviewIntent.DownloadImage(image = image, title = title))
                },
                onShare = { dispatch(ImagePreviewIntent.ShareImage(image)) },
                onCopy = {
                    dispatch(ImagePreviewIntent.CopyImage(image = image, clipboard = clipboard))
                },
            )
        }

        MviEventListener(viewModel.singleEvent) { event ->
            when (event) {
                is ImagePreviewEvent.ImageOperationFailed ->
                    snackbarHostState.showSnackbar(event.message)
            }
        }

        WaitingDialog(visible = uiState.loadingDialog)
    }
}

@Composable
private fun ImageActionsBottomSheet(
    image: String,
    onDismissRequest: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onCopy: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    AnimatedDismissModalBottomSheet(onDismissRequest = onDismissRequest) { animateToDismiss ->
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp),
        ) {
            ImageActionItem(
                icon = Icons.Outlined.Download,
                title = stringResource(Res.string.read_screen_download_image),
                onClick = {
                    onDownload()
                    animateToDismiss()
                },
            )
            if (!platform.isJvm) {
                ImageActionItem(
                    icon = Icons.Outlined.Share,
                    title = stringResource(Res.string.share),
                    onClick = {
                        onShare()
                        animateToDismiss()
                    },
                )
            }
            ImageActionItem(
                icon = Icons.Outlined.ContentCopy,
                title = stringResource(Res.string.copy),
                onClick = {
                    onCopy()
                    animateToDismiss()
                },
            )
            if (canOpenImageInBrowser(image)) {
                ImageActionItem(
                    icon = Icons.Outlined.Public,
                    title = stringResource(Res.string.read_screen_open_image_in_browser),
                    onClick = {
                        uriHandler.safeOpenUri(image)
                        animateToDismiss()
                    },
                )
            }
        }
    }
}

internal fun canOpenImageInBrowser(image: String): Boolean = image.isNetworkUrl()

@Composable
private fun ImageActionItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null)
        Spacer(modifier = Modifier.width(20.dp))
        Text(text = title)
    }
}

@Composable
private fun ImageLoadFailed(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.BrokenImage,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = Color.White,
        )
        Text(
            text = stringResource(Res.string.image_preview_load_failed),
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        if (message.isNotBlank()) {
            Text(
                text = message,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Button(onClick = onRetry) {
            Text(text = stringResource(Res.string.retry))
        }
    }
}
