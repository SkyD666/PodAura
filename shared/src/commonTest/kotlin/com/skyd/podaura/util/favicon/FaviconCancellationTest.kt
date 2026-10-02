package com.skyd.podaura.util.favicon

import com.skyd.podaura.util.favicon.extractor.BaseUrlIconTagExtractor
import com.skyd.podaura.util.favicon.extractor.HardCodedExtractor
import com.skyd.podaura.util.favicon.extractor.IconTagExtractor
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.api.createClientPlugin
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FaviconCancellationTest {
    @Test
    fun allIconRequestsStopWhenTheirParentIsCancelled() = runTest {
        val started = CompletableDeferred<Unit>()
        var requests = 0
        var stopped = 0
        val waitingRequest = createClientPlugin("WaitingRequest") {
            onRequest { _, _ ->
                if (++requests == 5) started.complete(Unit)
                try {
                    awaitCancellation()
                } finally {
                    stopped++
                }
            }
        }
        val config: HttpClientConfig<*>.() -> Unit = { install(waitingRequest) }
        val task = launch {
            listOf(
                HardCodedExtractor(config),
                IconTagExtractor(config),
                BaseUrlIconTagExtractor(config),
            ).forEach { extractor ->
                launch { extractor.extract("https://example.com/feed.xml") }
            }
        }

        started.await()
        task.cancelAndJoin()
        assertEquals(5, stopped)
    }
}
