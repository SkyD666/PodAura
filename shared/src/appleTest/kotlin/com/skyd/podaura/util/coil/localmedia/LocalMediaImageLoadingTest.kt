package com.skyd.podaura.util.coil.localmedia

import coil3.ImageLoader
import coil3.BitmapImage
import coil3.PlatformContext
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.Options
import coil3.request.SuccessResult
import coil3.decode.DataSource
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.ForwardingFileSystem
import okio.Path
import okio.Source
import platform.Foundation.NSFileManager
import platform.Foundation.NSData
import platform.Foundation.create
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.posix.truncate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LocalMediaImageLoadingTest {
    @Test
    fun realSixGigabyteMkvReturnsTheTenPercentFrameAndCachesIt() = runBlocking {
        val path = NSTemporaryDirectory() + NSUUID().UUIDString + " 视频.MKV"
        val files = NSFileManager.defaultManager
        val fixture = videoThumbnailFixture
        fixture.usePinned {
            assertTrue(files.createFileAtPath(path, NSData.create(it.addressOf(0), fixture.size.toULong()), null))
        }
        assertEquals(0, truncate(path, 6L * 1024L * 1024L * 1024L))
        val context = PlatformContext.INSTANCE
        fun loader() = ImageLoader.Builder(context).components { addLocalMediaComponents() }.build()
        val firstLoader = loader()
        val secondLoader = loader()
        try {
            val fileUrl = assertNotNull(NSURL.fileURLWithPath(path).absoluteString)
            for (model in listOf(path, fileUrl, LocalMedia(path))) {
                val request = ImageRequest.Builder(context).data(model).build()
                val firstResult = firstLoader.execute(request)
                val first = assertIs<SuccessResult>(firstResult, (firstResult as? ErrorResult)?.throwable?.stackTraceToString())
                val bitmap = assertIs<BitmapImage>(first.image).bitmap
                assertTrue(bitmap.width <= 320 && bitmap.height <= 320)
                val color = bitmap.getColor(bitmap.width / 2, bitmap.height / 2)
                assertTrue((color shr 8 and 255) > 200 && (color shr 16 and 255) < 50, "Expected green at 10%, got $color")
                assertEquals(DataSource.MEMORY_CACHE, assertIs<SuccessResult>(firstLoader.execute(request)).dataSource)
                val cacheKey = assertNotNull(first.diskCacheKey)
                assertNotNull(firstLoader.diskCache?.openSnapshot(cacheKey)).close()
                val cached = assertIs<SuccessResult>(secondLoader.execute(request))
                assertEquals(DataSource.DISK, cached.dataSource)
                assertEquals(cacheKey, cached.diskCacheKey)
            }
        } finally {
            firstLoader.shutdown()
            secondLoader.shutdown()
            files.removeItemAtPath(path, null)
        }
    }

    @Test
    fun sixGigabyteVideoUsesArtworkExtractionAndNeverReadsTheVideoAsAnImage() = runBlocking {
        val path = NSTemporaryDirectory() + NSUUID().UUIDString + " 视频.MKV"
        val files = NSFileManager.defaultManager
        assertTrue(files.createFileAtPath(path, null, null))
        assertEquals(0, truncate(path, 6L * 1024L * 1024L * 1024L))
        val context = PlatformContext.INSTANCE
        val loader = ImageLoader.Builder(context).components { addLocalMediaComponents() }.build()
        val imageFileSystem = object : ForwardingFileSystem(FileSystem.SYSTEM) {
            override fun source(file: Path): Source = error("Video reached the image file reader: $file")
        }
        try {
            val fileUrl = assertNotNull(NSURL.fileURLWithPath(path).absoluteString)
            for (model in listOf(path, fileUrl, LocalMedia(path))) {
                val result = assertIs<ErrorResult>(loader.execute(
                    ImageRequest.Builder(context).data(model).fileSystem(imageFileSystem).build()
                ))
                assertIs<LocalMediaArtworkNotFoundException>(result.throwable)
            }
            // Ordinary image paths and network URLs keep their existing fetchers.
            val options = Options(context)
            for (model in listOf("/tmp/cover.png", "https://example.com/video.mkv")) {
                val mapped = loader.components.map(model, options)
                assertFalse(loader.components.newFetcher(mapped, options, loader)?.first is LocalMediaFetcher)
            }
        } finally {
            loader.shutdown()
            files.removeItemAtPath(path, null)
        }
    }
}
