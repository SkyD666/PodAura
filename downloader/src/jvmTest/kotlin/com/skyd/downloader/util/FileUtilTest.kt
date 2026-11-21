package com.skyd.downloader.util

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

    @Test
    fun atomicPublicationRejectsAFileCreatedAfterTheCallerCheckedTheDestination() = runTest {
        val directory = Files.createTempDirectory("exclusive-publication")
        try {
            val part = directory.resolve("download.part").apply { writeText("download") }
            val target = directory.resolve("audio.mp3")
            assertFalse(target.exists())
            target.writeText("download")
            val staged = PlatformFile(part.toString())
            val final = PlatformFile(target.toString())
            assertFalse(publishPartWithoutReplacement(
                part = staged,
                target = final,
            ))
            assertFalse(shareFileIdentity(part = staged, target = final))
            assertEquals("download", target.readText())
            assertEquals("download", part.readText())
            Files.delete(target)
            assertTrue(publishPartWithoutReplacement(part = staged, target = final))
            assertTrue(shareFileIdentity(part = staged, target = final))
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
