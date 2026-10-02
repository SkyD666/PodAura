package com.skyd.podaura.ui.screen.article

import androidx.lifecycle.ViewModelStore
import androidx.paging.PagingConfig
import androidx.room3.Room
import androidx.room3.executeSQL
import androidx.room3.useWriterConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.skyd.downloader.download.DownloadConstraints
import com.skyd.podaura.model.bean.article.ArticleBean
import com.skyd.podaura.model.bean.article.EnclosureBean
import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.model.bean.playlist.MediaUrlWithArticleIdBean
import com.skyd.podaura.model.bean.playlist.PlaylistBean
import com.skyd.podaura.model.db.AppDatabase
import com.skyd.podaura.model.db.dao.playlist.PlaylistMediaDao
import com.skyd.podaura.model.db.instance
import com.skyd.podaura.model.download.ArticleDownloadSource
import com.skyd.podaura.model.download.DownloadInfoBean
import com.skyd.podaura.model.repository.BatchProgress
import com.skyd.podaura.model.repository.article.ArticleRepository
import com.skyd.podaura.model.repository.article.DownloadArticleProtectionResolver
import com.skyd.podaura.model.repository.download.IDownloadManager
import com.skyd.podaura.model.repository.download.SelectedArticleDownloader
import com.skyd.podaura.model.repository.feed.RssHelper
import com.skyd.podaura.model.repository.playlist.AddToPlaylistRepository
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class ArticleSelectionViewModelTest {
    private lateinit var directory: File
    private lateinit var database: AppDatabase
    private lateinit var viewModel: ArticleViewModel
    private lateinit var articleRepo: ArticleRepository
    private val store = ViewModelStore()
    private val feedUrl = "https://example.com/feed"

    @BeforeTest
    fun setUp() = runBlocking {
        directory = Files.createTempDirectory("article-selection-state-test").toFile()
        database = AppDatabase.instance(
            Room.databaseBuilder<AppDatabase>(File(directory, "test.db").absolutePath)
                .setDriver(BundledSQLiteDriver())
        )
        val manager = object : IDownloadManager {
            override suspend fun getAllDownloadTasks(): List<DownloadInfoBean> = emptyList()
            override suspend fun download(
                url: String, path: String, fileName: String?,
                articleDownloadSource: ArticleDownloadSource?, constraints: DownloadConstraints,
            ): String = error("Unexpected real download")
        }
        articleRepo = ArticleRepository(
            database.feedDao(), database.articleDao(), RssHelper {}, PagingConfig(20),
            DownloadArticleProtectionResolver(manager, database.enclosureDao()),
        )
        withContext(Dispatchers.Main) {
            viewModel = ArticleViewModel(
                articleRepo,
                AddToPlaylistRepository(database.playlistDao(), database.playlistItemDao()),
            )
            store.put("articles", viewModel)
        }
        database.feedDao().setFeed(FeedBean(url = feedUrl))
    }

    @AfterTest
    fun tearDown() = runBlocking {
        withContext(Dispatchers.Main) { store.clear() }
        database.close()
        directory.deleteRecursively()
        Unit
    }

    @Test
    fun overOneHundredRequiresConfirmationAndBusyStateRejectsChanges() = runBlocking {
        insertEpisodes(101)
        val gate = CompletableDeferred<Unit>()
        var queued = 0
        var preparations = 0
        val downloader = SelectedArticleDownloader(
            getTasks = { preparations++; gate.await(); emptyList() },
            fileExists = { true }, directory = { _, _ -> "/downloads" }, enqueue = { queued++ },
        )
        send(ArticleIntent.Selection.Enter("episode-0"))
        assertEquals(setOf("episode-0"), awaitSelection { it.active }.selectedIds)
        awaitSelection { it.active }
        send(ArticleIntent.Selection.SelectAll(listOf(feedUrl), emptyList(), emptyList(), 0))
        val selected = awaitSelection { !it.busy && it.selectedIds.size == 101 }
        send(ArticleIntent.Selection.Download(selected.selectedIds, downloader))
        awaitSelection { it.busy }
        send(ArticleIntent.Selection.Download(selected.selectedIds, downloader))
        send(ArticleIntent.Selection.Toggle("episode-0"))
        send(ArticleIntent.Selection.Clear)
        gate.complete(Unit)
        val confirmation = awaitSelection { !it.busy && it.confirmation != null }
        assertEquals(101, confirmation.selectedIds.size)
        assertEquals(1, preparations)
        assertEquals(0, queued)
        val plan = assertNotNull(confirmation.confirmation)
        assertEquals(101, plan.queueCount)

        val event = async(start = CoroutineStart.UNDISPATCHED) { awaitEvent() }
        send(ArticleIntent.Selection.ConfirmDownload(plan, downloader))
        val result = assertIs<ArticleEvent.SelectionResultEvent.Downloaded>(event.await()).result
        assertEquals(101, result.queuedCount)
        val completed = awaitSelection { it.active && !it.busy }
        assertEquals(101, queued)
        assertEquals(selected.selectedIds, completed.selectedIds)
    }

    @Test
    fun oneHundredQueuesDirectlyAndAllEpisodesStaySelected() = runBlocking {
        insertEpisodes(100)
        val downloader = SelectedArticleDownloader(
            getTasks = { emptyList() }, fileExists = { true }, directory = { _, _ -> "/downloads" },
            enqueue = { check(it.source.articleId != "episode-0") },
        )
        send(ArticleIntent.Selection.Enter())
        awaitSelection { it.active }
        repeat(100) { send(ArticleIntent.Selection.Toggle("episode-$it")) }
        val selected = awaitSelection { it.selectedIds.size == 100 }
        val event = async(start = CoroutineStart.UNDISPATCHED) { awaitEvent() }
        send(ArticleIntent.Selection.Download(selected.selectedIds, downloader))
        val result = assertIs<ArticleEvent.SelectionResultEvent.Downloaded>(event.await()).result
        val state = awaitSelection { !it.busy && it.selectedIds == selected.selectedIds }
        assertNull(state.confirmation)
        assertTrue(state.active)
        assertEquals(99, result.queuedCount)
    }

    @Test
    fun leavingDuringPreparationCannotRestoreOldSelection() = runBlocking {
        insertEpisodes(2)
        val started = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        val gate = CompletableDeferred<Unit>()
        val downloader = SelectedArticleDownloader(
            getTasks = {
                started.complete(Unit)
                try { gate.await() } finally { cancelled.complete(Unit) }
                emptyList()
            },
            fileExists = { true }, directory = { _, _ -> "/downloads" },
            enqueue = { error("Cancelled batch must not enqueue") },
        )
        send(ArticleIntent.Selection.Enter())
        awaitSelection { it.active }
        send(ArticleIntent.Selection.Toggle("episode-0"))
        val selected = awaitSelection { it.selectedIds == setOf("episode-0") }
        send(ArticleIntent.Selection.Download(selected.selectedIds, downloader))
        withTimeout(10_000) { started.await() }

        val stopped = async(start = CoroutineStart.UNDISPATCHED) { awaitEvent() }
        send(ArticleIntent.Selection.Exit)
        assertIs<ArticleEvent.SelectionResultEvent.Cancelled>(stopped.await())
        awaitSelection { !it.active }
        withTimeout(10_000) { cancelled.await() }
        send(ArticleIntent.Selection.Enter())
        awaitSelection { it.active }
        send(ArticleIntent.Selection.Toggle("episode-1"))
        val state = awaitSelection { it.selectedIds == setOf("episode-1") }
        assertFalse(state.busy)
        assertNull(state.confirmation)
        var queued = 0
        val nextDownloader = SelectedArticleDownloader(
            getTasks = { emptyList() }, fileExists = { true },
            directory = { _, _ -> "/downloads" }, enqueue = { queued++ },
        )
        val event = async(start = CoroutineStart.UNDISPATCHED) { awaitEvent() }
        send(ArticleIntent.Selection.Download(state.selectedIds, nextDownloader))
        assertIs<ArticleEvent.SelectionResultEvent.Downloaded>(event.await())
        awaitSelection { it.active && !it.busy }
        assertEquals(1, queued)
        send(ArticleIntent.Selection.Exit)
        assertEquals(ArticleSelectionState(), awaitSelection { !it.active })
    }

    @Test
    fun duplicateDownloadsBeforeBusyStateIsRenderedAreIgnored() = runBlocking {
        insertEpisodes(1)
        val gate = CompletableDeferred<Unit>()
        val started = CompletableDeferred<Unit>()
        var taskReads = 0
        var queued = 0
        val downloader = SelectedArticleDownloader(
            getTasks = {
                taskReads++
                started.complete(Unit)
                gate.await()
                emptyList()
            },
            fileExists = { true }, directory = { _, _ -> "/downloads" }, enqueue = { queued++ },
        )
        send(ArticleIntent.Selection.Enter())
        awaitSelection { it.active }
        send(ArticleIntent.Selection.Toggle("episode-0"))
        val selected = awaitSelection { it.selectedIds.isNotEmpty() }
        val event = async(start = CoroutineStart.UNDISPATCHED) { awaitEvent() }
        withContext(Dispatchers.Main.immediate) {
            repeat(2) {
                viewModel.processIntent(ArticleIntent.Selection.Download(selected.selectedIds, downloader))
            }
        }
        withTimeout(10_000) { started.await() }
        gate.complete(Unit)
        assertIs<ArticleEvent.SelectionResultEvent.Downloaded>(event.await())
        awaitSelection { it.active && !it.busy }
        assertEquals(1, queued)
        assertEquals(2, taskReads)
    }

    @Test
    fun preparationFailureEmitsEventAndKeepsSelectionForRetry() = runBlocking {
        insertEpisodes(1)
        val downloader = SelectedArticleDownloader(
            getTasks = { error("Cannot read download tasks") },
            fileExists = { true }, directory = { _, _ -> "/downloads" },
            enqueue = { error("Must not enqueue") },
        )
        send(ArticleIntent.Selection.Enter())
        awaitSelection { it.active }
        send(ArticleIntent.Selection.Toggle("episode-0"))
        val selected = awaitSelection { it.selectedIds.isNotEmpty() }
        val event = async(start = CoroutineStart.UNDISPATCHED) { awaitEvent() }
        send(ArticleIntent.Selection.Download(selected.selectedIds, downloader))
        assertEquals(
            ArticleEvent.SelectionResultEvent.Failed("Cannot read download tasks"),
            event.await(),
        )
        val state = awaitSelection { !it.busy }
        assertTrue(state.active)
        assertEquals(setOf("episode-0"), state.selectedIds)
    }

    @Test
    fun readAndFavoriteSetUniformStatesAndKeepHiddenAndFailedSelections() = runBlocking {
        insertEpisodes(2)
        database.articleDao().readArticle("episode-1", true)
        database.articleDao().favoriteArticle("episode-1", true)
        val ids = setOf("episode-0", "deleted", "episode-1")
        select(ids)
        val intents = listOf(
            ArticleIntent.Selection.Read(ids, true),
            ArticleIntent.Selection.Favorite(ids, true),
            ArticleIntent.Selection.Read(ids, false),
            ArticleIntent.Selection.Favorite(ids, false),
        )
        for (intent in intents) {
            val event = async(start = CoroutineStart.UNDISPATCHED) { awaitEvent() }
            send(intent)
            assertEquals(BatchProgress(3, successCount = 2, failedCount = 1),
                assertIs<ArticleEvent.SelectionResultEvent.Completed>(event.await()).result)
            assertEquals(ids, awaitSelection { !it.busy }.selectedIds)
            val articles = database.articleDao().getArticleListByIds(ids.toList())
            when (intent) {
                is ArticleIntent.Selection.Read -> {
                    assertTrue(articles.all { it.isRead == intent.read })
                    if (intent.read) {
                        val unread = FeedBean.newFilterMask(0, filterRead = false)
                        assertTrue(articleRepo.requestSelectionIds(listOf(feedUrl), emptyList(), emptyList(), unread).first().isEmpty())
                    }
                }
                is ArticleIntent.Selection.Favorite -> assertTrue(articles.all { it.isFavorite == intent.favorite })
                else -> error("Unexpected intent")
            }
        }
    }

    @Test
    fun batchUpdatesCommitChunksAndRetryOnlyFailedChunks() = runBlocking {
        insertEpisodes(901)
        val ids = (0..900).map { "episode-$it" }.toSet() + "deleted"
        val progress = articleRepo.readSelectedArticles(ids, true).toList()
        assertEquals(listOf(0, 900, 901), progress.map { it.successCount })
        assertEquals(BatchProgress(902, successCount = 901, failedCount = 1), progress.last())
        assertEquals(progress, articleRepo.favoriteSelectedArticles(ids, true).toList())

        database.useWriterConnection {
            it.executeSQL(
                "CREATE TRIGGER reject_read BEFORE UPDATE OF isRead ON Article " +
                        "WHEN NEW.articleId = 'episode-1' BEGIN SELECT RAISE(ABORT, 'Cannot update'); END"
            )
        }
        val subset = setOf("episode-0", "episode-1", "episode-2")
        val retried = articleRepo.readSelectedArticles(subset, false).toList()
        assertEquals(BatchProgress(3, successCount = 2, failedCount = 1), retried.last())
        val states = database.articleDao().getArticleListByIds(subset.toList()).associate { it.articleId to it.isRead }
        assertEquals(mapOf("episode-0" to false, "episode-1" to true, "episode-2" to false), states)
    }

    @Test
    fun playlistAppendIsAtomicAndDistinguishesDuplicatesFromDeletedPlaylists() = runBlocking {
        database.playlistDao().createPlaylist(PlaylistBean("list", "Test", 10.0, 0, false))
        val repo = AddToPlaylistRepository(database.playlistDao(), database.playlistItemDao())
        val media = MediaUrlWithArticleIdBean("https://example.com/missing.mp3", "deleted-article")
        assertEquals(BatchProgress(1, successCount = 1), repo.insertSelectedPlaylistMedias("list", listOf(media)).last())
        assertEquals(BatchProgress(1, skippedCount = 1), repo.insertSelectedPlaylistMedias("list", listOf(media)).last())
        assertEquals(BatchProgress(1, failedCount = 1), repo.insertSelectedPlaylistMedias("deleted-list", listOf(media)).last())

        val first = async { repo.insertSelectedPlaylistMedias("list", (1..10).map { MediaUrlWithArticleIdBean("first-$it", null) }).last() }
        val second = async { repo.insertSelectedPlaylistMedias("list", (1..10).map { MediaUrlWithArticleIdBean("second-$it", null) }).last() }
        assertEquals(10, first.await().successCount)
        assertEquals(10, second.await().successCount)
        val stored = database.playlistItemDao().getPlaylistMediaList("list")
        assertEquals((1..21).map { it * 10.0 }, stored.map { it.playlistMediaBean.orderPosition })
        assertNull(stored.first().playlistMediaBean.articleId)
    }

    @Test
    fun playlistPreparationFiltersMediaDeduplicatesAndFollowsArticleSort() = runBlocking {
        val oldAudio = "https://example.com/old-a.mp3"
        val oldVideo = "https://example.com/old-b.mp4"
        val newVideo = "https://example.com/new.mp4"
        for ((id, date, title) in listOf(Triple("old", 1L, "Zulu"), Triple("duplicate", 2L, "Beta"), Triple("new", 3L, "Alpha"), Triple("text", 4L, "Text"))) {
            database.articleDao().innerUpsertArticle(ArticleBean(id, feedUrl, title = title, date = date))
        }
        database.enclosureDao().upsert(listOf(
            EnclosureBean("old", oldAudio, 0, "audio/mpeg"),
            EnclosureBean("old", oldVideo, 0, "video/mp4"),
            EnclosureBean("duplicate", oldAudio, 0, "audio/mpeg"),
            EnclosureBean("new", newVideo, 0, "video/mp4"),
            EnclosureBean("text", "https://example.com/image.jpg", 0, "image/jpeg"),
        ))
        val ids = setOf("old", "text", "new", "deleted", "duplicate")
        select(ids)
        val event = async(start = CoroutineStart.UNDISPATCHED) { awaitEvent() }
        send(ArticleIntent.Selection.PreparePlaylist(ids, 0))
        val prepared = assertIs<ArticleEvent.SelectionResultEvent.PlaylistPrepared>(event.await()).result
        assertEquals(listOf(newVideo, oldAudio, oldVideo), prepared.medias.map { it.url })
        assertEquals("duplicate", prepared.medias[1].articleId)
        assertEquals(1, prepared.noMediaCount)
        assertEquals(1, prepared.failedCount)
        assertEquals(ids, awaitSelection { it.playlistMedias != null && !it.busy }.selectedIds)
        val dateAscending = FeedBean.newFilterMask(0, sort = FeedBean.SortBy.Date(true))
        assertEquals(listOf(oldAudio, oldVideo, newVideo), articleRepo.prepareSelectedPlaylistMedia(ids, dateAscending).first().medias.map { it.url })
        val titleAscending = FeedBean.newFilterMask(0, sort = FeedBean.SortBy.Title(true))
        assertEquals(listOf(newVideo, oldAudio, oldVideo), articleRepo.prepareSelectedPlaylistMedia(ids, titleAscending).first().medias.map { it.url })

        send(ArticleIntent.Selection.DismissPlaylist)
        awaitSelection { it.playlistMedias == null }
        val noMediaEvent = async(start = CoroutineStart.UNDISPATCHED) { awaitEvent() }
        send(ArticleIntent.Selection.PreparePlaylist(setOf("text"), 0))
        assertTrue(assertIs<ArticleEvent.SelectionResultEvent.PlaylistPrepared>(noMediaEvent.await()).result.medias.isEmpty())
        assertNull(awaitSelection { !it.busy }.playlistMedias)
    }

    @Test
    fun closingPlaylistKeepsAddingAndPartialFailuresCanBeRetriedWithoutDuplicates() = runBlocking {
        insertEpisodes(3)
        database.playlistDao().createPlaylist(PlaylistBean("list", "Test", 10.0, 0, false))
        val started = CompletableDeferred<Unit>()
        val gate = CompletableDeferred<Unit>()
        var fail = true
        val dao = object : PlaylistMediaDao by database.playlistItemDao() {
            override suspend fun appendPlaylistMedia(playlistId: String, url: String, articleId: String?, createTime: Long): Long {
                if (fail && articleId == "episode-1") {
                    started.complete(Unit)
                    gate.await()
                    error("One item failed")
                }
                return database.playlistItemDao().appendPlaylistMedia(playlistId, url, articleId, createTime)
            }
        }
        val repo = AddToPlaylistRepository(database.playlistDao(), dao)
        replacePlaylistRepository(repo)
        val ids = setOf("episode-0", "episode-1", "episode-2")
        select(ids)
        val prepared = async(start = CoroutineStart.UNDISPATCHED) { awaitEvent() }
        send(ArticleIntent.Selection.PreparePlaylist(ids, 0))
        assertIs<ArticleEvent.SelectionResultEvent.PlaylistPrepared>(prepared.await())
        val media = assertNotNull(awaitSelection { it.playlistMedias != null }.playlistMedias)
        val event = async(start = CoroutineStart.UNDISPATCHED) { awaitEvent() }
        send(ArticleIntent.Selection.AddToPlaylist("list", media))
        withTimeout(10_000) { started.await() }
        send(ArticleIntent.Selection.DismissPlaylist)
        assertTrue(awaitSelection { it.playlistMedias == null }.busy)
        gate.complete(Unit)
        assertEquals(BatchProgress(3, successCount = 2, failedCount = 1),
            assertIs<ArticleEvent.SelectionResultEvent.Completed>(event.await()).result)
        assertEquals(ids, awaitSelection { !it.busy }.selectedIds)
        assertEquals(2, database.playlistItemDao().getPlaylistMediaList("list").size)
        fail = false
        val retry = async(start = CoroutineStart.UNDISPATCHED) { awaitEvent() }
        send(ArticleIntent.Selection.AddToPlaylist("list", media + media))
        assertEquals(BatchProgress(3, successCount = 1, skippedCount = 2),
            assertIs<ArticleEvent.SelectionResultEvent.Completed>(retry.await()).result)
        awaitSelection { !it.busy }
        assertEquals(listOf("list"), repo.getCommonPlaylists(media + media).first())
        assertEquals(3, database.playlistItemDao().getPlaylistMediaList("list").size)
    }

    @Test
    fun exitingPlaylistBatchKeepsCompletedMediaAndReportsUnprocessedCount() = runBlocking {
        insertEpisodes(3)
        database.playlistDao().createPlaylist(PlaylistBean("list", "Test", 10.0, 0, false))
        val started = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        val dao = object : PlaylistMediaDao by database.playlistItemDao() {
            override suspend fun appendPlaylistMedia(playlistId: String, url: String, articleId: String?, createTime: Long): Long {
                if (articleId == "episode-1") {
                    started.complete(Unit)
                    try { CompletableDeferred<Unit>().await() } finally { cancelled.complete(Unit) }
                }
                return database.playlistItemDao().appendPlaylistMedia(playlistId, url, articleId, createTime)
            }
        }
        replacePlaylistRepository(AddToPlaylistRepository(database.playlistDao(), dao))
        val ids = setOf("episode-0", "episode-1", "episode-2")
        select(ids)
        val media = articleRepo.prepareSelectedPlaylistMedia(ids, 0).first().medias
        send(ArticleIntent.Selection.AddToPlaylist("list", media))
        withTimeout(10_000) { started.await() }
        awaitSelection { it.progress?.successCount == 1 }
        val event = async(start = CoroutineStart.UNDISPATCHED) { awaitEvent() }
        send(ArticleIntent.Selection.Exit)
        val progress = assertNotNull(assertIs<ArticleEvent.SelectionResultEvent.Cancelled>(event.await()).progress)
        assertEquals(1, progress.successCount)
        assertEquals(2, progress.remainingCount)
        awaitSelection { !it.active }
        withTimeout(10_000) { cancelled.await() }
        assertEquals(1, database.playlistItemDao().getPlaylistMediaList("list").size)
    }

    @Test
    fun commonPlaylistsRequireEveryMediaAcrossSqlChunks() = runBlocking {
        for (id in listOf("full", "partial")) {
            database.playlistDao().createPlaylist(PlaylistBean(id, id, 10.0, 0, false))
        }
        val repo = AddToPlaylistRepository(database.playlistDao(), database.playlistItemDao())
        val medias = (0..900).map { MediaUrlWithArticleIdBean("https://example.com/$it.mp3", null) }
        repo.insertSelectedPlaylistMedias("full", medias).last()
        repo.insertSelectedPlaylistMedias("partial", medias.take(900)).last()
        assertEquals(listOf("full"), repo.getCommonPlaylists(medias + medias.take(2)).first())
        assertTrue(repo.getCommonPlaylists(emptyList()).first().isEmpty())
    }

    @Test
    fun exitingDownloadBatchReportsQueuedAndRemainingArticles() = runBlocking {
        insertEpisodes(3)
        val started = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        var queued = 0
        val downloader = SelectedArticleDownloader(
            getTasks = { emptyList() }, fileExists = { true }, directory = { _, _ -> "/downloads" },
            enqueue = {
                if (it.source.articleId == "episode-1") {
                    started.complete(Unit)
                    try { CompletableDeferred<Unit>().await() } finally { cancelled.complete(Unit) }
                }
                queued++
            },
        )
        val ids = setOf("episode-0", "episode-1", "episode-2")
        select(ids)
        send(ArticleIntent.Selection.Download(ids, downloader))
        withTimeout(10_000) { started.await() }
        awaitSelection { it.progress?.successCount == 1 }
        val event = async(start = CoroutineStart.UNDISPATCHED) { awaitEvent() }
        send(ArticleIntent.Selection.Exit)
        val progress = assertNotNull(assertIs<ArticleEvent.SelectionResultEvent.Cancelled>(event.await()).progress)
        assertEquals(1, progress.successCount)
        assertEquals(2, progress.remainingCount)
        awaitSelection { !it.active }
        withTimeout(10_000) { cancelled.await() }
        assertEquals(1, queued)
    }

    private suspend fun select(ids: Set<String>) {
        send(ArticleIntent.Selection.Enter())
        awaitSelection { it.active }
        for (id in ids) send(ArticleIntent.Selection.Toggle(id))
        awaitSelection { it.selectedIds == ids }
    }

    private suspend fun replacePlaylistRepository(repo: AddToPlaylistRepository) = withContext(Dispatchers.Main) {
        viewModel = ArticleViewModel(articleRepo, repo)
        store.put("articles", viewModel)
    }

    private suspend fun awaitEvent(): ArticleEvent = withTimeout(10_000) {
        viewModel.singleEvent.first()
    }

    private suspend fun send(intent: ArticleIntent) = withContext(Dispatchers.Main.immediate) {
        viewModel.processIntent(intent)
    }

    private suspend fun awaitSelection(predicate: (ArticleSelectionState) -> Boolean): ArticleSelectionState =
        withTimeout(10_000) {
            viewModel.viewState.first { predicate(it.selectionState) }.selectionState
        }

    private suspend fun insertEpisodes(count: Int) {
        repeat(count) {
            val id = "episode-$it"
            database.articleDao().innerUpsertArticle(ArticleBean(articleId = id, feedUrl = feedUrl))
            database.enclosureDao().upsert(listOf(EnclosureBean(id, "https://example.com/$id.mp3", 0, "audio/mpeg")))
        }
    }
}
