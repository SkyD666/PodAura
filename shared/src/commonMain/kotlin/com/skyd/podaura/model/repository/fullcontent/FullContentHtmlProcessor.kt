package com.skyd.podaura.model.repository.fullcontent

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.safety.Safelist
import moe.tlaster.readability.Readability
import moe.tlaster.readability.ReadabilityOptions

internal object FullContentHtmlProcessor {
    private const val STYLE_SNAPSHOT_ATTRIBUTE = "data-podaura-style"
    private const val SRCSET_RESOLUTION_ATTRIBUTE = "data-podaura-srcset-url"
    private const val MAX_DOCUMENT_ELEMENTS = 50_000
    private const val MAX_EMBEDDED_STYLE_CHARS = 512 * 1024
    private const val MAX_CSS_RULES = 5_000

    private val inheritableProperties = setOf(
        "color",
        "font-style",
        "font-weight",
        "text-align",
        "text-decoration",
        "direction",
    )

    private val safeProperties = inheritableProperties + setOf(
        "background-color",
        "border",
        "border-top",
        "border-right",
        "border-bottom",
        "border-left",
        "border-color",
        "border-top-color",
        "border-right-color",
        "border-bottom-color",
        "border-left-color",
        "border-radius",
        "border-style",
        "border-top-style",
        "border-right-style",
        "border-bottom-style",
        "border-left-style",
        "border-width",
        "border-top-width",
        "border-right-width",
        "border-bottom-width",
        "border-left-width",
        "border-collapse",
        "caption-side",
        "table-layout",
        "text-transform",
        "vertical-align",
    )

    private val semanticContainerSelector = listOf(
        "article",
        "main",
        "[role=main]",
        "[itemprop=articleBody]",
        ".article-content",
        ".article-body",
        ".post-content",
        ".post-body",
        ".entry-content",
        ".story-body",
        "#article-content",
        "#article-body",
    ).joinToString(",")
    private val specificSemanticContainerSelector = listOf(
        "article",
        "[itemprop=articleBody]",
        ".article-content",
        ".article-body",
        ".post-content",
        ".post-body",
        ".entry-content",
        ".story-body",
        "#article-content",
        "#article-body",
    ).joinToString(",")

    fun process(html: String, baseUrl: String): String {
        val candidates = processPageCandidates(html = html, baseUrl = baseUrl)
        return candidates.firstOrNull { !it.fromSemanticContainer }
            ?.html
            ?: candidates.firstOrNull()
            ?.html
            ?: throw FullContentException("No readable article content")
    }

    /**
     * Produces both Readability's result and standards/convention based content containers. Some
     * pages split an article into sibling sections that Readability scores independently; keeping a
     * direct semantic candidate lets the repository compare the complete container as well.
     */
    fun processPageCandidates(html: String, baseUrl: String): List<ProcessedPageCandidate> {
        val preparedHtml = materializeStyles(html = html, baseUrl = baseUrl)
        val candidates = buildList {
            runCatching {
                Readability(
                    html = preparedHtml,
                    url = baseUrl,
                    options = ReadabilityOptions(
                        maxElemsToParse = MAX_DOCUMENT_ELEMENTS,
                        keepClasses = true,
                    ),
                ).parse()?.content?.let { cleanExtractedHtml(it, baseUrl) }
            }.getOrNull()?.let { html ->
                add(ProcessedPageCandidate(html = html, fromSemanticContainer = false))
            }

            val document = Ksoup.parse(preparedHtml, baseUrl)
            document.select(semanticContainerSelector).forEach { container ->
                val isGenericMain = container.tagName().equals("main", ignoreCase = true) ||
                    container.attr("role").equals("main", ignoreCase = true)
                val hasSpecificDescendant = container.select(specificSemanticContainerSelector)
                    .any { it !== container }
                if (isGenericMain && hasSpecificDescendant) {
                    return@forEach
                }
                runCatching { cleanExtractedHtml(container.outerHtml(), baseUrl) }
                    .getOrNull()
                    ?.let { html ->
                        add(ProcessedPageCandidate(html = html, fromSemanticContainer = true))
                    }
            }
        }
        return candidates.distinctBy { it.html }
    }

