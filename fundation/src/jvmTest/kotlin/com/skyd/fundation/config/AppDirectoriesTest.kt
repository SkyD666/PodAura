package com.skyd.fundation.config

import com.skyd.fundation.BuildKonfig
import java.io.File
import java.net.URLClassLoader
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppDirectoriesTest {
    @Test
    fun linuxPathsHonorAbsoluteXdgOverridesAndIgnoreMissingEmptyOrRelativeValues() {
        val home = File(System.getProperty("java.io.tmpdir"), "podaura xdg home").path
        val data = File(home, "custom data").path
        val cache = File(home, "custom cache").path
        val classpath = generateSequence(javaClass.classLoader) { it.parent }
            .filterIsInstance<URLClassLoader>().flatMap { it.urLs.asSequence() }
            .map { File(it.toURI()).path }
            .plus(System.getProperty("java.class.path").split(File.pathSeparator))
            .distinct().joinToString(File.pathSeparator)
        val cases = listOf(null to null, "" to "", "relative" to "~/cache", data to cache, data to null)
        for ((dataHome, cacheHome) in cases) {
            val builder = ProcessBuilder(listOf(
                File(System.getProperty("java.home"), "bin/java").path,
                "-Dos.name=Linux",
                "-Duser.home=$home",
                "-cp", classpath, AppDirectoriesProbe::class.java.name,
            )).redirectErrorStream(true)
            builder.environment().apply {
                remove("XDG_DATA_HOME")
                remove("XDG_CACHE_HOME")
                dataHome?.let { put("XDG_DATA_HOME", it) }
                cacheHome?.let { put("XDG_CACHE_HOME", it) }
            }
            val process = builder.start()
            try {
                assertTrue(process.waitFor(10, TimeUnit.SECONDS), "Directory probe timed out")
                val output = process.inputStream.bufferedReader().readLines()
                assertEquals(expected = 0, actual = process.exitValue(), message = output.joinToString("\n"))
                val expectedData = File(if (dataHome == data) data else File(home, ".local/share").path, BuildKonfig.packageName).path
                val expectedCache = File(if (cacheHome == cache) cache else File(home, ".cache").path, BuildKonfig.packageName).path
                assertEquals(
                    expected = listOf(expectedData, expectedCache, File(expectedData, "Database").path),
                    actual = output,
                )
            } finally {
                process.destroyForcibly()
                process.waitFor()
            }
        }
    }
}

object AppDirectoriesProbe {
    @JvmStatic
    fun main(args: Array<String>) {
        println(appDirectories.dataDir)
        println(appDirectories.cacheDir)
        println(Const.DB_DIR)
    }
}
