package com.skyd.podaura.buildlogic

import com.skyd.podaura.media.MediaTypes
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import java.nio.file.Files
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopNotificationPackagingTest {
    private val foundation = "http://schemas.microsoft.com/appx/manifest/foundation/windows10"
    private val uap = "http://schemas.microsoft.com/appx/manifest/uap/windows10"
    private val uap3 = "$uap/3"

    @Test
    fun protocolPassesTheQuotedUriToTheExistingFullTrustExecutableIdempotently() = withManifest {
        addWindowsNotificationProtocol(it)
        val first = it.readText()
        repeat(2) { _ -> addWindowsNotificationProtocol(it) }
        assertEquals(first, it.readText())
        val document = parse(it)
        val protocol = document.elements(uap3, "Protocol").single()
        assertEquals("podaura", protocol.getAttribute("Name"))
        assertEquals("\"%1\"", protocol.getAttribute("Parameters"))
        assertEquals(uap3, protocol.parentNode.namespaceURI)
        assertEquals("windows.protocol", (protocol.parentNode as Element).getAttribute("Category"))
        val application = document.elements(foundation, "Application").first()
        assertEquals("bin\\PodAura.exe", application.getAttribute("Executable"))
        assertEquals("Windows.FullTrustApplication", application.getAttribute("EntryPoint"))
        assertFalse((protocol.parentNode as Element).hasAttribute("Executable"))
        assertFalse((protocol.parentNode as Element).hasAttribute("EntryPoint"))
        assertEquals(application, protocol.parentNode.parentNode.parentNode)
        assertEquals(uap3, document.documentElement.lookupNamespaceURI("uap3"))
        assertEquals(listOf("uap", "rescap", "uap3"), ignoredNamespaces(document))
        assertEquals("SkyD666.PodAura", document.elements(foundation, "Identity").single().getAttribute("Name"))
        assertEquals("resources\\Logo.png", document.elements(uap, "VisualElements").single().getAttribute("Square150x150Logo"))
        assertEquals("runFullTrust", document.elements("*", "Capability").single().getAttribute("Name"))
    }

    @Test
    fun protocolUpgradesAndDeduplicatesOnlyPodAuraRegistrations() = withManifest(
        """
            <Extensions>
                <uap:Extension Category="windows.protocol">
                    <uap:Protocol Name="podaura" />
                </uap:Extension>
                <uap3:Extension Category="windows.protocol" Executable="Old.exe">
                    <uap3:Protocol Name="PodAura" Parameters="old %1" />
                </uap3:Extension>
                <uap:Extension Category="windows.protocol">
                    <uap:Protocol Name="unrelated" />
                </uap:Extension>
            </Extensions>
        """.trimIndent(),
    ) {
        repeat(2) { _ -> addWindowsNotificationProtocol(it) }
        val document = parse(it)
        assertEquals(listOf("podaura", "unrelated"), document.elements("*", "Protocol").map { protocol ->
            protocol.getAttribute("Name")
        })
        assertEquals("\"%1\"", document.elements(uap3, "Protocol").single().getAttribute("Parameters"))
        assertEquals("unrelated", document.elements(uap, "Protocol").single().getAttribute("Name"))
    }

    @Test
    fun notificationAndMediaPackagingComposeInEitherOrder() {
        listOf(false, true).forEach { mediaFirst ->
            withManifest { manifest ->
                repeat(2) {
                    if (mediaFirst) addWindowsMediaFileAssociations(manifest)
                    addWindowsNotificationProtocol(manifest)
                    if (!mediaFirst) addWindowsMediaFileAssociations(manifest)
                }
                val document = parse(manifest)
                assertEquals(1, document.elements(uap3, "Protocol").size)
                val media = document.elements(uap3, "FileTypeAssociation").single()
                assertEquals("mediafiles", media.getAttribute("Name"))
                assertEquals("\"%1\"", media.getAttribute("Parameters"))
                assertEquals("Player", media.getAttribute("MultiSelectModel"))
                assertEquals(MediaTypes.playableExtensions.map { ".$it" },
                    document.elements(uap, "FileType").map { it.textContent })
                assertEquals(1, ignoredNamespaces(document).count { it == "uap3" })
            }
        }
    }

    @Test
    fun addsMissingNamespacesAndExtensionsWithoutRegisteringOtherApplications() = withManifest {
        it.writeText("""
            <Package xmlns="$foundation">
                <Applications>
                    <Application Id="PodAura" Executable="PodAura.exe" EntryPoint="Windows.FullTrustApplication" />
                    <Application Id="Helper" Executable="Helper.exe" EntryPoint="Windows.FullTrustApplication" />
                </Applications>
            </Package>
        """.trimIndent())
        repeat(2) { _ -> addWindowsNotificationProtocol(it) }
        val document = parse(it)
        assertEquals(listOf("uap3"), ignoredNamespaces(document))
        assertEquals(1, document.elements(foundation, "Extensions").size)
        assertEquals("PodAura", (document.elements(uap3, "Protocol").single()
            .parentNode.parentNode.parentNode as Element).getAttribute("Id"))
    }

    @Test
    fun macOsFragmentRegistersOnlyPodAuraAndPreservesDocumentTypes() = withManifest {
        it.writeText("<plist><dict>${macOSMediaDocumentTypes()}${macOSNotificationUrlTypes()}</dict></plist>")
        val document = parse(it)
        val keys = document.elements("*", "key")
        assertEquals(1, keys.count { key -> key.textContent == "CFBundleDocumentTypes" })
        assertEquals(1, keys.count { key -> key.textContent == "CFBundleURLTypes" })
        val schemes = keys.single { key -> key.textContent == "CFBundleURLSchemes" }
        val array = generateSequence(schemes.nextSibling) { node -> node.nextSibling }
            .filterIsInstance<Element>().first()
        assertEquals("array", array.tagName)
        assertEquals(1, array.getElementsByTagName("string").length)
        assertEquals("podaura", array.getElementsByTagName("string").item(0).textContent)
        assertTrue(document.elements("*", "string").any { value -> value.textContent == "Viewer" })
    }

    private fun withManifest(extensions: String = "", block: (File) -> Unit) {
        val manifest = Files.createTempFile("podaura-notifications", ".xml").toFile()
        try {
            manifest.writeText("""
                <Package xmlns="$foundation" xmlns:uap="$uap" xmlns:uap3="$uap3"
                    xmlns:rescap="$foundation/restrictedcapabilities" IgnorableNamespaces="uap rescap">
                    <Identity Name="SkyD666.PodAura" />
                    <Applications>
                        <Application Id="PodAura" Executable="bin\PodAura.exe" EntryPoint="Windows.FullTrustApplication">
                            <uap:VisualElements Square150x150Logo="resources\Logo.png" />
                            $extensions
                        </Application>
                    </Applications>
                    <Capabilities><rescap:Capability Name="runFullTrust" /></Capabilities>
                </Package>
            """.trimIndent())
            block(manifest)
        } finally {
            manifest.delete()
        }
    }

    private fun parse(file: File): Document = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
    }.newDocumentBuilder().parse(file)

    private fun Document.elements(namespace: String, name: String): List<Element> =
        getElementsByTagNameNS(namespace, name).let { nodes ->
            (0 until nodes.length).map { nodes.item(it) as Element }
        }

    private fun ignoredNamespaces(document: Document): List<String> =
        document.documentElement.getAttribute("IgnorableNamespaces").split(Regex("\\s+"))
}