    /**
     * Processes content that a standards-based structured-data extractor has already isolated from
     * page chrome. Readability is intentionally skipped so it cannot discard short notes or media.
     */
    fun processArticleFragment(html: String, baseUrl: String): String {
        val preparedHtml = materializeStyles(html = html, baseUrl = baseUrl)
        val preparedDocument = Ksoup.parse(preparedHtml, baseUrl)
        return cleanExtractedHtml(preparedDocument.body().html(), baseUrl)
    }

    private fun cleanExtractedHtml(html: String, baseUrl: String): String {
        val extracted = Ksoup.parseBodyFragment(html, baseUrl)
        extracted.select(
            "script,style,noscript,form,input,button,textarea,select,option," +
                "nav,footer,aside,[role=navigation],[role=contentinfo],[role=complementary]," +
                "#comments,.comments,.comment-list,.related-posts,.recommended-content," +
                "[hidden],[aria-hidden=true]"
        ).forEach { it.remove() }
        normalizeLazyMedia(extracted)
        val generatedStyleElements = extracted.select("[$STYLE_SNAPSHOT_ATTRIBUTE]").toList()
        // Readability may carry source styles through. Strip all of them before restoring only
        // declarations that materializeStyles validated and generated itself.
        extracted.select("[style]").forEach { it.removeAttr("style") }
        generatedStyleElements.forEach { element ->
            element.attr("style", element.attr(STYLE_SNAPSHOT_ATTRIBUTE))
            element.removeAttr(STYLE_SNAPSHOT_ATTRIBUTE)
        }
        repairOrphanedContainerColors(extracted)
        sanitizeSourceSets(extracted)

        val cleaned = Ksoup.clean(
            bodyHtml = extracted.body().html(),
            safelist = fullContentSafelist(),
            baseUri = baseUrl,
        ).trim()
        val cleanedDocument = Ksoup.parseBodyFragment(cleaned, baseUrl)
        val hasRenderableMedia = cleanedDocument.select("img,picture,table,audio,video,hr").isNotEmpty()
        if (cleanedDocument.body().text().isBlank() && !hasRenderableMedia) {
            throw FullContentException("No readable article content")
        }
        return cleaned
    }

    private fun normalizeLazyMedia(document: Document) {
        document.select("img,source,audio,video").forEach { element ->
            val currentSource = element.attr("src")
            val lazySource = listOf("data-src", "data-original", "data-lazy-src", "data-url")
                .firstNotNullOfOrNull { attribute ->
                    element.attr(attribute).takeIf { it.isUsableMediaUrl() }
                }
            if (!currentSource.isUsableMediaUrl() && lazySource != null) {
                element.attr("src", lazySource)
            }
            if (element.attr("srcset").isBlank()) {
                listOf("data-srcset", "data-lazy-srcset")
                    .firstNotNullOfOrNull { attribute ->
                        element.attr(attribute).takeIf { it.isNotBlank() }
                    }
                    ?.let { element.attr("srcset", it) }
            }
        }
    }

    private fun String.isUsableMediaUrl(): Boolean {
        val value = trim()
        return value.isNotBlank() &&
            !value.startsWith("data:image/gif", ignoreCase = true) &&
            !value.startsWith("data:image/svg+xml", ignoreCase = true) &&
            !value.equals("about:blank", ignoreCase = true)
    }

