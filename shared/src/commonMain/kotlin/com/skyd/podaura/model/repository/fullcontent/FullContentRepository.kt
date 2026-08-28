package com.skyd.podaura.model.repository.fullcontent

import com.skyd.fundation.config.Const
import com.skyd.podaura.util.appVersion
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.request
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.charset
import io.ktor.http.contentLength
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.charsets.Charset
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.charsets.decode
import io.ktor.utils.io.charsets.forName
import io.ktor.utils.io.charsets.isSupported
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlinx.serialization.json.Json

private const val MAX_RESPONSE_BYTES = 5 * 1024 * 1024
private const val CHARSET_SNIFF_BYTES = 16 * 1024
private val SUPPORTED_CONTENT_TYPES = setOf(
    "text/html",
    "application/xhtml+xml",
    "text/plain",
)
private val META_CHARSET_PATTERN = Regex(
    """(?is)<meta\b[^>]*\bcharset\s*=\s*["']?\s*([a-z0-9._:-]+)"""
)
private val XML_CHARSET_PATTERN = Regex(
    """(?is)<\?xml\b[^>]*\bencoding\s*=\s*["']\s*([a-z0-9._:-]+)"""
)

class FullContentRepository internal constructor(
    private val httpClient: HttpClient,
    private val json: Json,
    private val renderedPageProvider: RenderedPageProvider,
    private val addressValidator: PublicAddressValidator = PublicAddressValidator(),
) : IFullContentRepository {

    override suspend fun fetch(url: String): FullContent {
        val source = fetchArticlePage(url)
        val staticCandidate = extractBestCandidate(
            html = source.html,
            sourceUrl = source.sourceUrl,
            rendered = false,
        )
        if (staticCandidate != null &&
            staticCandidate.source != ExtractionSource.STRUCTURED_SUMMARY &&
            !source.html.needsRenderedComparison()
        ) {
            return FullContent(html = staticCandidate.html, sourceUrl = staticCandidate.sourceUrl)
        }

        val rendered = try {
            renderedPageProvider.render(source.sourceUrl)
        } catch (exception: RenderedPageException) {
            if (staticCandidate != null) {
                return FullContent(html = staticCandidate.html, sourceUrl = staticCandidate.sourceUrl)
            }
            throw FullContentException(exception.message ?: "Rendered page unavailable", exception)
        }
        addressValidator.validate(rendered.finalUrl)
        val renderedCandidate = extractBestCandidate(
            html = rendered.html,
            sourceUrl = rendered.finalUrl,
            rendered = true,
        )
        val bestCandidate = listOfNotNull(renderedCandidate, staticCandidate)
            .maxWithOrNull(candidateComparator)
            ?: throw FullContentException("No readable article content")
        return FullContent(html = bestCandidate.html, sourceUrl = bestCandidate.sourceUrl)
    }

    private suspend fun extractBestCandidate(
        html: String,
        sourceUrl: String,
        rendered: Boolean,
    ): ExtractionCandidate? = withContext(Dispatchers.Default) {
        buildList {
            runCatching {
                FullContentHtmlProcessor.processPageCandidates(html = html, baseUrl = sourceUrl)
            }.getOrDefault(emptyList())
                .forEach { processed ->
                add(
                    processed.html.toCandidate(
                        sourceUrl = sourceUrl,
                        source = when {
                            rendered && processed.fromSemanticContainer ->
                                ExtractionSource.RENDERED_SEMANTIC_CONTAINER
                            rendered -> ExtractionSource.RENDERED_READABILITY
                            processed.fromSemanticContainer -> ExtractionSource.SEMANTIC_CONTAINER
                            else -> ExtractionSource.READABILITY
                        },
                    )
                )
            }
            StructuredContentExtractor.extract(html = html, baseUrl = sourceUrl, json = json)
                .forEach { fragment ->
                    runCatching {
                        FullContentHtmlProcessor.processArticleFragment(
                            html = fragment.html,
                            baseUrl = sourceUrl,
                        )
                    }.getOrNull()?.let { processed ->
                        add(
                            processed.toCandidate(
                                sourceUrl = sourceUrl,
                                source = if (fragment.summaryOnly) {
                                    ExtractionSource.STRUCTURED_SUMMARY
                                } else {
                                    ExtractionSource.STRUCTURED_DATA
                                },
                            )
                        )
                    }
                }
        }.filter { it.diagnostics.acceptable }
            .maxWithOrNull(candidateComparator)
    }

    private suspend fun fetchArticlePage(url: String): FetchedSource {
        var currentUrl = url
        repeat(MAX_REDIRECTS + 1) { redirectCount ->
            addressValidator.validate(currentUrl)
            val step = httpClient.prepareGet(currentUrl) {
                fullContentHeaders("text/html, application/xhtml+xml, text/plain;q=0.8")
            }.execute { response ->
                if (response.status.value in REDIRECT_STATUS_CODES) {
                    val location = response.headers[HttpHeaders.Location]
                        ?: throw FullContentException("Redirect response has no location")
                    FetchStep.Redirect(
                        URLBuilder(response.request.url).takeFrom(location).buildString()
                    )
                } else {
                    response.validate(SUPPORTED_CONTENT_TYPES)
                    val contentType = response.contentType()
                    val declaredCharset = runCatching { contentType?.charset() }.getOrNull()
                    FetchStep.Content(
                        FetchedSource(
                            html = decodeHtml(
                                bytes = response.readLimitedBytes(),
                                declaredCharset = declaredCharset,
                            ),
                            sourceUrl = response.request.url.toString(),
                        )
                    )
                }
            }
            when (step) {
                is FetchStep.Content -> return step.source
                is FetchStep.Redirect -> {
                    if (redirectCount == MAX_REDIRECTS) {
                        throw FullContentException("Too many article redirects")
                    }
                    currentUrl = step.url
                }
            }
        }
        error("Unreachable")
    }

}

