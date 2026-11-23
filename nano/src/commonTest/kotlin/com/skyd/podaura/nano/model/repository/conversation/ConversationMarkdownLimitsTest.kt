package com.skyd.podaura.nano.model.repository.conversation

import com.skyd.podaura.nano.core.AssistantAnswerKind
import com.skyd.podaura.nano.core.AssistantContext
import com.skyd.podaura.nano.core.AssistantError
import com.skyd.podaura.nano.core.AssistantException
import com.skyd.podaura.nano.core.AssistantLimits
import com.skyd.podaura.nano.core.AssistantRunEvent
import com.skyd.podaura.nano.model.bean.AssistantReplyContent
import com.skyd.podaura.nano.model.bean.conversation.AssistantSessionTurn
import com.skyd.podaura.nano.model.bean.conversation.AssistantTurnStatus
import com.skyd.podaura.nano.model.db.ConversationRecordCodec
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class ConversationMarkdownLimitsTest {
    @Test
    fun rawMarkdownRoundTripsWithoutSchemaChangesAndHistoryStaysBounded() {
        val markdown = "# " + "喵".repeat(7_998)
        val original = AssistantSessionTurn(
            id = 1,
            question = "**question**",
            reply = markdown,
            status = AssistantTurnStatus.COMPLETED,
        )
        val codec = ConversationRecordCodec(emptyList())
        val restored = codec.decode(codec.encode(id = Uuid.random(), value = original))
        assertEquals(markdown, restored.reply)
        val generation = ConversationGeneration()
        val history = generation.request(
            turns = listOf(restored),
            question = "next",
            context = AssistantContext.Empty,
        ).history
        assertEquals(markdown.take(800), history.single().reply)
        val fallback = original.copy(content = AssistantReplyContent(
            parts = emptyList(),
            kind = AssistantAnswerKind.CLARIFICATION,
            text = markdown,
        ))
        assertEquals(800, fallback.content?.historySnapshot()?.summary?.length)
        assertEquals(markdown, (codec.content(codec.content(fallback.content)) as AssistantReplyContent).text)
        assertEquals(800, generation.request(
            turns = listOf(fallback),
            question = "next",
            context = AssistantContext.Empty,
        ).history.single().reply.length)
    }

    @Test
    fun overflowCannotBecomeSuccessfulAndExistingTextIsRetained() {
        val generation = ConversationGeneration().apply {
            turn = AssistantSessionTurn(id = 1, question = "question")
        }
        generation.update(AssistantRunEvent.ReplyDelta("x".repeat(8_000)))
        assertEquals(AssistantTurnStatus.RUNNING, generation.turn?.status)
        generation.update(AssistantRunEvent.ReplyDelta("extra"))
        assertEquals(AssistantError.STREAM_INTERRUPTED, generation.turn?.error)
        assertEquals(AssistantTurnStatus.FAILED, generation.turn?.status)
        assertEquals(8_000, generation.turn?.reply?.length)
        assertFalse(generation.valid)
        assertNull(generation.update(AssistantRunEvent.Completed))
    }

    @Test
    fun replyHeadroomFitsTheExistingTwoMiBTurnBudget() {
        assertEquals(2 * 1024 * 1024, AssistantLimits.TURN_BYTES)
        assertEquals(
            AssistantLimits.TURN_BYTES,
            AssistantLimits.CONTENT_BYTES + (AssistantLimits.INPUT_CHARS + AssistantLimits.REPLY_CHARS) * 4 + 128,
        )
        val generation = ConversationGeneration().apply {
            turn = AssistantSessionTurn(id = 1, question = "question")
        }
        val tooLarge = AssistantReplyContent(
            parts = emptyList(),
            kind = AssistantAnswerKind.CLARIFICATION,
            text = "x".repeat(AssistantLimits.CONTENT_BYTES),
        )
        assertTrue(tooLarge.retainedBytes > AssistantLimits.CONTENT_BYTES)
        assertEquals(AssistantError.BUDGET_EXHAUSTED, assertFailsWith<AssistantException> {
            generation.update(AssistantRunEvent.ContentReady(tooLarge))
        }.error)
    }
}