    private fun materializeStyles(html: String, baseUrl: String): String {
        val document = Ksoup.parse(html, baseUrl)
        if (document.select("*").size > MAX_DOCUMENT_ELEMENTS) {
            throw FullContentException("Article document is too large")
        }
        // The marker is an internal trust boundary. Never accept a source-provided value because
        // cleanExtractedHtml later restores it as an inline style.
        document.select("[$STYLE_SNAPSHOT_ATTRIBUTE]")
            .forEach { it.removeAttr(STYLE_SNAPSHOT_ATTRIBUTE) }
        removePageChrome(document)
        val appliedStyles = mutableMapOf<Element, MutableMap<String, AppliedStyle>>()
        val globalVariables = mutableMapOf<String, String>()
        var order = 0
        var remainingStyleChars = MAX_EMBEDDED_STYLE_CHARS
        var remainingRules = MAX_CSS_RULES

        document.select("[color], [bgcolor], [align], [valign]")
            .forEach { element ->
                val presentationStyles = buildMap {
                    element.attr("color").takeIf { it.isNotBlank() }?.let { put("color", it) }
                    element.attr("bgcolor").takeIf { it.isNotBlank() }
                        ?.let { put("background-color", it) }
                    element.attr("align").trim().lowercase()
                        .takeIf { it in setOf("start", "end", "left", "right", "center", "justify") }
                        ?.let { put("text-align", it) }
                    element.attr("valign").trim().lowercase()
                        .takeIf { it in setOf("top", "middle", "bottom", "baseline") }
                        ?.let { put("vertical-align", it) }
                }
                presentationStyles.forEach { (property, value) ->
                    if (isSafeValue(value)) {
                        putStyle(
                            styles = appliedStyles,
                            element = element,
                            property = property,
                            value = value,
                            priority = stylePriority(important = false, specificity = 0, order = order++),
                        )
                    }
                }
            }

        document.select("style").forEach styleLoop@ { styleElement ->
            if (remainingStyleChars <= 0 || remainingRules <= 0) return@styleLoop
            val css = styleElement.data().ifBlank { styleElement.html() }
                .take(remainingStyleChars)
            remainingStyleChars -= css.length
            parseRules(css).take(remainingRules).forEach { rule ->
                remainingRules--
                val declarations = parseDeclarations(rule.declarations)
                if (rule.selector in setOf(":root", "html", "body")) {
                    declarations.filter { it.property.startsWith("--") }.forEach {
                        globalVariables[it.property] = it.value
                    }
                }
                declarations.filterNot { it.property.startsWith("--") }.forEach declarationLoop@ { declaration ->
                    val property = declaration.property
                    if (property !in safeProperties) return@declarationLoop
                    val resolvedValue = resolveCssVariables(declaration.value, globalVariables)
                    if (!isSafeValue(resolvedValue)) return@declarationLoop
                    val priority = stylePriority(
                        important = declaration.important,
                        specificity = selectorSpecificity(rule.selector),
                        order = order++,
                    )
                    runCatching { document.select(rule.selector) }.getOrNull()?.forEach { element ->
                        putStyle(appliedStyles, element, property, resolvedValue, priority)
                    }
                }
            }
        }

        val inlineStyleElements = document.select("[style]").toList()
        inlineStyleElements.forEach { element ->
            val declarations = parseDeclarations(element.attr("style"))
            declarations.filter { it.property.startsWith("--") }.forEach {
                if (element.tagName() in setOf("html", "body")) {
                    globalVariables[it.property] = it.value
                }
            }
            declarations.filterNot { it.property.startsWith("--") }.forEach { declaration ->
                if (declaration.property !in safeProperties) return@forEach
                val value = resolveCssVariables(declaration.value, globalVariables)
                if (!isSafeValue(value)) return@forEach
                putStyle(
                    styles = appliedStyles,
                    element = element,
                    property = declaration.property,
                    value = value,
                    priority = stylePriority(declaration.important, specificity = 1_000, order = order++),
                )
            }
        }
        inlineStyleElements.forEach { it.removeAttr("style") }

        val colorMapper = ThemeColorMapper()
        val visualBackgrounds = mutableMapOf<Element, MappedBackground>()
        document.select("*").forEach { element ->
            val styles = appliedStyles[element].orEmpty()
                .values
                .sortedBy { it.order }
            val sourceBackground = styles.firstOrNull { it.property == "background-color" }
            val ownBackground = sourceBackground?.let { colorMapper.background(it.value) }
                ?: if (element.tagName().equals("mark", ignoreCase = true)) {
                    MappedBackground(
                        value = "var(--podaura-secondary-container)",
                        onColor = "var(--podaura-on-secondary-container)",
                    )
                } else {
                    null
                }
            val inheritedVisualBackground = element.parent()?.let(visualBackgrounds::get)
            val visualBackground = ownBackground?.takeIf { it.onColor != null }
                ?: inheritedVisualBackground
            visualBackground?.let { visualBackgrounds[element] = it }
            val declarations = styles
                .mapNotNull { style ->
                    normalizeDeclaration(
                        element = element,
                        property = style.property,
                        value = style.value,
                        colorMapper = colorMapper,
                        ownBackground = ownBackground,
                        visualBackground = visualBackground,
                    )
                }
                .toMutableList()
            if (sourceBackground == null && ownBackground != null) {
                declarations += "background-color: ${ownBackground.value}"
            }
            if (styles.none { it.property == "color" } &&
                (ownBackground != null || element.tagName().equals("a", ignoreCase = true))
            ) {
                visualBackground?.onColor?.let { declarations += "color: $it" }
            }
            if (declarations.isNotEmpty()) {
                element.attr(STYLE_SNAPSHOT_ATTRIBUTE, declarations.joinToString("; "))
            }
        }
        return document.outerHtml()
    }

