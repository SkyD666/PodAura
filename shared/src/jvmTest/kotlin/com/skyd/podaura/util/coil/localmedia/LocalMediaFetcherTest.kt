package com.skyd.podaura.util.coil.localmedia

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.decode.DataSource
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.Options
import coil3.request.SuccessResult
import coil3.util.Logger
import com.skyd.podaura.ui.component.imageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.runBlocking
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.images.StandardArtwork
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path as JavaPath
import java.nio.file.attribute.FileTime
import java.util.Base64
import javax.imageio.ImageIO
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalMediaFetcherTest {

    @Test
    fun wavWithoutArtworkReturnsNull() {
        withTemporaryWav { wav ->
            assertNull(getLocalMediaThumbnailData(wav.toString()))
        }
    }

    @Test
    fun wavWithoutArtworkProducesAnExpectedErrorInsteadOfMissingFetcherError() = runBlocking {
        withTemporaryWav { wav ->
            val context = PlatformContext.INSTANCE
            val imageLoader = ImageLoader.Builder(context)
                .components { addLocalMediaComponents() }
                .build()
            val request = ImageRequest.Builder(context)
                .data(LocalMedia(wav.toString()))
                .build()

            try {
                val result = assertIs<ErrorResult>(imageLoader.execute(request))
                assertIs<LocalMediaArtworkNotFoundException>(result.throwable)
            } finally {
                imageLoader.shutdown()
            }
        }
    }

    @Test
    fun expectedMissingArtworkErrorIsNotLogged() {
        val delegate = RecordingLogger()
        val logger = LocalMediaImageLogger(delegate)
        val data = LocalMedia("/music/song.wav")

        logger.log(
            tag = "RealImageLoader",
            level = Logger.Level.Error,
            message = "Failed",
            throwable = LocalMediaArtworkNotFoundException(data),
        )
        assertEquals(0, delegate.callCount)

        logger.log(
            tag = "RealImageLoader",
            level = Logger.Level.Error,
            message = "Failed",
            throwable = IllegalStateException("Unexpected failure"),
        )
        assertEquals(1, delegate.callCount)
    }

    @Test
    fun extractsArtworkAndCachesDecodedImageInMemory() = runBlocking {
        withTemporaryWav { wav ->
            addArtwork(wav, TEST_PNG)
            assertContentEquals(TEST_PNG, getLocalMediaThumbnailData(wav.toString()))

            val context = PlatformContext.INSTANCE
            val imageLoader = ImageLoader.Builder(context)
                .components { addLocalMediaComponents() }
                .build()
            val request = ImageRequest.Builder(context)
                .data(LocalMedia(wav.toString()))
                .interceptorCoroutineContext(Dispatchers.IO)
                .build()

            try {
                val firstResult = assertIs<SuccessResult>(imageLoader.execute(request))
                assertEquals(DataSource.DISK, firstResult.dataSource)
                assertNotNull(firstResult.memoryCacheKey)

                val cachedResult = assertIs<SuccessResult>(imageLoader.execute(request))
                assertEquals(DataSource.MEMORY_CACHE, cachedResult.dataSource)
            } finally {
                imageLoader.shutdown()
            }
        }
    }

    @Test
    fun extractsArtworkFromAnEncodedFileUri() = runBlocking {
        withTemporaryWav { wav ->
            addArtwork(wav, TEST_PNG)
            val fileUri = wav.toUri().toASCIIString()
            assertTrue("%" in fileUri)
            assertContentEquals(TEST_PNG, getLocalMediaThumbnailData(fileUri))

            val context = PlatformContext.INSTANCE
            val imageLoader = ImageLoader.Builder(context)
                .components { addLocalMediaComponents() }
                .build()
            val request = ImageRequest.Builder(context)
                .data(LocalMedia(fileUri))
                .interceptorCoroutineContext(Dispatchers.IO)
                .build()

            try {
                val result = assertIs<SuccessResult>(imageLoader.execute(request))
                assertEquals(DataSource.DISK, result.dataSource)
            } finally {
                imageLoader.shutdown()
            }
        }
    }

    @Test
    fun changingArtworkAtTheSamePathInvalidatesTheMemoryCache() = runBlocking {
        withTemporaryWav { wav ->
            addArtwork(wav, TEST_PNG)
            val context = PlatformContext.INSTANCE
            val imageLoader = ImageLoader.Builder(context)
                .components { addLocalMediaComponents() }
                .build()
            val request = ImageRequest.Builder(context)
                .data(LocalMedia(wav.toString()))
                .interceptorCoroutineContext(Dispatchers.IO)
                .build()

            try {
                val firstResult = assertIs<SuccessResult>(imageLoader.execute(request))
                assertEquals(DataSource.DISK, firstResult.dataSource)
                val firstKey = assertNotNull(firstResult.memoryCacheKey)
                assertEquals(
                    DataSource.MEMORY_CACHE,
                    assertIs<SuccessResult>(imageLoader.execute(request)).dataSource,
                )

                val previousModifiedAt = Files.getLastModifiedTime(wav).toMillis()
                addArtwork(wav, UPDATED_TEST_PNG)
                Files.setLastModifiedTime(wav, FileTime.fromMillis(previousModifiedAt + 10_000))
                assertContentEquals(UPDATED_TEST_PNG, getLocalMediaThumbnailData(wav.toString()))

                val updatedResult = assertIs<SuccessResult>(imageLoader.execute(request))
                assertEquals(DataSource.DISK, updatedResult.dataSource)
                assertNotEquals(firstKey, updatedResult.memoryCacheKey)
                assertEquals(
                    DataSource.MEMORY_CACHE,
                    assertIs<SuccessResult>(imageLoader.execute(request)).dataSource,
                )
            } finally {
                imageLoader.shutdown()
            }
        }
    }

    @Test
    fun localMediaKeyUsesTheFilePath() {
        val data = LocalMedia("/music/アルバム/song.wav")

        val key = LocalMediaKeyer().key(
            data = data,
            options = Options(PlatformContext.INSTANCE),
        )

        assertEquals("local-media-thumbnail:/music/アルバム/song.wav", key)
    }

    @Test
    fun localMediaImageRequestRunsTheKeyerOnTheIoDispatcher() {
        val request = imageRequest(
            model = LocalMedia("/music/song.wav"),
            context = PlatformContext.INSTANCE,
        )

        assertEquals(Dispatchers.IO, request.interceptorCoroutineContext)
    }

    private fun addArtwork(wav: JavaPath, artworkData: ByteArray) {
        val audioFile = AudioFileIO.read(wav.toFile())
        val artwork = StandardArtwork().apply {
            binaryData = artworkData
            mimeType = "image/png"
            description = ""
            pictureType = 3
        }
        runCatching { audioFile.tag.deleteArtworkField() }
        audioFile.tag.setField(artwork)
        audioFile.commit()
    }

    private inline fun withTemporaryWav(block: (JavaPath) -> Unit) {
        val directory = Files.createTempDirectory("podaura-local-media-")
        val wav = directory.resolve("音 声.wav")
        try {
            val pcmData = ByteArray(882)
            val format = AudioFormat(44_100f, 16, 1, true, false)
            AudioInputStream(
                ByteArrayInputStream(pcmData),
                format,
                (pcmData.size / format.frameSize).toLong(),
            ).use { stream ->
                AudioSystem.write(stream, AudioFileFormat.Type.WAVE, wav.toFile())
            }
            block(wav)
        } finally {
            Files.deleteIfExists(wav)
            Files.deleteIfExists(directory)
        }
    }

    private companion object {
        val TEST_PNG: ByteArray = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk" +
                "+A8AAQUBAScY42YAAAAASUVORK5CYII=",
        )

        val UPDATED_TEST_PNG: ByteArray = ByteArrayOutputStream().use { output ->
            val image = BufferedImage(2, 1, BufferedImage.TYPE_INT_ARGB).apply {
                setRGB(0, 0, 0xFFFF0000.toInt())
                setRGB(1, 0, 0xFF0000FF.toInt())
            }
            check(ImageIO.write(image, "png", output))
            output.toByteArray()
        }
    }

    private class RecordingLogger : Logger {
        override var minLevel: Logger.Level = Logger.Level.Debug
        var callCount = 0

        override fun log(
            tag: String,
            level: Logger.Level,
            message: String?,
            throwable: Throwable?,
        ) {
            callCount++
        }
    }
}
