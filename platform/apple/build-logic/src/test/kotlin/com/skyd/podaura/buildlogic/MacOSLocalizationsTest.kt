package com.skyd.podaura.buildlogic

import org.w3c.dom.NodeList
import java.nio.file.Files
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MacOSLocalizationsTest {
    @Test
    fun developerLauncherCopiesJavaAndReusesTheSelectedRuntime() {
        if (!System.getProperty("os.name").startsWith("Mac")) return
        val directory = Files.createTempDirectory("podaura-launcher").toFile()
        try {
            val javaHome = directory.resolve("jdk").apply { mkdirs() }
            javaHome.resolve("lib").mkdirs()
            javaHome.resolve("bin/java").apply {
                parentFile.mkdirs()
                writeText("test Java launcher")
                setExecutable(true)
            }
            val app = directory.resolve("PodAura.app")
            prepareMacOSJvmLauncher(javaHome, app)
            val executable = app.resolve("Contents/MacOS/java")
            assertFalse(Files.isSymbolicLink(executable.toPath()))
            assertTrue(executable.canExecute())
            assertEquals("test Java launcher", executable.readText())
            assertEquals(
                javaHome.resolve("lib").toPath(),
                Files.readSymbolicLink(app.resolve("Contents/lib").toPath())
            )
            val document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(app.resolve("Contents/Info.plist"))
            assertEquals(
                "com.skyd.podaura", XPathFactory.newInstance().newXPath().evaluate(
                    "/plist/dict/key[.='CFBundleIdentifier']/following-sibling::string[1]",
                    document,
                )
            )
        } finally {
            Files.deleteIfExists(directory.resolve("PodAura.app/Contents/lib").toPath())
            directory.deleteRecursively()
        }
    }

    @Test
    fun bothPackagersDeclareLanguagesWithoutReplacingExistingMetadata() {
        val plist = Files.createTempFile("podaura-localizations", ".plist").toFile()
        try {
            plist.writeText(
                """
                <plist><dict>
                    <key>CFBundleIdentifier</key><string>com.skyd.podaura</string>
                    <key>CFBundleDocumentTypes</key><array><dict>
                        <key>CFBundleTypeRole</key><string>Viewer</string>
                    </dict></array>
                </dict></plist>
            """.trimIndent()
            )
            addMacOSAppLocalizations(plist)
            val jvmPlist = "<plist><dict>${macOSAppLocalizations()}</dict></plist>"
            for (content in listOf(jvmPlist, plist.readText())) {
                val document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(content.byteInputStream())
                val xpath = XPathFactory.newInstance().newXPath()
                val localizations = xpath.evaluate(
                    "/plist/dict/key[.='CFBundleLocalizations']/following-sibling::*[1]/string",
                    document, XPathConstants.NODESET,
                ) as NodeList
                assertEquals(
                    listOf("en", "zh-Hans", "zh-Hant", "ja", "tr", "eo"),
                    (0 until localizations.length).map { localizations.item(it).textContent },
                )
            }
            val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(plist)
            val xpath = XPathFactory.newInstance().newXPath()
            assertEquals(
                "com.skyd.podaura", xpath.evaluate(
                    "/plist/dict/key[.='CFBundleIdentifier']/following-sibling::string[1]",
                    document,
                )
            )
            assertEquals(
                "Viewer", xpath.evaluate(
                    "/plist/dict/array/dict/key[.='CFBundleTypeRole']/following-sibling::string[1]",
                    document,
                )
            )
        } finally {
            plist.delete()
        }
    }
}