    private fun removePageChrome(document: Document) {
        document.select("nav, footer, [role=navigation], [role=contentinfo]")
            .filter { it.closest("article") == null }
            .forEach { it.remove() }
    }

    private fun normalizeDeclaration(
        element: Element,
        property: String,
        value: String,
        colorMapper: ThemeColorMapper,
        ownBackground: MappedBackground?,
        visualBackground: MappedBackground?,
    ): String? {
        val normalizedValue = when (property) {
            "color" -> colorMapper.foreground(element, value, visualBackground)
            "background-color" -> ownBackground
                ?.takeUnless { it.value == "transparent" }
                ?.value
            "border", "border-top", "border-right", "border-bottom", "border-left" ->
                normalizeBorder(value)

            "border-color", "border-top-color", "border-right-color",
            "border-bottom-color", "border-left-color" -> "var(--podaura-outline-variant)"

            else -> value.trim().takeIf(::isSafeValue)
        } ?: return null
        return "$property: $normalizedValue"
    }

    private fun normalizeBorder(value: String): String? {
        val normalized = value.trim().lowercase()
        if (normalized in setOf("0", "none") || normalized.isGlobalCssKeyword()) return normalized
        if (!isSafeValue(normalized)) return null
        val width = Regex("(?:^|\\s)(thin|medium|thick|(?:\\d*\\.)?\\d+(?:px|em|rem|pt))(?=\\s|$)")
            .find(normalized)?.groupValues?.getOrNull(1)
        val style = Regex(
            "(?:^|\\s)(none|hidden|dotted|dashed|solid|double|groove|ridge|inset|outset)(?=\\s|$)"
        ).find(normalized)?.groupValues?.getOrNull(1)
        if (width == null && style == null) return null
        return listOfNotNull(width, style, "var(--podaura-outline-variant)").joinToString(" ")
    }

    private fun putStyle(
        styles: MutableMap<Element, MutableMap<String, AppliedStyle>>,
        element: Element,
        property: String,
        value: String,
        priority: Long,
    ) {
        val elementStyles = styles.getOrPut(element) { mutableMapOf() }
        val current = elementStyles[property]
        if (current == null || priority >= current.priority) {
            elementStyles[property] = AppliedStyle(
                property = property,
                value = value,
                priority = priority,
                order = priority,
            )
        }
    }