private data class FetchedSource(
    val html: String,
    val sourceUrl: String,
)

private sealed interface FetchStep {
    data class Redirect(val url: String) : FetchStep
    data class Content(val source: FetchedSource) : FetchStep
}

private enum class ExtractionSource {
    READABILITY,
    SEMANTIC_CONTAINER,
    STRUCTURED_DATA,
    STRUCTURED_SUMMARY,
    RENDERED_READABILITY,
    RENDERED_SEMANTIC_CONTAINER,
}

private data class ExtractionCandidate(
    val html: String,
    val sourceUrl: String,
    val source: ExtractionSource,
    val diagnostics: ContentDiagnostics,
)

private val candidateComparator = compareBy<ExtractionCandidate> {
    it.source != ExtractionSource.STRUCTURED_SUMMARY
}.thenBy { it.diagnostics.score }
    .thenBy { it.diagnostics.paragraphCount }
    .thenBy { it.diagnostics.textLength }
    .thenBy { it.diagnostics.mediaCount }

/**
 * A short server-rendered preview inside a hydrated application often passes Readability even
 * though the browser later adds the real body. Pages that advertise a client application require
 * a rendered comparison; ordinary server-rendered articles stay on the inexpensive static path.
 */
private fun String.needsRenderedComparison(): Boolean {
    val document = runCatching { com.fleeksoft.ksoup.Ksoup.parse(this) }.getOrNull() ?: return false
    val clientApplicationMarkers = document.select(
        "script#__NEXT_DATA__, script#__NUXT_DATA__, script[data-nuxt-data], " +
            "[data-reactroot], [data-react-root], [data-v-app], [ng-version], " +
            "#root, #app, #__next, #__nuxt"
    ).isNotEmpty()
    return clientApplicationMarkers
}

private fun String.toCandidate(
    sourceUrl: String,
    source: ExtractionSource,
): ExtractionCandidate = ExtractionCandidate(
    html = this,
    sourceUrl = sourceUrl,
    source = source,
    diagnostics = ContentQualityEvaluator.evaluate(html = this, baseUrl = sourceUrl),
)

private fun HttpRequestBuilder.fullContentHeaders(accept: String) {
    header(HttpHeaders.Accept, accept)
    header(
        HttpHeaders.UserAgent,
        "PodAura/${appVersion.name} (+${Const.GITHUB_REPO})",
    )
}

private fun HttpResponse.validate(supportedContentTypes: Set<String>) {
    if (!status.isSuccess()) throw FullContentException("HTTP ${status.value}")
    val contentType = contentType()
    val mimeType = contentType?.let { "${it.contentType}/${it.contentSubtype}" }
    if (mimeType != null && mimeType !in supportedContentTypes) {
        throw FullContentException("Unsupported content type: $mimeType")
    }
    val declaredLength = contentLength()
    if (declaredLength != null && declaredLength > MAX_RESPONSE_BYTES) {
        throw FullContentException("Article response is too large")
    }
}

private suspend fun HttpResponse.readLimitedBytes(): ByteArray {
    val channel: ByteReadChannel = body()
    val bytes = channel.readRemaining((MAX_RESPONSE_BYTES + 1).toLong()).readByteArray()
    if (bytes.size > MAX_RESPONSE_BYTES) {
        throw FullContentException("Article response is too large")
    }
    return bytes
}

private fun decodeHtml(bytes: ByteArray, declaredCharset: Charset?): String {
    val charset = declaredCharset ?: sniffCharset(bytes) ?: Charsets.UTF_8
    return decodeText(bytes = bytes, charset = charset)
}

private fun decodeText(bytes: ByteArray, charset: Charset): String =
    charset.newDecoder().decode(Buffer().apply { write(bytes) })

private fun sniffCharset(bytes: ByteArray): Charset? {
    when {
        bytes.startsWith(0xEF, 0xBB, 0xBF) -> return Charsets.UTF_8
        bytes.startsWith(0xFF, 0xFE) -> charsetOrNull("UTF-16LE")?.let { return it }
        bytes.startsWith(0xFE, 0xFF) -> charsetOrNull("UTF-16BE")?.let { return it }
    }

    val prefix = bytes.copyOf(minOf(bytes.size, CHARSET_SNIFF_BYTES))
    val asciiCompatibleText = Charsets.ISO_8859_1.newDecoder()
        .decode(Buffer().apply { write(prefix) })
    val charsetName = META_CHARSET_PATTERN
        .find(asciiCompatibleText)?.groupValues?.getOrNull(1)
        ?: XML_CHARSET_PATTERN
            .find(asciiCompatibleText)?.groupValues?.getOrNull(1)
        ?: return null
    return charsetOrNull(
        when (charsetName.lowercase()) {
            "gb2312", "x-gbk" -> "GBK"
            else -> charsetName
        }
    )
}

private fun charsetOrNull(name: String): Charset? = runCatching {
    if (Charsets.isSupported(name)) Charsets.forName(name) else null
}.getOrNull()

private fun ByteArray.startsWith(vararg prefix: Int): Boolean =
    size >= prefix.size && prefix.indices.all { index -> this[index].toInt() and 0xFF == prefix[index] }

private const val MAX_REDIRECTS = 5
private val REDIRECT_STATUS_CODES = setOf(301, 302, 303, 307, 308)
