package com.skyd.podaura.ui.player

import androidx.datastore.preferences.core.edit
import com.skyd.podaura.di.dataStoreModule
import com.skyd.podaura.model.bean.playlist.PlaylistMediaBean
import com.skyd.podaura.model.bean.playlist.PlaylistMediaWithArticleBean
import com.skyd.podaura.model.preference.dataStore
import com.skyd.podaura.model.preference.player.BackgroundPlayPreference
import com.skyd.podaura.model.preference.player.PauseOnScreenOffPreference
import com.skyd.podaura.model.repository.player.IPlayerRepository
import com.skyd.podaura.ui.player.coordinator.PlayerCoordinator
import com.skyd.podaura.ui.player.coordinator.PlayerEngineState
import com.skyd.podaura.ui.player.coordinator.isReady
import com.skyd.podaura.ui.player.mpv.MPV
import com.skyd.podaura.ui.player.service.PlayerState
import io.ktor.client.HttpClient
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.dsl.onClose
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSFileManager
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNumber
import platform.Foundation.NSRunLoop
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.create
import platform.Foundation.dateWithTimeIntervalSinceNow
import platform.Foundation.runUntilDate
import platform.Foundation.timeIntervalSinceNow
import platform.Foundation.writeToFile
import platform.MediaPlayer.MPMediaItemPropertyArtist
import platform.MediaPlayer.MPMediaItemPropertyPlaybackDuration
import platform.MediaPlayer.MPMediaItemPropertyTitle
import platform.MediaPlayer.MPNowPlayingInfoCenter
import platform.MediaPlayer.MPNowPlayingInfoPropertyDefaultPlaybackRate
import platform.MediaPlayer.MPNowPlayingInfoPropertyElapsedPlaybackTime
import platform.MediaPlayer.MPNowPlayingInfoPropertyPlaybackRate
import platform.MediaPlayer.MPRemoteCommandCenter
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationProtectedDataDidBecomeAvailable
import platform.UIKit.UIApplicationProtectedDataWillBecomeUnavailable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IosMediaSessionTest {
    @Test
    fun lockPauseSurvivesTransientActiveNotification() {
        startKoin { modules(dataStoreModule, module {
            single(named("coil")) { HttpClient() }.onClose { it?.close() }
            single<IPlayerRepository> { object : IPlayerRepository {
                override fun requestLastPlayPosition(path: String, fallbackPath: String?) = flowOf(0L)
                override fun insertPlayHistory(path: String, duration: Long, articleId: String?) = flowOf(Unit)
                override fun updateLastPlayPosition(path: String, lastPlayPosition: Long, duration: Long, articleId: String?) = flowOf(Unit)
            } }
        }) }
        val path = NSTemporaryDirectory() + "podaura-lock-pause-test.wav"
        AppleMpvIntegrationTest().silentWave().usePinned {
            NSData.create(bytes = it.addressOf(0), length = it.get().size.toULong()).writeToFile(path, true)
        }
        var coordinator: PlayerCoordinator? = null
        var session: IosMediaSession? = null
        try {
            runBlocking {
                dataStore.edit {
                    it[BackgroundPlayPreference.key] = true
                    it[PauseOnScreenOffPreference.key] = true
                }
            }
            val player = PlayerCoordinator().also { coordinator = it }
            awaitPlayback { player.engineState.value == PlayerEngineState.AwaitingMedia }
            player.renderPlayer.setPropertyString("ao", "null")
            session = IosMediaSession(player) {}
            val pauses = mutableListOf<Boolean>()
            // Exercise the real SystemPause actor channel and observe native playback changes.
            // Keep this test focused on notification routing, without activating system audio.
            player.preparePlaybackCommand = { true }
            fun notify(name: String?) = NSNotificationCenter.defaultCenter
                .postNotificationName(name, `object` = null)
            notify(UIApplicationProtectedDataDidBecomeAvailable)
            notify(UIApplicationDidBecomeActiveNotification)
            player.onCommand(PlayerCommand.LoadList(listOf(media(path))))
            awaitPlayback { player.engineState.value.isReady && !player.playerState.value.paused }
            var lastPaused = false
            player.addObserver { event ->
                if (event is PlayerEvent.Paused && event.value != lastPaused) {
                    lastPaused = event.value
                    pauses += event.value
                }
            }
            fun awaitPaused(paused: Boolean) {
                awaitPlayback { player.playerState.value.paused == paused && lastPaused == paused }
                // Process policy collectors too, so an unwanted automatic resume is observed.
                NSRunLoop.mainRunLoop.runUntilDate(NSDate.dateWithTimeIntervalSinceNow(0.1))
            }
            // Observed on a physical iPhone: lock warning -> active -> background.
            notify(UIApplicationProtectedDataWillBecomeUnavailable)
            awaitPaused(true)
            assertEquals(listOf(true), pauses)
            notify(UIApplicationDidBecomeActiveNotification)
            notify(UIApplicationDidEnterBackgroundNotification)
            awaitPaused(true)
            assertEquals(listOf(true), pauses)
            assertTrue(player.playerState.value.paused)

            notify(UIApplicationProtectedDataDidBecomeAvailable)
            notify(UIApplicationProtectedDataDidBecomeAvailable)
            notify(UIApplicationDidBecomeActiveNotification)
            awaitPaused(false)
            assertEquals(listOf(true, false), pauses)
            assertFalse(player.playerState.value.paused)

            notify(UIApplicationProtectedDataWillBecomeUnavailable)
            awaitPaused(true)
            player.onCommand(PlayerCommand.Paused(true))
            notify(UIApplicationDidBecomeActiveNotification)
            awaitPaused(true)
            notify(UIApplicationDidEnterBackgroundNotification)
            notify(UIApplicationProtectedDataDidBecomeAvailable)
            notify(UIApplicationDidBecomeActiveNotification)
            awaitPaused(true)
            assertTrue(player.playerState.value.paused)
            assertEquals(1, pauses.count { !it })
        } finally {
            session?.close()
            coordinator?.let { player ->
                player.onCommand(PlayerCommand.Destroy)
                var destroyed = false
                CoroutineScope(Dispatchers.Main).launch {
                    player.awaitDestroyed()
                    destroyed = true
                }
                awaitPlayback { destroyed }
            }
            NSFileManager.defaultManager.removeItemAtPath(path, null)
            stopKoin()
        }
    }

    private fun awaitPlayback(condition: () -> Boolean) {
        val deadline = NSDate.dateWithTimeIntervalSinceNow(15.0)
        while (!condition() && deadline.timeIntervalSinceNow > 0) {
            NSRunLoop.mainRunLoop.runUntilDate(NSDate.dateWithTimeIntervalSinceNow(0.01))
        }
        assertTrue(condition(), "Timed out waiting for native playback")
    }

    @Test
    fun iosAudioOutputDisablesMixingForNowPlaying() {
        val player = MPV()
        try {
            player.option("config", "no")
            player.option("audio-exclusive", "no")
            player.initialize()
            assertEquals("audiounit", player.getPropertyString("ao"))
            assertTrue(player.getPropertyBoolean("audio-exclusive"))
        } finally {
            player.destroy()
        }
    }

    @Test
    fun systemControlsFollowPlayerStateAndClearWithTheMedia() {
        val remote = MPRemoteCommandCenter.sharedCommandCenter()
        val center = MPNowPlayingInfoCenter.defaultCenter()
        val commands = listOf(
            remote.playCommand,
            remote.pauseCommand,
            remote.togglePlayPauseCommand,
            remote.previousTrackCommand,
            remote.nextTrackCommand,
            remote.changePlaybackPositionCommand,
            remote.skipForwardCommand,
            remote.skipBackwardCommand,
        )
        val playlist = linkedMapOf("first" to media("first"), "last" to media("last"))
        val playing = PlayerState(
            playlist = playlist, path = "first", playlistPosition = 0,
            mediaStarted = true, paused = false, seekable = true,
            duration = 120, position = 30, speed = 1.5f,
            mediaTitle = "Embedded title", artist = "Embedded artist",
            loop = LoopMode.LoopPlaylist,
        )

        fun publish(state: PlayerState, ready: Boolean = true) =
            updateIosNowPlaying(state, ready, artwork = null)

        fun number(key: String?) =
            (assertNotNull(center.nowPlayingInfo)[key] as NSNumber).doubleValue

        try {
            publish(playing)
            val info = assertNotNull(center.nowPlayingInfo)
            assertEquals("App title", info[MPMediaItemPropertyTitle])
            assertEquals("App artist", info[MPMediaItemPropertyArtist])
            assertEquals(120.0, number(MPMediaItemPropertyPlaybackDuration))
            assertEquals(30.0, number(MPNowPlayingInfoPropertyElapsedPlaybackTime))
            assertEquals(1.5, number(MPNowPlayingInfoPropertyPlaybackRate))
            assertFalse(remote.previousTrackCommand.enabled)
            assertTrue(remote.nextTrackCommand.enabled)
            assertTrue(remote.skipForwardCommand.enabled)
            assertTrue(remote.skipBackwardCommand.enabled)

            publish(playing.copy(path = "last", playlistPosition = 1, paused = true))
            assertTrue(remote.previousTrackCommand.enabled)
            assertFalse(remote.nextTrackCommand.enabled)
            assertTrue(remote.playCommand.enabled)
            assertTrue(remote.togglePlayPauseCommand.enabled)
            assertEquals(0.0, number(MPNowPlayingInfoPropertyPlaybackRate))
            assertEquals(1.5, number(MPNowPlayingInfoPropertyDefaultPlaybackRate))

            publish(playing.copy(loading = true, seekable = false))
            assertEquals(0.0, number(MPNowPlayingInfoPropertyPlaybackRate))
            assertFalse(remote.changePlaybackPositionCommand.enabled)
            assertFalse(remote.skipForwardCommand.enabled)
            assertFalse(remote.skipBackwardCommand.enabled)
            publish(playing)
            assertEquals(1.5, number(MPNowPlayingInfoPropertyPlaybackRate))
            assertTrue(remote.changePlaybackPositionCommand.enabled)

            publish(playing, ready = false)
            assertTrue(commands.none { it.enabled })
            assertEquals(0.0, number(MPNowPlayingInfoPropertyPlaybackRate))

            publish(playing.copy(playlist = linkedMapOf("first" to media("first", " ", ""))))
            assertFalse(remote.previousTrackCommand.enabled)
            assertFalse(remote.nextTrackCommand.enabled)
            assertEquals("Embedded title", center.nowPlayingInfo?.get(MPMediaItemPropertyTitle))
            assertEquals("Embedded artist", center.nowPlayingInfo?.get(MPMediaItemPropertyArtist))

            publish(playing.copy(playlistPosition = -1))
            assertFalse(remote.previousTrackCommand.enabled)
            assertFalse(remote.nextTrackCommand.enabled)

            // EndFile clears mediaStarted while other state may still describe the old item.
            publish(playing.copy(mediaStarted = false))
            assertNull(center.nowPlayingInfo)
            assertTrue(commands.none { it.enabled })
            publish(playing)
            assertNotNull(center.nowPlayingInfo)
            assertTrue(remote.pauseCommand.enabled)
        } finally {
            publish(PlayerState(), ready = false)
        }
    }

    @Test
    fun nowPlayingUsesEmbeddedTitleBeforeFilenameFallback() {
        val path = "file:///media/episode.m4a"
        val item = media(path, title = null)
        val playing = PlayerState(
            playlist = linkedMapOf(path to item), path = path,
            mediaStarted = true, mediaTitle = "Embedded title",
        )
        val center = MPNowPlayingInfoCenter.defaultCenter()
        try {
            // The bean's display title already includes the filename fallback.
            assertEquals("episode.m4a", item.title)
            updateIosNowPlaying(playing, engineReady = true, artwork = null)
            assertEquals("Embedded title", center.nowPlayingInfo?.get(MPMediaItemPropertyTitle))

            for (embeddedTitle in listOf(null, "", " ")) {
                updateIosNowPlaying(
                    playing.copy(mediaTitle = embeddedTitle), engineReady = true, artwork = null,
                )
                assertEquals("episode.m4a", center.nowPlayingInfo?.get(MPMediaItemPropertyTitle))
            }
        } finally {
            updateIosNowPlaying(PlayerState(), engineReady = false, artwork = null)
        }
    }

    @Test
    fun skipIntervalsTrackChangedSettingsAndUsePositiveReplayIntervals() {
        val remote = MPRemoteCommandCenter.sharedCommandCenter()
        try {
            for ((forward, replay) in listOf(10 to -10, 45 to -20, 300 to -5)) {
                updateIosSkipIntervals(forward, replay)
                assertEquals(
                    forward,
                    (remote.skipForwardCommand.preferredIntervals.single() as NSNumber).intValue
                )
                assertEquals(
                    -replay,
                    (remote.skipBackwardCommand.preferredIntervals.single() as NSNumber).intValue
                )
            }
        } finally {
            updateIosSkipIntervals(10, -10)
        }
    }

    private fun media(path: String, title: String? = "App title", artist: String = "App artist") =
        PlaylistMediaWithArticleBean(
            PlaylistMediaBean("test", path, null, 0.0, 0).apply {
                this.title = title
                this.artist = artist
            },
            article = null,
        )
}
