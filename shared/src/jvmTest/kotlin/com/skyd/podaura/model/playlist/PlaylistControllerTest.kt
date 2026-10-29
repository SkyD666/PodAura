package com.skyd.podaura.model.playlist

import androidx.paging.PagingConfig
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.skyd.podaura.model.bean.playlist.PlaylistBean
import com.skyd.podaura.model.bean.playlist.PlaylistMediaWithArticleBean
import com.skyd.podaura.model.db.AppDatabase
import com.skyd.podaura.model.db.instance
import com.skyd.podaura.model.preference.behavior.playlist.BasePlaylistSortByPreference.Companion.CREATE_TIME
import com.skyd.podaura.model.preference.behavior.playlist.BasePlaylistSortByPreference.Companion.MANUAL
import com.skyd.podaura.model.repository.playlist.PlaylistMediaRepository
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaylistControllerTest {
    @Test
    fun playlistOwnsSavingAndDispatchWithoutRequiringAPlayer() = runTest {
        val directory = Files.createTempDirectory("podaura-playlist-controller").toFile()
        val database = AppDatabase.instance(
            Room.databaseBuilder<AppDatabase>(directory.resolve("test.db").path)
                .setDriver(BundledSQLiteDriver())
        )
        try {
            val repository = PlaylistMediaRepository(
                database.articleDao(), database.enclosureDao(), database, PagingConfig(20),
            )
            val controller = PlaylistController(repository)
            val dao = database.playlistItemDao()
            val ids = listOf("a", "b", "c")
            database.playlistDao().createPlaylist(PlaylistBean("list", "List", 10.0, 0, false))
            ids.forEachIndexed { index, id -> dao.appendPlaylistMedia("list", id, null, index.toLong()) }

            controller.reorder("list", "c", "a", ids)
            assertEquals(listOf("c", "a", "b"), dao.getPlaylistMediaIdList("list"))

            var unavailableCalled = false
            val unavailable = object : PlaylistPlayback {
                override suspend fun synchronizePlaylist(
                    playlistId: String, readPlaylist: suspend () -> List<PlaylistMediaWithArticleBean>,
                ) = error("Reorder must use its transactional playback boundary")

                override suspend fun withQueueOrder(
                    playlistId: String, expectedOrder: List<String>?,
                    update: suspend ((List<String>) -> Unit) -> Unit,
                ): Boolean {
                    unavailableCalled = true
                    return false
                }
            }
            controller.reorder("list", "c", "b", playback = unavailable)
            assertTrue(unavailableCalled)
            assertEquals(ids, dao.getPlaylistMediaIdList("list"))
            assertFailsWith<IllegalStateException> {
                controller.reorder("list", "c", "a", ids, fromQueue = true, playback = unavailable)
            }
            assertEquals(ids, dao.getPlaylistMediaIdList("list"))

            var expectedQueue: List<String>? = null
            var orderSent = emptyList<String>()
            var reject = false
            var temporary = false
            val playback = object : PlaylistPlayback {
                override suspend fun synchronizePlaylist(
                    playlistId: String, readPlaylist: suspend () -> List<PlaylistMediaWithArticleBean>,
                ) = error("Reorder must use its transactional playback boundary")

                override suspend fun withQueueOrder(
                    playlistId: String, expectedOrder: List<String>?,
                    update: suspend ((List<String>) -> Unit) -> Unit,
                ): Boolean {
                    assertEquals(if (temporary) "" else "list", playlistId)
                    expectedQueue = expectedOrder
                    update { order ->
                        orderSent = order
                        if (reject) error("Playback rejected the order")
                    }
                    return true
                }
            }
            repository.setSort("list", CREATE_TIME, true)
            reject = true
            assertFailsWith<IllegalStateException> {
                controller.reorder("list", "c", "a", ids, playback = playback)
            }
            assertEquals(ids, dao.getPlaylistMediaIdList("list"))
            assertEquals(CREATE_TIME, database.playlistDao().getPlaylist("list")!!.mediaSortBy)

            reject = false
            controller.reorder("list", "c", "a", ids, fromQueue = true, playback = playback)
            assertEquals(ids, expectedQueue)
            assertEquals(listOf("c", "a", "b"), orderSent)
            assertEquals(orderSent, dao.getPlaylistMediaIdList("list"))
            assertEquals(MANUAL, database.playlistDao().getPlaylist("list")!!.mediaSortBy)

            temporary = true
            controller.reorder("", "b", "c", ids, fromQueue = true, playback = playback)
            assertEquals(listOf("a", "c", "b"), orderSent)
            assertEquals(listOf("c", "a", "b"), dao.getPlaylistMediaIdList("list"))
            assertEquals(null, database.playlistDao().getPlaylist(""))

            unavailableCalled = false
            controller.reorder("list", "a", "a", playback = unavailable)
            assertFalse(unavailableCalled, "An unchanged drag does not save or notify playback")
        } finally {
            database.close()
            directory.deleteRecursively()
        }
    }
}
