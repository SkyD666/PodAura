package com.skyd.podaura.model.repository.media

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.name
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MediaFileOperationsTest {
    @Test
    fun renameAndDeleteDirectoryRecursively() = runTest {
        val rootPath = Files.createTempDirectory("podaura-media-operations")
        try {
            val oldDirectoryPath = Files.createDirectories(rootPath.resolve("old/nested"))
            Files.writeString(oldDirectoryPath.resolve("media.mp3"), "test")

            val root = PlatformFile(rootPath.toString())
            val oldDirectory = PlatformFile(root, "old")
            val renamedDirectory = assertNotNull(oldDirectory.renameIn(root, "renamed"))

            assertEquals("renamed", renamedDirectory.name)
            assertFalse(oldDirectory.exists())
            assertTrue(PlatformFile(renamedDirectory, "nested/media.mp3").exists())
            assertTrue(renamedDirectory.deleteRecursively())
            assertFalse(renamedDirectory.exists())
        } finally {
            rootPath.toFile().deleteRecursively()
        }
    }
}
