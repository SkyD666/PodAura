package com.skyd.podaura.nano.capabilities.articles

import com.skyd.podaura.api.model.bean.article.ArticleQuery
import com.skyd.podaura.api.model.bean.article.ArticleRecord
import com.skyd.podaura.api.model.bean.article.ArticleSearchScope
import com.skyd.podaura.api.model.repository.IArticleRepository
import com.skyd.podaura.nano.capabilities.articles.model.ArticleHistoryCodec
import com.skyd.podaura.nano.capabilities.articles.model.bean.ArticleField
import com.skyd.podaura.nano.capabilities.articles.model.bean.ArticleReference
import com.skyd.podaura.nano.capabilities.articles.model.bean.ArticleReferences
import com.skyd.podaura.nano.capabilities.articles.model.bean.ArticleSearchContent
import com.skyd.podaura.nano.capabilities.articles.model.bean.ArticleSearchContext
import com.skyd.podaura.nano.capabilities.articles.model.bean.ArticleSearchError
import com.skyd.podaura.nano.capabilities.articles.model.bean.ArticleSearchLimits
import com.skyd.podaura.nano.capabilities.articles.model.bean.AssistantArticle
import com.skyd.podaura.nano.capabilities.articles.model.bean.RecommendedArticle
import com.skyd.podaura.nano.capabilities.articles.model.bean.ValidatedAssistantAnswer
import com.skyd.podaura.nano.capabilities.articles.model.repository.LocalArticleSearchRepository
import com.skyd.podaura.nano.core.AssistantAnswerKind
import com.skyd.podaura.nano.core.AssistantHistoryTurn
import com.skyd.podaura.nano.core.AssistantLimits
import com.skyd.podaura.nano.core.AssistantProtocol
import com.skyd.podaura.nano.core.AssistantRunEvent
import com.skyd.podaura.nano.core.AssistantTurnRequest
import com.skyd.podaura.nano.model.bean.AssistantReplyContent
import com.skyd.podaura.nano.model.bean.conversation.AssistantSessionTurn
import com.skyd.podaura.nano.model.bean.conversation.AssistantTurnStatus
import com.skyd.podaura.nano.model.repository.conversation.ConversationGeneration
import com.skyd.podaura.nano.model.db.ConversationRecordCodec
import com.skyd.podaura.nano.runtime.HttpReply
import com.skyd.podaura.nano.runtime.HttpScript
import com.skyd.podaura.nano.runtime.KoogAssistantRuntime
import com.skyd.podaura.nano.runtime.NativeScript
import com.skyd.podaura.nano.runtime.astra
import com.skyd.podaura.nano.runtime.emptyFeedSource
import com.skyd.podaura.nano.runtime.gemini
import com.skyd.podaura.nano.runtime.nanoJson
import com.skyd.podaura.nano.runtime.responsesCall
import com.skyd.podaura.nano.runtime.responsesMessage
import com.skyd.podaura.nano.runtime.responsesReply
import com.skyd.podaura.nano.runtime.testCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class ArticleFieldLookupTest {
    private val scope = ArticleSearchScope.Restricted(articleIds = setOf("a", "b", "c", "old", "older"))
    private val codec = ConversationRecordCodec(listOf(ArticleHistoryCodec))
    private val queries = mutableListOf<ArticleQuery>()
    private var row: ArticleRecord? = ArticleRecord(
        articleId = "b",
        title = "**Updated**",
        sourceTitle = "Fresh podcast",
        dateMillis = 0L,
    )
    private var fail = false
    private val source = LocalArticleSearchRepository(object : IArticleRepository {
        override suspend fun query(scope: ArticleSearchScope, query: ArticleQuery): List<ArticleRecord> {
            assertEquals(this@ArticleFieldLookupTest.scope, scope)
            assertEquals(1, query.limit)
            queries += query
            if (fail) error("database unavailable")
            return listOfNotNull(row)
        }
    })
    private val date = Instant.fromEpochMilliseconds(0).toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()

    private fun history(vararg ids: String): AssistantHistoryTurn {
        val content = ArticleSearchContent.recommendations(
            ValidatedAssistantAnswer(
                kind = AssistantAnswerKind.RESULTS,
                text = "",
                articles = ids.map { id ->
                    RecommendedArticle(
                        article = AssistantArticle(
                            articleId = id,
                            title = "Old title $id",
                            sourceName = "Old podcast",
                            snippet = "",
                        ),
                        reason = "Recommended",
                    )
                },
            )
        )
        return AssistantHistoryTurn(question = "Recommend episodes", reply = "", content = content.historySnapshot())
    }

    private fun pending(
        vararg titles: String,
        choiceIds: List<String> = emptyList(),
        pendingField: ArticleField? = ArticleField.DATE,
    ) = AssistantHistoryTurn(
        question = "Which recommendation?",
        reply = "",
        content = ArticleReferences(
            items = titles.mapIndexed { index, title -> ArticleReference(id = ('a' + index).toString(), title = title) },
            pendingField = pendingField,
            needsSelection = true,
            choiceIds = choiceIds,
        ).historySnapshot(),
    )

    private fun request(question: String, vararg history: AssistantHistoryTurn) = AssistantTurnRequest(
        question = question,
        context = ArticleSearchContext(scope),
        history = history.toList(),
    )

    private fun lookupScript(arguments: String) =
        HttpScript(responsesReply(responsesCall(id = "lookup", arguments = arguments, name = "lookup_article_field")))

    private suspend fun runLookup(
        request: AssistantTurnRequest,
        script: HttpScript,
        catalog: LocalArticleSearchRepository = source,
    ): List<AssistantRunEvent> {
        val runtime = KoogAssistantRuntime(
            capabilities = testCapabilities(catalog, emptyFeedSource()),
            httpClient = script::client,
        )
        val events = runtime.run(request = request, connection = astra()).toList()
        assertEquals(listOf("/v1/responses"), script.paths)
        return events
    }

    private suspend fun runLookup(question: String, json: String, history: AssistantHistoryTurn) =
        runLookup(request(question, history), lookupScript(json))

    private suspend fun select(
        request: AssistantTurnRequest,
        articleId: String?,
        catalog: LocalArticleSearchRepository = source,
    ): AssistantReplyContent {
        val arguments = buildJsonObject {
            articleId?.let { put("articleId", it) }
            put("articleField", "KEEP")
        }
        return content(runLookup(request = request, script = lookupScript(arguments.toString()), catalog = catalog))
    }

    private fun content(events: List<AssistantRunEvent>): AssistantReplyContent {
        assertEquals(AssistantRunEvent.Completed, events.last())
        val content = assertIs<AssistantReplyContent>(events.filterIsInstance<AssistantRunEvent.ContentReady>().single().content)
        assertTrue(content.parts.isEmpty())
        assertEquals(content.text, events.filterIsInstance<AssistantRunEvent.ReplyDelta>().single().text)
        return content
    }

    private fun restored(question: String, content: AssistantReplyContent): AssistantHistoryTurn {
        val saved = assertNotNull(codec.content(codec.content(content)))
        return AssistantHistoryTurn(question = question, reply = content.text, content = saved.historySnapshot())
    }

    private fun lookupSnapshot(script: HttpScript): JsonObject {
        val snapshot = script.requests.single().getValue("input").jsonArray.last().jsonObject
            .getValue("content").jsonArray.single().jsonObject.getValue("text").jsonPrimitive.content
            .substringAfter('\n')
        return nanoJson.parseToJsonElement(snapshot).jsonObject.getValue("articles").jsonObject.getValue("lookup").jsonObject
    }

    @Test
    fun geminiFunctionCallReturnsTheLocalFieldInOneRequest() = runTest {
        withContext(Dispatchers.Default) {
            val parts = """[{"functionCall":{"name":"lookup_article_field","args":{"articleId":"b","articleField":"SOURCE"}}}]"""
            val script = NativeScript(
                protocol = AssistantProtocol.GEMINI_GENERATE_CONTENT,
                replies = listOf(HttpReply(gemini(parts))),
            )
            val runtime = KoogAssistantRuntime(
                capabilities = testCapabilities(source, emptyFeedSource()),
                httpClient = script::client,
            )
            val events = runtime.run(request = request("Which podcast?", history("b")), connection = script.connection).toList()
            assertEquals("Fresh podcast", content(events).text)
            assertEquals(listOf("/gateway/v1beta/models/native-model:generateContent"), script.paths)
            val declarations = script.requests.single().getValue("tools").jsonArray.single().jsonObject
                .getValue("functionDeclarations").jsonArray
            val lookup = declarations.map { it.jsonObject }.single { it["name"] == JsonPrimitive("lookup_article_field") }
            val parameters = lookup.getValue("parameters").jsonObject
            assertEquals(setOf("articleId", "articleField"), parameters.getValue("properties").jsonObject.keys)
            assertEquals(listOf("b"), queries.single().withinArticleIds)
        }
    }

    @Test
    fun clarificationAndConsecutiveFieldsSurviveReloadAndUseFreshScopedRows() = runTest {
        withContext(Dispatchers.Default) {
            val question = "Which podcast made that one?"
            val clarification = content(runLookup(question = question, json = """{"articleField":"SOURCE"}""", history = history("a", "b")))
            val pending = assertIs<ArticleReferences>(clarification.reference)
            assertEquals(AssistantAnswerKind.CLARIFICATION, clarification.kind)
            assertEquals(ArticleField.SOURCE, pending.pendingField)
            assertTrue(pending.needsSelection)
            assertTrue(queries.isEmpty())

            val source = select(request("2", restored(question, clarification)), "b")
            assertEquals("Fresh podcast", source.text)
            val selected = assertIs<ArticleReferences>(source.reference)
            assertEquals("b", selected.selectedId)
            assertNull(selected.pendingField)
            assertFalse(selected.needsSelection)

            row = row?.copy(title = "**Updated**" + "t".repeat(400))
            val title = content(runLookup(
                question = "Quel est son titre ?",
                json = """{"articleId":"b","articleField":"TITLE"}""",
                history = restored("2", source),
            ))
            assertEquals(row?.title, title.text)

            val publication = content(runLookup(
                question = "¿Cuándo se publicó?",
                json = """{"articleId":"b","articleField":"DATE"}""",
                history = restored("Quel est son titre ?", title),
            ))
            assertEquals(date, publication.text)
            assertEquals(listOf("b", "b", "b"), queries.map { it.withinArticleIds?.single() })
        }
    }

    @Test
    fun pendingChoicesAcceptThePrintedNumberCompleteTitleAndCopiedEntry() = runTest {
        withContext(Dispatchers.Default) {
            val history = pending("Alpha", "Beta (2)", "Gamma", choiceIds = listOf("b", "c"))
            val notShown = select(request("1", history), null)
            assertEquals(AssistantAnswerKind.CLARIFICATION, notShown.kind)
            assertTrue(queries.isEmpty())
            for (question in listOf("2", "Beta (2)", "2. Beta (2)")) {
                val answer = select(request(question, history), "b")
                assertEquals(date, answer.text, question)
                assertEquals("b", assertIs<ArticleReferences>(answer.reference).selectedId)
            }
            val copied = select(request("2. Beta  ", pending("Alpha", "Beta  ")), "b")
            assertEquals(date, copied.text)
            val untitled = select(request("2.", pending("Alpha", "")), "b")
            assertEquals(date, untitled.text)
            assertEquals(List(5) { "b" }, queries.map { it.withinArticleIds?.single() })
        }
    }

    @Test
    fun duplicateTitlesAndConflictingNumericTitlesAskForAChoiceWithoutReading() = runTest {
        withContext(Dispatchers.Default) {
            val duplicates = select(request("Same title", pending("Same title", "Same title")), null)
            val numericTitle = select(request("2", pending("2", "Beta")), null)
            val numericTitleOutsideChoices = select(
                request = request("2", pending("2", "Beta", "Gamma", choiceIds = listOf("b", "c"))),
                articleId = null,
            )
            for (answer in listOf(duplicates, numericTitle, numericTitleOutsideChoices)) {
                val references = assertIs<ArticleReferences>(answer.reference)
                assertEquals(AssistantAnswerKind.CLARIFICATION, answer.kind)
                assertEquals(ArticleField.DATE, references.pendingField)
                assertTrue(references.needsSelection)
            }
            assertTrue(queries.isEmpty())
            assertEquals(date, select(request("2. Beta", restored("2", numericTitle)), "b").text)
            assertEquals(listOf("b"), queries.single().withinArticleIds)
        }
    }

    @Test
    fun copiedEntrySelectsTheItemAndAsksForTheFieldLocally() = runTest {
        withContext(Dispatchers.Default) {
            val answer = select(request("2. Beta", pending("Alpha", "Beta", pendingField = null)), "b")
            val references = assertIs<ArticleReferences>(answer.reference)
            assertEquals(AssistantAnswerKind.CLARIFICATION, answer.kind)
            assertEquals("b", references.selectedId)
            assertFalse(references.needsSelection)
            assertNull(references.pendingField)
            assertTrue(queries.isEmpty())
        }
    }

    @Test
    fun anExpiredPrintedPositionDoesNotBecomeAnotherItemsTitle() = runTest {
        withContext(Dispatchers.Default) {
            val waiting = pending("2", "Beta", "Gamma", choiceIds = listOf("b", "c"))
            val restricted = ArticleSearchContext(ArticleSearchScope.Restricted(articleIds = setOf("a")))
            val answer = select(request("2", waiting).copy(context = restricted), "b")

            assertEquals(AssistantAnswerKind.CLARIFICATION, answer.kind)
            val references = assertIs<ArticleReferences>(answer.reference)
            assertNull(references.selectedId)
            assertEquals(ArticleField.DATE, references.pendingField)
            assertFalse(answer.text.contains("Beta") || answer.text.contains("Gamma"))
            assertTrue(queries.isEmpty())
        }
    }

    @Test
    fun noAuthorizedChoicesEndsClarificationForBothWordings() = runTest {
        withContext(Dispatchers.Default) {
            val waiting = pending("Alpha", "Beta", choiceIds = listOf("b"))
            val restricted = ArticleSearchContext(ArticleSearchScope.Restricted(articleIds = setOf("retired")))
            val numbered = select(request("2", waiting).copy(context = restricted), "b")
            val prose = request("The second recommendation", waiting).copy(context = restricted)
            val model = content(runLookup(prose, lookupScript("""{"articleId":"b","articleField":"KEEP"}""")))

            for (answer in listOf(numbered, model)) {
                assertEquals(AssistantAnswerKind.NO_MATCH, answer.kind)
                val references = assertIs<ArticleReferences>(answer.reference)
                assertFalse(references.needsSelection)
                assertNull(references.pendingField)
            }
            assertTrue(queries.isEmpty())
        }
    }

    @Test
    fun ordinaryRepliesToClarificationUseTheModelAndCanChangeTheField() = runTest {
        withContext(Dispatchers.Default) {
            val history = pending("Alpha", "Beta")
            val resumed = content(runLookup(
                question = "後ろのほうです。",
                json = """{"articleId":"b","articleField":"KEEP"}""",
                history = history,
            ))
            assertEquals(date, resumed.text)
            val changed = content(runLookup(
                question = "后面那篇，我想问它来自哪个播客？",
                json = """{"articleId":"b","articleField":"SOURCE"}""",
                history = history,
            ))
            assertEquals("Fresh podcast", changed.text)
            assertEquals(listOf("b", "b"), queries.map { it.withinArticleIds?.single() })
        }
    }

    @Test
    fun onlyIdsFromTheActiveRecommendationsCanBeRead() = runTest {
        withContext(Dispatchers.Default) {
            val request = request("Which podcast made it?", history("old"), history("a", "b"))
            for (id in listOf("old", "invented")) {
                val answer = content(runLookup(request, lookupScript("""{"articleId":"$id","articleField":"SOURCE"}""")))
                assertEquals(AssistantAnswerKind.CLARIFICATION, answer.kind)
                assertNull(assertIs<ArticleReferences>(answer.reference).selectedId)
            }
            assertTrue(queries.isEmpty())
            val answer = content(runLookup(request, lookupScript("""{"articleId":"b","articleField":"SOURCE"}""")))
            assertEquals("Fresh podcast", answer.text)
            assertEquals(listOf("b"), queries.single().withinArticleIds)
        }
    }

    @Test
    fun missingFieldsAndUnsupportedFieldsNeverReadTheRecord() = runTest {
        withContext(Dispatchers.Default) {
            val history = history("a", "b")
            for (json in listOf("""{"articleId":"b"}""", """{"articleId":"b","articleField":"KEEP"}""")) {
                val answer = content(runLookup(question = "The second one", json = json, history = history))
                val references = assertIs<ArticleReferences>(answer.reference)
                assertEquals(AssistantAnswerKind.CLARIFICATION, answer.kind)
                assertEquals("b", references.selectedId)
                assertNull(references.pendingField)
            }
            val unsupported = content(runLookup(
                question = "How big is the audio file?",
                json = """{"articleId":"b","articleField":"UNSUPPORTED"}""",
                history = history,
            ))
            assertEquals(AssistantAnswerKind.UNSUPPORTED, unsupported.kind)
            assertTrue(queries.isEmpty())
        }
    }

    @Test
    fun unsupportedRepliesDoNotStartOrRestoreAChoice() = runTest {
        withContext(Dispatchers.Default) {
            val unsupported = content(runLookup(
                question = "How big is the audio file?",
                json = """{"articleField":"UNSUPPORTED"}""",
                history = history("a", "b", "c"),
            ))
            assertEquals(AssistantAnswerKind.UNSUPPORTED, unsupported.kind)
            val references = assertIs<ArticleReferences>(unsupported.reference)
            assertFalse(references.needsSelection)
            assertTrue(references.choiceIds.isEmpty())
            for (kind in listOf(AssistantAnswerKind.UNSUPPORTED, AssistantAnswerKind.NO_MATCH)) {
                val legacy = AssistantReplyContent(
                    parts = unsupported.parts,
                    kind = kind,
                    text = unsupported.text,
                    reference = references.copy(
                        selectedId = "b",
                        pendingField = ArticleField.DATE,
                        needsSelection = true,
                        choiceIds = listOf("c"),
                    ),
                )
                val script = lookupScript("""{"articleField":"UNSUPPORTED"}""")
                val answer = content(runLookup(request("3", restored("Earlier query", legacy)), script))
                assertEquals(AssistantAnswerKind.UNSUPPORTED, answer.kind)
                val lookup = lookupSnapshot(script)
                assertEquals(JsonNull, lookup["selectedId"])
                assertFalse(assertIs<JsonPrimitive>(lookup["needsSelection"]).content.toBoolean())
                assertTrue(lookup.getValue("choiceIds").jsonArray.isEmpty())
            }
            assertTrue(queries.isEmpty())
        }
    }

    @Test
    fun exactSelectionUsesTheCompleteLatestSnapshotWhenModelHistoryIsTrimmed() = runTest {
        withContext(Dispatchers.Default) {
            val fullTitle = "長".repeat(280) + "End"
            val state = ArticleReferences(
                items = listOf(
                    ArticleReference(id = "a", title = "Alpha"),
                    ArticleReference(id = "b", title = "Old title b", displayedTitle = fullTitle),
                ),
                pendingField = ArticleField.DATE,
            )
            val reply = AssistantReplyContent(parts = emptyList(), kind = AssistantAnswerKind.CLARIFICATION, text = "Choose one", reference = state)
            val turn = AssistantSessionTurn(
                id = 1,
                question = "When was it published?",
                content = codec.content(codec.content(reply)),
                status = AssistantTurnStatus.COMPLETED,
            )
            val generated = ConversationGeneration().request(
                turns = listOf(turn),
                question = fullTitle,
                context = ArticleSearchContext(scope),
            )
            assertFalse(generated.history.single().content!!.payload.contains(fullTitle))
            val answer = select(request = generated, articleId = "b")
            assertEquals(date, answer.text)
            assertEquals(listOf("b"), queries.single().withinArticleIds)
        }
    }

    @Test
    fun lookupPromptPreservesLongTitlesAndTheirDistinctEndsWhenCompacted() = runTest {
        withContext(Dispatchers.Default) {
            for (length in listOf(900, AssistantLimits.REPLY_CHARS)) {
                val refs = ArticleReferences(items = listOf("a", "b", "c").mapIndexed { index, id ->
                    val title = "長".repeat(length - 4) + "End${index + 1}"
                    ArticleReference(id = id, title = "Original $id", displayedTitle = title)
                })
                val request = request("哪档播客发布了 ‘End2’？").copy(latestLocalSnapshot = refs.historySnapshot())
                val script = lookupScript("""{"articleId":"b","articleField":"SOURCE"}""")
                assertEquals("Fresh podcast", content(runLookup(request, script)).text)

                val aliases = lookupSnapshot(script).getValue("items").jsonArray
                    .map { it.jsonObject.getValue("displayedTitle").jsonPrimitive.content }
                val original = refs.items.map { requireNotNull(it.displayedTitle) }
                if (length == 900) {
                    assertEquals(original, aliases)
                } else {
                    assertTrue(aliases.all { it.length < length && it.startsWith("長") && '…' in it })
                    assertEquals(listOf("End1", "End2", "End3"), aliases.map { it.takeLast(4) })
                }
            }
            assertEquals(listOf("b", "b"), queries.map { it.withinArticleIds?.single() })
        }
    }

    @Test
    fun toolSnapshotKeepsOriginalPositionsAndOmitsOutOfScopeMetadata() = runTest {
        withContext(Dispatchers.Default) {
            val active = ArticleReferences(
                items = listOf(
                    ArticleReference(id = "retired", title = "Retired private title", displayedTitle = "Retired private alias"),
                    ArticleReference(id = "b", title = "Beta"),
                    ArticleReference(id = "c", title = "Gamma"),
                ),
                selectedId = "retired",
                pendingField = ArticleField.DATE,
                choiceIds = listOf("retired", "c"),
            ).historySnapshot()
            val followUp = request("後ろのほうです。", history("older")).copy(latestLocalSnapshot = active)
            val script = lookupScript("""{"articleId":"b","articleField":"KEEP"}""")
            assertEquals(date, content(runLookup(followUp, script)).text)

            val lookup = lookupSnapshot(script)
            val items = lookup.getValue("items").jsonArray.map { it.jsonObject }
            assertEquals(listOf("b", "c").map(::JsonPrimitive), items.map { it["id"] })
            assertEquals(listOf("Beta", "Gamma").map(::JsonPrimitive), items.map { it["title"] })
            assertEquals(listOf(JsonPrimitive(2), JsonPrimitive(3)), items.map { it["position"] })
            assertEquals(listOf("c"), lookup.getValue("choiceIds").jsonArray.map { it.jsonPrimitive.content })
            assertEquals(JsonPrimitive("DATE"), lookup["pendingField"])
            assertFalse(script.requests.single().toString().contains("retired", ignoreCase = true))
            assertEquals(listOf("b"), queries.single().withinArticleIds)
        }
    }

    @Test
    fun feedScopeRechecksRecommendedIdsInOneBatchBeforeSendingTheirTitles() = runTest {
        withContext(Dispatchers.Default) {
            val feedScope = ArticleSearchScope.Restricted(feedUrls = setOf("subscribed"))
            val authorizedIds = mutableListOf<List<String>?>()
            val catalog = LocalArticleSearchRepository(object : IArticleRepository {
                override suspend fun query(scope: ArticleSearchScope, query: ArticleQuery): List<ArticleRecord> {
                    assertEquals(feedScope, scope)
                    assertEquals(ArticleSearchLimits.SEARCH_RESULTS, query.limit)
                    authorizedIds += query.withinArticleIds
                    return listOf(ArticleRecord(articleId = "b"))
                }
            })
            val request = AssistantTurnRequest(
                question = "Which article was that?",
                context = ArticleSearchContext(feedScope),
                latestLocalSnapshot = history("a", "b", "c").content,
            )
            val script = lookupScript("""{"articleId":"a","articleField":"TITLE"}""")
            val answer = content(runLookup(request = request, script = script, catalog = catalog))
            assertEquals(AssistantAnswerKind.CLARIFICATION, answer.kind)
            assertEquals(listOf("a", "b", "c"), authorizedIds.single())
            val visible = lookupSnapshot(script).getValue("items").jsonArray.single().jsonObject
            assertEquals(JsonPrimitive("b"), visible["id"])
            assertEquals(JsonPrimitive(2), visible["position"])
            assertEquals(JsonPrimitive("Old title b"), visible["title"])
        }
    }

    @Test
    fun feedScopeRechecksBeforeLocallyConfirmingOrDisplayingAnExactChoice() = runTest {
        withContext(Dispatchers.Default) {
            val feedScope = ArticleSearchScope.Restricted(feedUrls = setOf("subscribed"))
            val reads = mutableListOf<ArticleQuery>()
            val catalog = LocalArticleSearchRepository(object : IArticleRepository {
                override suspend fun query(scope: ArticleSearchScope, query: ArticleQuery): List<ArticleRecord> {
                    assertEquals(feedScope, scope)
                    reads += query
                    return listOfNotNull(row)
                }
            })
            val waiting = pending("Alpha", "Beta", "Gamma", choiceIds = listOf("b", "c"))
            val restricted = ArticleSearchContext(feedScope)
            val invalid = select(request = request("99", waiting).copy(context = restricted), articleId = null, catalog = catalog)
            assertEquals(AssistantAnswerKind.CLARIFICATION, invalid.kind)
            assertTrue(invalid.text.contains("2. Beta"))
            assertFalse(invalid.text.contains("Gamma"))
            assertEquals(listOf("b"), assertIs<ArticleReferences>(invalid.reference).choiceIds)
            assertEquals(listOf("a", "b", "c"), reads.single().withinArticleIds)

            val waitingForField = pending("Alpha", "Beta", "Gamma", pendingField = null)
            val retiredTitle = select(request = request("Gamma", waitingForField).copy(context = restricted), articleId = "c", catalog = catalog)
            assertEquals(AssistantAnswerKind.CLARIFICATION, retiredTitle.kind)
            assertNull(assertIs<ArticleReferences>(retiredTitle.reference).selectedId)
            assertFalse(retiredTitle.text.contains("Gamma"))
            assertEquals(2, reads.size)

            val allowed = select(request = request("2", waiting).copy(context = restricted), articleId = "b", catalog = catalog)
            assertEquals(date, allowed.text)
            assertEquals(List(3) { ArticleSearchLimits.SEARCH_RESULTS } + 1, reads.map { it.limit })
            assertEquals(listOf("b"), reads.last().withinArticleIds)
        }
    }

    @Test
    fun unavailableFeedScopeOnlyFailsARequestedLookup() = runTest {
        withContext(Dispatchers.Default) {
            val catalog = LocalArticleSearchRepository(object : IArticleRepository {
                override suspend fun query(scope: ArticleSearchScope, query: ArticleQuery): List<ArticleRecord> =
                    error("database unavailable")
            })
            val request = AssistantTurnRequest(
                question = "Book a flight",
                context = ArticleSearchContext(ArticleSearchScope.Restricted(feedUrls = setOf("subscribed"))),
                latestLocalSnapshot = history("a", "b").content,
            )
            val direct = HttpScript(responsesReply(responsesMessage(
                """{"kind":"UNSUPPORTED","text":"I cannot book flights.","outputs":{}}"""
            )))
            val other = content(runLookup(request = request, script = direct, catalog = catalog))
            assertEquals(AssistantAnswerKind.UNSUPPORTED, other.kind)
            assertTrue(lookupSnapshot(direct).getValue("items").jsonArray.isEmpty())

            val lookup = lookupScript("""{"articleId":"b","articleField":"DATE"}""")
            val failed = runLookup(request = request.copy(question = "Publication date?"), script = lookup, catalog = catalog)
            assertEquals(AssistantRunEvent.Failed(ArticleSearchError.LOCAL_SEARCH), failed.last())
        }
    }

    @Test
    fun repeatedReferencesDoNotDisplaceEarlierDistinctIds() {
        val earlier = history("earlier")
        val list = history(*(1..20).map { "id-$it" }.toTypedArray())
        val ids = articleSearchHistory(listOf(earlier, list, list, list)).flatMap { turn ->
            turn.articles.map { it.article.articleId }
        }
        assertEquals(21, ids.size)
        assertTrue("earlier" in ids)
    }

    @Test
    fun absentFieldUnavailableRecordAndStorageFailureAreDistinct() = runTest {
        withContext(Dispatchers.Default) {
            val last = history("b")
            val lookup = """{"articleId":"b","articleField":"DATE"}"""
            row = row?.copy(dateMillis = null)
            val missing = content(runLookup(question = "Publication date?", json = lookup, history = last))
            assertEquals(AssistantAnswerKind.RESULTS, missing.kind)
            assertNotNull(missing.reference)
            row = row?.copy(articleId = "outside")
            val unavailable = content(runLookup(question = "Publication date?", json = lookup, history = last))
            assertEquals(AssistantAnswerKind.NO_MATCH, unavailable.kind)
            assertTrue(missing.text != unavailable.text)
            fail = true
            val failed = runLookup(question = "Publication date?", json = lookup, history = last)
            assertEquals(AssistantRunEvent.Failed(ArticleSearchError.LOCAL_SEARCH), failed.last())
            assertEquals(List(3) { listOf("b") }, queries.map { it.withinArticleIds })
        }
    }
}
