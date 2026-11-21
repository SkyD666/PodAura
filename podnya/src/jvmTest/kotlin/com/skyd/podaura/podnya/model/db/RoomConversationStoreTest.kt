package com.skyd.podaura.podnya.model.db

import com.skyd.podaura.api.model.bean.article.ArticleSearchScope
import com.skyd.podaura.podnya.capabilities.articles.model.ArticleHistoryCodec
import com.skyd.podaura.podnya.capabilities.articles.model.bean.*
import com.skyd.podaura.podnya.capabilities.feeds.model.FeedHistoryCodec
import com.skyd.podaura.podnya.capabilities.feeds.model.bean.*
import com.skyd.podaura.podnya.core.*
import com.skyd.podaura.podnya.model.bean.AssistantReplyContent
import com.skyd.podaura.podnya.model.bean.conversation.*
import com.skyd.podaura.podnya.model.repository.conversation.RoomConversationStore
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.nio.file.Files
import kotlin.test.*
import kotlin.uuid.Uuid

class RoomConversationStoreTest {
    private val codec = ConversationRecordCodec(listOf(ArticleHistoryCodec, FeedHistoryCodec))

    @Test
    fun realDiskRoundTripRecoversWorkspaceCardsNavigationDraftAndInterruptedReply() = runBlocking {
        val directory = Files.createTempDirectory("podnya-room").toFile()
        val path = directory.resolve("history.db").path
        val id = Uuid.random()
        val article = AssistantArticle(articleId = "article-id", title = "Title", sourceName = "Source", dateMillis = 123, snippet = "Full snippet")
        val articleContent = ArticleSearchContent.recommendations(ValidatedAssistantAnswer(
            kind = AssistantAnswerKind.RESULTS,
            text = "Recommendation",
            articles = listOf(RecommendedArticle(article = article, reason = "Article reason")),
            resultsChanged = true,
            hadCandidates = true,
        ))
        val feeds = FeedSearchContent(
            feeds = listOf(RecommendedFeed(
                feed = AssistantFeed(feedId = "F1", title = "Feed title", description = "Description", muted = true),
                reason = "Feed reason",
                feedUrl = "https://private.example/feed.xml",
            )),
            hadCandidates = true,
            resultsChanged = true,
        )
        val context = ArticleSearchContext(ArticleSearchScope.Restricted(feedUrls = setOf("https://example/feed"), articleIds = setOf("article-id")))
        val position = ConversationPosition(turnId = 1, itemKey = "1-article-article-id", offset = 31, followLatest = false)
        val meta = AssistantSessionState(
            id = id,
            title = "Manual title",
            manuallyNamed = true,
            draft = "draft text",
            selectionStart = 2,
            selectionEnd = 7,
            editorId = "editor",
            draftRevision = 8,
            context = context,
            modelConfigId = "model-config",
            destinations = setOf("https://model.example/prefix/"),
            position = position,
            lastTurnId = 1,
            lastStatus = AssistantTurnStatus.RUNNING,
        )
        val turn = AssistantSessionTurn(
            id = 1,
            question = "Original question",
            reply = "Partial reply",
            context = context,
            source = AssistantReplySource(
                configId = "model-config",
                revision = 5,
                baseUrl = "https://api.openai.com/v1/",
                modelId = "gpt-6.1-terra",
                reasoningEffort = "max",
            ),
            content = AssistantReplyContent(parts = listOf(articleContent, feeds), kind = AssistantAnswerKind.RESULTS, text = "Combined"),
        )
        val first = RoomConversationStore(codec = codec, path = { path })
        try {
            first.open()
            first.commit(metadata = meta, turn = turn, workspace = ConversationWorkspace(selectedId = id, lastModelId = "model-config"))
            assertEquals(AssistantTurnStatus.RUNNING, first.summaries(offset = 0, limit = 10).single().status)
            first.close()
            val reopened = RoomConversationStore(codec = codec, path = { path })
            try {
                reopened.open()
                val saved = reopened.metadata(id)!!
                assertEquals(meta.copy(lastStatus = AssistantTurnStatus.INTERRUPTED), saved)
                assertEquals(ConversationWorkspace(selectedId = id, lastModelId = "model-config"), reopened.workspace())
                val recovered = reopened.tail(id = id, limit = 1).single()
                assertEquals(AssistantTurnStatus.INTERRUPTED, recovered.status)
                assertEquals("Partial reply", recovered.reply)
                assertEquals(turn.source, recovered.source)
                assertEquals(context, recovered.context)
                val aggregate = recovered.content as AssistantReplyContent
                val recoveredArticle = aggregate.parts[0] as ArticleSearchContent
                assertEquals(articleContent.answer, recoveredArticle.answer)
                val recoveredFeeds = aggregate.parts[1] as FeedSearchContent
                assertEquals(feeds, recoveredFeeds)
                assertFalse(recoveredFeeds.historySnapshot().payload.contains("private.example"))
                reopened.delete(id = id, workspace = ConversationWorkspace())
                assertTrue(reopened.tail(id = id, limit = 10).isEmpty())
            } finally {
                reopened.close()
            }
        } finally {
            first.close()
            directory.deleteRecursively()
        }
    }

