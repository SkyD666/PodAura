package com.skyd.podaura.model.playlist

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.paging.PagingConfig
import androidx.room3.Room
import androidx.room3.useWriterConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.skyd.podaura.model.bean.playlist.MediaUrlWithArticleIdBean
import com.skyd.podaura.model.bean.playlist.PlaylistBean
import com.skyd.podaura.model.bean.playlist.PlaylistMediaBean
import com.skyd.podaura.model.bean.playlist.PlaylistMediaWithArticleBean
import com.skyd.podaura.model.bean.playlist.movePlaylistItem
import com.skyd.podaura.model.db.AppDatabase
import com.skyd.podaura.model.db.instance
import com.skyd.podaura.model.preference.player.MpvCacheDirPreference
import com.skyd.podaura.model.preference.player.MpvConfigDirPreference
import com.skyd.podaura.model.repository.player.IPlayerRepository
import com.skyd.podaura.model.repository.playlist.IAddToPlaylistRepository
import com.skyd.podaura.model.repository.playlist.PlaylistMediaRepository
import com.skyd.podaura.ui.player.PlayerCommand
import com.skyd.podaura.ui.player.PlayerEvent
import com.skyd.podaura.ui.player.followPlaylistChanges
import com.skyd.podaura.model.preference.behavior.playlist.BasePlaylistSortByPreference.Companion.CREATE_TIME
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Job
import com.skyd.podaura.ui.player.coordinator.PlayerCoordinator
import com.skyd.podaura.ui.player.coordinator.PlayerEngineState
import com.skyd.podaura.ui.player.coordinator.isReady
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
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

