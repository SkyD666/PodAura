package com.skyd.podaura.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.layout.ContentScale
import coil3.ComponentRegistry
import coil3.EventListener
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.svg.SvgDecoder
import coil3.util.DebugLogger
import com.skyd.fundation.di.get
import io.ktor.client.HttpClient
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.koin.core.qualifier.named


fun imageRequest(model: Any?, context: PlatformContext) = ImageRequest.Builder(context)
    .diskCachePolicy(CachePolicy.ENABLED)
    .memoryCachePolicy(CachePolicy.ENABLED)
    .data(model)
    .crossfade(true)
    .build()

@OptIn(ExperimentalResourceApi::class)
@Composable
fun PodAuraImage(
    model: Any?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    imageLoader: ImageLoader = rememberPodAuraImageLoader(),
    contentScale: ContentScale = ContentScale.FillWidth,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
) {
    AsyncImage(
        model = if (model is ImageRequest) {
            model
        } else {
            val context = LocalPlatformContext.current
            remember(model) { imageRequest(model, context) }
        },
        modifier = modifier,
        contentDescription = contentDescription,
        contentScale = contentScale,
        imageLoader = imageLoader,
        alpha = alpha,
        colorFilter = colorFilter,
    )
}

@Composable
fun rememberPodAuraImageLoader(
    listener: EventListener? = null,
    components: ComponentRegistry.Builder.() -> Unit = {},
): ImageLoader {
    val context = LocalPlatformContext.current
    return remember {
        context.imageLoaderBuilder(components = components)
            .run { if (listener != null) eventListener(listener) else this }
            .logger(DebugLogger())
            .build()
    }
}

expect fun ComponentRegistry.Builder.platformComponents()

fun PlatformContext.imageLoaderBuilder(
    components: ComponentRegistry.Builder.() -> Unit = {},
): ImageLoader.Builder = ImageLoader.Builder(this).components {
    platformComponents()
    add(SvgDecoder.Factory())
    add(KtorNetworkFetcherFactory(httpClient = get<HttpClient>(named("coil"))))
    components()
}