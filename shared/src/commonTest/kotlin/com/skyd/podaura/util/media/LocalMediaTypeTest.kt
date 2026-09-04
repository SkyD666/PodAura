package com.skyd.podaura.util.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocalMediaTypeTest {
    @Test
    fun recognizesAudioAndVideoMimeTypes() {
        assertEquals(LocalMediaKind.Audio, detectLocalMediaKind("download", "audio/mpeg"))
        assertEquals(LocalMediaKind.Video, detectLocalMediaKind("download", "video/mp4"))
        assertEquals(LocalMediaKind.Audio, detectLocalMediaKind("download", "application/ogg"))
        assertEquals(LocalMediaKind.Audio, detectLocalMediaKind("download", "application/x-flac"))
        assertEquals(LocalMediaKind.Video, detectLocalMediaKind("download", "application/mp4"))
    }

    @Test
    fun fallsBackToCaseInsensitiveExtensionForGenericMimeType() {
        assertTrue(detectLocalMediaKind("episode.MP3", null).isPlayable)
        assertTrue(
            detectLocalMediaKind("episode.MKV", "application/octet-stream").isPlayable
        )
    }

    @Test
    fun explicitNonMediaMimeTypeWinsOverMediaExtension() {
        assertFalse(detectLocalMediaKind("document.mp3", "application/pdf").isPlayable)
        assertFalse(detectLocalMediaKind("cover.mp4", "image/jpeg").isPlayable)
    }

    @Test
    fun excludesHlsPlaylists() {
        assertEquals(LocalMediaKind.Playlist, detectLocalMediaKind("stream.m3u8", null))
        assertEquals(
            LocalMediaKind.Playlist,
            detectLocalMediaKind("stream.m3u8", "video/mp4"),
        )
        assertEquals(
            LocalMediaKind.Playlist,
            detectLocalMediaKind("stream", "application/vnd.apple.mpegurl"),
        )
    }

    @Test
    fun rejectsUnknownFiles() {
        assertEquals(LocalMediaKind.Other, detectLocalMediaKind("download", null))
        assertEquals(LocalMediaKind.Other, detectLocalMediaKind("document.pdf", null))
    }
}
