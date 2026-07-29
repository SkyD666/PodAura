package com.skyd.podaura.ui.screen.image

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import coil3.compose.LocalPlatformContext
import com.github.panpf.zoomimage.CoilZoomAsyncImage
import com.skyd.compone.component.BackIcon
import com.skyd.compone.component.ComponeTopBar
import com.skyd.compone.component.ComponeTopBarStyle
import com.skyd.compone.component.navigation.LocalNavBackStack
import com.skyd.mvi.getDispatcher
import com.skyd.podaura.ui.component.imageRequest
import com.skyd.podaura.ui.component.rememberPodAuraImageLoader
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.image_preview_description
import podaura.shared.generated.resources.image_preview_load_failed
import podaura.shared.generated.resources.retry


@Serializable
data class ImagePreviewRoute(val image: String) : NavKey {
    companion object {
        @Composable
        fun ImagePreviewLauncher(route: ImagePreviewRoute) {
            ImagePreviewScreen(image = route.image)
        }
    }
}

@Composable
fun ImagePreviewScreen(
    image: String,
    viewModel: ImagePreviewViewModel = koinViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val navBackStack = LocalNavBackStack.current
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

    Scaffold(
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
                        BackIcon(onClick = navBackStack::removeLastOrNull)
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
                CoilZoomAsyncImage(
                    model = imageRequest,
                    contentDescription = stringResource(Res.string.image_preview_description),
                    imageLoader = imageLoader,
                    modifier = Modifier.fillMaxSize(),
                    onLoading = {
                        dispatch(ImagePreviewIntent.LoadStarted)
                    },
                    onSuccess = {
                        dispatch(ImagePreviewIntent.LoadSucceeded)
                    },
                    onError = {
                        dispatch(
                            ImagePreviewIntent.LoadFailed(
                                it.result.throwable.message
                                    ?: it.result.throwable.toString()
                            )
                        )
                    },
                    onTap = {
                        dispatch(ImagePreviewIntent.ToggleWidgets)
                    },
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
