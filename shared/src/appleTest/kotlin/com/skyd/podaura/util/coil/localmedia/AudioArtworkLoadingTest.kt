package com.skyd.podaura.util.coil.localmedia

import coil3.BitmapImage
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.decode.DataSource
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.skyd.podaura.media.MediaTypes
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.runBlocking
import okio.Buffer
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.create
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AudioArtworkLoadingTest {
    @Test
    fun everySupportedAudioFormatLoadsFromPathsAndFileUrlsAndCachesIt() = runBlocking {
        assertEquals(MediaTypes.audioExtensions.toSet(), audioArtworkFixtures.keys)
        for ((extension, fixture) in audioArtworkFixtures.toList() + ("flac" to WITH_JPEG_COVER)) {
            withMedia(fixture, extension.uppercase()) { path ->
                val context = PlatformContext.INSTANCE
                fun loader() = ImageLoader.Builder(context).components { addLocalMediaComponents() }.build()
                val firstLoader = loader()
                val secondLoader = loader()
                try {
                    val fileUrl = assertNotNull(NSURL.fileURLWithPath(path).absoluteString)
                    for (model in listOf(path, fileUrl, LocalMedia(path))) {
                        val request = ImageRequest.Builder(context).data(model).build()
                        val result = firstLoader.execute(request)
                        val first = assertIs<SuccessResult>(result, (result as? ErrorResult)?.throwable?.stackTraceToString())
                        val bitmap = assertIs<BitmapImage>(first.image).bitmap
                        assertTrue(bitmap.width <= 320 && bitmap.height <= 320)
                        assertEquals(2.0, bitmap.width.toDouble() / bitmap.height)
                        val color = bitmap.getColor(bitmap.width / 2, bitmap.height / 2)
                        assertTrue((color shr 8 and 255) > 200 && (color shr 16 and 255) < 50, "Expected green cover, got $color")
                        assertEquals(DataSource.MEMORY_CACHE, assertIs<SuccessResult>(firstLoader.execute(request)).dataSource)
                        val cached = assertIs<SuccessResult>(secondLoader.execute(request))
                        assertEquals(DataSource.DISK, cached.dataSource)
                        assertEquals(assertNotNull(first.diskCacheKey), cached.diskCacheKey)
                    }
                } finally {
                    firstLoader.shutdown()
                    secondLoader.shutdown()
                }
            }
        }
    }

    @Test
    fun flacAndOpusArtworkIsReadByAppleWithoutTheFallback() {
        for (extension in listOf("flac", "opus")) {
            withMedia(assertNotNull(audioArtworkFixtures[extension]), extension) { path ->
                assertContentEquals(audioArtworkPng, getAppleArtworkData(NSURL.fileURLWithPath(path)))
            }
        }
    }

    @Test
    fun flacWithoutCoverReturnsExpectedArtworkError() = runBlocking {
        withMedia(WITHOUT_COVER) { path ->
            assertNull(getLocalMediaThumbnailData(path))
            val context = PlatformContext.INSTANCE
            val loader = ImageLoader.Builder(context).components { addLocalMediaComponents() }.build()
            try {
                val result = assertIs<ErrorResult>(loader.execute(
                    ImageRequest.Builder(context).data(path).build()
                ))
                assertIs<LocalMediaArtworkNotFoundException>(result.throwable)
            } finally {
                loader.shutdown()
            }
        }
    }

    @Test
    fun frontCoverIsPreferredAndOtherPicturesRemainAFallback() {
        // The real fixture's STREAMINFO ends at 42; its PICTURE block ends at 241.
        val streamInfo = WITH_COVER.copyOfRange(0, 42)
        val frontCover = WITH_COVER.copyOfRange(42, 241).apply { this[0] = 0x86.toByte() }
        val backCover = frontCover.copyOf().apply {
            this[0] = 6
            this[7] = 4 // Back cover.
            this[lastIndex] = 1 // Distinguish its image bytes from the front cover.
        }
        withMedia(streamInfo + backCover + frontCover) { path ->
            assertContentEquals(WITH_COVER.copyOfRange(98, 241), getFfmpegArtworkData(path))
        }
        backCover[0] = 0x86.toByte()
        withMedia(streamInfo + backCover) { path ->
            assertContentEquals(backCover.copyOfRange(56, backCover.size), getFfmpegArtworkData(path))
        }
    }

    @Test
    fun malformedPictureLengthsAndTruncatedBlocksReturnNull() {
        val badMimeLength = WITH_COVER.copyOf().apply { fill(0xff.toByte(), 50, 54) }
        val badImageLength = WITH_COVER.copyOf().apply { fill(0xff.toByte(), 94, 98) }
        for (fixture in listOf(badMimeLength, badImageLength, WITH_COVER.copyOf(240))) {
            withMedia(fixture) { path -> assertNull(getLocalMediaThumbnailData(path)) }
        }
    }

    @Test
    fun largePicturesAreSelectedBeforeCopyingAndOversizedPicturesAreSkipped() {
        val large = audioArtworkPng.copyOf(2 * 1024 * 1024)
        withMedia(mp3WithPictures(4 to large, 4 to large, 3 to audioArtworkPng), "mp3") { path ->
            assertContentEquals(audioArtworkPng, getFfmpegArtworkData(path))
        }
        // A picture can exceed probesize and still be a valid, bounded cover.
        withMedia(mp3WithPictures(3 to large), "mp3") { path ->
            assertContentEquals(large, getFfmpegArtworkData(path))
        }
        val oversized = audioArtworkPng.copyOf(MAX_FFMPEG_ARTWORK_BYTES + 1)
        withMedia(mp3WithPictures(3 to oversized), "mp3") { path ->
            assertNull(getFfmpegArtworkData(path))
        }
        withMedia(mp3WithPictures(3 to oversized, 4 to audioArtworkPng), "mp3") { path ->
            assertContentEquals(audioArtworkPng, getFfmpegArtworkData(path))
        }
    }

    private fun mp3WithPictures(vararg pictures: Pair<Int, ByteArray>): ByteArray {
        fun synchsafe(size: Int) = byteArrayOf(
            (size ushr 21 and 127).toByte(), (size ushr 14 and 127).toByte(),
            (size ushr 7 and 127).toByte(), (size and 127).toByte(),
        )
        val frames = Buffer()
        for ((type, picture) in pictures) {
            val payload = Buffer().writeByte(3).writeUtf8("image/png").writeByte(0)
                .writeByte(type).writeByte(0).write(picture)
            frames.writeUtf8("APIC").write(synchsafe(payload.size.toInt())).writeShort(0).writeAll(payload)
        }
        val original = audioArtworkFixtures.getValue("mp3")
        val tagSize = original.copyOfRange(6, 10).fold(0) { size, byte ->
            (size shl 7) or (byte.toInt() and 127)
        } + 10
        return Buffer().writeUtf8("ID3").writeByte(4).writeByte(0).writeByte(0)
            .write(synchsafe(frames.size.toInt())).apply { writeAll(frames) }
            .write(original, tagSize, original.size - tagSize).readByteArray()
    }

    private inline fun withMedia(fixture: ByteArray, extension: String = "FLAC", block: (String) -> Unit) {
        val path = NSTemporaryDirectory() + NSUUID().UUIDString + " 音频.$extension"
        val files = NSFileManager.defaultManager
        fixture.usePinned {
            assertTrue(files.createFileAtPath(path, NSData.create(it.addressOf(0), fixture.size.toULong()), null))
        }
        try {
            block(path)
        } finally {
            files.removeItemAtPath(path, null)
        }
    }

    private companion object {
        // One second of real FLAC audio, with a green 64x32 PNG in a front-cover PICTURE block.
        val WITH_COVER = Base64.decode(
            "ZkxhQwAAACICQAJAAAALAAALAfQA8AAAH0Ae4Bk2cWCcfWPP6JuSCtMTBgAAwwAAAAMAAAAJaW1hZ2Uv" +
                "cG5nAAAAC0FsYnVtIGNvdmVyAAAAQAAAACAAAAAYAAAAAAAAAI+JUE5HDQoaCgAAAA1JSERSAAAAQAAA" +
                "ACAIAgAAAC3/6dMAAAAJcEhZcwAAAAEAAAABAE8lxNYAAABBSURBVHic7c/BCQAgEMAwD9x/ZR3CRxCa" +
                "CdpZZ9bPtg541YDWgNaA1oDWgNaA1oDWgNaA1oDWgNaA1oDWgNaAdgEnwQF+sNk8QAAAAABJRU5ErkJg" +
                "goQAAC4NAAAATGF2ZjYyLjEyLjEwMgEAAAAVAAAAZW5jb2Rlcj1MYXZmNjIuMTIuMTAy//gkCADKAAAA" +
                "znb/+CQIAc0AAACiDv/4JAgCxAAAABaG//gkCAPDAAAAev7/+CQIBNYAAAD/k//4JAgF0QAAAJPr//gk" +
                "CAbYAAAAJ2P/+CQIB98AAABLG//4JAgI8gAAAK28//gkCAn1AAAAwcT/+CQICvwAAAB1TP/4JAgL+wAA" +
                "ABk0//gkCAzuAAAAnFn/+JQIDQMAAADDDA=="
        )
        val WITH_JPEG_COVER = Base64.decode(
            "ZkxhQwAAACICQAJAAAALAAALAfQA8AAAH0Ae4Bk2cWCcfWPP6JuSCtMTBgABLAAAAAMAAAAKaW1hZ2Uv" +
                "anBlZwAAAAtBbGJ1bSBjb3ZlcgAAAEAAAAAgAAAAGAAAAAAAAAD3/9j/4AAQSkZJRgABAgAAAQABAAD/" +
                "/gAQTGF2YzYyLjI4LjEwMgD/2wBDAAgEBAQEBAUFBQUFBQYGBgYGBgYGBgYGBgYHBwcICAgHBwcGBgcH" +
                "CAgICAkJCQgICAgJCQoKCgwMCwsODg4RERT/xABMAAEBAAAAAAAAAAAAAAAAAAAABQEBAQAAAAAAAAAA" +
                "AAAAAAAAAAcQAQAAAAAAAAAAAAAAAAAAAAARAQAAAAAAAAAAAAAAAAAAAAD/wAARCAAgAEADARIAAhIA" +
                "AxIA/9oADAMBAAIRAxEAPwCsJWIoAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAP/2YQAAC4NAAAATGF2ZjYy" +
                "LjEyLjEwMgEAAAAVAAAAZW5jb2Rlcj1MYXZmNjIuMTIuMTAy//gkCADKAAAAznb/+CQIAc0AAACiDv/4" +
                "JAgCxAAAABaG//gkCAPDAAAAev7/+CQIBNYAAAD/k//4JAgF0QAAAJPr//gkCAbYAAAAJ2P/+CQIB98A" +
                "AABLG//4JAgI8gAAAK28//gkCAn1AAAAwcT/+CQICvwAAAB1TP/4JAgL+wAAABk0//gkCAzuAAAAnFn/" +
                "+JQIDQMAAADDDA=="
        )
        val WITHOUT_COVER = Base64.decode(
            "ZkxhQwAAACICQAJAAAALAAALAfQA8AAAH0Ae4Bk2cWCcfWPP6JuSCtMThAAALg0AAABMYXZmNjIuMTIu" +
                "MTAyAQAAABUAAABlbmNvZGVyPUxhdmY2Mi4xMi4xMDL/+CQIAMoAAADOdv/4JAgBzQAAAKIO//gkCALE" +
                "AAAAFob/+CQIA8MAAAB6/v/4JAgE1gAAAP+T//gkCAXRAAAAk+v/+CQIBtgAAAAnY//4JAgH3wAAAEsb" +
                "//gkCAjyAAAArbz/+CQICfUAAADBxP/4JAgK/AAAAHVM//gkCAv7AAAAGTT/+CQIDO4AAACcWf/4lAgN" +
                "AwAAAMMM"
        )
    }
}