class PlaylistOrderPlaybackTest {
    @Test
    fun nativeMovesAndSourceWritesKeepPlaybackAndRollbackOnFailure() = runTest {
        assumeTrue("Requires the bundled MPV runtime", System.getenv("PODAURA_RUN_MPV_INTEGRATION") == "1")
        val directory = Files.createTempDirectory("podaura-playlist-order").toFile()
        val preferences = object : DataStore<Preferences> {
            override val data = MutableStateFlow(preferencesOf(
                checkNotNull(MpvConfigDirPreference.key) to directory.resolve("config").path,
                checkNotNull(MpvCacheDirPreference.key) to directory.resolve("cache").path,
            ))
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
                transform(data.value).also { data.value = it }
        }
        val database = AppDatabase.instance(Room.databaseBuilder<AppDatabase>(directory.resolve("test.db").path)
            .setDriver(BundledSQLiteDriver()))
        val repository = PlaylistMediaRepository(database.articleDao(), database.enclosureDao(), database, PagingConfig(20))
        val orderController = PlaylistController(repository)
        database.playlistDao().createPlaylist(PlaylistBean("saved-source", "Source", 10.0, 0, false))
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
                    emit(mediaList.size)
                }
            } }
        }) }
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        var coordinator: PlayerCoordinator? = null
        var following: Job? = null
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
                    }.path, null, index.toDouble(), 0L).apply { sourceUrl = "https://example.invalid/$index" }, null,
                )
            }
            val ids = items.map { it.playlistMediaBean.stableUrl }
            for (id in ids) database.playlistItemDao().appendPlaylistMedia("saved-source", id, null, 0)
            player.onCommand(PlayerCommand.LoadList(items, startPath = items[2].playlistMediaBean.url))
            await { player.engineState.value.isReady && player.playerState.value.mediaStarted && player.playerState.value.playlist.size == 5 }
            player.onCommand(PlayerCommand.Paused(true))
            await { player.playerState.value.paused }
            val path = player.renderPlayer.getPropertyString("path")
            val position = player.renderPlayer.getPropertyDouble("time-pos")
            orderController.reorder("saved-source", ids[2], ids[0], ids, fromQueue = true, playback = player)
            val expected = movePlaylistItem(ids, ids[2], ids[0])
            await { player.playerState.value.playlist.map { it.playlistMediaBean.stableUrl } == expected }
            assertEquals(expected, database.playlistItemDao().getPlaylistMediaIdList("saved-source"))
            assertEquals(path, player.renderPlayer.getPropertyString("path"))
            assertEquals(position, player.renderPlayer.getPropertyDouble("time-pos"), 0.02)
            assertTrue(player.playerState.value.paused)
            assertEquals(0, player.playerState.value.playlistPosition)
            // Both UI entry points use the playlist controller; playback has no repository binding.
            orderController.reorder("saved-source", ids[4], ids[0], playback = player)
            val fromPage = movePlaylistItem(expected, ids[4], ids[0])
            assertEquals(fromPage, database.playlistItemDao().getPlaylistMediaIdList("saved-source"))
            assertEquals(path, player.renderPlayer.getPropertyString("path"))
            database.playlistDao().createPlaylist(PlaylistBean("inactive", "Other", 20.0, 0, false))
            for (id in ids) database.playlistItemDao().appendPlaylistMedia("inactive", id, null, 0)
            orderController.reorder("inactive", ids[2], ids[0], ids, playback = player)
            assertEquals(expected, database.playlistItemDao().getPlaylistMediaIdList("inactive"))
            assertEquals(fromPage, player.playerState.value.playlist.map { it.playlistMediaBean.stableUrl })
            // A stale queue request must not silently become a saved-list-only operation.
            assertFailsWith<IllegalStateException> {
                orderController.reorder("inactive", ids[0], ids[1], expected, fromQueue = true, playback = player)
            }
            assertEquals(expected, database.playlistItemDao().getPlaylistMediaIdList("inactive"))
            assertFailsWith<IllegalStateException> {
                orderController.reorder("saved-source", ids[0], ids[1], fromPage.dropLast(1), fromQueue = true, playback = player)
            }
            assertEquals(fromPage, database.playlistItemDao().getPlaylistMediaIdList("saved-source"))
            player.onCommand(PlayerCommand.Shuffle(true))
            await { player.playerState.value.shuffle }
            assertFailsWith<IllegalStateException> { orderController.reorder("saved-source", ids[0], ids[1], playback = player) }
            player.onCommand(PlayerCommand.Shuffle(false))
            await { !player.playerState.value.shuffle }
            await { player.playerState.value.playlist.map { it.playlistMediaBean.stableUrl } == fromPage }
            database.useWriterConnection { connection ->
                // Deferred foreign keys fail at COMMIT, after applyOrder has moved the native
                // queue. This exercises compensation, rather than only a failed SQL UPDATE.
                connection.usePrepared("CREATE TABLE ReorderParent (id INTEGER PRIMARY KEY)") { it.step() }
                connection.usePrepared("CREATE TABLE ReorderFailure (parent INTEGER REFERENCES ReorderParent(id) DEFERRABLE INITIALLY DEFERRED)") { it.step() }
                connection.usePrepared("CREATE TRIGGER reject_order AFTER UPDATE OF orderPosition ON PlaylistMedia BEGIN INSERT INTO ReorderFailure(parent) VALUES (1); END") { it.step() }
            }
            val failure = assertFailsWith<Exception> { orderController.reorder("saved-source", ids[0], ids[1], playback = player) }
            assertTrue(failure.message.orEmpty().contains("FOREIGN KEY", ignoreCase = true))
            assertEquals(fromPage, database.playlistItemDao().getPlaylistMediaIdList("saved-source"))
            assertEquals(fromPage, player.playerState.value.playlist.map { it.playlistMediaBean.stableUrl })
            assertEquals(path, player.renderPlayer.getPropertyString("path"))
            assertTrue(player.playerState.value.paused)
            database.useWriterConnection { connection ->
                connection.usePrepared("DROP TRIGGER reject_order") { it.step() }
            }
            following = player.followPlaylistChanges(orderController)
            val oldLoad = player.playerState.value.playlist.toList()
            fun queue() = player.playerState.value.playlist.map { it.playlistMediaBean.stableUrl }
            suspend fun saved() = repository.observePlaylistMediaList("saved-source").first()
                .map { it.playlistMediaBean.stableUrl }
            val addedPath = directory.resolve("added.m4a").apply { writeBytes(bytes) }.path
            // A new video window may have a surface but no ready render context yet. Queue-only
            // append must still be immediate, rather than waiting behind video startup.
            val surfaces = player.renderPlayer.javaClass.getDeclaredField("activeSurfaces").apply {
                isAccessible = true
            }
            val renderContext = player.renderPlayer.javaClass.getDeclaredField("renderContextLifecycle").apply {
                isAccessible = true
            }
            val previousRenderContext = renderContext.get(player.renderPlayer)
            surfaces.setInt(player.renderPlayer, 1)
            renderContext.set(player.renderPlayer, null)
            try {
                database.playlistItemDao().appendPlaylistMedia("saved-source", addedPath, null, 100)
                await { queue() == fromPage + addedPath }
            } finally {
                surfaces.setInt(player.renderPlayer, 0)
                renderContext.set(player.renderPlayer, previousRenderContext)
            }
            assertEquals(path, player.renderPlayer.getPropertyString("path"))
            assertEquals(position, player.renderPlayer.getPropertyDouble("time-pos"), 0.02)
            assertTrue(player.playerState.value.paused)
            // Preparation may finish after an edit has already reached this same source's queue.
            // Wait for the old load to actually apply, then require reconciliation without a new edit.
            val oldLoadApplied = CompletableDeferred<Unit>()
            val loadObserver = PlayerCoordinator.Observer { event ->
                if (event is PlayerEvent.Playlist && event.playlistId == "saved-source" &&
                    event.newPlaylist.values.map { it.playlistMediaBean.stableUrl } == fromPage
                ) oldLoadApplied.complete(Unit)
            }
            player.addObserver(loadObserver)
            try {
                player.onCommand(PlayerCommand.LoadList(oldLoad, startPath = path))
                withContext(Dispatchers.IO) { withTimeout(10_000) { oldLoadApplied.await() } }
                await { queue() == fromPage + addedPath }
                assertEquals(saved(), queue())
            } finally {
                player.removeObserver(loadObserver)
            }
            player.onCommand(PlayerCommand.Paused(true))
            await { player.playerState.value.paused }
            // A full load already waiting for a surface must finish before diffing the queue.
            val queuedPath = directory.resolve("queued.m4a").apply { writeBytes(bytes) }.path
            val pendingList = directory.resolve("pending.m3u8").apply {
                writeText("#EXTM3U\n" + (player.playerState.value.playlist.map { it.playlistMediaBean.url } + queuedPath).joinToString("\n") + "\n")
            }
            surfaces.setInt(player.renderPlayer, 1)
            renderContext.set(player.renderPlayer, null)
            try {
                player.renderPlayer.command("loadlist", pendingList.path, "replace")
                assertTrue(player.renderPlayer.hasPendingCommands)
                database.playlistItemDao().appendPlaylistMedia("saved-source", queuedPath, null, 150)
                val readStarted = CompletableDeferred<Unit>()
                val pendingSync = async(Dispatchers.IO) {
                    player.synchronizePlaylist("saved-source") {
                        readStarted.complete(Unit)
                        repository.observePlaylistMediaList("saved-source").first()
                    }
                }
                withContext(Dispatchers.IO) { delay(100) }
                assertTrue(!readStarted.isCompleted, "Must not diff an unapplied full load")
                surfaces.setInt(player.renderPlayer, 0)
                player.renderPlayer.javaClass.getDeclaredMethod("flushPendingCommands").apply {
                    isAccessible = true
                }.invoke(player.renderPlayer)
                pendingSync.await()
                await { queue() == fromPage + listOf(addedPath, queuedPath) }
                assertEquals(queue().size, queue().toSet().size, "Deferred sync must not duplicate an append")
            } finally {
                surfaces.setInt(player.renderPlayer, 0)
                renderContext.set(player.renderPlayer, previousRenderContext)
            }
            player.onCommand(PlayerCommand.Paused(true))
            await { player.playerState.value.paused }
            orderController.remove("saved-source", listOf(queuedPath))
            await { queue() == fromPage + addedPath }
            // Changing the saved source must be published even if native paths/order are equal.
            val sameItems = player.playerState.value.playlist.map { item ->
                item.copy(playlistMediaBean = item.playlistMediaBean.copy(playlistId = "same-content").apply {
                    sourceUrl = item.playlistMediaBean.sourceUrl
                })
            }
            database.playlistDao().createPlaylist(PlaylistBean("same-content", "Same", 30.0, 0, false))
            for (item in sameItems) database.playlistItemDao().appendPlaylistMedia(
                "same-content", item.playlistMediaBean.stableUrl, null, 0,
            )
            player.onCommand(PlayerCommand.LoadList(sameItems, startPath = path))
            await { player.playerState.value.playlistId == "same-content" }
            player.onCommand(PlayerCommand.Paused(true))
            await { player.playerState.value.paused }
            orderController.remove("same-content", listOf(addedPath))
            await { queue() == fromPage }
            player.onCommand(PlayerCommand.LoadList(items.sortedBy { fromPage.indexOf(it.playlistMediaBean.stableUrl) }, startPath = path))
            await { player.playerState.value.playlistId == "saved-source" && queue() == fromPage + addedPath }
            player.onCommand(PlayerCommand.Paused(true))
            await { player.playerState.value.paused }
            // Removal from either UI now commits to the source; following updates the queue.
            orderController.remove("saved-source", listOf(ids[1]))
            await { queue() == (fromPage + addedPath).filterNot { it == ids[1] } }
            assertEquals(path, player.renderPlayer.getPropertyString("path"))
            repository.setSort("saved-source", CREATE_TIME, false)
            val timeOrder = saved()
            await { queue() == timeOrder }
            assertEquals(addedPath, queue().first())
            // Reordering and following share the actor. The listener must not replay stale order.
            orderController.reorder("saved-source", addedPath, ids[3], timeOrder, fromQueue = true, playback = player)
            val dragOrder = saved()
            await { queue() == dragOrder }
            player.onCommand(PlayerCommand.Shuffle(true))
            await { player.playerState.value.shuffle }
            val randomOrder = queue()
            val extraPath = directory.resolve("extra.m4a").apply { writeBytes(bytes) }.path
            database.playlistItemDao().appendPlaylistMedia("saved-source", extraPath, null, 200)
            await { queue().size == randomOrder.size + 1 }
            assertEquals(randomOrder, queue().filterNot { it == extraPath })
            repository.setSort("saved-source", CREATE_TIME, false)
            player.onCommand(PlayerCommand.Shuffle(false))
            val unshuffled = saved()
            await { !player.playerState.value.shuffle && queue() == unshuffled }
            assertEquals(path, player.renderPlayer.getPropertyString("path"))
            assertTrue(player.playerState.value.paused)
            // Delete the current media; the native queue chooses its successor without resuming.
            orderController.remove("saved-source", listOf(ids[2]))
            await { ids[2] !in queue() && player.renderPlayer.getPropertyString("path") != path }
            assertTrue(player.playerState.value.paused)
            orderController.remove("saved-source", saved())
            await { queue().isEmpty() && player.playerState.value.paused }
            // An emptied source continues to be followed; adding an item does not auto-play it.
            database.playlistItemDao().appendPlaylistMedia("saved-source", addedPath, null, 300)
            await { queue() == listOf(addedPath) }
            assertTrue(player.playerState.value.paused)
            database.playlistDao().deletePlaylist("saved-source")
            await { queue().isEmpty() }
            player.onCommand(PlayerCommand.Paused(false))
            await { !player.playerState.value.paused }
            // Temporary queues are not attached to any saved source.
            val temporary = items.map { it.copy(playlistMediaBean = it.playlistMediaBean.copy(playlistId = "")) }
            player.onCommand(PlayerCommand.LoadList(temporary, startPath = path))
            await { player.playerState.value.playlistId == "" && player.playerState.value.playlist.size == 5 }
            val urls = temporary.map { it.playlistMediaBean.url }
            val temporaryQueueId = player.playerState.value.queueId
            assertTrue(temporaryQueueId.isNotEmpty())
            orderController.reorder("", urls[0], urls[4], urls, fromQueue = true, playback = player)
            await { player.playerState.value.playlist.map { it.playlistMediaBean.url } == movePlaylistItem(urls, urls[0], urls[4]) }
            assertEquals(path, player.renderPlayer.getPropertyString("path"))
            assertTrue(!player.playerState.value.paused)
            assertEquals(temporaryQueueId, player.playerState.value.queueId)
            database.playlistDao().createPlaylist(PlaylistBean("saved-source", "Recreated", 10.0, 0, false))
            database.playlistItemDao().appendPlaylistMedia("saved-source", addedPath, null, 0)
            // Even a delayed notification must be ignored after switching away from its source.
            player.synchronizePlaylist("saved-source") { error("Stale source must not be read") }
            assertEquals(movePlaylistItem(urls, urls[0], urls[4]), player.playerState.value.playlist.map { it.playlistMediaBean.url })

            player.onCommand(PlayerCommand.LoadList(temporary.drop(1), startPath = path))
            await { player.playerState.value.queueId != temporaryQueueId }
            assertEquals("", player.playerState.value.playlistId)

        } finally {
            coordinator?.onCommand(PlayerCommand.Destroy)
            coordinator?.awaitDestroyed()
            following?.join()
            assertTrue(following?.isCompleted != false)
            Dispatchers.resetMain()
            stopKoin()
            database.close()
            directory.deleteRecursively()
        }
    }

    private suspend fun await(condition: () -> Boolean) = withContext(Dispatchers.IO) {
        withTimeout(10_000) { while (!condition()) delay(10) }
    }
}