    private fun stylePriority(important: Boolean, specificity: Int, order: Int): Long =
        (if (important) 1_000_000_000L else 0L) + specificity * 100_000L + order

    private fun selectorSpecificity(selector: String): Int {
        val ids = Regex("#[A-Za-z0-9_-]+").findAll(selector).count()
        val classes = Regex("[.\\[][A-Za-z0-9_-]+").findAll(selector).count()
        val tags = Regex("(?:^|[>+~\\s,])([A-Za-z][A-Za-z0-9-]*)").findAll(selector).count()
        return ids * 100 + classes * 10 + tags
    }

    private fun parseRules(css: String): List<CssRule> {
        val withoutComments = css.replace(Regex("(?s)/\\*.*?\\*/"), "")
        return Regex("(?s)([^{}]+)\\{([^{}]*)}")
            .findAll(withoutComments)
            .flatMap { match ->
                val selector = match.groupValues[1].trim()
                if (selector.startsWith("@")) emptySequence()
                else selector.split(',').asSequence().map { CssRule(it.trim(), match.groupValues[2]) }
            }
            .filter { it.selector.isNotBlank() }
            .toList()
    }

    private fun parseDeclarations(block: String): List<CssDeclaration> =
        splitCssValues(block, ';').mapNotNull { declaration ->
            val separator = declaration.indexOf(':')
            if (separator <= 0) return@mapNotNull null
            val property = declaration.substring(0, separator).trim().lowercase()
            val rawValue = declaration.substring(separator + 1).trim()
            val important = rawValue.endsWith("!important", ignoreCase = true)
            val value = if (important) rawValue.dropLast("!important".length).trim() else rawValue
            CssDeclaration(property, value, important)
        }

    private fun splitCssValues(value: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        var quote: Char? = null
        var parentheses = 0
        var start = 0
        value.forEachIndexed { index, char ->
            when {
                quote != null && char == quote -> quote = null
                quote == null && char in charArrayOf('\'', '"') -> quote = char
                quote == null && char == '(' -> parentheses++
                quote == null && char == ')' -> parentheses = (parentheses - 1).coerceAtLeast(0)
                quote == null && parentheses == 0 && char == delimiter -> {
                    result += value.substring(start, index)
                    start = index + 1
                }
            }
        }
        result += value.substring(start)
        return result
    }

    private fun resolveCssVariables(value: String, variables: Map<String, String>): String {
        var resolved = value
        repeat(3) {
            resolved = Regex("var\\((--[A-Za-z0-9_-]+)(?:\\s*,\\s*([^)]*))?\\)")
                .replace(resolved) { match ->
                    variables[match.groupValues[1]]
                        ?: match.groupValues.getOrNull(2).orEmpty()
                }
        }
        return resolved
    }

    private fun isSafeValue(value: String): Boolean {
        if (value.isBlank() || value.length > 256) return false
        val lower = value.lowercase()
        return listOf(
            "url(",
            "expression(",
            "javascript:",
            "@import",
            "behavior:",
            "-moz-binding",
            "var(",
        ).none(lower::contains)
    }

    private fun sanitizeSourceSets(document: Document) {
        document.select("[srcset]").forEach { element ->
            val candidates = element.attr("srcset").split(',').mapNotNull { candidate ->
                val parts = candidate.trim().split(Regex("\\s+"), limit = 2)
                val url = parts.firstOrNull().orEmpty()
                if (url.isBlank()) return@mapNotNull null
                element.attr(SRCSET_RESOLUTION_ATTRIBUTE, url)
                val absoluteUrl = element.absUrl(SRCSET_RESOLUTION_ATTRIBUTE)
                element.removeAttr(SRCSET_RESOLUTION_ATTRIBUTE)
                if (!absoluteUrl.startsWith("https://", ignoreCase = true) &&
                    !absoluteUrl.startsWith("http://", ignoreCase = true)
                ) {
                    return@mapNotNull null
                }
                buildString {
                    append(absoluteUrl)
                    parts.getOrNull(1)?.let { descriptor -> append(' ').append(descriptor) }
                }
            }
            if (candidates.isEmpty()) element.removeAttr("srcset")
            else element.attr("srcset", candidates.joinToString(", "))
        }
    }

