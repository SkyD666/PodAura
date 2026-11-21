package com.skyd.podaura.podnya.capabilities.downloads

import com.skyd.podaura.api.model.bean.download.AssistantDownloadArticle
import com.skyd.podaura.api.model.bean.download.AssistantDownloadReceipt
import com.skyd.podaura.api.model.bean.download.AssistantDownloadScope
import com.skyd.podaura.api.model.bean.download.AssistantDownloadTarget
import com.skyd.podaura.api.model.repository.IAssistantDownloadRepository
import com.skyd.podaura.podnya.capabilities.downloads.model.DownloadChoice
import com.skyd.podaura.podnya.capabilities.downloads.model.DownloadContent
import com.skyd.podaura.podnya.capabilities.downloads.model.DownloadFeedReference
import com.skyd.podaura.podnya.capabilities.downloads.model.DownloadHistoryCodec
import com.skyd.podaura.podnya.capabilities.downloads.model.DownloadIssue
import com.skyd.podaura.podnya.capabilities.downloads.model.DownloadIssueKind
import com.skyd.podaura.podnya.capabilities.downloads.model.DownloadOption
import com.skyd.podaura.podnya.capabilities.downloads.model.DownloadScopeClarification
import com.skyd.podaura.podnya.capabilities.downloads.model.DownloadScopeField
import com.skyd.podaura.podnya.capabilities.downloads.model.DownloadSubmission
import com.skyd.podaura.podnya.capabilities.downloads.model.DownloadSubmissionState
import com.skyd.podaura.podnya.capabilities.downloads.model.downloadEvidence
import com.skyd.podaura.podnya.capabilities.downloads.selection.ValidatedDownloadPlan
import com.skyd.podaura.podnya.core.AssistantConnection
import com.skyd.podaura.podnya.core.AssistantConnectionResult
import com.skyd.podaura.podnya.core.AssistantPhase
import com.skyd.podaura.podnya.core.AssistantRunEvent
import com.skyd.podaura.podnya.core.AssistantRuntime
import com.skyd.podaura.podnya.core.AssistantTurnRequest
import com.skyd.podaura.podnya.core.CapabilityRecord
import com.skyd.podaura.podnya.core.UnreadableContent
import com.skyd.podaura.podnya.model.bean.conversation.AssistantSessionState
import com.skyd.podaura.podnya.model.bean.conversation.AssistantSessionTurn
import com.skyd.podaura.podnya.model.bean.conversation.AssistantTurnStatus
import com.skyd.podaura.podnya.model.bean.conversation.ConversationStorageState
import com.skyd.podaura.podnya.model.bean.conversation.ConversationWorkspace
import com.skyd.podaura.podnya.model.db.ConversationRecordCodec
import com.skyd.podaura.podnya.model.repository.conversation.AssistantConversationRepository
import com.skyd.podaura.podnya.model.repository.conversation.ConversationStore
import com.skyd.podaura.podnya.model.repository.conversation.InMemoryConversationStore
import com.skyd.podaura.podnya.model.repository.conversation.conversationSettings
import com.skyd.podaura.podnya.model.repository.conversation.editDraft
import com.skyd.podaura.podnya.model.repository.conversation.retryLast
import com.skyd.podaura.podnya.model.repository.conversation.sendDraft
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DownloadConversationTest {
    @Test
    fun historyWritesShortTargetNamesAndPreservesUnknownTargets() {
        val payload = """
            {"choices":[{"id":"c","title":"Episode","options":[{"id":"1","title":"Audio","targets":[
              {"type":"Attachment","articleId":"a","url":"https://media.test/a.mp3"},
              {"type":"DirectLink","url":"https://media.test/b.mp3"}
            ]}]}]}
        """.trimIndent()
        val record = CapabilityRecord(capabilityId = "downloads", version = 1, payload = payload, fallback = "Episode")
        val content = DownloadHistoryCodec.decodeContent(record)
        val encoded = checkNotNull(DownloadHistoryCodec.encodeContent(content))
        assertEquals(Json.parseToJsonElement(payload), Json.parseToJsonElement(encoded.payload))

        val qualified = payload.replace("\"type\":\"", "\"type\":\"old.package.")
        assertEquals(content, DownloadHistoryCodec.decodeContent(record.copy(payload = qualified)))

        val unknown = Json.encodeToString(record.copy(payload = payload.replace("\"DirectLink\"", "\"old.package.Other\"")))
        val codec = ConversationRecordCodec(listOf(DownloadHistoryCodec))
        assertEquals(unknown, assertIs<UnreadableContent>(codec.content(unknown)).raw)
    }

    @Test
    fun stopBeforeSubmissionAndRetryAfterSubmissionOnlyReads() = runTest {
        val planning = Channel<Unit>(Channel.UNLIMITED)
        val submission = Channel<Unit>(Channel.UNLIMITED)
        val queue = TestAssistantDownloadQueue { submission.receive() }
        val assistant = Runtime {
            planning.receive()
            emit(AssistantRunEvent.Handoff(plan(targets = listOf(target))))
        }
        val repo = AssistantConversationRepository(
            assistant = assistant, settings = conversationSettings(), scope = backgroundScope, capabilities = listOf(DownloadConversation(queue)),
        )
        repo.editDraft("Download the episode")
        assertTrue(repo.sendDraft())
        runCurrent()
        val id = repo.state.value.selected.id
        repo.stop(id)
        runCurrent()
        assertEquals(AssistantTurnStatus.STOPPED, repo.state.value.selected.turns.single().status)
        assertTrue(queue.calls.isEmpty())
        assertTrue(repo.retryLast())
        planning.send(Unit)
        runCurrent()
        assertEquals(DownloadSubmissionState.SUBMITTING, repo.content().submissions.single().state)
        assertEquals(1, queue.calls.size)
        repo.stop(id)
        assertTrue(repo.retryLast())
        submission.send(Unit)
        runCurrent()
        assertEquals(1, repo.content().submissions.single().receipt?.added)
        assertEquals(1, queue.calls.size)
        assertEquals(2, assistant.requests.size)
    }

    @Test
    fun stopDuringDurableMarkerWriteLeavesTheRequestRetryableAndNeverEnqueues() = runTest {
        interruptDuringMarkerWrite(closeRepository = false)
    }

    @Test
    fun closeDuringDurableMarkerWriteLeavesTheRequestRetryableAfterReopening() = runTest {
        interruptDuringMarkerWrite(closeRepository = true)
    }

    private suspend fun TestScope.interruptDuringMarkerWrite(closeRepository: Boolean) {
        val writeStarted = CompletableDeferred<Unit>()
        val allowWrite = CompletableDeferred<Unit>()
        val memory = InMemoryConversationStore()
        val store = object : ConversationStore by memory {
            override suspend fun commit(metadata: AssistantSessionState?, turn: AssistantSessionTurn?, workspace: ConversationWorkspace?) {
                if (turn?.content is DownloadContent) {
                    writeStarted.complete(Unit)
                    allowWrite.await()
                }
                memory.commit(metadata = metadata, turn = turn, workspace = workspace)
            }
        }
        val queue = TestAssistantDownloadQueue()
        val assistant = Runtime { emit(AssistantRunEvent.Handoff(plan(targets = listOf(target)))) }
        val settings = conversationSettings()
        fun repository(scope: CoroutineScope) = AssistantConversationRepository(
            assistant = assistant,
            settings = settings,
            store = store,
            scope = scope,
            capabilities = listOf(DownloadConversation(queue)),
        )
        val repo = repository(if (closeRepository) applicationScope() else backgroundScope)
        repo.editDraft("Download the episode")
        assertTrue(repo.sendDraft())
        writeStarted.await()
        val id = repo.state.value.selected.id
        val stopping = if (closeRepository) repo.close() else backgroundScope.launch { repo.stop(id) }
        runCurrent()
        allowWrite.complete(Unit)
        stopping.join()
        runCurrent()
        val stopped = store.tail(id, 1).single()
        assertEquals(AssistantTurnStatus.STOPPED, stopped.status)
        assertFalse(stopped.content is DownloadContent)
        assertTrue(queue.calls.isEmpty())

        val active = if (closeRepository) repository(backgroundScope) else repo
        runCurrent()
        assertTrue(active.retryLast())
        runCurrent()
        assertEquals(1, queue.calls.size)
        assertEquals(1, active.content().submissions.single().receipt?.added)
    }

    @Test
    fun aDraftEditedDuringMarkerWriteCannotRestoreAnObsoleteRunningTurn() = runTest {
        val writeStarted = CompletableDeferred<Unit>()
        val allowWrite = CompletableDeferred<Unit>()
        val memory = InMemoryConversationStore()
        val store = object : ConversationStore by memory {
            override suspend fun commit(metadata: AssistantSessionState?, turn: AssistantSessionTurn?, workspace: ConversationWorkspace?) {
                if (turn?.content is DownloadContent) {
                    writeStarted.complete(Unit)
                    allowWrite.await()
                }
                memory.commit(metadata = metadata, turn = turn, workspace = workspace)
            }
        }
        val queue = TestAssistantDownloadQueue()
        val assistant = Runtime {
            emit(AssistantRunEvent.Progress(AssistantPhase.PROCESSING))
            emit(AssistantRunEvent.Handoff(plan(targets = listOf(target))))
        }
        val settings = conversationSettings()
        val repo = AssistantConversationRepository(
            assistant = assistant, settings = settings, store = store,
            scope = applicationScope(), saveClock = { testScheduler.currentTime }, capabilities = listOf(DownloadConversation(queue)),
        )
        repo.editDraft("Download the episode")
        assertTrue(repo.sendDraft())
        writeStarted.await()
        repo.editDraft("Write the next question")
        allowWrite.complete(Unit)
        advanceTimeBy(2_500)
        runCurrent()
        val id = repo.state.value.selected.id
        val written = store.tail(id, 1).single()
        assertEquals(AssistantTurnStatus.COMPLETED, written.status)
        assertEquals(1, assertIs<DownloadContent>(written.content).submissions.single().receipt?.added)
        repo.close().join()

        val reopened = AssistantConversationRepository(
            assistant = assistant, settings = settings, store = store, scope = backgroundScope, capabilities = listOf(DownloadConversation(queue)),
        )
        runCurrent()
        assertEquals("Write the next question", reopened.state.value.selected.draft)
        assertEquals(1, reopened.content().submissions.single().receipt?.added)
        assertEquals(1, queue.calls.size)
    }

    @Test
    fun stoppedTextSelectionRemainsRetryableAfterFailedStopWriteAndRestart() = runTest {
        val writingSelection = CompletableDeferred<Unit>()
        val allowWrite = CompletableDeferred<Unit>()
        val memory = InMemoryConversationStore()
        var rollbackFailed = false
        val store = object : ConversationStore by memory {
            override suspend fun commit(metadata: AssistantSessionState?, turn: AssistantSessionTurn?, workspace: ConversationWorkspace?) {
                if (turn?.id == 2L && (turn.content as? DownloadContent)?.preparing == true) {
                    writingSelection.complete(Unit)
                    allowWrite.await()
                } else if (turn?.id == 2L && turn.status == AssistantTurnStatus.STOPPED && !rollbackFailed) {
                    rollbackFailed = true
                    error("Temporary journal error")
                }
                memory.commit(metadata = metadata, turn = turn, workspace = workspace)
            }
        }
        val option = choice("episode", "mp3")
        val queue = TestAssistantDownloadQueue()
        val assistant = Runtime { request ->
            val origin = request.downloadEvidence.pendingSelections.singleOrNull()
            val result = if (origin == null) {
                plan(targets = emptyList(), choices = listOf(option))
            } else {
                ValidatedDownloadPlan(
                    targets = option.options.single().targets,
                    choices = emptyList(),
                    issues = emptyList(),
                    resolvedChoiceIds = setOf(option.id),
                    originTurnId = origin.originTurnId,
                )
            }
            emit(AssistantRunEvent.Handoff(result))
        }
        val settings = conversationSettings()
        val owner = applicationScope()
        val repo = AssistantConversationRepository(
            assistant = assistant, settings = settings, store = store, scope = owner, capabilities = listOf(DownloadConversation(queue)),
        )
        repo.editDraft("Download that episode")
        assertTrue(repo.sendDraft())
        runCurrent()
        repo.editDraft("The MP3 version")
        assertTrue(repo.sendDraft())
        writingSelection.await()
        val id = repo.state.value.selected.id
        val stopping = backgroundScope.launch { repo.stop(id) }
        runCurrent()
        allowWrite.complete(Unit)
        stopping.join()
        runCurrent()
        assertEquals(ConversationStorageState.SAVE_FAILED, repo.state.value.storage)
        assertEquals(listOf(option), repo.content(index = 0).availableChoices)
        assertEquals(listOf(option), assertIs<DownloadContent>(store.turn(id, 1)?.content).availableChoices)
        assertTrue(assertIs<DownloadContent>(store.turn(id, 2)?.content).preparing)
        owner.cancel()
        runCurrent()

        val reopened = AssistantConversationRepository(
            assistant = assistant, settings = settings, store = store, scope = backgroundScope, capabilities = listOf(DownloadConversation(queue)),
        )
        runCurrent()
        assertEquals(AssistantTurnStatus.STOPPED, reopened.state.value.selected.lastStatus)
        assertEquals(listOf(option), reopened.content(index = 0).availableChoices)
        assertTrue(queue.calls.isEmpty())
        assertTrue(reopened.retryLast())
        runCurrent()
        assertEquals(1, reopened.content().submissions.single().receipt?.added)
    }

    @Test
    fun unresolvedScopeSurvivesRestartAndExceedingTheOriginalQuotaAddsNothing() = runTest {
        val firstTargets = (1..127).map { number ->
            AssistantDownloadTarget.Attachment(articleId = "alpha-$number", url = "https://example.test/alpha-$number.mp3")
        }
        val subscriptionRequest = DownloadScopeClarification(
            id = "subscription",
            originalClause = "Download the episodes from that podcast from a while ago",
            field = DownloadScopeField.SUBSCRIPTION,
        )
        val scopedRequest = DownloadScopeClarification(
            id = "dates",
            originalClause = "Download the Beta episodes from a while ago",
            field = DownloadScopeField.RANGE,
            feed = DownloadFeedReference(title = "Beta", feedUrl = "https://example.test/beta"),
        )
        val beta = listOf(
            AssistantDownloadTarget.Attachment(articleId = "beta-1", url = "https://example.test/beta-1.mp3"),
            AssistantDownloadTarget.Attachment(articleId = "beta-2", url = "https://example.test/beta-2.mp3"),
        )
        val queue = TestAssistantDownloadQueue()
        val assistant = Runtime { request ->
            val result = if (request.question.startsWith("Download")) {
                ValidatedDownloadPlan(
                    targets = firstTargets,
                    choices = emptyList(),
                    issues = listOf(DownloadIssue(
                        title = "Which subscription?",
                        kind = DownloadIssueKind.CLARIFICATION_REQUIRED,
                        scopeClarification = subscriptionRequest,
                    )),
                )
            } else if (request.question == "I meant Beta") {
                val pending = request.downloadEvidence.pendingSelections.single()
                assertEquals(listOf(subscriptionRequest), pending.scopeClarifications)
                ValidatedDownloadPlan(
                    targets = emptyList(),
                    choices = emptyList(),
                    issues = listOf(DownloadIssue(
                        title = "Beta",
                        kind = DownloadIssueKind.CLARIFICATION_REQUIRED,
                        scopeClarification = scopedRequest,
                    )),
                    resolvedChoiceIds = setOf(subscriptionRequest.id),
                    originTurnId = pending.originTurnId,
                )
            } else {
                val open = request.downloadEvidence.pendingSelections.single()
                assertEquals(open.originTurnId, request.downloadEvidence.latestMessageId)
                assertEquals(listOf(scopedRequest), open.scopeClarifications)
                ValidatedDownloadPlan(
                    targets = beta,
                    choices = emptyList(),
                    issues = emptyList(),
                    resolvedChoiceIds = setOf(scopedRequest.id),
                    originTurnId = open.originTurnId,
                )
            }
            emit(AssistantRunEvent.Handoff(result))
        }
        val store = InMemoryConversationStore()
        val settings = conversationSettings()
        val repo = AssistantConversationRepository(
            assistant = assistant, settings = settings, store = store, scope = applicationScope(), capabilities = listOf(DownloadConversation(queue)),
        )
        repo.editDraft("Download the latest 127 Alpha episodes and Beta from a while ago")
        assertTrue(repo.sendDraft())
        runCurrent()
        assertEquals(127, repo.content().submissions.single().receipt?.added)
        assertEquals(listOf(subscriptionRequest), repo.content().availableScopeClarifications)
        repo.editDraft("I meant Beta")
        assertTrue(repo.sendDraft())
        runCurrent()
        assertEquals(1, queue.calls.size)
        assertEquals(listOf(scopedRequest), repo.content(index = 0).availableScopeClarifications)
        assertTrue(repo.content().availableScopeClarifications.isEmpty())
        val codec = ConversationRecordCodec(listOf(DownloadHistoryCodec))
        assertEquals(repo.content(index = 0), codec.content(codec.content(repo.content(index = 0))))
        repo.close().join()

        val reopened = AssistantConversationRepository(
            assistant = assistant, settings = settings, store = store, scope = backgroundScope, capabilities = listOf(DownloadConversation(queue)),
        )
        runCurrent()
        reopened.editDraft("2026-08-01 to 2026-08-08")
        assertTrue(reopened.sendDraft())
        runCurrent()
        val receipt = checkNotNull(reopened.content().submissions.single().receipt)
        assertTrue(receipt.limitExceeded)
        assertEquals(1, receipt.remainingCount)
        assertEquals(2, receipt.matchedCount)
        assertEquals(0, receipt.added)
        assertEquals(1, queue.calls.map { it.first }.distinct().size)
        assertEquals(listOf(scopedRequest), reopened.content(index = 0).availableScopeClarifications)
    }

    @Test
    fun lookupsAndAnOlderOptionDoNotReplaceTheCurrentScopeClarification() = runTest {
        val scope = DownloadScopeClarification(
            id = "subscription",
            originalClause = "Download the latest episode from that podcast",
            field = DownloadScopeField.SUBSCRIPTION,
        )
        val lookups = listOf(
            "Who hosts Astronomy?",
            "Could you look up Ways to Download Podcasts",
            "能查一下，播客下载指南吗",
            "Look up Research and Then Download Mars",
        )
        val archiveQuestion = "Download the archived episode"
        val archiveTitle = "Archived trailer.mp3"
        val archived = choice("archived", "mp3").let { choice ->
            choice.copy(options = listOf(choice.options.single().copy(title = archiveTitle)))
        }
        val queue = TestAssistantDownloadQueue()
        val assistant = Runtime { request ->
            when (request.question) {
                archiveQuestion -> emit(AssistantRunEvent.Handoff(
                    plan(targets = emptyList(), choices = listOf(archived))
                ))

                scope.originalClause -> emit(AssistantRunEvent.Handoff(ValidatedDownloadPlan(
                    targets = emptyList(),
                    choices = emptyList(),
                    issues = listOf(DownloadIssue(
                        title = "Which subscription?",
                        kind = DownloadIssueKind.CLARIFICATION_REQUIRED,
                        scopeClarification = scope,
                    )),
                )))

                in lookups -> {
                    emit(AssistantRunEvent.ReplyDelta("Audio for Beginners"))
                    emit(AssistantRunEvent.Completed)
                }

                archiveTitle -> {
                    assertEquals(2L, request.downloadEvidence.pendingSelections.single { scope in it.scopeClarifications }.originTurnId)
                    emit(AssistantRunEvent.Handoff(ValidatedDownloadPlan(
                        targets = archived.options.single().targets,
                        choices = emptyList(),
                        issues = emptyList(),
                        resolvedChoiceIds = setOf(archived.id),
                        originTurnId = 1L,
                    )))
                }

                else -> {
                    assertEquals(1L, request.downloadEvidence.latestMessageId)
                    assertEquals(2L, request.downloadEvidence.pendingSelections.single { scope in it.scopeClarifications }.originTurnId)
                    emit(AssistantRunEvent.Handoff(ValidatedDownloadPlan(
                        targets = listOf(target),
                        choices = emptyList(),
                        issues = emptyList(),
                        resolvedChoiceIds = setOf(scope.id),
                        originTurnId = 2L,
                    )))
                }
            }
        }
        val repo = AssistantConversationRepository(
            assistant = assistant,
            settings = conversationSettings(),
            scope = backgroundScope,
            capabilities = listOf(DownloadConversation(queue)),
        )
        val questions = listOf(archiveQuestion, scope.originalClause) +
                List(41) { lookups[it % lookups.size] } + listOf(archiveTitle, "Audio for Beginners")
        for (question in questions) {
            repo.editDraft(question)
            assertTrue(repo.sendDraft())
            runCurrent()
        }
        val id = repo.state.value.selected.id
        assertEquals(listOf("$id:1", "$id:2"), queue.calls.map { it.first })
        assertEquals(1, repo.content().submissions.single().receipt?.added)
    }

    @Test
    fun scopeClarificationCanAddAttachmentChoicesToTheOriginalBatchWithoutSubmitting() = runTest {
        val scope = DownloadScopeClarification(
            id = "subscription",
            originalClause = "Download the latest episode from that podcast",
            field = DownloadScopeField.SUBSCRIPTION,
        )
        val alternative = choice("beta", "mp3")
        val queue = TestAssistantDownloadQueue()
        val assistant = Runtime { request ->
            val pending = request.downloadEvidence.pendingSelections.singleOrNull()
            val result = if (pending == null) {
                ValidatedDownloadPlan(
                    targets = listOf(target),
                    choices = emptyList(),
                    issues = listOf(DownloadIssue(
                        title = "Subscription",
                        kind = DownloadIssueKind.CLARIFICATION_REQUIRED,
                        scopeClarification = scope,
                    )),
                )
            } else {
                ValidatedDownloadPlan(
                    targets = emptyList(),
                    choices = listOf(alternative),
                    issues = emptyList(),
                    resolvedChoiceIds = setOf(scope.id),
                    originTurnId = pending.originTurnId,
                )
            }
            emit(AssistantRunEvent.Handoff(result))
        }
        val repo = AssistantConversationRepository(
            assistant = assistant, settings = conversationSettings(), scope = backgroundScope, capabilities = listOf(DownloadConversation(queue)),
        )
        repo.editDraft("Download the file and the latest episode from that podcast")
        assertTrue(repo.sendDraft())
        runCurrent()
        repo.editDraft("I meant Beta")
        assertTrue(repo.sendDraft())
        runCurrent()
        assertEquals(1, queue.calls.size)
        assertTrue(repo.content().choicesAddedToOrigin)
        assertEquals(listOf(alternative), repo.content(index = 0).availableChoices)
        assertTrue(repo.content(index = 0).availableScopeClarifications.isEmpty())

        val id = repo.state.value.selected.id
        repo.performAction(
            id = id,
            action = SelectDownloadOption(turnId = 1, choiceId = alternative.id, optionId = "1"),
        )
        runCurrent()
        assertEquals(2, queue.calls.size)
        assertEquals(setOf("$id:1"), queue.calls.map { it.first }.toSet())
    }

    @Test
    fun repeatedButtonsAndTextClarificationsUseOriginalBatchAndPreserveNewerStreaming() = runTest {
        val release = Channel<Unit>(Channel.UNLIMITED)
        val stream = Channel<Unit>(Channel.UNLIMITED)
        val queue = TestAssistantDownloadQueue { release.receive() }
        val first = choice("first", "mp3")
        val second = choice("second", "ogg")
        val assistant = Runtime { request ->
            when (request.question) {
                "Download these" -> emit(AssistantRunEvent.Handoff(
                    plan(targets = listOf(target), choices = listOf(first, second))
                ))

                "Why?" -> {
                    emit(AssistantRunEvent.ReplyDelta("First "))
                    stream.receive()
                    emit(AssistantRunEvent.ReplyDelta("and second."))
                    emit(AssistantRunEvent.Completed)
                }

                else -> {
                    val pending = request.downloadEvidence.pendingSelections.single()
                    assertEquals(listOf(second), pending.choices)
                    emit(AssistantRunEvent.Handoff(
                        ValidatedDownloadPlan(
                            targets = second.options.single().targets,
                            choices = emptyList(),
                            issues = emptyList(),
                            resolvedChoiceIds = setOf(second.id),
                            originTurnId = pending.originTurnId,
                        )
                    ))
                }
            }
        }
        val store = InMemoryConversationStore()
        val repo = AssistantConversationRepository(
            assistant = assistant, settings = conversationSettings(), store = store, scope = backgroundScope, capabilities = listOf(DownloadConversation(queue)),
        )
        repo.editDraft("Download these")
        assertTrue(repo.sendDraft())
        runCurrent()
        val id = repo.state.value.selected.id
        val origin = repo.state.value.selected.lastTurnId
        repo.editDraft("Why?")
        assertTrue(repo.sendDraft())
        runCurrent()
        repeat(2) {
            repo.performAction(
                id = id,
                action = SelectDownloadOption(turnId = origin, choiceId = first.id, optionId = "1"),
            )
        }
        release.send(Unit)
        release.send(Unit)
        runCurrent()
        assertEquals(2, queue.calls.size)
        assertEquals("First ", repo.state.value.selected.turns.last().reply)
        assertEquals(AssistantTurnStatus.RUNNING, repo.state.value.selected.lastStatus)
        stream.send(Unit)
        runCurrent()
        assertEquals("First and second.", store.tail(id, 1).single().reply)

        repo.editDraft("The ogg version")
        assertTrue(repo.sendDraft())
        release.send(Unit)
        runCurrent()
        assertEquals(3, queue.calls.size)
        assertEquals(setOf("$id:$origin"), queue.calls.map { it.first }.toSet())
        assertEquals(3, repo.content(index = 0).submissions.count { it.receipt?.added == 1 })
        assertEquals(1, repo.content().submissions.single().receipt?.added)
        assertTrue(repo.content(index = 0).availableChoices.isEmpty())
    }

    @Test
    fun textSelectionFindsTheOriginalRequestOutsideTheLoadedHistory() = runTest {
        val store = InMemoryConversationStore()
        val option = choice("episode", "ogg")
        val metadata = AssistantSessionState(lastTurnId = 65, lastStatus = AssistantTurnStatus.COMPLETED)
        val origin = AssistantSessionTurn(
            id = 1,
            question = "Download the episode",
            status = AssistantTurnStatus.COMPLETED,
            content = DownloadContent(choices = listOf(option)),
        )
        store.commit(metadata = metadata, turn = origin, workspace = ConversationWorkspace(selectedId = metadata.id))
        store.commitTurns(
            id = metadata.id,
            turns = (2L..65L).map { id ->
                AssistantSessionTurn(
                    id = id,
                    question = "Question $id",
                    status = AssistantTurnStatus.COMPLETED,
                    content = DownloadContent().takeIf { id <= 45 },
                )
            },
        )
        val queue = TestAssistantDownloadQueue()
        val assistant = Runtime { request ->
            val pending = request.downloadEvidence.pendingSelections.single()
            assertEquals(origin.id, pending.originTurnId)
            assertEquals(listOf(option), pending.choices)
            emit(AssistantRunEvent.Handoff(ValidatedDownloadPlan(
                targets = option.options.single().targets,
                choices = emptyList(),
                issues = emptyList(),
                resolvedChoiceIds = setOf(option.id),
                originTurnId = pending.originTurnId,
            )))
        }
        val repo = AssistantConversationRepository(
            assistant = assistant,
            settings = conversationSettings(),
            store = store,
            scope = backgroundScope,
            capabilities = listOf(DownloadConversation(queue)),
        )
        runCurrent()
        assertTrue(repo.state.value.selected.turns.none { it.id == origin.id })

        repo.editDraft("The Ogg version of Episode episode")
        assertTrue(repo.sendDraft())
        runCurrent()

        assertEquals("${metadata.id}:${origin.id}", queue.calls.single().first)
        assertEquals(1, repo.content().submissions.single().receipt?.added)
        val updatedOrigin = assertIs<DownloadContent>(store.turn(id = metadata.id, turnId = origin.id)?.content)
        assertTrue(updatedOrigin.availableChoices.isEmpty())
        assertEquals(1, updatedOrigin.submissions.single().receipt?.added)
    }

    @Test
    fun restoresCommittedAndMissingReceiptsWithoutSubmitting() = runTest {
        val store = InMemoryConversationStore()
        val queue = TestAssistantDownloadQueue()
        val codec = ConversationRecordCodec(listOf(DownloadHistoryCodec))
        val metadata = AssistantSessionState(lastTurnId = 1, lastStatus = AssistantTurnStatus.COMPLETED)
        val content = DownloadContent(
            submissions = listOf(
                DownloadSubmission(id = "committed", messageId = 1, requestedCount = 1),
                DownloadSubmission(id = "interrupted", messageId = 1, requestedCount = 1),
            ),
        )
        val turn = AssistantSessionTurn(
            id = 1, question = "Download this", content = content, status = AssistantTurnStatus.COMPLETED,
        )
        val persisted = codec.decode(codec.encode(id = metadata.id, value = turn))
        assertEquals(content, persisted.content)
        store.commit(metadata = metadata, turn = persisted)
        queue.submit(batchId = "${metadata.id}:1", submissionId = "committed", targets = listOf(target))
        queue.calls.clear()
        val assistant = Runtime { error("Restoring downloads cannot run the model") }
        val repo = AssistantConversationRepository(
            assistant = assistant, settings = conversationSettings(), store = store, scope = backgroundScope, capabilities = listOf(DownloadConversation(queue)),
        )
        runCurrent()
        assertEquals(1, repo.content().submissions.first().receipt?.added)
        assertEquals(DownloadSubmissionState.INTERRUPTED, repo.content().submissions.last().state)
        assertTrue(repo.retryLast())
        runCurrent()
        assertTrue(queue.calls.isEmpty())
        assertTrue(assistant.requests.isEmpty())
        assertFalse(repo.content().submissions.any { it.state == DownloadSubmissionState.SUBMITTING })
    }

    private fun AssistantConversationRepository.content(index: Int = state.value.selected.turns.lastIndex) =
        assertIs<DownloadContent>(state.value.selected.turns[index].content)

    private fun TestScope.applicationScope() = CoroutineScope(
        backgroundScope.coroutineContext + SupervisorJob(backgroundScope.coroutineContext[Job]),
    )

    private fun choice(id: String, version: String) = DownloadChoice(
        id = id,
        title = "Episode $id",
        options = listOf(DownloadOption(
            id = "1",
            title = version,
            targets = listOf(AssistantDownloadTarget.Attachment(articleId = id, url = "https://example.test/$id.$version")),
        )),
    )

    private fun plan(targets: List<AssistantDownloadTarget>, choices: List<DownloadChoice> = emptyList()) =
        ValidatedDownloadPlan(targets = targets, choices = choices, issues = emptyList())

    private val target = AssistantDownloadTarget.Attachment(articleId = "episode", url = "https://example.test/episode.mp3")

    private class Runtime(
        val respond: suspend FlowCollector<AssistantRunEvent>.(AssistantTurnRequest) -> Unit,
    ) : AssistantRuntime {
        val requests = mutableListOf<AssistantTurnRequest>()
        override val available = true
        override fun run(request: AssistantTurnRequest, connection: AssistantConnection) = flow {
            requests += request
            respond(request)
        }
        override suspend fun verify(connection: suspend () -> AssistantConnection) = AssistantConnectionResult(true)
    }
}

