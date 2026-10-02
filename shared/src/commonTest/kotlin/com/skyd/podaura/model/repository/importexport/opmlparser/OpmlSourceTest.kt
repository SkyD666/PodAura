package com.skyd.podaura.model.repository.importexport.opmlparser

import kotlinx.io.Buffer
import kotlinx.io.writeString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class OpmlSourceTest {
    @Test
    fun readsUtf8OutlinesAcrossBufferBoundaries() {
        val title = "中文播客 café 🎧"
        val description = "内容".repeat(5000)
        val document = """
            <?xml version="1.0" encoding="UTF-8"?>
            <opml version="2.0">
              <head><title>$title</title></head>
              <body>
                <outline text="$title" description="$description" xmlUrl="https://example.com/feed" />
                <outline text="最后一个" xmlUrl="https://example.com/last" />
              </body>
            </opml>
        """.trimIndent()
        val result = OPML().decodeFromSource(Buffer().apply { writeString(document) })
        assertEquals(title, result.head.title)
        assertEquals(2, result.body.outlines.size)
        assertEquals(title, result.body.outlines.first().text)
        assertEquals(description, result.body.outlines.first().description)
        assertEquals("https://example.com/last", result.body.outlines.last().xmlUrl)
        assertFails {
            OPML().decodeFromSource(Buffer().apply { writeString(document.substringBefore("</body>")) })
        }
    }
}
