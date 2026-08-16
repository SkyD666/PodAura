package com.skyd.podaura.model.repository.translation

import com.fleeksoft.ksoup.Ksoup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TranslationDocumentTest {
    private val builder = TranslationDocumentBuilder()
    private val validator = TranslationHtmlValidator()

    @Test
    fun buildsOneEnvelopeAndProtectsInteractiveAndCodeNodes() {
        val original = """
            <p>A <em>single sentence</em> across tags.</p>
            <pre><code>println("unchanged")</code></pre>
            <a href="https://example.com" data-position="42">Listen</a>
            <picture><source srcset="a.webp 1x, b.webp 2x"><img src="cover.jpg" alt="Cover"></picture>
            <video poster="poster.jpg"><source src="episode.mp3" type="audio/mpeg"></video>
            <script>steal()</script>
        """.trimIndent()

        val envelope = builder.build("Title", original)
        val document = Ksoup.parse(envelope.html)

        assertEquals(1, document.select("[data-podaura-document=article]").size)
        assertEquals("Title", document.selectFirst("[data-podaura-field=title]")?.text())
        assertEquals(0, document.select("script").size)
        document.select("pre,code").forEach {
            assertEquals("no", it.attr("translate"))
            assertTrue(it.hasClass("notranslate"))
            assertTrue(it.attr("data-podaura-node-id").isNotBlank())
        }
        assertEquals(5, envelope.criticalNodes.count { it.tagName in setOf("a", "img", "source", "video") })
        assertEquals(1, document.select("[data-podaura-field=content]").size)
    }

    @Test
    fun extractsTranslatedTitleAndBodyAfterStructuredValidation() {
        val envelope = builder.build(
            title = "Original title",
            contentHtml = "<p>Hello <strong>world</strong>.</p><a href='https://example.com'>Link</a>",
        )
        val response = Ksoup.parse(envelope.html).apply {
            selectFirst("[data-podaura-field=title]")?.text("Translated title")
            selectFirst("p")?.html("Bonjour <strong>le monde</strong>.")
            outputSettings().prettyPrint(false)
        }.outerHtml()

        val result = validator.validate(response, envelope)

        assertNotNull(result)
        assertEquals("Translated title", result.title)
        val content = Ksoup.parseBodyFragment(result.contentHtml)
        assertEquals("Bonjour le monde. Link", content.text())
        assertEquals("https://example.com", content.selectFirst("a")?.attr("href"))
        assertFalse(result.contentHtml.contains("data-podaura-node-id"))
    }

    @Test
    fun rejectsMissingCriticalNodesAndRestoresProtectedContent() {
        val envelope = builder.build(
            title = "Title",
            contentHtml = "<code>val token = 1</code><a href='https://example.com'>Link</a><img src='x.jpg'>",
        )
        val missingImage = Ksoup.parse(envelope.html).apply {
            selectFirst("img")?.remove()
        }.outerHtml()
        val changedUrl = Ksoup.parse(envelope.html).apply {
            selectFirst("a")?.attr("href", "https://attacker.example")
        }.outerHtml()
        val changedCode = Ksoup.parse(envelope.html).apply {
            selectFirst("code")?.text("val token = 2")
        }.outerHtml()
        val duplicatedImage = Ksoup.parse(envelope.html).apply {
            selectFirst("img")?.let { image -> image.before(image.outerHtml()) }
        }.outerHtml()

        assertNull(validator.validate(missingImage, envelope))
        val diagnostic = assertIs<TranslationHtmlValidationResult.Invalid>(
            validator.validateDetailed(missingImage, envelope)
        ).diagnostic
        assertEquals(TranslationHtmlValidationFailureReason.CriticalNodeCount, diagnostic.reason)
        assertEquals(0, diagnostic.actualCount)
        assertNotNull(diagnostic.nodeId)
        val restoredUrl = assertNotNull(validator.validate(changedUrl, envelope))
        val restoredCode = assertNotNull(validator.validate(changedCode, envelope))
        assertEquals(
            "https://example.com",
            Ksoup.parseBodyFragment(restoredUrl.contentHtml).selectFirst("a")?.attr("href"),
        )
        assertEquals(
            "val token = 1",
            Ksoup.parseBodyFragment(restoredCode.contentHtml).selectFirst("code")?.text(),
        )
        val duplicateDiagnostic = assertIs<TranslationHtmlValidationResult.Invalid>(
            validator.validateDetailed(duplicatedImage, envelope)
        ).diagnostic
        assertEquals(TranslationHtmlValidationFailureReason.CriticalNodeCount, duplicateDiagnostic.reason)
        assertEquals(2, duplicateDiagnostic.actualCount)
    }

    @Test
    fun acceptsChineseToEnglishTranslationAndRestoresMachineAttributes() {
        val envelope = builder.build(
            title = "中文标题",
            contentHtml = """
                <p data-section="正文"><a href="https://example.com/?q=中文" data-action="打开">阅读详情</a></p>
                <code>val 标签 = "保持不变"</code>
            """.trimIndent(),
        )
        val response = Ksoup.parse(envelope.html).apply {
            selectFirst("[data-podaura-field=title]")?.text("English title")
            selectFirst("p")
                ?.attr("data-section", "body")
                ?.selectFirst("a")
                ?.attr("href", "https://attacker.example")
                ?.attr("data-action", "open")
                ?.text("Read more")
            selectFirst("code")?.text("val label = \"changed\"")
            outputSettings().prettyPrint(false)
        }.outerHtml()

        val result = assertNotNull(validator.validate(response, envelope))
        val content = Ksoup.parseBodyFragment(result.contentHtml)

        assertEquals("English title", result.title)
        assertEquals("Read more", content.selectFirst("a")?.text())
        assertEquals("https://example.com/?q=中文", content.selectFirst("a")?.attr("href"))
        assertEquals("打开", content.selectFirst("a")?.attr("data-action"))
        assertEquals("正文", content.selectFirst("p")?.attr("data-section"))
        assertEquals("val 标签 = \"保持不变\"", content.selectFirst("code")?.text())
    }

    @Test
    fun acceptsChineseToEnglishTranslationWhenDeepLSplitsAnAnchor() {
        val originalUrl = "https://example.com/?q=中文"
        val envelope = builder.build(
            title = "中文标题",
            contentHtml = "<p>请<a href='$originalUrl' data-action='打开'>阅读详情</a>以继续。</p>",
        )
        val response = Ksoup.parse(envelope.html).apply {
            selectFirst("[data-podaura-field=title]")?.text("English title")
            selectFirst("a")?.let { anchor ->
                val nodeId = anchor.attr("data-podaura-node-id")
                anchor.before(
                    "<a data-podaura-node-id='$nodeId' href='https://attacker.example/one'>Read</a> " +
                        "the <a data-podaura-node-id='$nodeId' href='javascript:steal()'>details</a>"
                )
                anchor.remove()
            }
            outputSettings().prettyPrint(false)
        }.outerHtml()

        val result = assertNotNull(validator.validate(response, envelope))
        val content = Ksoup.parseBodyFragment(result.contentHtml)
        val links = content.select("a")

        assertEquals("English title", result.title)
        assertEquals(2, links.size)
        assertTrue(links.all { it.attr("href") == originalUrl })
        assertTrue(links.all { it.attr("data-action") == "打开" })
        assertFalse(result.contentHtml.contains("data-podaura-node-id"))
    }

    @Test
    fun acceptsTranslationWhenDeepLRemovesAnAnchor() {
        val envelope = builder.build(
            title = "简体标题",
            contentHtml = "<p>阅读<a href='https://example.com'>详细内容</a>。</p>",
        )
        val response = Ksoup.parse(envelope.html).apply {
            selectFirst("[data-podaura-field=title]")?.text("繁體標題")
            selectFirst("a")?.let { anchor ->
                anchor.before("詳細內容")
                anchor.remove()
            }
            outputSettings().prettyPrint(false)
        }.outerHtml()

        val result = assertNotNull(validator.validate(response, envelope))
        val content = Ksoup.parseBodyFragment(result.contentHtml)

        assertEquals("繁體標題", result.title)
        assertEquals(0, content.select("a").size)
        assertTrue(content.text().contains("詳細內容"))
    }

    @Test
    fun unwrapsReturnedAnchorWhenDeepLDropsItsStableId() {
        val envelope = builder.build(
            title = "简体标题",
            contentHtml = "<p><a href='https://example.com'>详细内容</a></p>",
        )
        val response = Ksoup.parse(envelope.html).apply {
            selectFirst("a")
                ?.removeAttr("data-podaura-node-id")
                ?.attr("href", "javascript:steal()")
                ?.text("詳細內容")
            outputSettings().prettyPrint(false)
        }.outerHtml()

        val result = assertNotNull(validator.validate(response, envelope))
        val content = Ksoup.parseBodyFragment(result.contentHtml)

        assertEquals(0, content.select("a").size)
        assertTrue(content.text().contains("詳細內容"))
        assertFalse(result.contentHtml.contains("javascript:"))
    }

    @Test
    fun acceptsUnchangedNestedPreAndCodeNodes() {
        val envelope = builder.build(
            title = "Code",
            contentHtml = "<pre><code>val answer = 42</code></pre>",
        )

        val result = validator.validate(envelope.html, envelope)

        assertNotNull(result)
        assertTrue(result.contentHtml.contains("val answer = 42"))
        assertFalse(result.contentHtml.contains("data-podaura-node-id"))
    }

    @Test
    fun sanitizesMaliciousReturnedContentAndRejectsWrappers() {
        val envelope = builder.build("Title", "<p>Hello</p>")
        val malicious = Ksoup.parse(envelope.html).apply {
            selectFirst("p")?.attr("onclick", "steal()")
                ?.append("<script>steal()</script><iframe src='https://attacker.example'></iframe>")
        }.outerHtml()

        val result = validator.validate(malicious, envelope)

        assertNotNull(result)
        assertFalse(result.contentHtml.contains("onclick"))
        assertEquals(0, Ksoup.parseBodyFragment(result.contentHtml).select("script,iframe").size)
        assertNull(validator.validate("```html\n${envelope.html}\n```", envelope))
        assertNull(validator.validate("The translation is: ${envelope.html}", envelope))
        assertNull(validator.validate("{\"html\":\"escaped\"}", envelope))
        assertNull(validator.validate("<p>Translation:</p>${envelope.html}", envelope))
    }

    @Test
    fun acceptsEmptyTitleAndImageOnlyBody() {
        val envelope = builder.build(null, "<img src='https://example.com/image.jpg' alt='image'>")
        val result = validator.validate(envelope.html, envelope)

        assertNotNull(result)
        assertEquals("", result.title)
        assertEquals(1, Ksoup.parseBodyFragment(result.contentHtml).select("img").size)
    }

    @Test
    fun validatesSrcSetAudioVideoAndTimestampAttributes() {
        val source = """
            <a href="podaura://timestamp/15" data-podaura-timestamp="15">00:15</a>
            <img src="a.jpg" srcset="a.jpg 1x, a2.jpg 2x">
            <audio src="episode.mp3" controls></audio>
            <video poster="poster.jpg"><source src="episode.webm" type="video/webm"></video>
            <time datetime="2026-08-15T10:00:00Z">today</time>
        """.trimIndent()
        val envelope = builder.build("Media", source)

        assertNotNull(validator.validate(envelope.html, envelope))
        val corrupted = Ksoup.parse(envelope.html).apply {
            selectFirst("[srcset]")?.attr("srcset", "evil.jpg 1x")
        }.outerHtml()
        val restored = assertNotNull(validator.validate(corrupted, envelope))
        assertEquals(
            "a.jpg 1x, a2.jpg 2x",
            Ksoup.parseBodyFragment(restored.contentHtml).selectFirst("[srcset]")?.attr("srcset"),
        )
    }
}
