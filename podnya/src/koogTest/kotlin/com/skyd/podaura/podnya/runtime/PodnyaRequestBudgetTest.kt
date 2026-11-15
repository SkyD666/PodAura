package com.skyd.podaura.podnya.runtime

import com.skyd.podaura.podnya.core.AssistantError
import com.skyd.podaura.podnya.core.AssistantException
import com.skyd.podaura.podnya.core.AssistantLimits
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PodnyaRequestBudgetTest {
    @Serializable
    private data class Request(val text: String, val defaultValue: String = "included")

    @Test
    fun objectRequestsUseActualSerializedUtf8BytesIncludingDefaults() = runTest {
        val json = Json { encodeDefaults = true }
        val bodies = mutableListOf<String>()
        val base = HttpClient(MockEngine { request ->
            bodies += request.body.toByteArray().decodeToString()
            respond("{}")
        })
        val client = PodnyaHttpFactory(baseClient = base, responses = false).create(
            clientName = "budget-test", baseUrl = "https://example.test/v1/",
            headers = emptyMap(), queryParameters = emptyMap(),
            requestTimeoutMillis = 1000, connectTimeoutMillis = 1000, socketTimeoutMillis = 1000,
            json = json,
        )
        try {
            val request = Request("你好\"\n")
            client.post(
                path = "chat/completions", requestBody = request,
                requestBodyType = Request::class, responseType = String::class,
            )
            assertEquals(listOf(json.encodeToString(request)), bodies)
            val tooLarge = Request("喵".repeat(AssistantLimits.PROMPT_BYTES / 3))
            val failure = assertFailsWith<AssistantException> {
                client.post(
                    path = "chat/completions", requestBody = tooLarge,
                    requestBodyType = Request::class, responseType = String::class,
                )
            }
            assertEquals(AssistantError.BUDGET_EXHAUSTED, failure.error)
            assertEquals(1, bodies.size)
        } finally {
            client.close()
            base.close()
        }
    }
}
