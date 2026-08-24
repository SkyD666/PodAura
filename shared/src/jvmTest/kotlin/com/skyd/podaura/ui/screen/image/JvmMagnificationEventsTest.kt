package com.skyd.podaura.ui.screen.image

import com.skyd.fundation.util.Platform
import com.skyd.fundation.util.platform
import javax.swing.JPanel
import kotlin.test.Test
import kotlin.test.assertEquals

internal object MagnificationWithoutModuleExportProcess {
    @JvmStatic
    fun main(args: Array<String>) {
        installJvmMagnificationListener(JPanel()) { }.close()
    }
}

class JvmMagnificationEventsTest {

    @Test
    fun macMagnificationListenerRegistersAndCloses() {
        if (platform != Platform.macOS_Jvm) return

        val registration = installJvmMagnificationListener(JPanel()) { }
        registration.close()
    }

    @Test
    fun missingModuleExportDoesNotCrash() {
        if (platform != Platform.macOS_Jvm) return

        val javaExecutable = "${System.getProperty("java.home")}/bin/java"
        val process = ProcessBuilder(
            javaExecutable,
            "-cp",
            System.getProperty("java.class.path"),
            MagnificationWithoutModuleExportProcess::class.java.name,
        ).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().use { it.readText() }

        assertEquals(0, process.waitFor(), output)
    }
}
