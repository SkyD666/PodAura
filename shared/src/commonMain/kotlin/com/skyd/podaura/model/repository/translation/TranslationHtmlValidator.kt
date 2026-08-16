package com.skyd.podaura.model.repository.translation

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element

data class ValidatedTranslationDocument(
    val title: String,
    val contentHtml: String,
)

enum class TranslationHtmlValidationFailureReason {
    ResponseTooLarge,
    DisallowedResponsePrefix,
    MissingHtmlPrefix,
    HtmlParseFailed,
    EnvelopeFieldCount,
    RootNotOnlyBodyChild,
    EnvelopeChildLayout,
    FieldOutsideRoot,
    UnexpectedBlankContent,
    CriticalNodeCount,
    CriticalNodeTagChanged,
    UnexpectedCriticalElement,
    UnknownNodeId,
    UnsafeUrl,
    UnsafeSrcSet,
}

data class TranslationHtmlValidationDiagnostic(
    val reason: TranslationHtmlValidationFailureReason,
    val nodeId: String? = null,
    val expectedTag: String? = null,
    val actualTag: String? = null,
    val actualCount: Int? = null,
    val rootCount: Int? = null,
    val titleCount: Int? = null,
    val contentCount: Int? = null,
    val bodyChildCount: Int? = null,
    val rootChildCount: Int? = null,
)

sealed interface TranslationHtmlValidationResult {
    data class Valid(val document: ValidatedTranslationDocument) : TranslationHtmlValidationResult
    data class Invalid(
        val diagnostic: TranslationHtmlValidationDiagnostic,
    ) : TranslationHtmlValidationResult
}

