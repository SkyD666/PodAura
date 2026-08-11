package com.skyd.podaura.model.repository.fullcontent

import com.fleeksoft.ksoup.Ksoup
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

internal object StructuredContentExtractor {
    private val supportedTypes = setOf(
        "article",
        "newsarticle",
        "blogposting",
        "report",
        "review",
        "techarticle",
        "discussionforumposting",
        "podcastepisode",
        "audioobject",
    )
    private val bodyFields = listOf("articleBody", "text", "transcript")
    private val htmlPattern = Regex("""<\s*[a-zA-Z][^>]*>""")

    fun extract(html: String, baseUrl: String, json: Json): List<StructuredContentFragment> {
        val source = Ksoup.parse(html, baseUrl)
        return source.select("script[type=application/ld+json]")
            .flatMap { script ->
                val payload = script.data().ifBlank { script.html() }
                val root = runCatching { json.parseToJsonElement(payload) }.getOrNull()
                    ?: return@flatMap emptyList()
                structuredObjects(root).mapNotNull(::toArticleFragment).toList()
            }
    }

    private fun structuredObjects(element: JsonElement): Sequence<JsonObject> = sequence {
        when (element) {
            is JsonArray -> element.forEach { yieldAll(structuredObjects(it)) }
            is JsonObject -> {
                yield(element)
                element["@graph"]?.let { yieldAll(structuredObjects(it)) }
            }
            else -> Unit
        }
    }

    private fun toArticleFragment(value: JsonObject): StructuredContentFragment? {
        val types = when (val type = value["@type"]) {
            is JsonPrimitive -> listOfNotNull(type.contentOrNull)
            is JsonArray -> type.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
            else -> emptyList()
        }.map { it.substringAfterLast('/').lowercase() }
        if (types.none { it in supportedTypes }) return null

        val body = bodyFields.firstNotNullOfOrNull { field ->
            (value[field] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
        }
        val description = (value["description"] as? JsonPrimitive)?.contentOrNull
            ?.takeIf { it.isNotBlank() }
        val content = body ?: description ?: return null
        val headline = (value["headline"] as? JsonPrimitive)?.contentOrNull
            ?.takeIf { it.isNotBlank() }
        val imageUrl = imageUrl(value["image"])

        val fragment = Ksoup.parseBodyFragment("")
        headline?.let { fragment.body().appendElement("h1").text(it) }
        if (htmlPattern.containsMatchIn(content)) {
            fragment.body().append(content)
        } else {
            content.lineSequence()
                .map(String::trim)
                .filter(String::isNotEmpty)
                .forEach { fragment.body().appendElement("p").text(it) }
        }
        imageUrl?.let { fragment.body().appendElement("img").attr("src", it) }
        return StructuredContentFragment(
            html = fragment.body().html(),
            summaryOnly = body == null,
        )
    }

    private fun imageUrl(element: JsonElement?): String? = when (element) {
        is JsonPrimitive -> element.contentOrNull
        is JsonArray -> element.firstNotNullOfOrNull(::imageUrl)
        is JsonObject -> listOf("url", "contentUrl").firstNotNullOfOrNull { field ->
            (element[field] as? JsonPrimitive)?.contentOrNull
        }
        else -> null
    }?.takeIf { it.isNotBlank() }
}

internal data class StructuredContentFragment(
    val html: String,
    val summaryOnly: Boolean,
)