    @Test
    fun pagingQueriesAreBoundedAndFailedTransactionDoesNotChangeSummary() = runBlocking {
        val directory = Files.createTempDirectory("podnya-room-pages").toFile()
        val path = directory.resolve("history.db").path
        var database: ConversationDatabase? = null
        val store = RoomConversationStore(codec = codec, path = { path }, build = {
            ConversationDatabase.builder(it).build().also { database = it }
        })
        try {
            store.open()
            val id = Uuid.random()
            repeat(150) { index ->
                val turn = AssistantSessionTurn(id = index + 1L, question = "Question $index", status = AssistantTurnStatus.COMPLETED)
                store.commit(metadata = AssistantSessionState(id = id, title = "Long", lastTurnId = turn.id, lastStatus = turn.status), turn = turn)
            }
            repeat(121) { index ->
                store.commit(metadata = AssistantSessionState(title = "Draft $index", draft = "draft", activityAt = index.toLong()))
            }
            assertEquals(50, store.summaries(offset = 0, limit = 50).size)
            assertEquals(22, store.summaries(offset = 100, limit = 50).size)
            val window = store.window(id = id, anchor = 75, limit = 40)
            assertEquals(40, window.turns.size)
            assertTrue(window.hasBefore && window.hasAfter)
            assertTrue(window.turns.any { it.id == 75L })
            assertEquals((145L..150L).toList(), store.tail(id = id, limit = 6).map { it.id })
            assertEquals((70L..75L).toList(), store.tail(id = id, limit = 6, throughTurnId = 75L).map { it.id })
            val dao = database!!.conversationDao()
            val original = dao.metadata(id.toString())!!
            val originalTurn = dao.turn(id = id.toString(), turnId = 75)!!
            assertFails {
                dao.commit(
                    metadata = original.copy(title = "Should roll back"),
                    turns = listOf(
                        originalTurn.copy(question = "Should roll back"),
                        ConversationTurnRecord(
                            conversationId = Uuid.random().toString(),
                            turnId = 1,
                            question = "invalid FK",
                            createdAt = 0,
                        ),
                    ),
                    workspace = null,
                )
            }
            assertEquals(original, dao.metadata(id.toString()))
            assertEquals(originalTurn, dao.turn(id = id.toString(), turnId = 75))
        } finally {
            store.close()
            directory.deleteRecursively()
        }
    }

    @Test
    fun recentCapabilityTurnsSkipMalformedAndUnrelatedRecordsAndLimitMatches() = runBlocking {
        val directory = Files.createTempDirectory("podnya-room-capability").toFile()
        val path = directory.resolve("history.db").path
        val store = RoomConversationStore(codec = codec, path = { path })
        val metadata = AssistantSessionState()
        fun record(capabilityId: String): String = Json.encodeToString(CapabilityRecord(
            capabilityId = capabilityId,
            version = 1,
            payload = "{}",
            fallback = "",
        ))
        try {
            store.open()
            store.commit(metadata = metadata)
            store.commitTurns(
                id = metadata.id,
                turns = listOf(record("example"), record("other"), record("example"), "{invalid", null)
                    .mapIndexed { index, raw ->
                        AssistantSessionTurn(
                            id = index + 1L,
                            question = "Question $index",
                            content = raw?.let { UnreadableContent(raw = it, fallback = "") },
                        )
                    },
            )
            assertEquals(listOf(1L, 3L), store.recentCapabilityTurns(
                id = metadata.id,
                capabilityId = "example",
                limit = 2,
            ).map { it.id })
            assertEquals(listOf(3L), store.recentCapabilityTurns(
                id = metadata.id,
                capabilityId = "example",
                limit = 1,
            ).map { it.id })
        } finally {
            store.close()
            directory.deleteRecursively()
        }
    }

    @Test
    fun unknownAndCorruptPayloadsSurviveLocalEditsAndNeverBecomeSearchOrNavigation() = runBlocking {
        val unknown = Json.encodeToString(CapabilityRecord(capabilityId = "future", version = 8, payload = "{secret-shaped-data}", fallback = "Readable result"))
        val content = codec.content(unknown) as UnreadableContent
        assertEquals("Readable result", content.fallback)
        assertNull(content.historySnapshot())
        assertEquals(unknown, codec.content(content))
        val context = codec.context(unknown)
        assertIs<UnreadableContext>(context)
        assertEquals(unknown, codec.context(context))
        for (failure in listOf(ArticleSearchError.LOCAL_SEARCH, FeedSearchError.LOCAL_SEARCH)) {
            val turn = AssistantSessionTurn(id = 1, question = "question", status = AssistantTurnStatus.FAILED, error = failure)
            assertEquals(failure, codec.decode(codec.encode(id = Uuid.random(), value = turn)).error)
        }
        val broken = "{invalid"
        assertEquals(broken, codec.content(codec.content(broken)))
        val nested = AssistantReplyContent(parts = listOf(content), kind = AssistantAnswerKind.RESULTS, text = "Summary")
        val decoded = codec.content(codec.content(nested)) as AssistantReplyContent
        assertEquals(unknown, (decoded.parts.single() as UnreadableContent).raw)
    }

