package com.skyd.podaura.podnya.model.db

import com.skyd.podaura.podnya.core.AssistantProtocol
import com.skyd.podaura.podnya.model.bean.conversation.AssistantReplySource
import com.skyd.podaura.podnya.model.bean.conversation.AssistantSessionTurn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.uuid.Uuid

class ConversationProtocolSourceTest {
    @Test
    fun sourceProtocolSurvivesStorageAndOldOrUnknownSourcesRemainReadable() {
        val codec = ConversationRecordCodec(emptyList())
        val id = Uuid.random()
        val source = AssistantReplySource(
            configId = "config", revision = 1, baseUrl = "https://example.org/v1/",
            modelId = "model", protocol = AssistantProtocol.ANTHROPIC_MESSAGES.id,
        )
        val record = codec.encode(
            id = id,
            value = AssistantSessionTurn(id = 1, question = "question", source = source),
        )
        assertEquals(source, codec.decode(record).source)
        val legacy = record.copy(source = """{"configId":"config","revision":1,"baseUrl":"https://example.org/v1/","modelId":"model"}""")
        assertNull(codec.decode(legacy).source!!.protocol)
        val future = record.copy(source = record.source!!.replace("anthropic-messages", "future-protocol"))
        assertEquals("future-protocol", codec.decode(future).source!!.protocol)
        val extended = record.copy(source = record.source!!.dropLast(1) + ",\"futureField\":{\"keep\":true}}")
        assertEquals(extended.source, codec.encode(id = id, value = codec.decode(extended)).source)
        val unreadable = record.copy(source = "{unknown future representation")
        val decoded = codec.decode(unreadable)
        assertNull(decoded.source)
        assertEquals(unreadable.source, codec.encode(id = id, value = decoded).source)
    }
}
