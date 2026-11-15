package com.skyd.podaura.podnya.settings

import com.skyd.podaura.podnya.core.AssistantProtocol
import com.skyd.podaura.podnya.core.assistantEndpointPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AssistantProviderPresetsTest {
    @Test
    fun catalogueHasUniqueIdsAndUsableDefaultsWithoutChangingSavedModels() {
        val entries = AssistantProviderPresets.entries
        assertEquals(setOf("openai", "deepseek", "claude", "gemini", "grok"), entries.map { it.id }.toSet())
        assertEquals(5, entries.size)
        entries.forEach { preset ->
            assertTrue(preset.defaultModelId in preset.models)
            assertEquals(preset.models.distinct(), preset.models)
            val saved = AssistantSettings(baseUrl = preset.baseUrl, modelId = "removed-model")
            assertEquals(preset.id, AssistantProviderPresets.selection(saved))
            assertEquals("removed-model", saved.modelId)
            assertEquals(AssistantProtocol.OPENAI_COMPATIBLE, saved.protocol)
        }
    }

    @Test
    fun explicitCustomAndUnknownProviderIdsRemainExplicitAndHostsAreNeverGuessed() {
        for (id in listOf("custom", "future-brand")) {
            assertEquals(id, AssistantProviderPresets.selection(AssistantSettings(
                baseUrl = "https://api.openai.com/v1/", modelId = "model", providerId = id,
            )))
        }
        for (url in listOf("https://api.openai.com.evil.example/v1/", "https://api.openai.com/other/")) {
            assertEquals("custom", AssistantProviderPresets.selection(AssistantSettings(baseUrl = url)))
        }
        assertEquals(null, AssistantProviderPresets.selection(AssistantSettings()))
        assertEquals("gemini", AssistantProviderPresets.selection(AssistantSettings(
            baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai/",
        )))
    }

    @Test
    fun currentOpenAiToolModelsUseResponsesOnlyAtTheOfficialService() {
        for (model in AssistantProviderPresets.find("openai")!!.models) {
            assertEquals("responses", assistantEndpointPath(baseUrl = "https://api.openai.com/v1/", modelId = model))
            assertEquals("chat/completions", assistantEndpointPath(baseUrl = "https://gateway.example/v1/", modelId = model))
        }
    }
}
