package com.skyd.podaura.ui.screen.media

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.ViewModelStore
import com.skyd.podaura.model.bean.MediaGroupBean
import com.skyd.podaura.model.repository.media.MediaRepository
import com.skyd.podaura.ui.screen.media.list.ListState
import com.skyd.podaura.ui.screen.media.list.MediaListIntent
import com.skyd.podaura.ui.screen.media.list.MediaListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.lang.reflect.Proxy
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class MediaRefreshTest {
    @Test
    fun groupRefreshReloadsAddedAndRemovedFilesInAnAlreadyLoadedTab() =
        runBlocking(Dispatchers.Main.immediate) {
            val root = Files.createTempDirectory("podaura-media-refresh")
            val path = root.toString()
            val preferences = object : DataStore<Preferences> {
                override val data = MutableStateFlow(emptyPreferences())
                override suspend fun updateData(transform: suspend (Preferences) -> Preferences) =
                    transform(data.value).also { data.value = it }
            }
            val store = ViewModelStore()
            startKoin { modules(module { single<DataStore<Preferences>> { preferences } }) }
            try {
                withTimeout(5_000) {
                    Files.writeString(root.resolve("original.mp3"), "media")
                    val repo = MediaRepository(Json, emptyDao(), emptyDao())
                    val media = MediaViewModel(repo).also { store.put("media", it) }
                    val list = MediaListViewModel(repo).also { store.put("list", it) }
                    // Let all intent collectors subscribe before sending the initial intents.
                    yield()
                    val initialVersion = media.viewState.value.groups.first().second
                    media.processIntent(MediaIntent.Init(path))
                    var version = media.viewState.first { it.groups.first().second != initialVersion }
                        .groups.first().second
                    suspend fun loadVersion() {
                        list.processIntent(MediaListIntent.Init(
                            path, MediaGroupBean.DefaultMediaGroup, isSubList = false, version = version,
                        ))
                    }
                    loadVersion()
                    list.viewState.first { it.listState is ListState.Success }

                    Files.writeString(root.resolve("added.mp3"), "media")
                    media.processIntent(MediaIntent.RefreshGroup(path))
                    version = media.viewState.first { it.groups.first().second != version }
                        .groups.first().second
                    loadVersion()
                    val added = list.viewState.first {
                        (it.listState as? ListState.Success)?.list?.size == 2
                    }.listState as ListState.Success
                    assertEquals(setOf("original.mp3", "added.mp3"), added.list.map { it.name }.toSet())

                    Files.delete(root.resolve("original.mp3"))
                    media.processIntent(MediaIntent.RefreshGroup(path))
                    version = media.viewState.first { it.groups.first().second != version }
                        .groups.first().second
                    loadVersion()
                    val removed = list.viewState.first {
                        (it.listState as? ListState.Success)?.list?.size == 1
                    }.listState as ListState.Success
                    assertEquals(listOf("added.mp3"), removed.list.map { it.name })
                }
            } finally {
                store.clear()
                stopKoin()
                root.toFile().deleteRecursively()
            }
        }

    private inline fun <reified T> emptyDao(): T = Proxy.newProxyInstance(
        T::class.java.classLoader, arrayOf(T::class.java),
    ) { _, method, _ ->
        when (method.name) {
            "getArticleWithFeedListByIds", "getFeedsIn" -> emptyList<Any>()
            else -> error("Unexpected database access: ${method.name}")
        }
    } as T
}