    private fun repairOrphanedContainerColors(document: Document) {
        val containerForegroundPattern = Regex(
            "var\\(--podaura-on-(primary|secondary|tertiary)-container\\)"
        )
        val foregroundPropertyPattern = Regex("(?:^|;)\\s*color\\s*:", RegexOption.IGNORE_CASE)
        val blockTags = setOf(
            "p", "div", "section", "li", "blockquote", "td", "th", "figcaption", "details",
        )
        document.select("[style]").toList().forEach { element ->
            val role = containerForegroundPattern.find(element.attr("style"))
                ?.groupValues?.getOrNull(1)
                ?: return@forEach
            val backgroundToken = "var(--podaura-$role-container)"
            val hasMatchingBackground = generateSequence(element) { it.parent() }
                .any { ancestor ->
                    ancestor.attr("style").contains("background-color: $backgroundToken")
                }
            if (hasMatchingBackground) return@forEach

            val host = generateSequence(element.parent()) { it.parent() }
                .firstOrNull { it.tagName().lowercase() in blockTags }
                ?: element
            val additions = buildList {
                add("background-color: $backgroundToken")
                if (!foregroundPropertyPattern.containsMatchIn(host.attr("style"))) {
                    add("color: var(--podaura-on-$role-container)")
                }
            }
            host.attr(
                "style",
                listOf(host.attr("style").trim().trimEnd(';'), additions.joinToString("; "))
                    .filter { it.isNotBlank() }
                    .joinToString("; "),
            )
        }
    }

    private fun fullContentSafelist(): Safelist = Safelist.relaxed()
        .addTags(
            "article", "section", "main", "header", "figure", "figcaption", "picture",
            "source", "mark", "details", "summary", "ruby", "rt", "rp", "kbd", "samp",
            "var", "time", "del", "ins", "font", "center", "audio", "video",
        )
        .addAttributes(":all", "class", "id", "style", "dir", "lang", "title")
        .addAttributes("a", "href", "name", "rel")
        .addAttributes("img", "src", "srcset", "sizes", "alt", "width", "height", "loading")
        .addAttributes("source", "src", "srcset", "sizes", "type", "media")
        .addAttributes("audio", "src", "controls", "preload")
        .addAttributes("video", "src", "poster", "controls", "preload", "playsinline")
        .addAttributes("td", "colspan", "rowspan", "headers")
        .addAttributes("th", "colspan", "rowspan", "headers", "scope")
        .addProtocols("a", "href", "http", "https", "mailto", "tel")
        .addProtocols("img", "src", "http", "https")
        .addProtocols("source", "src", "http", "https")
        .addProtocols("audio", "src", "http", "https")
        .addProtocols("video", "src", "http", "https")
        .addProtocols("video", "poster", "http", "https")

    private data class CssRule(val selector: String, val declarations: String)
    private data class CssDeclaration(val property: String, val value: String, val important: Boolean)
    private data class MappedBackground(val value: String, val onColor: String?)
    private data class AppliedStyle(
        val property: String,
        val value: String,
        val priority: Long,
        val order: Long,
    )

    private fun String.isGlobalCssKeyword(): Boolean =
        trim().lowercase() in setOf(
            "currentcolor", "inherit", "initial", "revert", "revert-layer", "unset",
        )

    private class ThemeColorMapper {
        private val accentRoles = linkedMapOf<String, String>()

