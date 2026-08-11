package com.skyd.podaura.model.repository.fullcontent

import com.fleeksoft.ksoup.Ksoup

internal data class ContentDiagnostics(
    val score: Int,
    val acceptable: Boolean,
    val textLength: Int,
    val paragraphCount: Int,
    val mediaCount: Int,
    val linkTextRatio: Double,
)

internal object ContentQualityEvaluator {
    fun evaluate(html: String, baseUrl: String): ContentDiagnostics {
        val document = Ksoup.parseBodyFragment(html, baseUrl)
        val body = document.body()
        val textLength = body.text().count { !it.isWhitespace() }
        val linkTextLength = body.select("a").sumOf { link ->
            link.text().count { !it.isWhitespace() }
        }
        val linkTextRatio = if (textLength == 0) 0.0 else {
            (linkTextLength.toDouble() / textLength).coerceIn(0.0, 1.0)
        }
        val paragraphCount = body.select("p,blockquote,pre,li").count { it.text().isNotBlank() }
        val headingCount = body.select("h1,h2,h3,h4,h5,h6").count { it.text().isNotBlank() }
        val mediaCount = body.select("img,table,audio,video,figure,hr").size
        val semanticCount = body.select("article,main,[itemprop=articleBody]").size
        val linkCount = body.select("a").size

        var score = 0
        score += paragraphCount.coerceAtMost(8) * 2
        score += headingCount.coerceAtMost(3)
        score += mediaCount.coerceAtMost(4) * 3
        score += semanticCount.coerceAtMost(2) * 3
        if (textLength > 0 && linkCount == 0) score += 4
        if (linkTextRatio <= 0.25) score += 3
        if (linkTextRatio >= 0.65) score -= 8
        if (linkCount >= 6 && paragraphCount <= 1) score -= 6

        val hasRenderableContent = textLength > 0 || mediaCount > 0
        val isNavigationLike = linkTextRatio >= 0.65 ||
            (linkCount >= 6 && paragraphCount <= 1 && mediaCount == 0)
        val hasArticleStructure = paragraphCount > 0 || headingCount > 0 ||
            semanticCount > 0 || mediaCount > 0
        val acceptable = hasRenderableContent && !isNavigationLike &&
            (hasArticleStructure || linkCount == 0)

        return ContentDiagnostics(
            score = score,
            acceptable = acceptable,
            textLength = textLength,
            paragraphCount = paragraphCount,
            mediaCount = mediaCount,
            linkTextRatio = linkTextRatio,
        )
    }
}

