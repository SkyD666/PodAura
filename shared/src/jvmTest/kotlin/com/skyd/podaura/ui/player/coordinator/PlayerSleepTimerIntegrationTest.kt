package com.skyd.podaura.ui.player.coordinator

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.preferencesOf
import com.skyd.podaura.model.bean.playlist.PlaylistMediaBean
import com.skyd.podaura.model.bean.playlist.PlaylistMediaWithArticleBean
import com.skyd.podaura.model.preference.player.MpvCacheDirPreference
import com.skyd.podaura.model.preference.player.MpvConfigDirPreference
import com.skyd.podaura.model.repository.player.IPlayerRepository
import com.skyd.podaura.ui.player.PlaybackResumePolicy
import com.skyd.podaura.ui.player.PlayerCommand
import com.skyd.podaura.ui.player.SleepTimer
import com.skyd.podaura.ui.player.SleepTimerOption
import com.skyd.podaura.ui.player.isExplicitPlay
import com.skyd.podaura.ui.player.mpv.PlayerKeyInput
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

class PlayerSleepTimerIntegrationTest {
    @Test
    fun countdownFadesAndRestoresGainAcrossCancellationPauseSkipAndResume() = runTest {
        if (System.getenv("PODAURA_RUN_MPV_INTEGRATION") != "1") return@runTest
        val directory = Files.createTempDirectory("podaura-timer-coordinator").toFile()
        val historyGate = AtomicReference<CompletableDeferred<Unit>?>(null)
        val historyEntered = Channel<Unit>(Channel.CONFLATED)
        val preferences = object : DataStore<Preferences> {
            override val data = MutableStateFlow(preferencesOf(
                checkNotNull(MpvConfigDirPreference.key) to directory.resolve("config").path,
                checkNotNull(MpvCacheDirPreference.key) to directory.resolve("cache").path,
            ))
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
                transform(data.value).also { data.value = it }
        }
        startKoin { modules(module {
            single<DataStore<Preferences>> { preferences }
            single<IPlayerRepository> { object : IPlayerRepository {
                override fun requestLastPlayPosition(path: String, fallbackPath: String?) = flowOf(0L)
                override fun insertPlayHistory(path: String, duration: Long, articleId: String?) = flowOf(Unit)
                override fun updateLastPlayPosition(path: String, lastPlayPosition: Long, duration: Long, articleId: String?) = flow {
                    historyGate.get()?.let {
                        historyEntered.trySend(Unit)
                        it.await()
                    }
                    emit(Unit)
                }
            } }
        }) }
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        var coordinator: PlayerCoordinator? = null
        try {
            val player = PlayerCoordinator().also { coordinator = it }
            await { player.engineState.value == PlayerEngineState.AwaitingMedia }
            val mpv = player.renderPlayer
            mpv.setPropertyString("ao", "null")
            mpv.setPropertyString("keep-open", "yes")
            mpv.setPropertyDouble("speed", 0.1)
            val bytes = checkNotNull(javaClass.getResourceAsStream("/sleep-timer.m4a"))
                .use { it.readBytes() }
            val files = (0..2).map { directory.resolve("$it.m4a").apply { writeBytes(bytes) } }
            player.onCommand(PlayerCommand.LoadList(files.mapIndexed { index, file ->
                PlaylistMediaWithArticleBean(PlaylistMediaBean(
                    playlistId = "timer-test", url = file.path, articleId = null,
                    orderPosition = index.toDouble(), createTime = 0L,
                ), article = null)
            }))
            await { player.engineState.value.isReady && !player.playerState.value.paused }

            // Control elapsed time without sleeping for a preset's five real minutes.
            val now = AtomicLong(System.currentTimeMillis())
            val controller = PlayerCoordinator::class.java.getDeclaredField("sleepTimerController").run {
                isAccessible = true
                get(player) as PlayerSleepTimer
            }
            val timer = PlayerSleepTimer::class.java.getDeclaredField("timer").run {
                isAccessible = true
                get(controller) as SleepTimer
            }
            SleepTimer::class.java.getDeclaredField("nowMillis").apply {
                isAccessible = true
                set(timer, { now.get() })
            }
            suspend fun armDuration() {
                player.setSleepTimer(SleepTimerOption.AfterMinutes(5))
                await { player.sleepTimer.value.option is SleepTimerOption.AfterMinutes }
            }
            suspend fun expireAtEnd() {
                player.setSleepTimer(SleepTimerOption.EndOfMedia)
                await {
                    player.sleepTimer.value.option == SleepTimerOption.EndOfMedia &&
                            mpv.getPropertyString("keep-open") == "always"
                }
                mpv.command("seek", "1.3", "absolute+exact")
                await { player.sleepTimer.value.option == SleepTimerOption.Off && player.playerState.value.paused }
            }

            mpv.setPropertyDouble("volume", 73.0)
            mpv.setPropertyDouble("volume-gain", -3.0)
            armDuration()
            now.addAndGet(300_000)
            val fadeStarted = TimeSource.Monotonic.markNow()
            player.refreshSleepTimer()
            await { mpv.getPropertyDouble("volume-gain") < -3.0 }
            assertFalse(player.playerState.value.paused)
            assertEquals(SleepTimerOption.AfterMinutes(5), player.sleepTimer.value.option)
            assertEquals(0L, player.sleepTimer.value.remainingSeconds(now.get()))
            val firstGain = mpv.getPropertyDouble("volume-gain")
            delayReal(100)
            assertTrue(mpv.getPropertyDouble("volume-gain") < firstGain)
            // The user's volume remains independent of the temporary attenuation.
            mpv.setPropertyDouble("volume", 81.0)
            await { player.sleepTimer.value.option == SleepTimerOption.Off && player.playerState.value.paused }
            assertTrue(fadeStarted.elapsedNow() >= 900.milliseconds)
            assertTrue(fadeStarted.elapsedNow() < 1_500.milliseconds)
            assertEquals(-3.0, mpv.getPropertyDouble("volume-gain"), 0.001)
            assertEquals(81.0, mpv.getPropertyDouble("volume"), 0.001)
            player.sendNativeKey("keydown", "SPACE")
            player.sendNativeKey("keydown", "SPACE") // Holding the key must not toggle twice.
            player.sendNativeKey("keyup", "SPACE")
            await { !player.playerState.value.paused }
            mpv.command("playlist-next")
            await { player.playerState.value.path == files[1].path && player.playerState.value.mediaStarted }
            assertFalse(player.playerState.value.paused)

            expireAtEnd()
            player.sendNativeKey("keydown", "SPACE")
            player.sendNativeKey("keyup", "SPACE")
            await { !player.playerState.value.paused && !mpv.getPropertyBoolean("eof-reached") }
            await { mpv.getPropertyString("keep-open") == "yes" }
            assertEquals(files[1].path, player.playerState.value.path)

            player.onCommand(PlayerCommand.PlayFileInPlaylist(files[0].path))
            await { player.playerState.value.path == files[0].path && player.playerState.value.mediaStarted }
            expireAtEnd()
            player.onCommand(PlayerCommand.NextMedia)
            await { player.playerState.value.path == files[1].path && player.playerState.value.mediaStarted }
            delayReal(150)
            assertEquals(1, mpv.getPropertyInt("playlist-pos"))
            assertFalse(player.playerState.value.paused)

            for (android in listOf(false, true)) {
                val policy = PlaybackResumePolicy(initiallyScreenLocked = true, independentScreenPlayback = android)
                val restoredIntent = CompletableDeferred<Unit>()
                var expired = false
                player.onPlaybackCommand = { command, intent ->
                    intent?.let { policy.userAction(it, explicitPlay = command.isExplicitPlay(player.playerState.value.paused)) }
                    if (intent == false) expired = true
                    if (intent == true && expired) restoredIntent.complete(Unit)
                }
                // An already-paused player expires immediately, so a queued explicit play wins.
                player.onCommand(PlayerCommand.Paused(true))
                await { player.playerState.value.paused }
                expired = false
                armDuration()
                val blocked = CompletableDeferred<Unit>()
                val release = CompletableDeferred<Unit>()
                player.preparePlaybackCommand = { intent ->
                    if (intent == null) {
                        blocked.complete(Unit)
                        release.await()
                    }
                    true
                }
                player.onCommand(PlayerCommand.SeekTo(0))
                withContext(Dispatchers.IO) { withTimeout(5_000) { blocked.await() } }
                now.addAndGet(300_000)
                player.refreshSleepTimer()
                await { player.sleepTimer.value.option == SleepTimerOption.Off }
                // Explicit play after expiry still wins, even while the actor is blocked.
                player.onCommand(PlayerCommand.Paused(false))
                release.complete(Unit)
                withContext(Dispatchers.IO) { withTimeout(5_000) { restoredIntent.await() } }
                await { !player.playerState.value.paused }
                assertFalse(policy.pauseWhenRequired(playing = true, ready = true, backgroundEnabled = true))
                player.preparePlaybackCommand = null
            }

            player.onPlaybackCommand = null
            suspend fun startFade(): TimeMark {
                player.onCommand(PlayerCommand.SeekTo(0))
                player.onCommand(PlayerCommand.Paused(false))
                await { !player.playerState.value.paused }
                armDuration()
                now.addAndGet(300_000)
                val started = TimeSource.Monotonic.markNow()
                player.refreshSleepTimer()
                await { mpv.getPropertyDouble("volume-gain") < -3.0 }
                return started
            }
            suspend fun assertRestoredAndPlaying() {
                await { mpv.getPropertyDouble("volume-gain") == -3.0 }
                assertFalse(player.playerState.value.paused)
                // No callback left over from the cancelled fade may pause the player later.
                delayReal(1_100)
                assertFalse(player.playerState.value.paused)
                assertEquals(81.0, mpv.getPropertyDouble("volume"), 0.001)
            }

            startFade()
            player.setSleepTimer(SleepTimerOption.Off)
            await { player.sleepTimer.value.option == SleepTimerOption.Off }
            assertRestoredAndPlaying()

            startFade()
            player.setSleepTimer(SleepTimerOption.AfterMinutes(10))
            await { player.sleepTimer.value.option == SleepTimerOption.AfterMinutes(10) }
            assertRestoredAndPlaying()
            assertEquals(now.get() + 600_000, player.sleepTimer.value.deadlineMillis)
            player.setSleepTimer(SleepTimerOption.Off)
            await { player.sleepTimer.value.option == SleepTimerOption.Off }

            startFade()
            player.onCommand(PlayerCommand.Paused(true))
            await { player.sleepTimer.value.option == SleepTimerOption.Off && player.playerState.value.paused }
            assertEquals(-3.0, mpv.getPropertyDouble("volume-gain"), 0.001)
            player.setSystemPaused(false)
            delayReal(100)
            assertTrue(player.playerState.value.paused)

            startFade()
            player.onCommand(PlayerCommand.NextMedia)
            await { player.playerState.value.path == files[2].path && player.playerState.value.mediaStarted }
            await { player.sleepTimer.value.option == SleepTimerOption.Off && player.playerState.value.paused }
            assertEquals(-3.0, mpv.getPropertyDouble("volume-gain"), 0.001)

            suspend fun blockHistory(): CompletableDeferred<Unit> {
                val release = CompletableDeferred<Unit>()
                historyGate.set(release)
                player.onCommand(PlayerCommand.SeekTo(0))
                withContext(Dispatchers.IO) { withTimeout(5_000) { historyEntered.receive() } }
                return release
            }
            fun releaseHistory(release: CompletableDeferred<Unit>) {
                historyGate.set(null)
                release.complete(Unit)
            }

            // History I/O must not freeze gain updates, delay expiry, or let a queued skip resume.
            val blockedFadeStarted = startFade()
            val releaseSkip = blockHistory()
            player.onCommand(PlayerCommand.PreviousMedia)
            val blockedGain = mpv.getPropertyDouble("volume-gain")
            delayReal(100)
            assertTrue(mpv.getPropertyDouble("volume-gain") < blockedGain)
            await(1_500) {
                player.sleepTimer.value.option == SleepTimerOption.Off && mpv.getPropertyBoolean("pause")
            }
            assertTrue(blockedFadeStarted.elapsedNow() < 1_500.milliseconds)
            releaseHistory(releaseSkip)
            await { player.playerState.value.path == files[1].path && player.playerState.value.mediaStarted }
            delayReal(100)
            assertTrue(mpv.getPropertyBoolean("pause"))
            assertEquals(-3.0, mpv.getPropertyDouble("volume-gain"), 0.001)

            // A newly armed countdown must not prevent explicit play after an earlier expiry.
            player.setSleepTimer(SleepTimerOption.AfterMinutes(10))
            await { player.sleepTimer.value.option == SleepTimerOption.AfterMinutes(10) }
            player.onCommand(PlayerCommand.Paused(false))
            await { !player.playerState.value.paused && !mpv.getPropertyBoolean("pause") }
            player.setSleepTimer(SleepTimerOption.Off)
            await { player.sleepTimer.value.option == SleepTimerOption.Off }

            // Cancel, rearm, and manual pause must also work before a blocked save returns.
            for (option in listOf(SleepTimerOption.Off, SleepTimerOption.AfterMinutes(10))) {
                startFade()
                val release = blockHistory()
                player.setSleepTimer(option)
                await(500) {
                    player.sleepTimer.value.option == option && mpv.getPropertyDouble("volume-gain") == -3.0
                }
                assertRestoredAndPlaying()
                releaseHistory(release)
                player.setSleepTimer(SleepTimerOption.Off)
                await { player.sleepTimer.value.option == SleepTimerOption.Off }
            }
            for (binding in listOf(null, "cycle pause", "no-osd cycle pause", "osd-msg-bar cycle pause")) {
                startFade()
                val releasePause = blockHistory()
                if (binding != null) {
                    mpv.command("keybind", "SPACE", binding)
                    player.sendNativeKey("keydown", "SPACE")
                    player.sendNativeKey("keydown", "SPACE")
                    player.sendNativeKey("keyup", "SPACE")
                } else player.onCommand(PlayerCommand.Paused(true))
                await(500) {
                    player.sleepTimer.value.option == SleepTimerOption.Off && mpv.getPropertyBoolean("pause")
                }
                releaseHistory(releasePause)
                delayReal(100)
                assertTrue(mpv.getPropertyBoolean("pause"))
            }
            mpv.command("keybind", "SPACE", "cycle pause")

            // keep-open's EOF pause is not a manual pause and must retain the fade deadline.
            val eofFadeStarted = startFade()
            mpv.setPropertyString("keep-open", "always")
            mpv.command("seek", "1.3", "absolute+exact")
            await { mpv.getPropertyBoolean("eof-reached") && mpv.getPropertyBoolean("pause") }
            delayReal(100)
            assertEquals(SleepTimerOption.AfterMinutes(5), player.sleepTimer.value.option)
            assertTrue(mpv.getPropertyDouble("volume-gain") < -3.0)
            mpv.command("seek", "0", "absolute+exact")
            await { !mpv.getPropertyBoolean("eof-reached") }
            delayReal(100)
            assertEquals(SleepTimerOption.AfterMinutes(5), player.sleepTimer.value.option)
            await(1_500) { player.sleepTimer.value.option == SleepTimerOption.Off }
            assertTrue(eofFadeStarted.elapsedNow() >= 900.milliseconds)
            assertEquals(-3.0, mpv.getPropertyDouble("volume-gain"), 0.001)

            // Play-from-EOF seeks before unpausing; expiry during that save must still win.
            startFade()
            mpv.command("seek", "1.3", "absolute+exact")
            await { mpv.getPropertyBoolean("eof-reached") && mpv.getPropertyBoolean("pause") }
            val releaseEofPlay = CompletableDeferred<Unit>()
            historyGate.set(releaseEofPlay)
            player.onCommand(PlayerCommand.Paused(false))
            withContext(Dispatchers.IO) { withTimeout(5_000) { historyEntered.receive() } }
            await(1_500) { player.sleepTimer.value.option == SleepTimerOption.Off }
            releaseHistory(releaseEofPlay)
            delayReal(100)
            assertTrue(mpv.getPropertyBoolean("pause"))

            startFade()
            player.onCommand(PlayerCommand.Destroy)
            player.awaitDestroyed()
            assertEquals(SleepTimerOption.Off, player.sleepTimer.value.option)
            assertEquals(PlayerEngineState.Destroyed, player.engineState.value)
        } finally {
            historyGate.getAndSet(null)?.complete(Unit)
            coordinator?.onCommand(PlayerCommand.Destroy)
            coordinator?.awaitDestroyed()
            Dispatchers.resetMain()
            stopKoin()
            directory.deleteRecursively()
        }
    }

    // JVM UI does not forward native keys. Send the same actor input as the Apple adapter.
    private fun PlayerCoordinator.sendNativeKey(action: String, key: String) {
        onNativeKey(PlayerKeyInput(action, key))
    }

    private suspend fun await(timeoutMillis: Long = 15_000, condition: () -> Boolean) = withContext(Dispatchers.IO) {
        withTimeout(timeoutMillis) { while (!condition()) delay(10) }
    }

    private suspend fun delayReal(millis: Long) = withContext(Dispatchers.IO) { delay(millis) }
}