        fun foreground(
            element: Element,
            sourceColor: String,
            background: MappedBackground?,
        ): String {
            val tag = element.tagName().lowercase()
            return when {
                isTransparent(sourceColor) -> "transparent"
                sourceColor.isGlobalCssKeyword() -> sourceColor.trim().lowercase()
                background?.onColor != null -> background.onColor
                tag == "a" -> "var(--podaura-primary)"
                tag in setOf("code", "pre", "kbd", "samp") -> "var(--podaura-tertiary)"
                tag in setOf("figcaption", "caption", "small") -> "var(--podaura-on-surface-variant)"
                isNeutral(sourceColor) -> "var(--podaura-on-surface)"
                else -> "var(--podaura-${accentRole(sourceColor)})"
            }
        }

        fun background(sourceColor: String): MappedBackground {
            val normalized = sourceColor.trim().lowercase()
            return when {
                isTransparent(normalized) -> MappedBackground(value = "transparent", onColor = null)
                normalized.isGlobalCssKeyword() -> MappedBackground(value = normalized, onColor = null)

                isNeutral(sourceColor) -> MappedBackground(
                    value = "var(--podaura-surface-variant)",
                    onColor = "var(--podaura-on-surface-variant)",
                )

                else -> accentRole(sourceColor).let { role ->
                    MappedBackground(
                        value = "var(--podaura-$role-container)",
                        onColor = "var(--podaura-on-$role-container)",
                    )
                }
            }
        }

        private fun accentRole(color: String): String = accentRoles.getOrPut(color.trim().lowercase()) {
            when (accentRoles.size) {
                0 -> "primary"
                1 -> "secondary"
                else -> "tertiary"
            }
        }

        private fun isNeutral(value: String): Boolean {
            val color = value.trim().lowercase()
            if (color in setOf(
                    "black", "white", "gray", "grey", "darkgray", "darkgrey", "lightgray",
                    "lightgrey", "silver", "currentcolor", "inherit", "initial", "unset",
                )
            ) return true

            if (color.startsWith('#')) {
                val hex = color.removePrefix("#")
                val channels = when (hex.length) {
                    3, 4 -> hex.take(3).map { "${it}${it}".toIntOrNull(16) }
                    6, 8 -> listOf(0, 2, 4).map { hex.substring(it, it + 2).toIntOrNull(16) }
                    else -> emptyList()
                }
                if (channels.size == 3 && channels.all { it != null }) {
                    val values = channels.filterNotNull()
                    return values.max() - values.min() <= 12
                }
            }

            if (color.startsWith("rgb")) {
                val channels = Regex("-?\\d+(?:\\.\\d+)?").findAll(color)
                    .mapNotNull { it.value.toDoubleOrNull() }
                    .take(3)
                    .toList()
                if (channels.size == 3) return channels.max() - channels.min() <= 12.0
            }

            if (color.startsWith("hsl")) {
                val saturation = color.substringAfter('(').substringBefore(')')
                    .replace(',', ' ')
                    .split(Regex("\\s+"))
                    .getOrNull(1)
                    ?.removeSuffix("%")
                    ?.toDoubleOrNull()
                if (saturation != null) return saturation <= 5.0
            }
            return false
        }

        private fun isTransparent(value: String): Boolean {
            val color = value.trim().lowercase().replace(" ", "")
            if (color == "transparent") return true
            if (color.startsWith('#')) {
                val hex = color.removePrefix("#")
                if (hex.length == 4 && hex.last() == '0') return true
                if (hex.length == 8 && hex.endsWith("00")) return true
            }
            if (color.startsWith("rgba(") || color.startsWith("hsla(")) {
                val alpha = color.substringAfterLast(',').substringBefore(')').toDoubleOrNull()
                if (alpha == 0.0) return true
            }
            return false
        }

    }
}

internal data class ProcessedPageCandidate(
    val html: String,
    val fromSemanticContainer: Boolean,
)