class TranslationHtmlValidator(
    private val maximumResponseBytes: Long = 2L * 1024L * 1024L,
) {
    fun validate(
        responseHtml: String,
        envelope: TranslationEnvelope,
    ): ValidatedTranslationDocument? =
        when (val result = validateDetailed(responseHtml, envelope)) {
            is TranslationHtmlValidationResult.Valid -> result.document
            is TranslationHtmlValidationResult.Invalid -> null
        }

    fun validateDetailed(
        responseHtml: String,
        envelope: TranslationEnvelope,
    ): TranslationHtmlValidationResult {
        if (responseHtml.encodeToByteArray().size.toLong() > maximumResponseBytes) {
            return invalid(TranslationHtmlValidationFailureReason.ResponseTooLarge)
        }
        val trimmed = responseHtml.trimStart()
        if (trimmed.startsWith("```") || trimmed.startsWith("{") || trimmed.startsWith("\"")) {
            return invalid(TranslationHtmlValidationFailureReason.DisallowedResponsePrefix)
        }
        if (!trimmed.startsWith("<")) {
            return invalid(TranslationHtmlValidationFailureReason.MissingHtmlPrefix)
        }

        val document = runCatching { Ksoup.parse(responseHtml) }.getOrNull()
            ?: return invalid(TranslationHtmlValidationFailureReason.HtmlParseFailed)
        val roots = document.select("[data-podaura-document=article]")
        val titles = document.select("[data-podaura-field=title]")
        val contents = document.select("[data-podaura-field=content]")
        if (roots.size != 1 || titles.size != 1 || contents.size != 1) {
            return invalid(
                reason = TranslationHtmlValidationFailureReason.EnvelopeFieldCount,
                rootCount = roots.size,
                titleCount = titles.size,
                contentCount = contents.size,
            )
        }
        val root = roots.single()
        val title = titles.single()
        val content = contents.single()
        if (document.body().children().singleOrNull() != root) {
            return invalid(
                reason = TranslationHtmlValidationFailureReason.RootNotOnlyBodyChild,
                bodyChildCount = document.body().children().size,
            )
        }
        if (root.children().size != 2 || root.children()[0] != title || root.children()[1] != content) {
            return invalid(
                reason = TranslationHtmlValidationFailureReason.EnvelopeChildLayout,
                rootChildCount = root.children().size,
            )
        }
        if (!root.getAllElements().contains(title) || !root.getAllElements().contains(content)) {
            return invalid(TranslationHtmlValidationFailureReason.FieldOutsideRoot)
        }
        if (!envelope.originalContentBlank && content.html().isBlank()) {
            return invalid(TranslationHtmlValidationFailureReason.UnexpectedBlankContent)
        }

        val expectedById = envelope.criticalNodes.associateBy { it.id }
        for (criticalNode in envelope.criticalNodes) {
            val matches = content.select("[data-podaura-node-id=${criticalNode.id}]")
            if (criticalNode.tagName != "a" && matches.size != 1) {
                return invalid(
                    reason = TranslationHtmlValidationFailureReason.CriticalNodeCount,
                    nodeId = criticalNode.id,
                    expectedTag = criticalNode.tagName,
                    actualCount = matches.size,
                )
            }
            val changedTag = matches.firstOrNull {
                it.tagName().lowercase() != criticalNode.tagName
            }
            if (changedTag != null) {
                return invalid(
                    reason = TranslationHtmlValidationFailureReason.CriticalNodeTagChanged,
                    nodeId = criticalNode.id,
                    expectedTag = criticalNode.tagName,
                    actualTag = changedTag.tagName().lowercase().take(MAX_LOGGED_TAG_LENGTH),
                )
            }
            matches.forEach { translated ->
                criticalNode.protectedAttributes.forEach { (key, value) ->
                    translated.attr(key, value)
                }
                criticalNode.protectedInnerHtml?.let(translated::html)
            }
        }

        content.select("a").filter { element ->
            element.attr("data-podaura-node-id") !in expectedById
        }.forEach(Element::unwrap)
        val responseCriticalElements =
            content.select("img,audio,video,source,time,pre,code,kbd,samp")
        responseCriticalElements.firstOrNull { element ->
            element.attr("data-podaura-node-id") !in expectedById
        }?.let { element ->
            return invalid(
                reason = TranslationHtmlValidationFailureReason.UnexpectedCriticalElement,
                actualTag = element.tagName().lowercase().take(MAX_LOGGED_TAG_LENGTH),
            )
        }
        content.select("[data-podaura-node-id]").firstOrNull { element ->
            element.attr("data-podaura-node-id") !in expectedById
        }?.let { element ->
            return invalid(
                reason = TranslationHtmlValidationFailureReason.UnknownNodeId,
                actualTag = element.tagName().lowercase().take(MAX_LOGGED_TAG_LENGTH),
            )
        }

        return when (val sanitized = sanitize(content, expectedById)) {
            is SanitizationResult.Invalid -> invalid(sanitized.reason)
            is SanitizationResult.Valid -> TranslationHtmlValidationResult.Valid(
                ValidatedTranslationDocument(
                    title = title.text(),
                    contentHtml = sanitized.html,
                )
            )
        }
    }

    private fun invalid(
        reason: TranslationHtmlValidationFailureReason,
        nodeId: String? = null,
        expectedTag: String? = null,
        actualTag: String? = null,
        actualCount: Int? = null,
        rootCount: Int? = null,
        titleCount: Int? = null,
        contentCount: Int? = null,
        bodyChildCount: Int? = null,
        rootChildCount: Int? = null,
    ) = TranslationHtmlValidationResult.Invalid(
        TranslationHtmlValidationDiagnostic(
            reason = reason,
            nodeId = nodeId,
            expectedTag = expectedTag,
            actualTag = actualTag,
            actualCount = actualCount,
            rootCount = rootCount,
            titleCount = titleCount,
            contentCount = contentCount,
            bodyChildCount = bodyChildCount,
            rootChildCount = rootChildCount,
        )
    )

    private fun sanitize(
        content: Element,
        expectedById: Map<String, TranslationCriticalNode>,
    ): SanitizationResult {
        content.select("script,style,noscript,iframe,object,embed,form,input,button,textarea,select")
            .remove()

        content.select("*").toList().asReversed().forEach { element ->
            if (element.tagName().lowercase() !in allowedTags) {
                element.unwrap()
            }
        }

        content.select("*").forEach { element ->
            val critical = expectedById[element.attr("data-podaura-node-id")]
            element.attributes().toList().forEach { attribute ->
                val key = attribute.key.lowercase()
                val allowed = key in globalAttributes ||
                        key in tagAttributes[element.tagName().lowercase()].orEmpty() ||
                        (key.startsWith("data-") && critical?.protectedAttributes?.containsKey(key) == true)
                if (!allowed || key.startsWith("on")) element.removeAttr(attribute.key)
            }
            for (attributeName in urlAttributes) {
                if (element.hasAttr(attributeName) && !isSafeUrl(element.attr(attributeName))) {
                    return SanitizationResult.Invalid(TranslationHtmlValidationFailureReason.UnsafeUrl)
                }
            }
            if (element.hasAttr("srcset") && !isSafeSrcSet(element.attr("srcset"))) {
                return SanitizationResult.Invalid(TranslationHtmlValidationFailureReason.UnsafeSrcSet)
            }
            element.removeAttr("data-podaura-node-id")
        }
        return SanitizationResult.Valid(content.html())
    }

    private fun isSafeSrcSet(value: String): Boolean = value.split(',').all { candidate ->
        val url = candidate.trim().substringBefore(' ')
        url.isNotBlank() && isSafeUrl(url)
    }

    private fun isSafeUrl(value: String): Boolean {
        val normalized = value.trim().lowercase()
        if (normalized.isBlank()) return true
        if (normalized.startsWith("#") || normalized.startsWith("/") ||
            normalized.startsWith("./") || normalized.startsWith("../")
        ) return true
        if (!normalized.contains(':')) return true
        return normalized.startsWith("https://") ||
                normalized.startsWith("http://") ||
                normalized.startsWith("mailto:") ||
                normalized.startsWith("tel:") ||
                normalized.startsWith("podaura:")
    }

    private companion object {
        const val MAX_LOGGED_TAG_LENGTH = 32

        sealed interface SanitizationResult {
            data class Valid(val html: String) : SanitizationResult
            data class Invalid(
                val reason: TranslationHtmlValidationFailureReason,
            ) : SanitizationResult
        }

        val allowedTags = setOf(
            "a", "abbr", "article", "aside", "audio", "b", "blockquote", "br", "caption",
            "cite", "code", "col", "colgroup", "dd", "del", "details", "div", "dl", "dt",
            "em", "figcaption", "figure", "h1", "h2", "h3", "h4", "h5", "h6", "hr", "i",
            "img", "ins", "kbd", "li", "main", "mark", "ol", "p", "picture", "pre", "q",
            "rp", "rt", "ruby", "s", "samp", "section", "small", "source", "span", "strong",
            "sub", "summary", "sup", "table", "tbody", "td", "tfoot", "th", "thead", "time",
            "tr", "u", "ul", "var", "video", "wbr",
        )
        val globalAttributes = setOf(
            "class", "id", "title", "dir", "lang", "translate", "role", "aria-label",
        )
        val tagAttributes = mapOf(
            "a" to setOf("href", "name", "rel", "target"),
            "img" to setOf("src", "srcset", "sizes", "alt", "width", "height", "loading"),
            "source" to setOf("src", "srcset", "sizes", "type", "media"),
            "audio" to setOf("src", "controls", "preload"),
            "video" to setOf(
                "src",
                "poster",
                "controls",
                "preload",
                "playsinline",
                "width",
                "height"
            ),
            "time" to setOf("datetime"),
            "td" to setOf("colspan", "rowspan", "headers"),
            "th" to setOf("colspan", "rowspan", "headers", "scope"),
            "col" to setOf("span"),
            "colgroup" to setOf("span"),
        )
        val urlAttributes = setOf("href", "src", "poster")
    }
}
