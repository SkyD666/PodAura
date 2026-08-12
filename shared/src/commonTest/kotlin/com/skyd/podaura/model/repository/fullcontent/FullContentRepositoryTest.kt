package com.skyd.podaura.model.repository.fullcontent

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.charsets.encodeToByteArray
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FullContentRepositoryTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun fetchesAndExtractsHtml() = runTest {
        val engine = MockEngine { request ->
            assertEquals("text/html, application/xhtml+xml, text/plain;q=0.8", request.headers[HttpHeaders.Accept])
            respond(
                content = """
                    <html><head><title>Article</title></head><body><article>
                    <p style="color: blue">A complete article body returned by the test web server.</p>
                    </article></body></html>
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html; charset=utf-8"),
            )
        }
        val client = HttpClient(engine)

        val result = repository(client).fetch("https://example.com/article")

        assertEquals("https://example.com/article", result.sourceUrl)
        assertContains(result.html, "complete article body")
        assertContains(result.html, "var(--podaura-primary)")
        client.close()
    }

    @Test
    fun rejectsExplicitNonTextResponses() = runTest {
        val client = HttpClient(MockEngine {
            respond(
                content = "binary",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/octet-stream"),
            )
        })

        val failure = runCatching {
            repository(client).fetch("https://example.com/file")
        }.exceptionOrNull()

        assertIs<FullContentException>(failure)
        client.close()
    }

    @Test
    fun detectsCharsetFromHtmlMetadata() = runTest {
        val source = """
            <html><head><meta charset="iso-8859-1"><title>Café article</title></head>
            <body><article><p>Café text remains correctly decoded in the extracted article.</p></article></body></html>
        """.trimIndent()
        val bytes = Charsets.ISO_8859_1.newEncoder().encodeToByteArray(source)
        val client = HttpClient(MockEngine {
            respond(
                content = ByteReadChannel(bytes),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html"),
            )
        })

        val result = repository(client).fetch("https://example.com/cafe")

        assertContains(result.html, "Café text")
        client.close()
    }

    @Test
    fun fallsBackToRenderedDomWithoutSiteSpecificRequests() = runTest {
        val sourceUrl = "https://example.com/client-rendered"
        var requestCount = 0
        val client = HttpClient(MockEngine {
            requestCount++
            respond(
                content = """
                    <html><body><footer>
                    <a href='/about'>About</a><a href='/contact'>Contact</a>
                    <a href='/jobs'>Jobs</a><a href='/terms'>Terms</a>
                    <a href='/privacy'>Privacy</a><a href='/copyright'>Copyright</a>
                    </footer></body></html>
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html; charset=utf-8"),
            )
        })
        var renderedUrl: String? = null
        val provider = object : RenderedPageProvider {
            override suspend fun render(url: String): RenderedPageSnapshot {
                renderedUrl = url
                return RenderedPageSnapshot(
                    html = """
                        <html><body><article>
                        <p style='color: blue; font-size: 16px; line-height: 30px'>
                        Actual dynamically rendered episode description.
                        </p><img src='//cdn.example.com/episode.jpg'>
                        </article></body></html>
                    """.trimIndent(),
                    finalUrl = url,
                )
            }
        }

        val result = repository(client, provider).fetch(sourceUrl)

        assertEquals(1, requestCount)
        assertEquals(sourceUrl, renderedUrl)
        assertEquals(sourceUrl, result.sourceUrl)
        assertContains(result.html, "Actual dynamically rendered episode description")
        assertContains(result.html, "color: var(--podaura-primary)")
        assertFalse(result.html.contains("font-size: 16px"))
        assertContains(result.html, "https://cdn.example.com/episode.jpg")
        assertFalse(result.html.contains("Copyright"))
        client.close()
    }

    @Test
    fun usesJsonLdDescriptionOnlyAfterRenderedFallbackFails() = runTest {
        val client = HttpClient(MockEngine {
            respond(
                content = """
                    <html><head><script type="application/ld+json">
                    {
                      "@context": "https://schema.org",
                      "@type": "PodcastEpisode",
                      "headline": "A short episode",
                      "description": "Short but complete."
                    }
                    </script></head><body><footer>Site footer</footer></body></html>
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html; charset=utf-8"),
            )
        })
        var rendered = false
        val provider = object : RenderedPageProvider {
            override suspend fun render(url: String): RenderedPageSnapshot {
                rendered = true
                throw RenderedPageException("Should not render")
            }
        }

        val result = repository(client, provider).fetch("https://example.com/episode")

        assertTrue(rendered)
        assertContains(result.html, "Short but complete")
        client.close()
    }

    @Test
    fun jsonLdDescriptionDoesNotPreventRenderedFullContent() = runTest {
        val client = HttpClient(MockEngine {
            respond(
                content = """
                    <html><head><script type="application/ld+json">
                    {
                      "@context": "https://schema.org",
                      "@type": "Article",
                      "headline": "SEO headline",
                      "description": "This is only the short SEO summary."
                    }
                    </script></head><body><nav>
                    <a href='/1'>One</a><a href='/2'>Two</a><a href='/3'>Three</a>
                    <a href='/4'>Four</a><a href='/5'>Five</a><a href='/6'>Six</a>
                    </nav></body></html>
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html; charset=utf-8"),
            )
        })
        val provider = object : RenderedPageProvider {
            override suspend fun render(url: String) = RenderedPageSnapshot(
                html = """
                    <html><body><article>
                    <p>The first paragraph of the actual rendered article.</p>
                    <p>The second paragraph contains the remaining full content.</p>
                    </article></body></html>
                """.trimIndent(),
                finalUrl = url,
            )
        }

        val result = repository(client, provider).fetch("https://example.com/article")

        assertContains(result.html, "actual rendered article")
        assertFalse(result.html.contains("short SEO summary"))
        client.close()
    }

    @Test
    fun comparesRenderedContentWhenClientAppStaticHtmlContainsOnlyAPreview() = runTest {
        val client = HttpClient(MockEngine {
            respond(
                content = """
                    <html><body><div id="app"><article>
                      <p>This static preview is readable but incomplete.</p>
                    </article></div></body></html>
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html; charset=utf-8"),
            )
        })
        var rendered = false
        val provider = object : RenderedPageProvider {
            override suspend fun render(url: String): RenderedPageSnapshot {
                rendered = true
                return RenderedPageSnapshot(
                    html = """
                        <html><body><div id="app"><article>
                          <p>This static preview is readable but incomplete.</p>
                          <p>The dynamically rendered middle section contains important details.</p>
                          <p>The dynamically rendered final section completes the article.</p>
                        </article></div></body></html>
                    """.trimIndent(),
                    finalUrl = url,
                )
            }
        }

        val result = repository(client, provider).fetch("https://example.com/client-article")

        assertTrue(rendered)
        assertContains(result.html, "middle section")
        assertContains(result.html, "final section")
        client.close()
    }

    @Test
    fun rejectsPrivateAddressBeforeSendingRequest() = runTest {
        var requestCount = 0
        val client = HttpClient(MockEngine {
            requestCount++
            error("Private target must not be requested")
        })
        val resolver = HostAddressResolver { listOf(byteArrayOf(127, 0, 0, 1)) }

        val failure = runCatching {
            repository(client, resolver = resolver).fetch("http://127.0.0.1/admin")
        }.exceptionOrNull()

        assertIs<FullContentException>(failure)
        assertEquals(0, requestCount)
        client.close()
    }

    @Test
    fun rejectsIpv6LoopbackBeforeSendingRequest() = runTest {
        var requestCount = 0
        val client = HttpClient(MockEngine {
            requestCount++
            error("IPv6 loopback target must not be requested")
        })
        val loopback = ByteArray(16).apply { this[15] = 1 }

        val failure = runCatching {
            repository(
                client,
                resolver = HostAddressResolver { listOf(loopback) },
            ).fetch("http://[::1]/admin")
        }.exceptionOrNull()

        assertIs<FullContentException>(failure)
        assertEquals(0, requestCount)
        client.close()
    }

    @Test
    fun rejectsRedirectToPrivateAddressBeforeFollowingIt() = runTest {
        var requestCount = 0
        val client = HttpClient(MockEngine {
            requestCount++
            respond(
                content = "",
                status = HttpStatusCode.Found,
                headers = headersOf(HttpHeaders.Location, "http://127.0.0.1/internal"),
            )
        }) {
            followRedirects = false
        }
        val resolver = HostAddressResolver { host ->
            if (host == "127.0.0.1") listOf(byteArrayOf(127, 0, 0, 1))
            else listOf(PUBLIC_TEST_ADDRESS)
        }

        val failure = runCatching {
            repository(client, resolver = resolver).fetch("https://example.com/start")
        }.exceptionOrNull()

        assertIs<FullContentException>(failure)
        assertEquals(1, requestCount)
        client.close()
    }

    @Test
    fun rejectsHostWhenAnyDnsResultIsPrivate() = runTest {
        val client = HttpClient(MockEngine { error("Mixed DNS target must not be requested") })
        val resolver = HostAddressResolver {
            listOf(PUBLIC_TEST_ADDRESS, byteArrayOf(10, 0, 0, 5))
        }

        val failure = runCatching {
            repository(client, resolver = resolver).fetch("https://example.com/article")
        }.exceptionOrNull()

        assertIs<FullContentException>(failure)
        client.close()
    }

    private fun repository(
        client: HttpClient,
        provider: RenderedPageProvider = object : RenderedPageProvider {
            override suspend fun render(url: String): RenderedPageSnapshot =
                throw RenderedPageException("Unexpected rendered-page fallback")
        },
        resolver: HostAddressResolver = HostAddressResolver { listOf(PUBLIC_TEST_ADDRESS) },
    ) = FullContentRepository(client, json, provider, PublicAddressValidator(resolver))

    private companion object {
        val PUBLIC_TEST_ADDRESS = byteArrayOf(93.toByte(), 184.toByte(), 216.toByte(), 34)
    }
}
