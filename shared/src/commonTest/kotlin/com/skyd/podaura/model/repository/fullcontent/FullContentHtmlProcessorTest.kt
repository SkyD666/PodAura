package com.skyd.podaura.model.repository.fullcontent

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FullContentHtmlProcessorTest {

    @Test
    fun preservesSemanticStylesButDropsPageSpecificTypographyAndLayout() {
        val html = """
            <html>
              <head>
                <title>Styled article</title>
                <style>
                  .lead {
                    color: blue;
                    font-size: 28px;
                    line-height: 50px;
                    width: 920px;
                    margin-left: 240px;
                  }
                  .note {
                    color: #ff0000;
                    background-color: rgb(255, 240, 200);
                    border-left: 3px solid #ff0000;
                  }
                </style>
              </head>
              <body>
                <article>
                  <h1>Styled article</h1>
                  <p class="lead">
                    Beijing is the capital of China and this paragraph carries the primary article text.
                  </p>
                  <p class="note" style="font-weight: 700">A second paragraph keeps its emphasis and safe layout.</p>
                </article>
              </body>
            </html>
        """.trimIndent()

        val result = FullContentHtmlProcessor.process(html, "https://example.com/news/article")

        assertContains(result, "font-weight: 700")
        assertContains(result, "var(--podaura-primary)")
        assertContains(result, "var(--podaura-secondary-container)")
        assertContains(result, "var(--podaura-on-secondary-container)")
        assertContains(result, "border-left: 3px solid var(--podaura-outline-variant)")
        assertFalse(result.contains("color: blue", ignoreCase = true))
        assertFalse(result.contains("#ff0000", ignoreCase = true))
        assertFalse(result.contains("font-size", ignoreCase = true))
        assertFalse(result.contains("line-height", ignoreCase = true))
        assertFalse(result.contains("width: 920px", ignoreCase = true))
        assertFalse(result.contains("margin-left", ignoreCase = true))
    }

    @Test
    fun removesActiveContentAndUnsafeUrls() {
        val html = """
            <html><head><title>Safe article</title></head><body><article>
              <p onclick="steal()" style="position: fixed; color: green">
                Safe article text with enough context to identify this node as the article body.
              </p>
              <a href="javascript:steal()">Unsafe link</a>
              <script>steal()</script>
              <img src="/images/photo.jpg"
                   srcset="/images/photo.jpg 1x, //cdn.example.com/photo@2x.jpg 2x"
                   onerror="steal()" alt="Photo">
            </article></body></html>
        """.trimIndent()

        val result = FullContentHtmlProcessor.process(html, "https://example.com/posts/one")

        assertFalse(result.contains("script", ignoreCase = true))
        assertFalse(result.contains("onclick", ignoreCase = true))
        assertFalse(result.contains("onerror", ignoreCase = true))
        assertFalse(result.contains("javascript:", ignoreCase = true))
        assertFalse(result.contains("position:", ignoreCase = true))
        assertContains(result, "https://example.com/images/photo.jpg")
        assertContains(result, "https://cdn.example.com/photo@2x.jpg 2x")
    }

    @Test
    fun keepsOnlyValidatedGeneratedInlineStyles() {
        val result = FullContentHtmlProcessor.process(
            html = """
                <html><body><article>
                <p style="position: fixed; inset: 0; font-weight: 700"
                   data-podaura-style="position: fixed; inset: 0; background-image: url(https://attacker.example/track)">
                  Safe article text remains visible while forged and unsafe styles are removed.
                  <span style="position: absolute; inset: 0">Nested article text.</span>
                </p>
                </article></body></html>
            """.trimIndent(),
            baseUrl = "https://example.com/article",
        )

        assertContains(result, "font-weight: 700")
        assertFalse(result.contains("position", ignoreCase = true))
        assertFalse(result.contains("inset", ignoreCase = true))
        assertFalse(result.contains("background-image", ignoreCase = true))
        assertFalse(result.contains("attacker.example", ignoreCase = true))
        assertFalse(result.contains("data-podaura-style", ignoreCase = true))
    }

    @Test
    fun acceptsShortArticlesWithoutAnApplicationLengthThreshold() {
        val result = FullContentHtmlProcessor.process(
            html = "<html><head><title>Note</title></head><body><article><p>Short note.</p></article></body></html>",
            baseUrl = "https://example.com/note",
        )

        assertTrue(result.contains("Short note."))
    }

    @Test
    fun doesNotTreatStandaloneNavigationAndFooterAsArticleContent() {
        assertFailsWith<FullContentException> {
            FullContentHtmlProcessor.process(
                html = """
                    <html><body>
                    <nav>Home Categories Contact</nav>
                    <footer>About Privacy Copyright 2026 Customer support</footer>
                    </body></html>
                """.trimIndent(),
                baseUrl = "https://example.com/empty-shell",
            )
        }
    }

    @Test
    fun givesSemanticHighlightsThemeContainerColors() {
        val result = FullContentHtmlProcessor.process(
            html = """
                <html><head><title>Highlight</title></head><body><article>
                <p>A paragraph with a <mark>highlighted phrase</mark> in its body.</p>
                </article></body></html>
            """.trimIndent(),
            baseUrl = "https://example.com/highlight",
        )

        assertContains(result, "background-color: var(--podaura-secondary-container)")
        assertContains(result, "color: var(--podaura-on-secondary-container)")
    }

    @Test
    fun mapsNestedForegroundsToTheVisibleContainerRole() {
        val result = FullContentHtmlProcessor.process(
            html = """
                <html><head><title>Callout</title></head><body><article>
                <div style="background-color: red">
                  <span style="color: white">Text inside a colored callout remains readable.</span>
                </div>
                </article></body></html>
            """.trimIndent(),
            baseUrl = "https://example.com/callout",
        )

        assertContains(result, "background-color: var(--podaura-primary-container)")
        assertContains(result, "color: var(--podaura-on-primary-container)")
    }

    @Test
    fun doesNotMapTransparentComputedBackgroundsToVisibleContainers() {
        val result = FullContentHtmlProcessor.process(
            html = """
                <html><body><article>
                <div style="background-color: rgba(0, 0, 0, 0)">
                  <p>Transparent source layout remains transparent in reading mode.</p>
                </div>
                </article></body></html>
            """.trimIndent(),
            baseUrl = "https://example.com/transparent",
        )

        assertFalse(result.contains("background-color"))
        assertFalse(result.contains("surface-variant"))
    }

    @Test
    fun retainsSafeLegacyColorsWithoutImportingSourceTypography() {
        val result = FullContentHtmlProcessor.process(
            html = """
                <html><head><title>Legacy style</title></head><body><article>
                <p><font color="blue" face="serif" size="5">Legacy styled article text.</font></p>
                </article></body></html>
            """.trimIndent(),
            baseUrl = "https://example.com/legacy",
        )

        assertContains(result, "color: var(--podaura-primary)")
        assertFalse(result.contains("font-family", ignoreCase = true))
        assertFalse(result.contains("font-size", ignoreCase = true))
    }

    @Test
    fun providesACompleteSemanticContainerCandidate() {
        val candidates = FullContentHtmlProcessor.processPageCandidates(
            html = """
                <html><body><main itemprop="articleBody">
                  <section><p>The opening section of the article.</p></section>
                  <section><p>The middle section must remain in the extracted article.</p></section>
                  <section><p>The final section must not be dropped.</p></section>
                  <aside><p>A promotional sidebar must not become article content.</p></aside>
                  <section id="comments"><p>A reader discussion must not become article content.</p></section>
                </main></body></html>
            """.trimIndent(),
            baseUrl = "https://example.com/split-article",
        )

        val semanticCandidate = candidates.first { it.fromSemanticContainer }
        assertContains(semanticCandidate.html, "opening section")
        assertContains(semanticCandidate.html, "middle section")
        assertContains(semanticCandidate.html, "final section")
        assertFalse(semanticCandidate.html.contains("promotional sidebar"))
        assertFalse(semanticCandidate.html.contains("reader discussion"))
    }

    @Test
    fun restoresLazyImagesAndKeepsNativeMedia() {
        val result = FullContentHtmlProcessor.processArticleFragment(
            html = """
                <article>
                  <p>An article with lazy media.</p>
                  <img src="data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw=="
                       data-src="/images/full.jpg" data-srcset="/images/full.jpg 1x, /images/full@2x.jpg 2x">
                  <audio controls src="/audio/episode.mp3"></audio>
                  <video controls poster="/images/poster.jpg" src="/video/clip.mp4"></video>
                </article>
            """.trimIndent(),
            baseUrl = "https://example.com/posts/one",
        )

        assertContains(result, "https://example.com/images/full.jpg")
        assertContains(result, "https://example.com/images/full@2x.jpg 2x")
        assertContains(result, "https://example.com/audio/episode.mp3")
        assertContains(result, "https://example.com/video/clip.mp4")
        assertContains(result, "https://example.com/images/poster.jpg")
    }

    @Test
    fun dropsSourceListResetsWhenPseudoElementMarkersCannotBeSnapshotted() {
        val result = FullContentHtmlProcessor.processArticleFragment(
            html = """
                <article class="sn-content">
                  <p>Episode show notes.</p>
                  <ul style="list-style-type: none; list-style-position: outside">
                    <li style="list-style-type: none; list-style-position: outside">
                      <p>Other podcasts from this producer.</p>
                    </li>
                    <li style="list-style-type: none; list-style-position: outside">
                      <p>Recommend this episode to friends.</p>
                    </li>
                    <li style="list-style-type: none; list-style-position: outside">
                      <p>Copyright notice.</p>
                    </li>
                  </ul>
                </article>
            """.trimIndent(),
            baseUrl = "https://www.xiaoyuzhoufm.com/episode/example",
        )

        assertContains(result, "<ul>")
        assertContains(result, "<li>")
        assertFalse(result.contains("list-style", ignoreCase = true))
    }
}
