package com.skyd.podaura.buildlogic

import org.w3c.dom.Element
import java.io.File
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/** Registers podaura://notification/<uuid>, preserving the generated app and media extensions. */
fun addWindowsNotificationProtocol(manifest: File) {
    val foundation = "http://schemas.microsoft.com/appx/manifest/foundation/windows10"
    val uap = "http://schemas.microsoft.com/appx/manifest/uap/windows10"
    val uap3 = "$uap/3"
    val document = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    }.newDocumentBuilder().parse(manifest)
    val root = document.documentElement
    root.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:uap3", uap3)
    val ignored = root.getAttribute("IgnorableNamespaces").split(Regex("\\s+"))
        .filter { it.isNotBlank() }.toMutableSet()
    ignored += "uap3"
    root.setAttribute("IgnorableNamespaces", ignored.joinToString(" "))
    val application = document.getElementsByTagNameNS(foundation, "Application").item(0) as Element
    val extensions = (application.getElementsByTagNameNS(foundation, "Extensions").item(0) as? Element)
        ?: document.createElementNS(foundation, "Extensions").also(application::appendChild)
    val existing = extensions.childNodes.let { children ->
        (0 until children.length).mapNotNull { children.item(it) as? Element }.filter {
            it.localName == "Extension" && it.namespaceURI in setOf(uap, uap3) &&
                it.getAttribute("Category") == "windows.protocol" &&
                it.getElementsByTagNameNS("*", "Protocol").let { protocols ->
                    (0 until protocols.length).any { index ->
                        val protocol = protocols.item(index) as Element
                        protocol.namespaceURI in setOf(uap, uap3) &&
                            protocol.getAttribute("Name").equals("podaura", ignoreCase = true)
                    }
                }
        }
    }

    // Full-trust protocol activation uses uap3:Protocol's unqualified Parameters attribute:
    // https://learn.microsoft.com/windows/apps/desktop/modernize/desktop-to-uwp-extensions#start-your-application-by-using-a-protocol
    // Inherit the existing Application's executable and full-trust entry point.
    val extension = document.createElementNS(uap3, "uap3:Extension").apply {
        setAttribute("Category", "windows.protocol")
        appendChild(document.createElementNS(uap3, "uap3:Protocol").apply {
            setAttribute("Name", "podaura")
            setAttribute("Parameters", "\"%1\"")
        })
    }
    if (existing.isEmpty()) {
        extensions.appendChild(extension)
    } else {
        extensions.replaceChild(extension, existing.first())
        existing.drop(1).forEach(extensions::removeChild)
    }
    TransformerFactory.newInstance().newTransformer().apply {
        setOutputProperty(OutputKeys.ENCODING, "UTF-8")
    }.transform(DOMSource(document), StreamResult(manifest))
}

/** A plist dictionary fragment to append alongside CFBundleDocumentTypes. */
fun macOSNotificationUrlTypes(): String = """
    <key>CFBundleURLTypes</key>
    <array>
        <dict>
            <key>CFBundleURLName</key>
            <string>com.skyd.podaura.notification</string>
            <key>CFBundleTypeRole</key>
            <string>Viewer</string>
            <key>CFBundleURLSchemes</key>
            <array><string>podaura</string></array>
        </dict>
    </array>
""".trimIndent()
