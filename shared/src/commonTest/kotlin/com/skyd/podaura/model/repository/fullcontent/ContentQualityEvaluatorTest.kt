package com.skyd.podaura.model.repository.fullcontent

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContentQualityEvaluatorTest {
    @Test
    fun acceptsShortContentWithoutAMinimumCharacterThreshold() {
        val result = ContentQualityEvaluator.evaluate(
            html = "<article><p>更新完成。</p></article>",
            baseUrl = "https://example.com/notice",
        )

        assertTrue(result.acceptable)
    }

    @Test
    fun acceptsMediaOnlyContent() {
        val result = ContentQualityEvaluator.evaluate(
            html = "<article><img src='https://example.com/poster.jpg'></article>",
            baseUrl = "https://example.com/poster",
        )

        assertTrue(result.acceptable)
    }

    @Test
    fun rejectsLinkHeavyPageChrome() {
        val result = ContentQualityEvaluator.evaluate(
            html = """
                <div><a href='/1'>About</a><a href='/2'>Contact</a>
                <a href='/3'>Jobs</a><a href='/4'>Terms</a>
                <a href='/5'>Privacy</a><a href='/6'>Copyright</a></div>
            """.trimIndent(),
            baseUrl = "https://example.com/shell",
        )

        assertFalse(result.acceptable)
    }
}
