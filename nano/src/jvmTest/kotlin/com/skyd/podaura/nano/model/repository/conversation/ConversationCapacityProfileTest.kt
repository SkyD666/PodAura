package com.skyd.podaura.nano.model.repository.conversation

import com.skyd.podaura.nano.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.lang.management.ManagementFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Diagnostic measurements, not timing thresholds: verify near-budget concurrent streaming. */
class ConversationCapacityProfileTest {
    @Test fun largeCardsAndThreeStreamsKeepAllContent() = runBlocking {
        val owner = CoroutineScope(coroutineContext + SupervisorJob())
        val gate = CompletableDeferred<Unit>()
        var streaming = false
        var started = 0
        val runtime = object : AssistantRuntime {
            override val available = true
            override suspend fun verify(connection: suspend () -> AssistantConnection) = AssistantConnectionResult(true)
            override fun run(request: AssistantTurnRequest, connection: AssistantConnection) = flow {
                if (streaming) { if (++started == 3) gate.complete(Unit); gate.await() }
                emit(AssistantRunEvent.ContentReady(LargeContent("喵".repeat(AssistantLimits.CONTENT_BYTES / 3))))
                if (streaming) repeat(AssistantLimits.REPLY_CHARS) {
                    emit(AssistantRunEvent.ReplyDelta("喵")); yield()
                }
                emit(AssistantRunEvent.Completed)
            }
        }
        val settings = conversationSettings()
        val repo = AssistantConversationRepository(runtime, settings, scope = owner)
        try {
            repeat(12) {
                repo.editDraft("retained $it")
                assertTrue(repo.sendDraft())
                repo.state.first { it.runningIds.isEmpty() }
            }
            System.gc()
            val before = ManagementFactory.getMemoryMXBean().heapMemoryUsage.used
            val allocations = ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
            val threadId = Thread.currentThread().id
            val allocatedBefore = allocations.getThreadAllocatedBytes(threadId)
            val start = System.nanoTime()
            streaming = true
            repeat(3) {
                repo.newConversation()
                repo.editDraft("stream $it")
                assertTrue(repo.sendDraft())
            }
            repo.state.first { it.runningIds.isEmpty() }
            val millis = (System.nanoTime() - start) / 1_000_000
            val allocated = allocations.getThreadAllocatedBytes(threadId) - allocatedBefore
            System.gc()
            val heap = ManagementFactory.getMemoryMXBean().heapMemoryUsage.used
            assertEquals(4, repo.state.value.summaries.size)
            assertEquals(AssistantLimits.REPLY_CHARS, repo.state.value.selected.turns.single().reply.length)
            val report = "15 retained bounded cards, 3 concurrent streams, 2400 deltas\n" +
                "Streaming elapsed: $millis ms\n" +
                "Current-thread allocations during streaming: $allocated bytes\n" +
                "JVM heap after GC: $heap bytes (before streaming: $before bytes)\n" +
                "Synthetic repository profile; heap includes test/runtime infrastructure, not device UI latency.\n"
            File("build/reports/nano-capacity-profile.txt").apply { parentFile.mkdirs(); writeText(report) }
            println(report)
        } finally { repo.close() }
    }

    private class LargeContent(val text: String) : AssistantContent {
        override val retainedBytes = text.encodeToByteArray().size
        override fun historySnapshot() = AssistantHistorySnapshot("profile", "bounded summary", "{}")
    }
}
