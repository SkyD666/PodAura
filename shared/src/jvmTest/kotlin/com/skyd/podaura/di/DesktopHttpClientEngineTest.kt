package com.skyd.podaura.di

import io.ktor.client.HttpClient
import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopHttpClientEngineTest {
    @Test
    fun defaultHttpClientUsesOkHttp() {
        HttpClient().use { client ->
            assertEquals(
                "io.ktor.client.engine.okhttp.OkHttpEngine",
                client.engine::class.qualifiedName,
            )
        }
    }
}
