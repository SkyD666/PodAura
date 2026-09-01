package com.skyd.downloader.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FileUtilTest {
    @Test
    fun sanitizesUnsafeAndEmptyFileNames() {
        assertEquals("episode___01.mp3", FileUtil.sanitizeFileName(" episode:*/01.mp3 "))
        assertEquals("download", FileUtil.sanitizeFileName(" .. "))
        assertEquals("_CON.mp3", FileUtil.sanitizeFileName("CON.mp3"))
    }

    @Test
    fun targetValidationRejectsPathSeparators() {
        assertFailsWith<IllegalArgumentException> {
            FileUtil.validateTarget("/downloads", "../episode.mp3")
        }
    }

    @Test
    fun urlFileNameDoesNotIncludeQueryOrFragment() {
        assertEquals(
            "episode 01.mp3",
            FileUtil.getFileNameFromUrl(
                "https://example.com/episode%2001.mp3?token=secret#part"
            ),
        )
    }
}
