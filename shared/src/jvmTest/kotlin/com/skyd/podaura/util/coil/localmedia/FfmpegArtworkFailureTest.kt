package com.skyd.podaura.util.coil.localmedia

import kotlinx.coroutines.CancellationException
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class FfmpegArtworkFailureTest {
    @Test
    fun unavailableLibrariesAndUnsupportedAbiReturnNullForFrameFallback() {
        for (failure in listOf(NoSuchElementException("Missing library"), UnsatisfiedLinkError("Missing symbol"))) {
            assertNull(getFfmpegArtworkData("video.mkv") { throw failure })
        }
        val unsupported = Proxy.newProxyInstance(
            ArtworkAvformat::class.java.classLoader,
            arrayOf(ArtworkAvformat::class.java),
        ) { _, method, _ ->
            check(method.name == "avformat_version") { "Unsupported ABI must not access native structures" }
            63 shl 16
        } as ArtworkAvformat
        assertNull(getFfmpegArtworkData("video.mkv") { unsupported })
    }

    @Test
    fun cancellationAndFatalErrorsAreNotConvertedIntoFallback() {
        assertFailsWith<CancellationException> {
            getFfmpegArtworkData("video.mkv") { throw CancellationException() }
        }
        assertFailsWith<OutOfMemoryError> {
            getFfmpegArtworkData("video.mkv") { throw OutOfMemoryError() }
        }
    }
}
