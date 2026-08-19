package com.skyd.podaura.model.preference.player

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MpvStorageTest {
    @Test
    fun mirrorReplacesRuntimeWithCompleteSourceTree() = runTest {
        val root = Files.createTempDirectory("podaura-mpv-storage")
        try {
            val source = Files.createDirectories(root.resolve("source/scripts"))
            Files.writeString(root.resolve("source/mpv.conf"), "profile=fast")
            Files.writeString(source.resolve("autoload.lua"), "return true")

            val runtime = Files.createDirectories(root.resolve("runtime"))
            Files.writeString(runtime.resolve("stale.conf"), "stale")

            mirrorMpvConfigDirectory(
                source = PlatformFile(root.resolve("source").toString()),
                runtime = PlatformFile(runtime.toString()),
            )

            assertEquals("profile=fast", Files.readString(runtime.resolve("mpv.conf")))
            assertEquals("return true", Files.readString(runtime.resolve("scripts/autoload.lua")))
            assertFalse(Files.exists(runtime.resolve("stale.conf")))
            assertFalse(Files.exists(root.resolve("runtime.importing")))
            assertFalse(Files.exists(root.resolve("runtime.backup")))
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun failedMirrorKeepsLastRuntimeSnapshot() = runTest {
        val root = Files.createTempDirectory("podaura-mpv-storage")
        try {
            val runtime = Files.createDirectories(root.resolve("runtime"))
            Files.writeString(runtime.resolve("mpv.conf"), "last-good-config")

            assertFailsWith<IllegalArgumentException> {
                mirrorMpvConfigDirectory(
                    source = PlatformFile(root.resolve("missing").toString()),
                    runtime = PlatformFile(runtime.toString()),
                )
            }

            assertTrue(Files.isDirectory(runtime))
            assertEquals("last-good-config", Files.readString(runtime.resolve("mpv.conf")))
        } finally {
            root.toFile().deleteRecursively()
        }
    }
}
