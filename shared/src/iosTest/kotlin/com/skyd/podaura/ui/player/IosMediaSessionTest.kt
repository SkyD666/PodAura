package com.skyd.podaura.ui.player

import androidx.datastore.preferences.core.edit
import com.skyd.podaura.di.dataStoreModule
import com.skyd.podaura.model.bean.playlist.PlaylistMediaBean
import com.skyd.podaura.model.bean.playlist.PlaylistMediaWithArticleBean
import com.skyd.podaura.model.preference.dataStore
import com.skyd.podaura.model.preference.player.BackgroundPlayPreference
import com.skyd.podaura.model.preference.player.PauseOnScreenOffPreference
import com.skyd.podaura.ui.player.coordinator.PlayerCoordinator
import com.skyd.podaura.ui.player.mpv.MPV
import com.skyd.podaura.ui.player.mpv.MPVPlayer
import com.skyd.podaura.ui.player.service.PlayerState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNumber
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
    fun lockPauseSurvivesTransientActiveNotification() = runTest {
        startKoin { modules(dataStoreModule) }
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val acquired = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        // Keep the native engine idle: this exercises the real notification/command adapter.
        val owner = launch(Dispatchers.IO) {
            MPVPlayer.instance.withOwnership {
                acquired.complete(Unit)
                release.await()
            }
        }
        var coordinator: PlayerCoordinator? = null
        var session: IosMediaSession? = null
        try {
            acquired.await()
            dataStore.edit {
                it[BackgroundPlayPreference.key] = true
                it[PauseOnScreenOffPreference.key] = true
            }
            val player = PlayerCoordinator().also { coordinator = it }
            session = IosMediaSession(player) {}
            val pauses = mutableListOf<Boolean>()
            val intentHook = player.onPlaybackCommand
            player.onPlaybackCommand = { command, intent ->
                intentHook?.invoke(command, intent)
                if (command is PlayerCommand.Paused) {
                    pauses += command.paused
                    player.model.onEvent(PlayerEvent.Paused(command.paused))
                }
            }
            fun notify(name: String?) = NSNotificationCenter.defaultCenter
                .postNotificationName(name, `object` = null)
            notify(UIApplicationProtectedDataDidBecomeAvailable)
            notify(UIApplicationDidBecomeActiveNotification)
            player.model.onEvent(PlayerEvent.Paused(false))
            // Observed on a physical iPhone: lock warning -> active -> background.
            notify(UIApplicationProtectedDataWillBecomeUnavailable)
            assertEquals(listOf(true), pauses)
            notify(UIApplicationDidBecomeActiveNotification)
            notify(UIApplicationDidEnterBackgroundNotification)
            assertEquals(listOf(true), pauses)
            assertTrue(player.playerState.value.paused)

            notify(UIApplicationProtectedDataDidBecomeAvailable)
            notify(UIApplicationProtectedDataDidBecomeAvailable)
            notify(UIApplicationDidBecomeActiveNotification)
            assertEquals(listOf(true, false), pauses)
            assertFalse(player.playerState.value.paused)

            notify(UIApplicationProtectedDataWillBecomeUnavailable)
            player.onCommand(PlayerCommand.Paused(true))
            notify(UIApplicationDidBecomeActiveNotification)
            notify(UIApplicationDidEnterBackgroundNotification)
            notify(UIApplicationProtectedDataDidBecomeAvailable)
            notify(UIApplicationDidBecomeActiveNotification)
            assertTrue(player.playerState.value.paused)
            assertEquals(1, pauses.count { !it })
        } finally {
            session?.close()
            coordinator?.onCommand(PlayerCommand.Destroy)
            coordinator?.awaitDestroyed()
            release.complete(Unit)
            owner.join()
            Dispatchers.resetMain()
            stopKoin()
        }
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