internal class TestAssistantDownloadQueue(private val beforeSubmit: suspend () -> Unit = {}) : IAssistantDownloadRepository {
    val calls = mutableListOf<Pair<String, String>>()
    private val receipts = mutableMapOf<Pair<String, String>, AssistantDownloadReceipt>()

    override suspend fun articlesByIds(articleIds: List<String>): List<AssistantDownloadArticle> = emptyList()
    override suspend fun articlesInFeed(scope: AssistantDownloadScope): List<AssistantDownloadArticle> = emptyList()
    override suspend fun checkDirectLink(url: String) = true

    override suspend fun submit(
        batchId: String,
        submissionId: String,
        targets: List<AssistantDownloadTarget>,
    ): AssistantDownloadReceipt {
        val key = batchId to submissionId
        calls += key
        beforeSubmit()
        val previous = receipts.values.filter { it.batchId == batchId }.flatMap { it.queued.entries }
            .associate { it.toPair() }
        val remaining = 128 - previous.size
        val matched = targets.associate { it.key to "queued" }
        val existing = previous.filterKeys { it in matched }
        val new = matched - existing.keys
        val overLimit = new.size > remaining
        val queued = if (overLimit) emptyMap() else new
        return AssistantDownloadReceipt(
            batchId = batchId,
            submissionId = submissionId,
            queued = queued,
            existing = existing,
            failed = emptySet(),
            matchedCount = matched.size,
            remainingCount = remaining - queued.size,
            limitExceeded = overLimit,
        ).also { receipts[key] = it }
    }

    override suspend fun receipt(batchId: String, submissionId: String) = receipts[batchId to submissionId]
}
