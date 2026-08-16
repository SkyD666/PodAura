package com.skyd.podaura.model.repository.translation

import com.fleeksoft.ksoup.Ksoup

const val TRANSLATION_ENVELOPE_VERSION = 1
const val TRANSLATION_PROMPT_VERSION = 0

data class TranslationCriticalNode(
    val id: String,
    val tagName: String,
    val protectedAttributes: Map<String, String>,
    val protectedInnerHtml: String?,
)

data class TranslationEnvelope(
    val html: String,
    val originalContentBlank: Boolean,
    val criticalNodes: List<TranslationCriticalNode>,
)

class TranslationDocumentBuilder {
    fun build(title: String?, contentHtml: String): TranslationEnvelope {
        val document = Ksoup.parse(
            """
            <!doctype html>
            <html><head><meta charset="utf-8"></head><body>
            <div data-podaura-document="article" data-podaura-version="$TRANSLATION_ENVELOPE_VERSION">
            <h1 data-podaura-field="title"></h1><article data-podaura-field="content"></article>
            </div></body></html>
            """.trimIndent()
        )
        val titleElement = checkNotNull(document.selectFirst("[data-podaura-field=title]"))
        val contentElement = checkNotNull(document.selectFirst("[data-podaura-field=content]"))
        titleElement.text(title.orEmpty())

        val source = Ksoup.parseBodyFragment(contentHtml)
        source.select("script,style,noscript").remove()
        contentElement.html(source.body().html())
        document.select("[data-podaura-node-id]").forEach {
            it.removeAttr("data-podaura-node-id")
        }

        document.select("pre,code,kbd,samp").forEach { element ->
            element.attr("translate", "no")
            element.addClass("notranslate")
        }

        val criticalElements = contentElement.select("*").filter { element ->
            val tagName = element.tagName().lowercase()
            tagName in protectedCodeTags ||
                    tagName in criticalTags ||
                    element.attributes().any { it.key.startsWith("data-", ignoreCase = true) }
        }
        criticalElements.forEachIndexed { index, element ->
            element.attr("data-podaura-node-id", "n$index")
        }
        val criticalNodes = criticalElements.map { element ->
            val attributes = element.attributes().associate { it.key.lowercase() to it.value }
            val tagName = element.tagName().lowercase()
            TranslationCriticalNode(
                id = element.attr("data-podaura-node-id"),
                tagName = tagName,
                protectedAttributes = attributes.filterKeys { key ->
                    key != "data-podaura-node-id" &&
                            (key in protectedAttributeNames || key.startsWith("data-"))
                },
                protectedInnerHtml = element.html().takeIf { tagName in protectedCodeTags },
            )
        }

        document.outputSettings().prettyPrint(false)
        return TranslationEnvelope(
            html = document.outerHtml(),
            originalContentBlank = contentElement.html().isBlank(),
            criticalNodes = criticalNodes,
        )
    }

    private companion object {
        val criticalTags = setOf("a", "img", "audio", "video", "source", "time")
        val protectedCodeTags = setOf("pre", "code", "kbd", "samp")
        val protectedAttributeNames = setOf(
            "href", "src", "srcset", "poster", "sizes", "type", "media", "datetime",
            "controls", "preload", "playsinline", "target", "rel", "id", "name",
        )
    }
}
