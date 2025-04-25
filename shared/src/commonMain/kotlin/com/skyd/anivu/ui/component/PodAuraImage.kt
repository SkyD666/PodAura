package com.skyd.anivu.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.skyd.anivu.di.get
import io.ktor.client.HttpClient
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.getDrawableResourceBytes
import org.jetbrains.compose.resources.rememberResourceEnvironment
import org.koin.core.qualifier.named


fun imageRequest(model: Any?, context: PlatformContext) = ImageRequest.Builder(context)
    .diskCachePolicy(CachePolicy.ENABLED)
    .memoryCachePolicy(CachePolicy.ENABLED)
    .data(model)
    .crossfade(true)
    .build()

@Composable
fun convertDrawableResource(model: Any?): Any? {
    var currentModel by remember(model) {
        mutableStateOf(if (model is DrawableResource) byteArrayOf() else model)
    }
    if (model is DrawableResource) {
        val environment = rememberResourceEnvironment()
        LaunchedEffect(model) {
            currentModel = getDrawableResourceBytes(environment = environment, resource = model)
        }
    }
    return currentModel
}

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
    var currentModel = convertDrawableResource(model)
    AsyncImage(
        model = if (currentModel is ImageRequest) {
            currentModel
        } else {
            val context = LocalPlatformContext.current
            remember(currentModel) { imageRequest(currentModel, context) }
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