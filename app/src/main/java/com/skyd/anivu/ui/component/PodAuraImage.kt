package com.skyd.anivu.ui.component

import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.ComponentRegistry
import coil3.EventListener
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.svg.SvgDecoder
import coil3.util.DebugLogger
import coil3.video.VideoFrameDecoder
import com.skyd.anivu.ext.dataStore
import com.skyd.anivu.ext.getOrDefault
import com.skyd.anivu.ext.isWifiAvailable
import com.skyd.anivu.model.preference.behavior.LoadNetImageOnWifiOnlyPreference
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException


fun imageRequest(model: Any?, context: Context) = ImageRequest.Builder(context)
    .diskCachePolicy(CachePolicy.ENABLED)
    .memoryCachePolicy(CachePolicy.ENABLED)
    .data(model)
    .crossfade(true)
    .build()

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
            val context = LocalContext.current
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

class WifiOnlyInterceptor(
    private val context: Context
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        if (context.dataStore.getOrDefault(LoadNetImageOnWifiOnlyPreference) &&
            !context.isWifiAvailable()
        ) {
            throw IOException("Not on Wi-Fi; network load denied.")
        }
        return chain.proceed(chain.request())
    }
}

@Composable
fun rememberPodAuraImageLoader(
    listener: EventListener? = null,
    components: ComponentRegistry.Builder.() -> Unit = {},
): ImageLoader {
    val context = LocalContext.current
    return remember(context) {
        context.imageLoaderBuilder()
            .components(components)
            .run { if (listener != null) eventListener(listener) else this }
            .logger(DebugLogger())
            .build()
    }
}

fun Context.imageLoaderBuilder(): ImageLoader.Builder {
    return ImageLoader.Builder(this)
        .components {
            if (Build.VERSION.SDK_INT >= 28) {
                add(AnimatedImageDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
            add(SvgDecoder.Factory())
            add(VideoFrameDecoder.Factory())
        }
        .components {
            add(OkHttpNetworkFetcherFactory(callFactory = {
                OkHttpClient.Builder()
                    .addNetworkInterceptor(Interceptor { chain ->
                        chain.proceed(chain.request()).newBuilder()
                            .header("Cache-Control", "max-age=31536000,public")
                            .build()
                    })
                    .addNetworkInterceptor(WifiOnlyInterceptor(this@imageLoaderBuilder))
                    .build()
            }))
        }
}