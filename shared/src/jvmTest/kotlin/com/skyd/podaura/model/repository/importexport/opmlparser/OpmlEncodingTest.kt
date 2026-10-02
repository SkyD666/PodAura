package com.skyd.podaura.model.repository.importexport.opmlparser

import kotlinx.io.Buffer
import java.nio.charset.Charset
import kotlin.test.Test
import kotlin.test.assertEquals

class OpmlEncodingTest {
    @Test
    fun preservesXmlDeclaredEncoding() {
        for ((encoding, title) in listOf(
            "UTF-16" to "中文播客 café 🎧",
            "UTF-16LE" to "中文播客 café 🎧",
            "UTF-16BE" to "中文播客 café 🎧",
            "ISO-8859-1" to "café déjà vu",
        )) {
            val document = """
                <?xml version="1.0" encoding="$encoding"?>
                <opml version="2.0">
                  <head><title>$title</title></head>
                  <body><outline text="$title" xmlUrl="https://example.com/feed" /></body>
                </opml>
            """.trimIndent()
            val result = OPML().decodeFromSource(Buffer().apply {
                write(document.toByteArray(Charset.forName(encoding)))
            })
            assertEquals(title, result.head.title, encoding)
            assertEquals(title, result.body.outlines.single().text, encoding)
        }
    }
}
