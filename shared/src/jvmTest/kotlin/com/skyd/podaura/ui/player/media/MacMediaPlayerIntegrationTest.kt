package com.skyd.podaura.ui.player.media

import com.skyd.fundation.jna.mac.MacArtwork
import com.skyd.fundation.jna.mac.MacMediaPlayer
import com.skyd.fundation.jna.mac.MacMediaType
import com.skyd.fundation.jna.mac.MacNowPlayingInfo
import com.skyd.fundation.jna.mac.MacPlaybackState
import com.skyd.fundation.jna.mac.MacRemoteCommandAvailability
import com.sun.jna.Library
import com.sun.jna.Native
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MacMediaPlayerIntegrationTest {
    @Test
    fun usesMediampRuntimeForTheBuildJvmArchitecture() {
        if (!System.getProperty("os.name").contains("mac", ignoreCase = true)) return
        val (expectedArchitecture, unexpectedArchitecture) = when (
            System.getProperty("os.arch").lowercase()
        ) {
            "aarch64", "arm64" -> "arm64" to "x64"
            "amd64", "x86_64" -> "x64" to "arm64"
            else -> return
        }
        val classLoader = checkNotNull(javaClass.classLoader)

        assertNotNull(classLoader.getResource("mpv-natives-macos-$expectedArchitecture.txt"))
        assertNull(classLoader.getResource("mpv-natives-macos-$unexpectedArchitecture.txt"))
    }

    @Test
    fun packagesMediaPlayerShimForTheBuildJvmArchitecture() {
        if (!System.getProperty("os.name").contains("mac", ignoreCase = true)) return
        val expectedResourcePrefix = when (System.getProperty("os.arch").lowercase()) {
            "aarch64", "arm64" -> "darwin-aarch64"
            "amd64", "x86_64" -> "darwin-x86-64"
            else -> return
        }

        assertNotNull(
            javaClass.classLoader.getResource(
                "$expectedResourcePrefix/libpodaura_media_player.dylib"
            )
        )
    }

    @Test
    fun publishesArtworkAndCommandsThroughTheRealMediaPlayerFramework() {
        if (System.getenv("PODAURA_RUN_MAC_MEDIA_INTEGRATION") != "1") return
        check(System.getProperty("os.name").contains("mac", ignoreCase = true))

        val session = MacMediaPlayer.openSession { true }
        try {
            session.update(
                info = MacNowPlayingInfo(
                    title = "PodAura media integration test",
                    artist = "PodAura",
                    album = "System media controls",
                    durationSeconds = 120.0,
                    elapsedSeconds = 30.0,
                    playbackRate = 1.0,
                    defaultPlaybackRate = 1.0,
                    queueIndex = 0,
                    queueCount = 1,
                    mediaType = MacMediaType.Audio,
                    playbackState = MacPlaybackState.Playing,
                    artwork = MacArtwork(
                        id = "integration-test",
                        pngBytes = Base64.getDecoder().decode(TEST_PNG_BASE64),
                        width = 1,
                        height = 1,
                    ),
                ),
                commandAvailability = MacRemoteCommandAvailability(
                    canPlay = false,
                    canPause = true,
                    canTogglePlayPause = true,
                    canGoPrevious = false,
                    canGoNext = false,
                    canChangePlaybackPosition = true,
                ),
            )
            System.gc()
            Thread.sleep(750L)
            requestPublishedArtwork()
        } finally {
            session.close()
        }

        MacMediaPlayer.openSession { true }.close()
    }

    private fun requestPublishedArtwork() {
        val libraryFile = Native.extractFromResourcePath(
            "podaura_media_player",
            javaClass.classLoader,
        )
        val library = Native.load(
            libraryFile.absolutePath,
            MacMediaPlayerTestLibrary::class.java,
        )
        check(library.podaura_media_player_request_published_artwork(64.0, 64.0) != 0) {
            "The macOS artwork request handler returned nil"
        }
    }

    private companion object {
        const val TEST_PNG_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUB" +
                    "AScY42YAAAAASUVORK5CYII="
    }
}

internal interface MacMediaPlayerTestLibrary : Library {
    fun podaura_media_player_request_published_artwork(width: Double, height: Double): Int
}
