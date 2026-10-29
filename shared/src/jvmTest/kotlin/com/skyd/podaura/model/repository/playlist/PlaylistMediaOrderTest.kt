package com.skyd.podaura.model.repository.playlist

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.paging.PagingConfig
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.skyd.podaura.model.bean.playlist.PlaylistBean
import com.skyd.podaura.model.bean.playlist.mergePlaylistOrder
import com.skyd.podaura.model.db.AppDatabase
import com.skyd.podaura.model.db.instance
import com.skyd.podaura.model.preference.behavior.playlist.BasePlaylistSortByPreference.Companion.CREATE_TIME
import com.skyd.podaura.model.preference.behavior.playlist.BasePlaylistSortByPreference.Companion.MANUAL
import com.skyd.podaura.model.preference.behavior.playlist.PlaylistMediaSortAscPreference
import com.skyd.podaura.model.preference.behavior.playlist.PlaylistMediaSortByPreference
import com.skyd.podaura.model.preference.dataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PlaylistMediaOrderTest {
    @Test
    fun migratesLegacyRulesIndependentlyAndCommitsVisibleTimeOrderAtomically() = runTest {
        val dir = Files.createTempDirectory("playlist-sort-test").toFile()
        val preferences = object : DataStore<Preferences> {
            override val data = MutableStateFlow(preferencesOf(
                PlaylistMediaSortByPreference.key to CREATE_TIME,
                PlaylistMediaSortAscPreference.key to false,
            ))
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences) =
                transform(data.value).also { data.value = it }
        }
        startKoin { modules(module { single<DataStore<Preferences>> { preferences } }) }
        // The application-level delegate can retain a store from an earlier test class.
        val previousPreferences = dataStore.data.first()
        dataStore.updateData { preferences.data.value }
        val db = AppDatabase.instance(Room.databaseBuilder<AppDatabase>(dir.resolve("test.db").path).setDriver(BundledSQLiteDriver()))
        try {
            val dao = db.playlistDao()
            val media = db.playlistItemDao()
            val repo = PlaylistMediaRepository(db.articleDao(), db.enclosureDao(), db, PagingConfig(20))
            for (id in listOf("a", "b", "new")) {
                dao.createPlaylist(PlaylistBean(id, id, 10.0, 0, false, if (id == "new") MANUAL else null))
                for ((url, time) in listOf("one" to 30L, "two" to 10L, "three" to 20L)) {
                    media.appendPlaylistMedia(id, "https://example.invalid/$url", null, time)
                }
            }
            fun url(name: String) = "https://example.invalid/$name"
            suspend fun order(id: String) = repo.requestPlaylistMediaList(id).first().map { it.playlistMediaBean.url }
            assertEquals(listOf("one", "three", "two").map(::url), order("a"))
            assertEquals(CREATE_TIME, dao.getPlaylist("b")!!.mediaSortBy)
            assertEquals(false, dao.getPlaylist("b")!!.mediaSortAsc)
            assertEquals(listOf("one", "two", "three").map(::url), order("new"))
            repo.reorderPlaylistMedia("a", url("one"), url("three"))
            assertEquals(listOf("three", "one", "two").map(::url), order("a"))
            assertEquals(MANUAL, dao.getPlaylist("a")!!.mediaSortBy)
            assertEquals(CREATE_TIME, dao.getPlaylist("b")!!.mediaSortBy)
            repo.setSort("a", CREATE_TIME, true)
            assertEquals(listOf("two", "three", "one").map(::url), order("a"))
            assertEquals(false, dao.getPlaylist("b")!!.mediaSortAsc)
            assertFailsWith<IllegalStateException> {
                repo.reorderPlaylistMedia("a", url("one"), url("two")) { error("Engine rejected the move") }
            }
            assertEquals(CREATE_TIME, dao.getPlaylist("a")!!.mediaSortBy)
            assertEquals(listOf("two", "three", "one").map(::url), order("a"))
            // A queue can lack DB items and retain deleted items. Only shared slots change.
            repo.reorderPlaylistMedia("a", url("one"), url("three"), listOf(url("one"), url("deleted"), url("three")), fromQueue = true)
            assertEquals(listOf("two", "three", "one").map(::url), order("a"))
            assertEquals(3, media.getPlaylistMediaList("a").size)
            val displayed = listOf("two", "three", "one").map(::url)
            repo.reorderPlaylistMedia("a", url("one"), url("two"))
            // Another window has changed the DB order since this page started its drag.
            repo.reorderPlaylistMedia("a", url("two"), url("three"), displayed)
            assertEquals(listOf("three", "two", "one").map(::url), order("a"))
            var applied = emptyList<String>()
            repo.reorderPlaylistMedia("a", url("three"), url("one"), listOf(url("three"), url("one"))) {
                applied = it
            }
            assertEquals(listOf("one", "two", "three").map(::url), order("a"))
            assertEquals(order("a"), applied, "A partial page snapshot must synchronize the full saved order")
            assertEquals(listOf("x", "b", "y", "a"), mergePlaylistOrder(listOf("x", "a", "y", "b"), listOf("b", "missing", "a")))
        } finally {
            db.close()
            dataStore.updateData { previousPreferences }
            stopKoin()
            dir.deleteRecursively()
        }
    }
}
