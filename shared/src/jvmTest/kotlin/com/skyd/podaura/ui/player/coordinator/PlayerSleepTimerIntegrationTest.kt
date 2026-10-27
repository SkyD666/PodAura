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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class PlayerSleepTimerIntegrationTest {
    @Test
    fun expiryResumesThroughKeyboardSkipAndAlreadyQueuedPlay() = runTest {
        if (System.getenv("PODAURA_RUN_MPV_INTEGRATION") != "1") return@runTest
        val directory = Files.createTempDirectory("podaura-timer-coordinator").toFile()
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
                override fun updateLastPlayPosition(path: String, lastPlayPosition: Long, duration: Long, articleId: String?) = flowOf(Unit)
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
            val timer = PlayerCoordinator::class.java.getDeclaredField("timer").run {
                isAccessible = true
                get(player) as SleepTimer
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
                mpv.command("seek", "1.2", "absolute+exact")
                await { player.sleepTimer.value.option == SleepTimerOption.Off && player.playerState.value.paused }
            }

            armDuration()
            now.addAndGet(300_000)
            player.refreshSleepTimer()
            await { player.sleepTimer.value.option == SleepTimerOption.Off && player.playerState.value.paused }
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
                player.refreshSleepTimer() // This expiry is ahead of the user's Play in the actor.
                player.onCommand(PlayerCommand.Paused(false))
                release.complete(Unit)
                withContext(Dispatchers.IO) { withTimeout(5_000) { restoredIntent.await() } }
                await { !player.playerState.value.paused }
                assertFalse(policy.pauseWhenRequired(playing = true, ready = true, backgroundEnabled = true))
                player.preparePlaybackCommand = null
            }
        } finally {
            coordinator?.onCommand(PlayerCommand.Destroy)
            coordinator?.awaitDestroyed()
            Dispatchers.resetMain()
            stopKoin()
            directory.deleteRecursively()
        }
    }

    // JVM UI does not forward native keys. Send the same actor input as the Apple adapter.
    private fun PlayerCoordinator.sendNativeKey(action: String, key: String) {
        val keyMessage = Class.forName("${PlayerCoordinator::class.java.name}\$ActorMessage\$Key")
            .getDeclaredConstructor(PlayerKeyInput::class.java).newInstance(PlayerKeyInput(action, key))
        @Suppress("UNCHECKED_CAST")
        val channel = PlayerCoordinator::class.java.getDeclaredField("commands").run {
            isAccessible = true
            get(this@sendNativeKey) as Channel<Any>
        }
        check(channel.trySend(keyMessage).isSuccess)
    }

    private suspend fun await(condition: () -> Boolean) = withContext(Dispatchers.IO) {
        withTimeout(15_000) { while (!condition()) delay(10) }
    }

    private suspend fun delayReal(millis: Long) = withContext(Dispatchers.IO) { delay(millis) }
}
