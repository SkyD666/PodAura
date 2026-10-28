package com.skyd.podaura.ui.player.media

import com.skyd.podaura.model.bean.playlist.PlaylistMediaBean
import com.skyd.podaura.model.bean.playlist.PlaylistMediaWithArticleBean
import com.skyd.podaura.ui.player.service.PlayerState
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.jetbrains.skia.Bitmap
import kotlinx.cinterop.toKString
import kotlinx.cinterop.useContents
import platform.AppKit.NSImage
import platform.Foundation.NSMakeSize
import platform.Foundation.NSNumber
import platform.MediaPlayer.MPMediaItemArtwork
import platform.MediaPlayer.MPMediaItemPropertyAlbumTitle
import platform.MediaPlayer.MPMediaItemPropertyArtist
import platform.MediaPlayer.MPMediaItemPropertyArtwork
import platform.MediaPlayer.MPMediaItemPropertyPlaybackDuration
import platform.MediaPlayer.MPMediaItemPropertyPodcastTitle
import platform.MediaPlayer.MPMediaItemPropertyTitle
import platform.MediaPlayer.MPNowPlayingInfoCenter
import platform.MediaPlayer.MPNowPlayingInfoMediaTypeAudio
import platform.MediaPlayer.MPNowPlayingInfoMediaTypeVideo
import platform.MediaPlayer.MPNowPlayingInfoPropertyDefaultPlaybackRate
import platform.MediaPlayer.MPNowPlayingInfoPropertyElapsedPlaybackTime
import platform.MediaPlayer.MPNowPlayingInfoPropertyMediaType
import platform.MediaPlayer.MPNowPlayingInfoPropertyPlaybackQueueCount
import platform.MediaPlayer.MPNowPlayingInfoPropertyPlaybackQueueIndex
import platform.MediaPlayer.MPNowPlayingInfoPropertyPlaybackRate
import platform.MediaPlayer.MPNowPlayingPlaybackStatePaused
import platform.MediaPlayer.MPNowPlayingPlaybackStatePlaying
import platform.MediaPlayer.MPNowPlayingPlaybackStateStopped
import platform.MediaPlayer.MPRemoteCommandCenter
import platform.posix.getenv
import kotlin.io.encoding.Base64
import kotlin.native.runtime.GC
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MacosMediaSessionAdapterTest {
    @Test
    fun failedCoverLoadFallsBackToDecodedMediaArtwork() = runTest {
        val snapshots = mutableListOf<DesktopMediaSnapshot>()
        val loaded = mutableListOf<Any>()
        val image = Bitmap().apply { allocN32Pixels(1, 1) }
        val data = DesktopArtworkData(byteArrayOf(1), 1, 1)
        val controller = DesktopMediaSessionController(
            adapter = object : DesktopMediaSessionAdapter {
                override fun setCommandListener(listener: (DesktopMediaCommand) -> Unit) = Unit
                override fun update(snapshot: DesktopMediaSnapshot) { snapshots += snapshot }
                override fun clear() = Unit
                override fun close() = Unit
            },
            artworkLoader = { source ->
                loaded += source
                if (source === image) data else error("No cover in local media")
            },
            commandSink = {},
            scope = this,
        )
        val path = "/audio.mp3"
        try {
            controller.update(PlayerState(
                path = path,
                playlist = listOf(PlaylistMediaWithArticleBean(
                    PlaylistMediaBean("test", path, null, 0.0, 0), null,
                )),
                mediaThumbnail = image,
            ))
            advanceUntilIdle()
            assertEquals(2, loaded.size)
            assertEquals(data, snapshots.last().artwork?.data)
        } finally {
            controller.close()
            image.close()
        }
    }

    /**
     * Publishes to the real system-wide Now Playing center. Run the linked test.kexe with
     * PODAURA_RUN_MAC_MEDIA_INTEGRATION=1 and --ktest_filter=*MacosMediaSessionAdapterTest*.
     */
    @OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
    @Test
    fun publishesMetadataArtworkAndAvailabilityAndCleansUp() {
        if (getenv("PODAURA_RUN_MAC_MEDIA_INTEGRATION")?.toKString() != "1") {
            println("SKIP system media integration: run test.kexe with PODAURA_RUN_MAC_MEDIA_INTEGRATION=1")
            return
        }
        val center = MPNowPlayingInfoCenter.defaultCenter()
        val remote = MPRemoteCommandCenter.sharedCommandCenter()
        val sharedImages = mutableListOf<NSImage?>()
        val adapter = MacosMediaSessionAdapter { sharedImages += it }
        val snapshot = DesktopMediaSnapshot(
            mediaId = "test",
            title = "PodAura media integration test",
            artist = "PodAura",
            album = "System media controls",
            durationSeconds = 120.0,
            positionSeconds = 30.0,
            playbackRate = 1.5,
            defaultPlaybackRate = 1.5,
            queueIndex = 0,
            queueCount = 2,
            isVideo = false,
            playbackState = DesktopPlaybackState.Playing,
            canPlay = false,
            canPause = true,
            canTogglePlayPause = true,
            canGoPrevious = false,
            canGoNext = true,
            canChangePlaybackPosition = true,
            artwork = DesktopArtwork(
                "cover",
                DesktopArtworkData(
                    Base64.decode(
                        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUB" +
                                "AScY42YAAAAASUVORK5CYII="
                    ),
                    width = 1,
                    height = 1,
                ),
            ),
        )
        try {
            assertFalse(remote.playCommand.enabled)
            adapter.update(snapshot)
            assertNotNull(sharedImages.last(), "PiP must receive the decoded system artwork")
            val publishedImages = sharedImages.size
            adapter.update(snapshot.copy(positionSeconds = 31.0))
            assertEquals(publishedImages, sharedImages.size, "metadata changes must reuse decoded artwork")
            adapter.update(snapshot)
            GC.collect()
            val info = assertNotNull(center.nowPlayingInfo)
            assertEquals(snapshot.title, info[MPMediaItemPropertyTitle])
            assertEquals(snapshot.artist, info[MPMediaItemPropertyArtist])
            assertEquals(snapshot.album, info[MPMediaItemPropertyAlbumTitle])
            assertEquals(snapshot.album, info[MPMediaItemPropertyPodcastTitle])
            fun number(key: String?) = (info[key] as NSNumber).doubleValue
            assertEquals(120.0, number(MPMediaItemPropertyPlaybackDuration))
            assertEquals(30.0, number(MPNowPlayingInfoPropertyElapsedPlaybackTime))
            assertEquals(1.5, number(MPNowPlayingInfoPropertyPlaybackRate))
            assertEquals(1.5, number(MPNowPlayingInfoPropertyDefaultPlaybackRate))
            assertEquals(0.0, number(MPNowPlayingInfoPropertyPlaybackQueueIndex))
            assertEquals(2.0, number(MPNowPlayingInfoPropertyPlaybackQueueCount))
            assertEquals(MPNowPlayingInfoMediaTypeAudio.toDouble(), number(MPNowPlayingInfoPropertyMediaType))
            assertEquals(MPNowPlayingPlaybackStatePlaying, center.playbackState)
            assertFalse(remote.playCommand.enabled)
            assertTrue(remote.pauseCommand.enabled)
            assertTrue(remote.togglePlayPauseCommand.enabled)
            assertFalse(remote.previousTrackCommand.enabled)
            assertTrue(remote.nextTrackCommand.enabled)
            assertTrue(remote.changePlaybackPositionCommand.enabled)
            val artwork = info[MPMediaItemPropertyArtwork] as MPMediaItemArtwork
            val image = assertNotNull(artwork.imageWithSize(NSMakeSize(64.0, 64.0))) as NSImage
            assertEquals(1.0, image.size.useContents { width })

            adapter.update(snapshot.copy(
                isVideo = true,
                playbackState = DesktopPlaybackState.Paused,
                playbackRate = 0.0,
                canPlay = true,
                canPause = false,
                canGoPrevious = true,
                canGoNext = false,
                canChangePlaybackPosition = false,
                artwork = null,
            ))
            assertNull(sharedImages.last(), "changing to no cover must clear PiP artwork")
            val paused = assertNotNull(center.nowPlayingInfo)
            assertNull(paused[MPMediaItemPropertyArtwork])
            assertEquals(MPNowPlayingInfoMediaTypeVideo.toDouble(),
                (paused[MPNowPlayingInfoPropertyMediaType] as NSNumber).doubleValue)
            assertEquals(MPNowPlayingPlaybackStatePaused, center.playbackState)
            assertTrue(remote.playCommand.enabled)
            assertFalse(remote.pauseCommand.enabled)
            assertTrue(remote.previousTrackCommand.enabled)
            assertFalse(remote.nextTrackCommand.enabled)
            assertFalse(remote.changePlaybackPositionCommand.enabled)

            adapter.clear()
            assertNull(center.nowPlayingInfo)
            assertEquals(MPNowPlayingPlaybackStateStopped, center.playbackState)
            assertFalse(remote.togglePlayPauseCommand.enabled)
            adapter.update(snapshot)
            assertNotNull(center.nowPlayingInfo)
        } finally {
            adapter.close()
        }
        adapter.close()
        adapter.update(snapshot)
        assertNull(center.nowPlayingInfo)
        assertFalse(remote.pauseCommand.enabled)
        assertFalse(remote.changePlaybackPositionCommand.enabled)
        MacosMediaSessionAdapter().close()
    }
}
