package com.skyd.podaura.ui.window

import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetAddress
import java.net.Socket
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopInstanceTest {
    private val directory = Files.createTempDirectory("podaura-instance-test")
    private val executable = Files.createFile(directory.resolve("PodAura.exe"))
    private val requests = LinkedBlockingQueue<List<String>>()
    private val instances = mutableListOf<DesktopInstance>()

    private fun instance(onRequest: (List<String>) -> Unit = { requests.add(it) }) = DesktopInstance(
        directory.resolve("state"), executable, onRequest,
    ).also { instances += it }

    @AfterTest
    fun cleanup() {
        instances.asReversed().forEach { it.close() }
        directory.toFile().deleteRecursively()
    }

    @Test
    fun coldLaunchBuffersFilesAndWarmLaunchPreservesUnicodeSpacesAndOrder() {
        assertTrue(instance().startOrForward(listOf("first.mp3")))
        assertEquals(listOf("first.mp3"), requests.poll(3, TimeUnit.SECONDS))
        val files = listOf("C:\\media\\\u4e2d\u6587 audio.mp3", "C:\\media\\a & b #1.mp4")
        assertFalse(instance().startOrForward(files))
        assertEquals(files, requests.poll(3, TimeUnit.SECONDS))
    }

    @Test
    fun ordinaryLaunchForwardsAnEmptyActivationWithoutStartingAnotherOwner() {
        assertTrue(instance().startOrForward(listOf("playing.mp3")))
        requests.take()
        assertFalse(instance().startOrForward(emptyList()))
        assertEquals(emptyList(), requests.poll(3, TimeUnit.SECONDS))
    }

    @Test
    fun differentApplicationCopiesHaveIndependentInstances() {
        assertTrue(instance().startOrForward(emptyList()))
        val otherExecutable = Files.createFile(directory.resolve("OtherPodAura.exe"))
        DesktopInstance(directory.resolve("state"), otherExecutable, {}).use {
            assertTrue(it.startOrForward(emptyList()))
        }
    }

    @Test
    fun closingASecondaryDoesNotRemoveThePrimaryEndpoint() {
        assertTrue(instance().startOrForward(emptyList()))
        requests.take()
        instance().use { assertFalse(it.startOrForward(listOf("one.mp3"))) }
        assertEquals(listOf("one.mp3"), requests.poll(3, TimeUnit.SECONDS))
        instance().use { assertFalse(it.startOrForward(listOf("two.mp3"))) }
        assertEquals(listOf("two.mp3"), requests.poll(3, TimeUnit.SECONDS))
    }

    @Test
    fun aNewOwnerCanStartAfterShutdown() {
        instance().use { assertTrue(it.startOrForward(emptyList())) }
        assertTrue(instance().startOrForward(listOf("restart.mp3")))
    }

    @Test
    fun failedPrimaryStartupReleasesTheLock() {
        val broken = instance { error("Initialization failed") }
        kotlin.test.assertFailsWith<IllegalStateException> {
            broken.startOrForward(emptyList())
        }
        assertTrue(instance().startOrForward(emptyList()))
    }

    @Test
    fun simultaneousLaunchesElectOneOwnerAndDeliverEveryRequest() {
        val contenders = List(6) { instance() }
        val executor = Executors.newFixedThreadPool(contenders.size)
        try {
            val results = executor.invokeAll(contenders.mapIndexed { index, contender ->
                Callable { contender.startOrForward(listOf("$index.mp3")) }
            }).map { it.get(15, TimeUnit.SECONDS) }
            assertEquals(1, results.count { it })
            val received = List(contenders.size) { requests.poll(3, TimeUnit.SECONDS) }
            assertEquals((0..5).map { listOf("$it.mp3") }.toSet(), received.toSet())
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun unauthenticatedRequestsAreRejectedWithoutDisablingTheListener() {
        assertTrue(instance().startOrForward(emptyList()))
        requests.take()
        val endpointFile = Files.list(directory.resolve("state")).use { files ->
            files.filter { it.toString().endsWith(".properties") }.findFirst().orElseThrow()
        }
        val endpoint = Properties().apply { Files.newInputStream(endpointFile).use(::load) }
        Socket(InetAddress.getLoopbackAddress(), endpoint.getProperty("port").toInt()).use {
            it.soTimeout = 3_000
            DataOutputStream(it.getOutputStream()).apply {
                writeUTF("wrong token")
                flush()
            }
            assertEquals(-1, it.getInputStream().read())
        }
        assertTrue(requests.isEmpty())
        assertFalse(instance().startOrForward(listOf("valid.mp3")))
        assertEquals(listOf("valid.mp3"), requests.poll(3, TimeUnit.SECONDS))
    }

    @Test
    fun forwardsAcrossProcessesAndRecoversAfterTheOwnerIsKilled() {
        val classpath = listOf(DesktopInstance::class.java, DesktopInstanceProcess::class.java, Unit::class.java)
            .map { Path.of(it.protectionDomain.codeSource.location.toURI()).toString() }
            .distinct().joinToString(java.io.File.pathSeparator)
        val process = ProcessBuilder(
            Path.of(System.getProperty("java.home"), "bin", "java").toString(),
            "-cp", classpath, DesktopInstanceProcess::class.java.name,
            directory.resolve("state").toString(), executable.toString(),
        ).redirectError(ProcessBuilder.Redirect.INHERIT).start()
        val reader = Executors.newSingleThreadExecutor()
        try {
            val input = DataInputStream(process.inputStream)
            assertEquals("ready", reader.submit(Callable { input.readUTF() }).get(15, TimeUnit.SECONDS))
            val files = listOf("C:\\media\\\u4e2d\u6587 track.mp3", "C:\\media\\second.mp4")
            assertFalse(instance().startOrForward(files))
            val received = reader.submit(Callable { List(input.readInt()) { input.readUTF() } })
            assertEquals(files, received.get(5, TimeUnit.SECONDS))
            process.destroyForcibly()
            assertTrue(process.waitFor(10, TimeUnit.SECONDS))
            assertTrue(instance().startOrForward(listOf("after-crash.mp3")))
        } finally {
            process.destroyForcibly()
            process.waitFor(10, TimeUnit.SECONDS)
            reader.shutdownNow()
        }
    }

    @Test
    fun commandLinePathsAreResolvedWithoutSplittingOrUnquotingFileNames() {
        val names = arrayOf("folder/../a b.mp3", "\u4e2d\u6587 & #.mp4", "")
        assertEquals(
            names.take(2).map { java.io.File(it).absoluteFile.normalize().path },
            desktopLaunchArguments(names),
        )
    }
}

internal object DesktopInstanceProcess {
    @JvmStatic
    fun main(arguments: Array<String>) {
        val output = DataOutputStream(System.out)
        DesktopInstance(Path.of(arguments[0]), Path.of(arguments[1]), { files ->
            if (files.isNotEmpty()) {
                output.writeInt(files.size)
                files.forEach(output::writeUTF)
                output.flush()
            }
        }).use {
            check(it.startOrForward(emptyList()))
            output.writeUTF("ready")
            output.flush()
            System.`in`.read()
        }
    }
}
