package com.skyd.podaura.ui.player

import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.request.SuccessResult
import coil3.toBitmap
import com.skyd.podaura.ui.component.imageLoaderBuilder
import com.skyd.podaura.ui.component.imageRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.cinterop.useContents
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path.Companion.toPath
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class IosArtworkLoadingTest {
    @Test
    fun cachedSvgAndDecodedBitmapReachNativeArtworkWithoutNetwork() = runBlocking {
        val directory = (NSTemporaryDirectory() + NSUUID().UUIDString).toPath()
        val cache = DiskCache.Builder().directory(directory).maxSizeBytes(10L * 1024 * 1024).build()
        var offline = false
        var networkRequests = 0
        val client = HttpClient(MockEngine {
            networkRequests++
            check(!offline) { "Cached artwork attempted to access the network" }
            respond(
                """<svg xmlns="http://www.w3.org/2000/svg" width="256" height="128"><rect width="256" height="128" fill="green"/></svg>""",
                headers = headersOf(HttpHeaders.ContentType, "image/svg+xml"),
            )
        })
        startKoin { modules(module { single<HttpClient>(named("coil")) { client } }) }
        val context = PlatformContext.INSTANCE
        val previewLoader = context.imageLoaderBuilder().diskCache(cache).build()
        val nativeLoader = context.imageLoaderBuilder().diskCache(cache).build()
        try {
            val url = "https://example.com/cover.svg"
            // Populate the same cache as a visible Coil thumbnail, at a different size.
            val preview = assertIs<SuccessResult>(previewLoader.execute(
                imageRequest(url, context).newBuilder().size(256, 256).build()
            ))
            assertNotNull(preview.diskCacheKey)
            previewLoader.shutdown()
            offline = true
            val image = assertNotNull(loadIosArtwork(url, nativeLoader))
            assertNotNull(image.CGImage)
            image.size.useContents {
                assertTrue(width > 0 && width <= 1024)
                assertTrue(height > 0 && height <= 1024)
                assertEquals(2.0, width / height, 0.01)
            }
            // The mpv-provided bitmap fallback uses the same pipeline too.
            assertNotNull(loadIosArtwork(preview.image.toBitmap(), nativeLoader))
            assertEquals(1, networkRequests)
        } finally {
            previewLoader.shutdown()
            nativeLoader.shutdown()
            client.close()
            cache.shutdown()
            FileSystem.SYSTEM.deleteRecursively(directory)
            stopKoin()
        }
    }
}
