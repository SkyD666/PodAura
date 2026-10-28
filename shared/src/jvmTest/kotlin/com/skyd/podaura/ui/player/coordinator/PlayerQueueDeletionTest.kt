package com.skyd.podaura.ui.player.coordinator

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.preferencesOf
import com.skyd.podaura.model.bean.playlist.MediaUrlWithArticleIdBean
import com.skyd.podaura.model.bean.playlist.PlaylistMediaBean
import com.skyd.podaura.model.bean.playlist.PlaylistMediaWithArticleBean
import com.skyd.podaura.model.preference.player.MpvCacheDirPreference
import com.skyd.podaura.model.preference.player.MpvConfigDirPreference
import com.skyd.podaura.model.repository.player.IPlayerRepository
import com.skyd.podaura.model.repository.playlist.IAddToPlaylistRepository
import com.skyd.podaura.ui.player.PlayerCommand
import kotlinx.coroutines.Dispatchers
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
import org.junit.Assume.assumeTrue
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.nio.file.Files
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertEquals

class PlayerQueueDeletionTest {
    @Test
    fun bulkDeletionRemovesExactlyTheSelectedEntriesFromEngineAndSourcePlaylist() = runTest {
        assumeTrue("Requires the bundled MPV runtime", System.getenv("PODAURA_RUN_MPV_INTEGRATION") == "1")
        val directory = Files.createTempDirectory("podaura-queue-deletion").toFile()
        val preferences = object : DataStore<Preferences> {
            override val data = MutableStateFlow(preferencesOf(
                checkNotNull(MpvConfigDirPreference.key) to directory.resolve("config").path,
                checkNotNull(MpvCacheDirPreference.key) to directory.resolve("cache").path,
            ))
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
                transform(data.value).also { data.value = it }
        }
        val removedFromSource = CopyOnWriteArrayList<Pair<String, List<String>>>()
        startKoin { modules(module {
            single<DataStore<Preferences>> { preferences }
            single<IPlayerRepository> { object : IPlayerRepository {
                override fun requestLastPlayPosition(path: String, fallbackPath: String?) = flowOf(0L)
                override fun insertPlayHistory(path: String, duration: Long, articleId: String?) = flowOf(Unit)
                override fun updateLastPlayPosition(path: String, lastPlayPosition: Long, duration: Long, articleId: String?) = flowOf(Unit)
            } }
            single<IAddToPlaylistRepository> { object : IAddToPlaylistRepository {
                override fun getCommonPlaylists(medias: List<MediaUrlWithArticleIdBean>) = flowOf(emptyList<String>())
                override fun insertPlaylistMedia(playlistId: String, url: String, articleId: String?) = flowOf(true)
                override fun insertPlaylistMedias(toPlaylistId: String, medias: List<MediaUrlWithArticleIdBean>) = flowOf(Unit)
                override fun removeMediaFromPlaylist(playlistId: String, mediaList: List<MediaUrlWithArticleIdBean>) = flow {
                    removedFromSource += playlistId to mediaList.map { it.url }
                    emit(mediaList.size)
                }
            } }
        }) }
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        var coordinator: PlayerCoordinator? = null
        try {
            val player = PlayerCoordinator().also { coordinator = it }
            await { player.engineState.value == PlayerEngineState.AwaitingMedia }
            player.renderPlayer.setPropertyString("ao", "null")
            player.renderPlayer.setPropertyDouble("speed", 0.1)
            val bytes = checkNotNull(javaClass.getResourceAsStream("/player/queue-silence.m4a")).use { it.readBytes() }
            val items = (0..4).map { index ->
                PlaylistMediaWithArticleBean(
                    PlaylistMediaBean("saved-source", directory.resolve("$index.m4a").apply {
                        writeBytes(bytes)
                    }.path, null, index.toDouble(), 0L), null,
                )
            }
            player.onCommand(PlayerCommand.LoadList(items))
            await { player.engineState.value.isReady && player.playerState.value.playlist.size == 5 }
            player.onCommand(PlayerCommand.Paused(true))
            await { player.playerState.value.paused }
            val selected = listOf(items[0], items[2], items[3])
            player.onCommand(PlayerCommand.RemoveMediaFromPlaylist(selected))
            await { player.playerState.value.playlist.size == 2 && removedFromSource.size == 1 }
            val remaining = listOf(items[1], items[4])
            assertEquals(remaining.map { it.playlistMediaBean.url }, player.playerState.value.playlist.map { it.playlistMediaBean.url })
            assertEquals("saved-source" to selected.map { it.playlistMediaBean.url }, removedFromSource.single())
            // The current entry was included; removing the last entries must also settle to empty.
            player.onCommand(PlayerCommand.RemoveMediaFromPlaylist(remaining))
            await { player.playerState.value.playlist.isEmpty() && removedFromSource.size == 2 }
            assertEquals("saved-source" to remaining.map { it.playlistMediaBean.url }, removedFromSource.last())
        } finally {
            coordinator?.onCommand(PlayerCommand.Destroy)
            coordinator?.awaitDestroyed()
            Dispatchers.resetMain()
            stopKoin()
            directory.deleteRecursively()
        }
    }

    private suspend fun await(condition: () -> Boolean) = withContext(Dispatchers.IO) {
        withTimeout(10_000) { while (!condition()) delay(10) }
    }
}