    @Test
    fun exclusiveOwnershipPrecedesRecoveryAndCanBeRetriedAfterRelease() = runBlocking {
        val directory = Files.createTempDirectory("podnya-room-lock").toFile()
        val path = directory.resolve("history.db").path
        val first = RoomConversationStore(codec = codec, path = { path })
        val second = RoomConversationStore(codec = codec, path = { path })
        try {
            first.open()
            val meta = AssistantSessionState(lastTurnId = 1, lastStatus = AssistantTurnStatus.RUNNING)
            first.commit(metadata = meta, turn = AssistantSessionTurn(id = 1, question = "active"))
            val failure = assertFailsWith<ConversationStorageException> { second.open() }
            assertEquals(ConversationStorageState.OCCUPIED, failure.state)
            assertEquals(AssistantTurnStatus.RUNNING, first.tail(id = meta.id, limit = 1).single().status)
            first.close()
            second.open()
            assertEquals(AssistantTurnStatus.INTERRUPTED, second.tail(id = meta.id, limit = 1).single().status)
        } finally {
            second.close()
            first.close()
            directory.deleteRecursively()
        }
    }
    @Test
    fun newerAndDamagedDatabasesAreNotReplaced() = runBlocking {
        val directory = Files.createTempDirectory("podnya-room-version").toFile()
        val path = directory.resolve("history.db").path
        val store = RoomConversationStore(codec = codec, path = { path })
        try {
            store.open()
            val meta = AssistantSessionState(draft = "preserve")
            store.commit(metadata = meta)
            store.close()
            val driver = androidx.sqlite.driver.bundled.BundledSQLiteDriver()
            driver.open(path).use { connection ->
                connection.prepare("PRAGMA user_version = 99").use { it.step() }
            }
            val failure = assertFailsWith<ConversationStorageException> { store.open() }
            assertEquals(ConversationStorageState.NEWER_VERSION, failure.state)
            driver.open(path).use { connection ->
                connection.prepare("PRAGMA user_version").use {
                    assertTrue(it.step())
                    assertEquals(99L, it.getLong(0))
                }
            }
            val brokenPath = directory.resolve("broken.db")
            val bytes = "not a SQLite database".toByteArray()
            brokenPath.writeBytes(bytes)
            val broken = RoomConversationStore(codec = codec, path = { brokenPath.path })
            assertEquals(ConversationStorageState.OPEN_FAILED, assertFailsWith<ConversationStorageException> { broken.open() }.state)
            assertContentEquals(bytes, brokenPath.readBytes())
            broken.close()
        } finally {
            store.close()
            directory.deleteRecursively()
        }
    }

    @Test
    fun operatingSystemLockIsReleasedAfterOwnerProcessIsKilled() {
        val directory = Files.createTempDirectory("podnya-process-lock").toFile()
        val path = directory.resolve("history.db").path
        val classpath = generateSequence(javaClass.classLoader) { it.parent }
            .filterIsInstance<java.net.URLClassLoader>().flatMap { it.urLs.asSequence() }
            .map { java.io.File(it.toURI()).path }.toList()
            .plus(System.getProperty("java.class.path").split(java.io.File.pathSeparator))
            .distinct().joinToString(java.io.File.pathSeparator)
        val process = ProcessBuilder(
            java.io.File(System.getProperty("java.home"), "bin/java").path,
            "-cp", classpath, ConversationLockProcess::class.java.name, path,
        ).redirectErrorStream(true).start()
        try {
            assertEquals("owned", process.inputStream.bufferedReader().readLine())
            val contender = ConversationDatabaseLock(path)
            assertEquals(ConversationStorageState.OCCUPIED, assertFailsWith<ConversationStorageException> { contender.acquire() }.state)
            process.destroyForcibly()
            assertTrue(process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS))
            contender.acquire()
            contender.release()
        } finally {
            process.destroyForcibly()
            process.waitFor()
            directory.deleteRecursively()
        }
    }

}


object ConversationLockProcess {
    @JvmStatic
    fun main(args: Array<String>) {
        val lock = ConversationDatabaseLock(args.single())
        lock.acquire()
        println("owned")
        readlnOrNull()
        lock.release()
    }
}
