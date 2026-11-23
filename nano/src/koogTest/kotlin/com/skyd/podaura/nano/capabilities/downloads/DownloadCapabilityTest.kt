package com.skyd.podaura.nano.capabilities.downloads

import com.skyd.podaura.api.model.bean.download.AttachmentMediaKind
import com.skyd.podaura.api.model.bean.article.ArticleQuery
import com.skyd.podaura.api.model.bean.article.ArticleRecord
import com.skyd.podaura.api.model.bean.article.ArticleSearchScope
import com.skyd.podaura.api.model.bean.download.AssistantDownloadArticle
import com.skyd.podaura.api.model.bean.download.AssistantDownloadAttachment
import com.skyd.podaura.api.model.bean.download.AssistantDownloadScope
import com.skyd.podaura.api.model.bean.download.AssistantDownloadTarget
import com.skyd.podaura.api.model.bean.feed.FeedQuery
import com.skyd.podaura.api.model.bean.feed.FeedRecord
import com.skyd.podaura.api.model.repository.IArticleRepository
import com.skyd.podaura.api.model.repository.IAssistantDownloadRepository
import com.skyd.podaura.api.model.repository.IFeedRepository
import com.skyd.podaura.nano.capabilities.ArticleTargetSource
import com.skyd.podaura.nano.capabilities.articles.articleSearchModule
import com.skyd.podaura.nano.capabilities.articles.model.bean.ArticleReference
import com.skyd.podaura.nano.capabilities.articles.model.bean.ArticleReferences
import com.skyd.podaura.nano.capabilities.articles.model.bean.ArticleSearchContext
import com.skyd.podaura.nano.capabilities.downloads.model.DownloadContent
import com.skyd.podaura.nano.capabilities.downloads.model.DownloadFeedReference
import com.skyd.podaura.nano.capabilities.downloads.model.DownloadHistoryCodec
import com.skyd.podaura.nano.capabilities.downloads.model.DownloadIssue
import com.skyd.podaura.nano.capabilities.downloads.model.DownloadIssueKind
import com.skyd.podaura.nano.capabilities.downloads.model.DownloadRequestEvidence
import com.skyd.podaura.nano.capabilities.downloads.model.DownloadScopeClarification
import com.skyd.podaura.nano.capabilities.downloads.model.PendingDownloadSelection
import com.skyd.podaura.nano.capabilities.downloads.selection.DownloadChoiceSelection
import com.skyd.podaura.nano.capabilities.downloads.selection.DownloadFeedSelection
import com.skyd.podaura.nano.capabilities.downloads.selection.DownloadSelection
import com.skyd.podaura.nano.capabilities.downloads.selection.DownloadLink
import com.skyd.podaura.nano.capabilities.downloads.selection.DownloadSelectionValidator
import com.skyd.podaura.nano.capabilities.downloads.selection.ValidatedDownloadPlan
import com.skyd.podaura.nano.capabilities.feeds.feedSearchModule
import com.skyd.podaura.nano.core.AssistantError
import com.skyd.podaura.nano.core.AssistantHistoryTurn
import com.skyd.podaura.nano.core.AssistantRunEvent
import com.skyd.podaura.nano.core.AssistantTurnRequest
import com.skyd.podaura.nano.di.nanoModule
import com.skyd.podaura.nano.runtime.HttpReply
import com.skyd.podaura.nano.runtime.HttpScript
import com.skyd.podaura.nano.runtime.KoogAssistantRuntime
import com.skyd.podaura.nano.runtime.KoogTurnCapabilities
import com.skyd.podaura.nano.runtime.chatCompletion
import com.skyd.podaura.nano.runtime.chatToolCalls
import com.skyd.podaura.nano.runtime.nanoJson
import com.skyd.podaura.nano.runtime.testConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.datetime.TimeZone
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import com.skyd.podaura.nano.capabilities.downloads.model.DownloadScopeField
import com.skyd.podaura.nano.capabilities.downloads.model.DownloadChoice
import com.skyd.podaura.nano.capabilities.downloads.model.DownloadOption
import com.skyd.podaura.nano.capabilities.downloads.model.DownloadExclusion
import com.skyd.podaura.nano.capabilities.downloads.model.DownloadExclusionKind
import com.skyd.podaura.nano.core.AssistantException
import kotlin.test.assertFailsWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DownloadCapabilityTest {
    @Test
    fun delegatedRequestUsesRealSearchEvidenceAndPublishesOnlyLocalPlan() = runTest {
        val question = "帮我挑 3 集适合入门的并下载"
        val repository = Downloads((1..4).map { episode(it.toString(), audio(it.toString())) })
        val script = HttpScript(
            chatToolCalls("search_articles", "search" to """{"keywords":["入门"]}"""),
            chatCompletion("Found candidates."),
            selected(DownloadSelection(articleIds = listOf("1", "3", "4"))),
        )
        val events = scriptedRun(script, repository, AssistantTurnRequest(question))
        assertEquals(listOf("1", "3", "4"), events.plan().targets.map {
            (it as AssistantDownloadTarget.Attachment).articleId
        })
        assertEquals(3, script.requests.size)
        assertEquals(AssistantRunEvent.Completed, events.last())
        assertTrue(events.none { it is AssistantRunEvent.ContentReady || it is AssistantRunEvent.ReplyDelta })
    }

    @Test
    fun requestedEpisodeComesFromLiveSearchOrOrderedHistory() = runTest {
        val ids = listOf("local-a", "local-b")
        val repository = Downloads(ids.map { episode(it, audio(it)) })
        val search = "找一期讲黑洞的节目并下载"
        val selectedId = ids.last()
        val searched = scriptedRun(
            request = AssistantTurnRequest(search),
            repository = repository,
            script = HttpScript(
                chatToolCalls("search_articles", "search" to """{"keywords":["黑洞"]}"""),
                chatCompletion("Ready"),
                selected(DownloadSelection(articleIds = listOf(selectedId))),
            ),
        )
        assertEquals(listOf(audioTarget(selectedId)), searched.plan().targets)

        val previous = "下载刚才推荐的第二期"
        val references = ArticleReferences(items = ids.map { ArticleReference(id = it, title = it) })
        val script = scriptOf(DownloadSelection(recommendationPositions = listOf(2)))
        val remembered = scriptedRun(
            request = AssistantTurnRequest(question = previous, latestLocalSnapshot = references.historySnapshot()),
            repository = repository,
            script = script,
        )
        assertEquals(listOf(audioTarget(selectedId)), remembered.plan().targets)
        assertEquals(2, script.requests.size)
    }

    @Test
    fun conflictingDisplaysCannotAuthorizeAnOrdinalInEitherPluginOrder() {
        fun source(vararg ids: String) = object : ArticleTargetSource {
            override val recentArticleIds = ids.toList()
            override val focusedArticleId = ids.singleOrNull()
            override val rememberedArticles = ids.associateWith { it }
            override fun currentArticles() = mapOf("A" to ids.first())
        }
        val sources = listOf(source("A", "B"), source("C"))
        for (order in listOf(sources, sources.reversed())) {
            val targets = DownloadArticleTargets(order)
            assertTrue(targets.recentArticleIds.isEmpty())
            assertEquals(null, targets.focusedArticleId)
            assertEquals(setOf("A", "B", "C"), targets.rememberedArticles.keys)
            val candidates = targets.candidates(context = mapOf("A" to "context"))
            assertFalse("A" in candidates)
        }
    }

    @Test
    fun localScopeUsesRequestedQuantityBeyondTheSearchDisplayLimit() = runTest {
        val question = "下载这个播客最新 130 集"
        val repository = Downloads((1..135).map { episode(it.toString(), audio(it.toString())) })
        val request = AssistantTurnRequest(
            question = question,
            evidence = listOf(DownloadRequestEvidence(
                localFeeds = listOf(DownloadFeedReference(title = "Example podcast", feedUrl = feedUrl)),
            )),
        )
        val script = HttpScript(
            chatCompletion("Ready to make a structured selection."),
            selected(DownloadSelection(
                feeds = listOf(DownloadFeedSelection(feedId = "remembered-feed-1", latest = 130)),
            )),
        )
        assertEquals(130, scriptedRun(script, repository, request).plan().targets.size)
        assertEquals(130, repository.scopes.single().limit)
        assertTrue(script.requests.none { it.toString().contains(feedUrl) })
    }

    @Test
    fun verifiesStructuredDateBoundsLimitsAndFeedIdentity() = runTest {
        val repository = Downloads(listOf(episode("known", audio("known"))))
        val validator = DownloadSelectionValidator(
            repository = repository,
            scope = ArticleSearchScope.All,
            timeZone = TimeZone.UTC,
        )
        val question = "下载这个播客 2025-01-01 到 2025-01-31 的所有节目"
        val request = AssistantTurnRequest(question)
        val feed = DownloadFeedSelection(
            feedId = "known",
            publishedFrom = "2025-01-01",
            publishedBefore = "2025-02-01",
        )
        suspend fun resolve(selection: DownloadFeedSelection) = validator.validate(
            selection = DownloadSelection(feeds = listOf(selection)),
            request = request,
            knownArticles = emptyMap(),
            feeds = mapOf("known" to DownloadFeedReference(title = "Example podcast", feedUrl = feedUrl)),
        )
        assertEquals(1, resolve(feed).targets.size)
        assertEquals(1_735_689_600_000L, repository.scopes.single().publishedFromMillis)
        assertEquals(1_738_368_000_000L, repository.scopes.single().publishedBeforeMillis)
        assertFailsWith<AssistantException> { resolve(feed.copy(publishedBefore = "2024-01-01")) }
        assertFailsWith<AssistantException> { resolve(feed.copy(feedId = "invented")) }
        assertFailsWith<AssistantException> { resolve(feed.copy(latest = 0)) }
        assertEquals(DownloadIssueKind.CLARIFICATION_REQUIRED, resolve(DownloadFeedSelection(feedId = "known")).issues.single().kind)
    }

    @Test
    fun sameRequestCanDownloadEitherAudioFileSelectedByTheModel() = runTest {
        val episode = episode("a", audio("short"), audio("full"), attachment("notes.pdf", "application/pdf"))
        val repository = Downloads(listOf(episode))
        val request = AssistantTurnRequest(question = "Download either the short or the full recording")
        fun selectFile(id: String) = selected(DownloadSelection(articleIds = listOf("a"), attachmentIds = listOf(id)))

        for ((id, name) in listOf("file-1" to "short", "file-2" to "full")) {
            val script = HttpScript(
                chatToolCalls("search_articles", "search" to """{"keywords":["example"]}"""),
                chatToolCalls("list_download_attachments", "list" to """{"articleId":"a"}"""),
                chatCompletion("Ready"),
                selectFile(id),
            )
            val plan = scriptedRun(script = script, repository = repository, request = request).plan()
            val expected = AssistantDownloadTarget.Attachment(articleId = "a", url = audio(name).url)
            assertEquals(listOf(expected), plan.targets)
            assertTrue(plan.choices.isEmpty())
            val prompt = script.requests.last().toString()
            assertTrue("$name.mp3" in prompt && id in prompt && "Audio" in prompt)
        }

        val forged = HttpScript(
            chatToolCalls("search_articles", "search" to """{"keywords":["example"]}"""),
            chatCompletion("Ready"),
            selectFile("file-2"),
            selectFile("file-2"),
        )
        val events = scriptedRun(script = forged, repository = repository, request = request)
        assertTrue(events.none { it is AssistantRunEvent.Handoff })
        assertTrue(events.last() is AssistantRunEvent.Failed)
    }

    @Test
    fun formatSelectionUsesHostMediaKindAndPreservesExactFileFormats() = runTest {
        val file = attachment("asset", "application/x-flac").copy(mediaKind = AttachmentMediaKind.Audio)
        val playlist = attachment("stream.m3u8", "application/vnd.apple.mpegurl").copy(mediaKind = AttachmentMediaKind.Playlist)
        val pdf = attachment("notes.pdf", "application/pdf")
        val queue = attachment("queue.playlist", "application/octet-stream")
        val row = episode("a", file, playlist, pdf, queue)
        val validator = DownloadSelectionValidator(repository = Downloads(listOf(row)), scope = ArticleSearchScope.All, timeZone = TimeZone.UTC)
        for ((format, expected) in listOf("audio" to file, "playlist" to playlist, ".playlist" to queue, "application/pdf" to pdf, "pdf" to pdf)) {
            val plan = validator.validate(
                selection = DownloadSelection(articleIds = listOf(row.articleId), attachmentFormat = format),
                request = AssistantTurnRequest(question = "download"),
                knownArticles = mapOf(row.articleId to row.title),
                feeds = emptyMap(),
            )
            assertEquals(listOf(AssistantDownloadTarget.Attachment(articleId = row.articleId, url = expected.url)), plan.targets)
        }
    }

    @Test
    fun selectedFileMustStillBelongToTheSelectedArticle() = runTest {
        val row = episode("a", audio("replacement"))
        val other = episode("b", audio("original"))
        val repository = Downloads(listOf(row, other))
        val validator = DownloadSelectionValidator(repository = repository, scope = ArticleSearchScope.All, timeZone = TimeZone.UTC)
        for (id in listOf("a", "b")) {
            val listed = AssistantDownloadTarget.Attachment(articleId = id, url = audio("original").url)
            assertFailsWith<AssistantException> {
                validator.validate(
                    selection = DownloadSelection(articleIds = listOf("a"), attachmentIds = listOf("file-1")),
                    request = AssistantTurnRequest(question = "Download it"),
                    knownArticles = mapOf("a" to row.title, "b" to other.title),
                    feeds = emptyMap(),
                    listedAttachments = mapOf("file-1" to listed),
                )
            }
        }
    }

    @Test
    fun attachmentPicksPreserveArticleClarificationAndIndependentFeedFormats() = runTest {
        val picked = AssistantDownloadTarget.Attachment(articleId = "a", url = audio("full").url)
        val pdf = attachment("notes.pdf", "application/pdf")
        val first = episode("a", audio("full"), pdf).copy(title = "Episode")
        val second = episode("b", audio("preview")).copy(title = "Episode")
        val validator = DownloadSelectionValidator(repository = Downloads(listOf(first, second)), scope = ArticleSearchScope.All, timeZone = TimeZone.UTC)
        val selection = DownloadSelection(articleIds = listOf("a"), attachmentIds = listOf("file-1"))
        suspend fun validate(value: DownloadSelection) = validator.validate(
            selection = value,
            request = AssistantTurnRequest(question = "download"),
            knownArticles = mapOf("a" to first.title, "b" to second.title),
            recentArticleIds = listOf("a", "b"),
            feeds = mapOf("feed" to DownloadFeedReference(title = "Feed", feedUrl = feedUrl)),
            listedAttachments = mapOf("file-1" to picked),
        )
        val ambiguous = validate(selection)
        assertTrue(ambiguous.targets.isEmpty())
        assertEquals(listOf(picked, AssistantDownloadTarget.Attachment(articleId = "b", url = audio("preview").url)),
            ambiguous.choices.single().options.flatMap { it.targets })

        val distinctRequests = validate(selection.copy(
            recommendationPositions = listOf(1),
            feeds = listOf(DownloadFeedSelection(feedId = "feed", latest = 1, attachmentFormat = "pdf")),
        ))
        assertEquals(listOf(picked, AssistantDownloadTarget.Attachment(articleId = "a", url = pdf.url)), distinctRequests.targets)
    }

    @Test
    fun attachmentSnapshotReportsPartialListingsAndRefreshesLabelsWithStableIds() = runTest {
        val prefix = "x".repeat(220)
        val full = attachment("${prefix}-full.mp3", "audio/mpeg")
        val preview = attachment("${prefix}-preview.mp3", "audio/mpeg")
        var row = episode("a", full, preview, *(1..19).map { audio(it.toString()) }.toTypedArray())
        val tool = DownloadAttachmentsTool { row }
        val firstPage = AttachmentPage(articleId = "a")
        val first = tool.execute(firstPage)
        assertTrue("full.mp3" in first && "preview.mp3" in first)
        val initialIds = tool.targets
        assertEquals(20, initialIds.size)
        val initialSnapshot = tool.snapshot(32).single().jsonObject
        assertEquals("21", initialSnapshot.getValue("totalFiles").jsonPrimitive.content)
        assertTrue("full.mp3" in initialSnapshot.toString() && "preview.mp3" in initialSnapshot.toString())
        row = row.copy(attachments = listOf(full.copy(type = "audio/updated", length = 999L)) + row.attachments.drop(1))
        val refreshed = tool.execute(firstPage)
        assertEquals(initialIds, tool.targets)
        val firstFile = tool.snapshot(32).single().jsonObject.getValue("files").jsonArray.first().jsonObject
        assertTrue("audio/updated" in refreshed)
        assertEquals("999", firstFile.getValue("bytes").jsonPrimitive.content)
        tool.execute(AttachmentPage(articleId = "a", offset = 20))
        assertEquals(21, tool.targets.size)
    }

    @Test
    fun historicalEpisodesResolveAttachmentsAndContinueTextChoiceInOriginalBatch() = runTest {
        val question = "下载刚才推荐的那几集"
        val repository = Downloads(listOf(
            episode("one", audio("one"), attachment("one.pdf", "application/pdf")),
            episode("two", audio("two"), attachment("two.mp4", "video/mp4")),
            episode("missing"),
        ))
        val ids = repository.articles.keys.toList()
        val references = ArticleReferences(items = ids.map { ArticleReference(id = it, title = it) })
        val first = scriptedRun(
            script = scriptOf(DownloadSelection(articleIds = ids)),
            repository = repository,
            request = AssistantTurnRequest(question = question, latestLocalSnapshot = references.historySnapshot()),
        ).plan()
        assertTrue(first.targets.isEmpty())
        assertEquals(listOf("one", "two"), first.choices.map { it.title })
        assertEquals(2, first.choices.first().options.size)
        assertEquals(listOf(DownloadIssue(title = "missing", kind = DownloadIssueKind.MISSING_ATTACHMENT)), first.issues)
        val choice = first.choices.last()
        assertEquals(2, choice.options.size)

        val answer = "选视频那个"
        val selection = DownloadSelection(
            choices = listOf(DownloadChoiceSelection(choiceId = choice.id, optionIds = listOf("2"))),
        )
        val request = AssistantTurnRequest(
            question = answer,
            evidence = listOf(DownloadRequestEvidence(
                pendingSelections = listOf(PendingDownloadSelection(originTurnId = 17, choices = first.choices)),
                latestMessageId = 17L,
            )),
        )
        val confirmed = scriptedRun(scriptOf(selection), repository, request).plan()
        assertEquals(17L, confirmed.originTurnId)
        assertEquals(setOf(choice.id), confirmed.resolvedChoiceIds)
        assertEquals(choice.options.last().targets, confirmed.targets)

        repository.articles.remove("two")
        val stale = scriptedRun(scriptOf(selection), repository, request).plan()
        assertEquals(emptyList(), stale.targets)
        assertEquals(emptySet(), stale.resolvedChoiceIds)
        assertEquals(listOf(DownloadIssue(title = choice.title, kind = DownloadIssueKind.STALE_CHOICE)), stale.issues)
    }

    @Test
    fun allAttachmentsIncludesDocumentsAndReturnsArticleAlternativesForOneAmbiguousReference() = runTest {
        val question = "下载 unambiguous 的所有附件，另外下载另一集"
        val repository = Downloads(listOf(
            episode("unambiguous", audio("unambiguous"), attachment("notes.pdf", "application/pdf")),
            episode("match1", audio("match1")),
            episode("match2", audio("match2")),
        ))
        val validator = DownloadSelectionValidator(
            repository = repository,
            scope = ArticleSearchScope.All,
            timeZone = TimeZone.UTC,
        )
        val plan = validator.validate(
            selection = DownloadSelection(
                allAttachments = true,
                articleIds = listOf("unambiguous", "unambiguous"),
                articleAlternatives = listOf(listOf("match1", "match2")),
            ),
            request = AssistantTurnRequest(question),
            knownArticles = repository.articles.keys.associateWith { it },
            feeds = emptyMap(),
        )
        assertEquals(listOf(audioTarget("unambiguous"), AssistantDownloadTarget.Attachment(
            articleId = "unambiguous", url = "https://media.test/notes.pdf"
        )), plan.targets)
        assertEquals(listOf("match1", "match2"), plan.choices.single().options.map { it.title })
    }

    @Test
    fun checksOnlyUserSuppliedFileLinksAndReportsPlaybackPages() = runTest {
        val question = "下载 [文件](https://media.test/episode(a).mp3) 和 https://media.test/watch。"
        val repository = Downloads(emptyList())
        val script = HttpScript(
            chatCompletion("Ready"),
            selected(DownloadSelection(directLinks = listOf(
                DownloadLink(url = "https://media.test/episode(a).mp3", before = "下载 [文件](", after = ") 和 https://media.test/watch。"),
                DownloadLink(url = "https://media.test/watch", before = "下载 [文件](https://media.test/episode(a).mp3) 和 ", after = "。"),
            ))),
        )
        val plan = scriptedRun(
            script = script,
            repository = repository,
            request = AssistantTurnRequest(
                question = question,
                history = listOf(AssistantHistoryTurn(
                    question = "推荐一集",
                    reply = "https://assistant-created.test/file.mp3",
                ))
            ),
        ).plan()
        assertEquals(listOf(AssistantDownloadTarget.DirectLink("https://media.test/episode(a).mp3")), plan.targets)
        assertEquals(listOf(DownloadIssue(title = "watch", kind = DownloadIssueKind.FILE_LINK_REQUIRED)), plan.issues)
        assertEquals(setOf("https://media.test/episode(a).mp3", "https://media.test/watch"), repository.probed.toSet())

        val forged = DownloadSelection(directLinks = listOf(DownloadLink("https://assistant-created.test/file.mp3")))
        val events = scriptedRun(
            script = HttpScript(chatCompletion("Ready"), selected(forged), selected(forged)),
            repository = repository,
            request = AssistantTurnRequest(question),
        )
        assertEquals(AssistantRunEvent.Failed(AssistantError.INVALID_OUTPUT), events.last())
        assertTrue(events.none { it is AssistantRunEvent.Handoff })
    }

    @Test
    fun aChangedUrlCannotBeDownloadedWithTheOriginalCitation() = runTest {
        val url = "https://media.test/episode.mp3"
        val citation = DownloadLink(url = url, before = "Download ")
        val selection = DownloadSelection(directLinks = listOf(citation))
        val repository = Downloads(emptyList())
        val events = scriptedRun(
            script = HttpScript(chatCompletion("Ready"), selected(selection), selected(selection)),
            repository = repository,
            request = AssistantTurnRequest("Download $url?version=other"),
        )
        assertTrue(events.none { it is AssistantRunEvent.Handoff })
        assertTrue(repository.probed.isEmpty())
    }

    @Test
    fun historicalUrlsRequireCurrentAuthorizationAndKnownArticleMustStayWithinScope() = runTest {
        val repository = Downloads(listOf(episode("a", audio("a"))))
        val link = "https://media.test/file.mp3"
        val request = AssistantTurnRequest(
            question = "Download the link I sent earlier",
            evidence = listOf(DownloadRequestEvidence(currentTurnId = 9, userMessage = { if (it == 1L) link else null })),
        )
        val pickedLink = DownloadSelection(directLinks = listOf(DownloadLink(link)))
        val script = HttpScript(
            chatToolCalls("read_download_history", "history" to """{"turnId":1}"""),
            chatCompletion("Ready"),
            selected(pickedLink),
        )
        val recovered = scriptedRun(script = script, repository = repository, request = request)
        assertTrue(link in script.requests.last().toString())
        assertEquals(listOf(AssistantDownloadTarget.DirectLink(link)), recovered.plan().targets)

        val question = "Download the recommended episode"
        val references = ArticleReferences(items = listOf(ArticleReference(id = "a", title = "A")))
        val events = scriptedRun(
            script = scriptOf(DownloadSelection(articleIds = listOf("a"))),
            repository = repository,
            request = AssistantTurnRequest(
                question = question,
                context = ArticleSearchContext(ArticleSearchScope.Restricted(feedUrls = setOf("https://other.test"))),
                latestLocalSnapshot = references.historySnapshot(),
            )
        )
        assertTrue(events.plan().targets.isEmpty())
        assertEquals(DownloadIssueKind.ARTICLE_UNAVAILABLE, events.plan().issues.single().kind)
    }

    @Test
    fun explicitlyExcludedAttachmentIsRemovedBeforeAllVersionsIsExpanded() = runTest {
        val article = episode(
            "Episode Alpha",
            audio("episode"),
            attachment("episode.mp4", "video/mp4"),
            attachment("notes.pdf", "application/pdf"),
            attachment("Bonus", "application/octet-stream"),
        )
        val repository = Downloads(listOf(article))
        val validator = DownloadSelectionValidator(repository = repository, scope = ArticleSearchScope.All, timeZone = TimeZone.UTC)
        suspend fun plan(question: String, exclusion: DownloadExclusion) = validator.validate(
            selection = DownloadSelection(articleIds = listOf(article.articleId), allAttachments = true, exclusions = listOf(exclusion)),
            request = AssistantTurnRequest(question),
            knownArticles = mapOf(article.articleId to article.title),
            feeds = emptyMap(),
        )
        fun expectedExcept(suffix: String? = null) = article.attachments
            .filterNot { suffix != null && it.url.endsWith(suffix) }
            .map { AssistantDownloadTarget.Attachment(articleId = article.articleId, url = it.url) }
        val pdf = DownloadExclusion(DownloadExclusionKind.FORMAT, "pdf")
        for (question in listOf("下载全部附件，PDF 留在服务器", "Please leave the documents behind", "添付資料以外を手元に")) {
            assertEquals(expectedExcept(".pdf"), plan(question, pdf).targets)
        }
        val file = DownloadExclusion(DownloadExclusionKind.FILENAME, "Bonus")
        assertEquals(expectedExcept("/Bonus"), plan("Skip the bonus file", file).targets)
        val unresolved = plan("Another episode", pdf.copy(episodeTitle = "Other"))
        assertTrue(unresolved.targets.isEmpty())
        assertEquals(DownloadIssueKind.CLARIFICATION_REQUIRED, unresolved.issues.single().kind)
    }

    @Test
    fun scopedExclusionsKeepFeedAndEpisodeNamesDistinctAndApplyBeforeLatest() = runTest {
        val newest = episode("Newest", attachment("video.mp4", "video/mp4"))
        val namesake = episode("Astronomy", audio("namesake"))
        val compound = episode("Science and Society", audio("science"))
        val other = episode("Old episode", audio("old"))
        val repository = Downloads(listOf(newest, namesake, compound, other))
        val validator = DownloadSelectionValidator(repository = repository, scope = ArticleSearchScope.All, timeZone = TimeZone.UTC)
        val feeds = mapOf("context-feed-1" to DownloadFeedReference(title = "Astronomy", feedUrl = feedUrl))
        val scope = DownloadFeedSelection(feedId = "context-feed-1")
        suspend fun plan(text: String, vararg excluded: String, latest: Int? = null, format: String? = null) = validator.validate(
            selection = DownloadSelection(
                feeds = listOf(scope.copy(latest = latest, allEpisodes = latest == null)),
                exclusions = excluded.map { DownloadExclusion(DownloadExclusionKind.EPISODE, it) } +
                        listOfNotNull(format?.let { DownloadExclusion(DownloadExclusionKind.FORMAT, it) }),
            ),
            request = AssistantTurnRequest(question = text),
            knownArticles = emptyMap(),
            feeds = feeds,
        )
        fun target(article: AssistantDownloadArticle) =
            AssistantDownloadTarget.Attachment(articleId = article.articleId, url = article.attachments.single().url)

        val namedFeed = plan("Download all episodes of Astronomy except the episode Astronomy, and Science and Society", namesake.title, compound.title)
        assertEquals(listOf(target(newest), target(other)), namedFeed.targets)
        assertTrue(namedFeed.issues.isEmpty())
        val contextualFeed = plan("Download all episodes of this feed except the episode Astronomy", namesake.title)
        assertEquals(listOf(target(newest), target(compound), target(other)), contextualFeed.targets)
        assertTrue(contextualFeed.issues.isEmpty())

        val excludingLatest = plan(text = "Download the latest episode of Astronomy except MP4", latest = 1, format = "mp4")
        assertEquals(listOf(target(namesake)), excludingLatest.targets)
        assertTrue(excludingLatest.issues.isEmpty())
        assertEquals(null, repository.scopes.last().limit)
        val unchangedLatest = plan(text = "Download the latest episode of Astronomy", latest = 1)
        assertEquals(listOf(target(newest)), unchangedLatest.targets)
        assertEquals(1, repository.scopes.last().limit)

        val containingFeedTitle = episode("Science and Astronomy", audio("complete-title"))
        repository.articles[containingFeedTitle.articleId] = containingFeedTitle
        val excludingCompound = plan("Download all episodes of this feed except the episode Science and Astronomy", containingFeedTitle.title)
        assertEquals(listOf(target(newest), target(namesake), target(compound), target(other)), excludingCompound.targets)
        assertTrue(excludingCompound.issues.isEmpty())
    }

    @Test
    fun verifiedFilenameExclusionBackfillsLatestAndSurvivesScopeClarification() = runTest {
        val betaUrl = "https://subscriptions.test/beta"
        val latest = episode("Latest Astronomy", attachment("trailer.mp4", "video/mp4"))
        val older = episode("Older Astronomy", audio("older"))
        val betaEpisode = episode("Beta episode", audio("beta")).copy(feedUrl = betaUrl)
        val repository = Downloads(listOf(latest, older, betaEpisode))
        val validator = DownloadSelectionValidator(repository = repository, scope = ArticleSearchScope.All, timeZone = TimeZone.UTC)
        val feeds = mapOf(
            "astronomy" to DownloadFeedReference(title = "Astronomy", feedUrl = feedUrl),
            "beta" to DownloadFeedReference(title = "Beta", feedUrl = betaUrl),
        )
        val astronomy = DownloadFeedSelection(feedId = "astronomy", latest = 1)
        val beta = DownloadFeedSelection(feedId = "beta")
        val exclusion = DownloadExclusion(DownloadExclusionKind.FILENAME, "trailer.mp4")
        suspend fun plan(
            text: String,
            scopes: List<DownloadFeedSelection>,
            pending: DownloadScopeClarification? = null,
        ) = validator.validate(
            selection = DownloadSelection(feeds = scopes, scopeClarificationId = pending?.id,
                exclusions = if (pending == null) listOf(exclusion) else emptyList()),
            request = AssistantTurnRequest(
                question = text,
                evidence = listOfNotNull(pending?.let {
                    DownloadRequestEvidence(
                        pendingSelections = listOf(PendingDownloadSelection(
                            originTurnId = 31L,
                            choices = emptyList(),
                            scopeClarifications = listOf(it),
                        )),
                        latestMessageId = 31L,
                    )
                }),
            ),
            knownArticles = emptyMap(),
            feeds = feeds,
        )
        fun target(article: AssistantDownloadArticle) =
            AssistantDownloadTarget.Attachment(articleId = article.articleId, url = article.attachments.single().url)

        for (filename in listOf("trailer.mp4", "\"trailer.mp4\"", "`trailer.mp4`")) {
            val fresh = plan(
                text = "Download the latest episode of Astronomy except $filename",
                scopes = listOf(astronomy),
            )
            assertEquals(listOf(target(older)), fresh.targets, filename)
            assertTrue(fresh.issues.isEmpty(), filename)
            val partial = plan(
                text = "Download the latest episode of Astronomy and all episodes of Beta during summer except $filename",
                scopes = listOf(astronomy, beta),
            )
            assertEquals(listOf(target(older)), partial.targets, filename)
            val pending = partial.issues.single().scopeClarification!!
            assertEquals(listOf(exclusion), pending.exclusions, filename)
            val saved = DownloadHistoryCodec.encodeContent(DownloadContent(issues = partial.issues))!!
            val restored = (DownloadHistoryCodec.decodeContent(saved) as DownloadContent).issues.single().scopeClarification!!
            assertEquals(pending, restored)

            val resumed = plan(
                text = "2025-07-01 to 2025-07-31",
                scopes = listOf(beta.copy(publishedFrom = "2025-07-01", publishedBefore = "2025-08-01")),
                pending = restored,
            )
            assertEquals(listOf(target(betaEpisode)), resumed.targets, filename)
            assertTrue(resumed.issues.isEmpty(), filename)
            assertEquals(setOf(pending.id), resumed.resolvedChoiceIds)
        }
    }

    @Test
    fun sameTitleRequiresAChoiceUnlessAnExactDisplayPositionWasSelected() = runTest {
        val fullTitle = "星".repeat(210)
        val rows = listOf("a", "b", "c").map { id -> episode(id, audio(id)).copy(title = fullTitle) }
        val validator = DownloadSelectionValidator(repository = Downloads(rows), scope = ArticleSearchScope.All, timeZone = TimeZone.UTC)
        suspend fun resolve(selection: DownloadSelection, current: Set<String> = emptySet()) = validator.validate(
            selection = selection,
            request = AssistantTurnRequest("2"),
            currentArticleIds = current,
            priorArticleIds = setOf("a", "b"),
            knownArticles = rows.filter { it.articleId != "c" || it.articleId in current }.associate { it.articleId to it.title.take(200) },
            feeds = emptyMap(),
            recentArticleIds = listOf("a", "b"),
        )
        val ambiguous = resolve(DownloadSelection(articleIds = listOf("a")))
        assertTrue(ambiguous.targets.isEmpty())
        assertEquals(listOf(audioTarget("a"), audioTarget("b")), ambiguous.choices.single().options.flatMap { it.targets })
        assertEquals(2, ambiguous.choices.single().options.map { it.title }.distinct().size)
        assertEquals(listOf(audioTarget("a"), audioTarget("b")), resolve(DownloadSelection(articleIds = listOf("a", "b"))).targets)
        assertEquals(listOf(audioTarget("a"), audioTarget("b")), resolve(
            selection = DownloadSelection(articleIds = listOf("a", "b")), current = setOf("b", "c"),
        ).targets)
        assertEquals(listOf(audioTarget("a")), resolve(DownloadSelection(articleIds = listOf("a")), current = setOf("a")).targets)
        assertEquals(listOf(audioTarget("b")), resolve(DownloadSelection(recommendationPositions = listOf(2))).targets)
        assertFailsWith<AssistantException> { resolve(DownloadSelection(recommendationPositions = listOf(3))) }
        assertFailsWith<AssistantException> { resolve(DownloadSelection(articleIds = listOf("invented"))) }
    }

    @Test
    fun modelCanAnswerAnInquiryOrChooseDownloadsInAnyLanguage() = runTest {
        val repository = Downloads(emptyList())
        val url = "https://media.test/test.mp3"
        val question = "Garde-moi ce fichier hors ligne：$url"
        val selected = DownloadSelection(directLinks = listOf(DownloadLink(url = url, before = "Garde-moi ce fichier hors ligne：")))
        assertEquals(listOf(AssistantDownloadTarget.DirectLink(url)), scriptedRun(
            script = scriptOf(selected),
            repository = repository,
            request = AssistantTurnRequest(question),
        ).plan().targets)
        val inquiry = scriptedRun(
            script = HttpScript(chatCompletion("""{"kind":"CLARIFICATION","text":"Which file?","outputs":{}}""")),
            repository = repository,
            request = AssistantTurnRequest("What does download mean? $url"),
        )
        assertTrue(inquiry.none { it is AssistantRunEvent.Handoff })
        assertEquals(listOf(url), repository.probed)
    }

    @Test
    fun persistedExclusionsAndLegacyScopeClarificationsRemainBoundToTheirBatch() = runTest {
        val article = episode("Episode", audio("one"), attachment("notes.pdf", "application/pdf"))
        val feed = DownloadFeedReference(title = "Feed", feedUrl = feedUrl)
        val validator = DownloadSelectionValidator(repository = Downloads(listOf(article)), scope = ArticleSearchScope.All, timeZone = TimeZone.UTC)
        val old = DownloadScopeClarification(
            id = "saved",
            originalClause = "Original request",
            field = DownloadScopeField.RANGE,
            feed = feed,
            excludedClauses = listOf("except PDF"),
        )
        suspend fun resume(pending: DownloadScopeClarification, exclusions: List<DownloadExclusion>) = validator.validate(
            selection = DownloadSelection(
                scopeClarificationId = pending.id,
                feeds = listOf(DownloadFeedSelection(feedId = "f", latest = 2)),
                exclusions = exclusions,
                allAttachments = true,
            ),
            request = AssistantTurnRequest(
                question = "Two",
                evidence = listOf(DownloadRequestEvidence(pendingSelections = listOf(
                    PendingDownloadSelection(originTurnId = 12, choices = emptyList(), scopeClarifications = listOf(pending)),
                ))),
            ),
            knownArticles = emptyMap(),
            feeds = mapOf("f" to feed),
        )
        val unanswered = resume(old, emptyList())
        assertTrue(unanswered.targets.isEmpty())
        assertTrue(unanswered.resolvedChoiceIds.isEmpty())
        val filter = DownloadExclusion(kind = DownloadExclusionKind.FORMAT, value = "pdf")
        val resumed = resume(old, listOf(filter))
        assertEquals(listOf(audioTarget("one").copy(articleId = article.articleId)), resumed.targets)
        assertEquals(12L, resumed.originTurnId)
        assertEquals(setOf(old.id), resumed.resolvedChoiceIds)
    }

    @Test
    fun aScopedFilenameMustBelongToTheNamedEpisodeAndEmptyGroupsAndFeedsKeepTheirIssues() = runTest {
        val a = episode("a", audio("a"))
        val b = episode("b", attachment("notes.pdf", "application/pdf"))
        val missing = episode("missing")
        val other = episode("other").copy(title = missing.title)
        val repository = Downloads(listOf(a, b, missing, other))
        val validator = DownloadSelectionValidator(repository = repository, scope = ArticleSearchScope.All, timeZone = TimeZone.UTC)
        suspend fun plan(selection: DownloadSelection) = validator.validate(
            selection = selection,
            request = AssistantTurnRequest("model-selected targets"),
            knownArticles = repository.articles.mapValues { it.value.title },
            feeds = mapOf("context" to DownloadFeedReference("Feed", feedUrl), "old" to DownloadFeedReference("Feed", "https://old.test")),
            contextFeedUrls = setOf(feedUrl),
        )
        val unknown = plan(DownloadSelection(
            articleIds = listOf("a", "b"),
            allAttachments = true,
            exclusions = listOf(DownloadExclusion(kind = DownloadExclusionKind.FILENAME, value = "notes.pdf", episodeTitle = "a")),
        ))
        assertTrue(unknown.targets.isEmpty())
        assertEquals(DownloadIssueKind.CLARIFICATION_REQUIRED, unknown.issues.single().kind)
        val partial = plan(DownloadSelection(
            articleIds = listOf("a"),
            articleAlternatives = listOf(listOf("missing", "other")),
            feeds = listOf(DownloadFeedSelection(feedId = "context", latest = 1, attachmentFormat = "mp4")),
        ))
        assertEquals(listOf(audioTarget("a")), partial.targets)
        assertEquals(listOf("missing", "missing"), partial.issues.filter { it.kind == DownloadIssueKind.MISSING_ATTACHMENT }.map { it.title })
        assertTrue(partial.issues.any { it.title == "Feed" && it.kind == DownloadIssueKind.NO_MATCH })
    }

    @Test
    fun aModelCanPageToAPendingOptionOutsideTheInitialSnapshot() = runTest {
        val rows = (1..70).map { episode("ep-$it", audio("file-$it"), attachment("file-$it.pdf", "application/pdf")) }
        val repository = Downloads(rows)
        val choices = rows.mapIndexed { index, article ->
            DownloadChoice(
                id = "choice-$index",
                title = "节".repeat(150) + article.title,
                options = article.attachments.mapIndexed { position, file ->
                    DownloadOption(id = position.toString(), title = "附".repeat(150) + file.url.substringAfterLast('/'),
                        targets = listOf(AssistantDownloadTarget.Attachment(article.articleId, file.url)))
                },
            )
        }
        val last = choices.last()
        val request = AssistantTurnRequest(
            question = "The last PDF please",
            evidence = listOf(DownloadRequestEvidence(pendingSelections = listOf(PendingDownloadSelection(1, choices)))),
        )
        val script = HttpScript(
            chatToolCalls("read_download_history",
                "page1" to """{"turnId":1,"offset":80}""",
                "page2" to """{"turnId":1,"offset":100}""",
                "page3" to """{"turnId":1,"offset":120}""",
            ),
            selected(DownloadSelection(choices = listOf(DownloadChoiceSelection(choiceId = last.id, optionIds = listOf("1"))))),
        )
        val result = scriptedRun(script = script, repository = repository, request = request).plan()
        assertFalse(last.id in script.requests.first().toString())
        assertTrue(last.id in script.requests.last().toString())
        assertEquals(last.options.last().targets, result.targets)
    }

    @Test
    fun oldScopeOnlyBatchCanResumeByTurnNumber() = runTest {
        val feed = DownloadFeedReference(title = "Feed", feedUrl = feedUrl)
        val pending = (1L..90L).map { turn ->
            val scope = DownloadScopeClarification(
                id = "range-$turn-end", originalClause = "Download from this feed", field = DownloadScopeField.RANGE, feed = feed,
            )
            PendingDownloadSelection(originTurnId = turn, choices = emptyList(), scopeClarifications = listOf(scope))
        }
        val id = pending.first().scopeClarifications.single().id
        val script = HttpScript(
            chatToolCalls("read_download_history", "read" to """{"turnId":1}"""),
            chatCompletion("Ready"),
            selected(DownloadSelection(scopeClarificationId = id, feeds = listOf(DownloadFeedSelection(feedId = "scope-$id", latest = 1)))),
        )
        val result = scriptedRun(
            script = script,
            request = AssistantTurnRequest(question = "The latest from the first batch",
                evidence = listOf(DownloadRequestEvidence(pendingSelections = pending, currentTurnId = 91))),
            repository = Downloads(listOf(episode("a", audio("a")))),
        ).plan()
        assertEquals(1L, result.originTurnId)
        assertEquals(setOf(id), result.resolvedChoiceIds)
        assertEquals(listOf(audioTarget("a")), result.targets)
        assertTrue(id in script.requests.last().toString())
    }

    private suspend fun scriptedRun(
        script: HttpScript,
        repository: Downloads,
        request: AssistantTurnRequest,
        feedSource: IFeedRepository = object : IFeedRepository {
            override suspend fun query(query: FeedQuery) = emptyList<FeedRecord>()
        },
    ) = withContext(Dispatchers.Default) {
        val articleSource = object : IArticleRepository {
            override suspend fun query(scope: ArticleSearchScope, query: ArticleQuery) =
                repository.articles.values.take(query.limit).map { article ->
                    ArticleRecord(articleId = article.articleId, title = article.title, description = "适合入门")
                }
        }
        val app = koinApplication {
            modules(nanoModule, downloadModule, feedSearchModule, articleSearchModule, module {
                single<IArticleRepository> { articleSource }
                single<IFeedRepository> { feedSource }
                single<IAssistantDownloadRepository> { repository }
            })
        }
        try {
            val runtime = KoogAssistantRuntime(
                capabilities = app.koin.get<KoogTurnCapabilities>()::create,
                httpClient = script::client,
            )
            runtime.run(request, testConnection()).toList()
        } finally {
            app.close()
        }
    }

    private class Downloads(
        rows: List<AssistantDownloadArticle>,
        private val dates: Map<String, Long> = emptyMap(),
    ) : IAssistantDownloadRepository {
        val articles = rows.associateBy { it.articleId }.toMutableMap()
        private val probeLock = Mutex()
        val probed = mutableListOf<String>()
        val scopes = mutableListOf<AssistantDownloadScope>()

        override suspend fun articlesByIds(articleIds: List<String>) = articleIds.mapNotNull(articles::get)

        override suspend fun articlesInFeed(scope: AssistantDownloadScope): List<AssistantDownloadArticle> {
            scopes += scope
            val matching = articles.values.filter { article ->
                val published = dates[article.articleId]
                article.feedUrl == scope.feedUrl && (published == null ||
                        (scope.publishedFromMillis?.let { published >= it } ?: true) &&
                        (scope.publishedBeforeMillis?.let { published < it } ?: true))
            }
            return matching.take(scope.limit ?: matching.size)
        }

        override suspend fun checkDirectLink(url: String): Boolean {
            probeLock.withLock { probed += url }
            return url.endsWith(".mp3")
        }

        override suspend fun submit(batchId: String, submissionId: String, targets: List<AssistantDownloadTarget>) =
            error("Validation must never submit downloads")

        override suspend fun receipt(batchId: String, submissionId: String) =
            error("Validation must never access receipts")
    }

    private companion object {
        const val feedUrl = "https://subscriptions.test/feed?token=private"

        fun episode(id: String, vararg attachments: AssistantDownloadAttachment) =
            AssistantDownloadArticle(articleId = id, title = id, feedUrl = feedUrl, attachments = attachments.toList())

        fun attachment(name: String, type: String) = AssistantDownloadAttachment(
            url = "https://media.test/$name",
            type = type,
            length = 123L,
            mediaKind = when (type.substringBefore('/')) {
                "audio" -> AttachmentMediaKind.Audio
                "video" -> AttachmentMediaKind.Video
                else -> AttachmentMediaKind.Other
            },
        )

        fun audio(id: String) = attachment("$id.mp3", "audio/mpeg")

        fun audioTarget(id: String) = AssistantDownloadTarget.Attachment(articleId = id, url = audio(id).url)

        fun scriptOf(selection: DownloadSelection) = HttpScript(chatCompletion("Ready"), selected(selection))

        fun selected(selection: DownloadSelection): HttpReply =
            chatCompletion("""{"kind":"RESULTS","text":"","outputs":{"downloads":${nanoJson.encodeToString(selection)}}}""")

        fun List<AssistantRunEvent>.plan() =
            filterIsInstance<AssistantRunEvent.Handoff>().single().handoff as ValidatedDownloadPlan
    }
}
