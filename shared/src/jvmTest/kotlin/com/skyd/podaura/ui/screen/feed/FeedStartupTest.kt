package com.skyd.podaura.ui.screen.feed

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.ViewModelStore
import androidx.paging.LoadState
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingDataEvent
import androidx.paging.PagingDataPresenter
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.model.bean.feed.FeedViewBean
import com.skyd.podaura.model.bean.group.GroupBean
import com.skyd.podaura.model.bean.group.GroupWithFeedCount
import com.skyd.podaura.model.db.AppDatabase
import com.skyd.podaura.model.db.instance
import com.skyd.podaura.model.repository.article.DownloadArticleProtectionResolver
import com.skyd.podaura.model.repository.feed.FeedRepository
import com.skyd.podaura.model.repository.feed.RssHelper
import com.skyd.podaura.model.repository.feed.sheet.FeedSheetRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.io.File
import java.lang.reflect.Proxy
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

class FeedStartupTest {
    @Test
    fun firstEntryLoadsFeedsAndGroupsWithoutSwitchingTabs() = runBlocking(Dispatchers.Main.immediate) {
        val directory = Files.createTempDirectory("feed-startup-test").toFile()
        val database = AppDatabase.instance(
            Room.databaseBuilder<AppDatabase>(File(directory, "test.db").absolutePath)
                .setDriver(BundledSQLiteDriver())
        )
        val preferences = object : DataStore<Preferences> {
            override val data = MutableStateFlow(emptyPreferences())
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences) =
                transform(data.value).also { data.value = it }
        }
        val store = ViewModelStore()
        startKoin { modules(module { single<DataStore<Preferences>> { preferences } }) }
        try {
            val group = GroupBean("group", "Group", true, 10.0)
            val feed = FeedBean("https://example.com/feed", groupId = group.groupId)
            database.groupDao().setGroup(group)
            database.feedDao().setFeed(feed)
            val protection = DownloadArticleProtectionResolver(unused(), database.enclosureDao())
            val rssHelper = RssHelper { error("Unexpected network access") }
            val pagingConfig = PagingConfig(pageSize = 20)
            val sheetRepository = FeedSheetRepository(
                database.groupDao(), database.feedDao(), database.articleDao(), rssHelper,
                pagingConfig, protection,
            )
            val repository = FeedRepository(
                database.groupDao(), database.feedDao(), database.articleDao(), rssHelper,
                pagingConfig, sheetRepository, protection, unused(),
            )
            val viewModel = FeedViewModel(repository, unused()).also { store.put("feed", it) }

            // The first composition can dispatch Init before the intent collectors subscribe.
            // Do not yield here: switching tabs must not be needed to start loading.
            viewModel.processIntent(FeedIntent.Init)
            withTimeout(5_000) {
                val state = viewModel.viewState.first { it.listState is ListState.Success }
                val list = assertIs<ListState.Success>(state.listState).dataPagingDataFlow
                val items = list.loadedItems()
                assertEquals(listOf(group.groupId), items.filterIsInstance<GroupWithFeedCount>()
                    .map { it.group.groupId })
                assertEquals(listOf(feed.url), items.filterIsInstance<FeedViewBean>()
                    .map { it.feed.url })
                assertEquals(listOf(group.groupId), state.groups.loadedItems().map { it.groupId })

                // Returning to the same ViewModel must keep the existing Paging flows.
                viewModel.processIntent(FeedIntent.Init)
                yield()
                assertSame(list, assertIs<ListState.Success>(viewModel.viewState.value.listState)
                    .dataPagingDataFlow)
                assertSame(state.groups, viewModel.viewState.value.groups)
            }
        } finally {
            store.clear()
            stopKoin()
            database.close()
            directory.deleteRecursively()
        }
    }

    private suspend fun <T : Any> Flow<PagingData<T>>.loadedItems(): List<T> = coroutineScope {
        val presenter = object : PagingDataPresenter<T>(Dispatchers.Main.immediate) {
            override suspend fun presentPagingDataEvent(event: PagingDataEvent<T>) = Unit
        }
        val job = launch { collectLatest(presenter::collectFrom) }
        try {
            val states = presenter.loadStateFlow.filterNotNull().first {
                it.refresh !is LoadState.Loading
            }
            assertIs<LoadState.NotLoading>(states.refresh)
            presenter.snapshot().items
        } finally {
            job.cancelAndJoin()
        }
    }

    private inline fun <reified T> unused(): T = Proxy.newProxyInstance(
        T::class.java.classLoader, arrayOf(T::class.java),
    ) { _, method, _ -> error("Unexpected call: ${method.name}") } as T
}
