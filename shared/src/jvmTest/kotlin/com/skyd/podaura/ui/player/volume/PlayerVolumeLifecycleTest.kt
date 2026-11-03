package com.skyd.podaura.ui.player.volume

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import com.skyd.podaura.model.bean.playlist.PlaylistMediaBean
import com.skyd.podaura.model.bean.playlist.PlaylistMediaWithArticleBean
import com.skyd.podaura.model.preference.player.MpvCacheDirPreference
import com.skyd.podaura.model.preference.player.MpvConfigDirPreference
import com.skyd.podaura.model.repository.player.IPlayerRepository
import com.skyd.podaura.ui.player.PlayerCommand
import com.skyd.podaura.ui.player.coordinator.PlayerCoordinator
import com.skyd.podaura.ui.player.coordinator.PlayerEngineState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlayerVolumeLifecycleTest {
    @Test
    fun destroyFlushesVolumeSubmittedWhileActorIsWaitingForHistory() = runTest {
        if (System.getenv("PODAURA_RUN_MPV_INTEGRATION") != "1") return@runTest
        val directory = Files.createTempDirectory("podaura-volume").toFile()
        val historyGate = AtomicReference<CompletableDeferred<Unit>?>(null)
        val historyEntered = CompletableDeferred<Unit>()
        val store = object : DataStore<Preferences> {
            override val data = MutableStateFlow(preferencesOf(
                checkNotNull(MpvConfigDirPreference.key) to directory.resolve("config").path,
                checkNotNull(MpvCacheDirPreference.key) to directory.resolve("cache").path,
            ))
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences) =
                transform(data.value).also { data.value = it }
        }
        startKoin { modules(module {
            single<DataStore<Preferences>> { store }
            single<IPlayerRepository> { object : IPlayerRepository {
                override fun requestLastPlayPosition(path: String, fallbackPath: String?) = flowOf(0L)
                override fun insertPlayHistory(path: String, duration: Long, articleId: String?) = flowOf(Unit)
                override fun updateLastPlayPosition(path: String, lastPlayPosition: Long, duration: Long, articleId: String?) = flow {
                    historyGate.getAndSet(null)?.let {
                        historyEntered.complete(Unit)
                        it.await()
                    }
                    emit(Unit)
                }
            } }
        }) }
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        var coordinator: PlayerCoordinator? = null
        try {
            withContext(Dispatchers.IO) {
                val player = PlayerCoordinator().also { coordinator = it }
                withTimeout(15_000) { while (player.engineState.value != PlayerEngineState.AwaitingMedia) delay(20) }
                player.renderPlayer.setPropertyString("ao", "null")
                val file = directory.resolve("audio.m4a")
                file.writeBytes(checkNotNull(javaClass.getResourceAsStream("/volume-silence.m4a")).use { it.readBytes() })
                player.onCommand(PlayerCommand.LoadList(listOf(PlaylistMediaWithArticleBean(
                    PlaylistMediaBean(playlistId = "volume", url = file.path, articleId = null,
                        orderPosition = 0.0, createTime = 0L), article = null,
                ))))
                withTimeout(15_000) { while (player.engineState.value != PlayerEngineState.Ready) delay(20) }
                historyGate.set(CompletableDeferred())
                player.onCommand(PlayerCommand.Paused(true))
                withTimeout(5_000) { historyEntered.await() }
                player.onCommand(PlayerCommand.SetVolume(42f))
                player.onCommand(PlayerCommand.ToggleMute)
                player.onCommand(PlayerCommand.Destroy)
                withTimeout(5_000) { player.awaitDestroyed() }
                assertEquals(42f, store.data.value[floatPreferencesKey("playerVolume")])
                assertTrue(store.data.value[booleanPreferencesKey("playerMuted")] == true)
            }
        } finally {
            historyGate.getAndSet(null)?.complete(Unit)
            coordinator?.onCommand(PlayerCommand.Destroy)
            coordinator?.awaitDestroyed()
            Dispatchers.resetMain()
            stopKoin()
            directory.deleteRecursively()
        }
    }
}
