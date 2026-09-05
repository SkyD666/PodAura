package com.skyd.podaura.buildlogic

import com.skyd.podaura.media.MediaTypes
import org.w3c.dom.Element
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/** macOS serialization stays in build logic; media definitions are shared with all targets. */
fun macOSMediaDocumentTypes(): String {
    val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
    val fragment = document.createDocumentFragment()
    fun element(name: String, text: String? = null): Element = document.createElement(name).apply {
        text?.let { textContent = it }
    }
    fragment.appendChild(element("key", "CFBundleDocumentTypes"))
    val types = element("array")
    fragment.appendChild(types)
    val type = element("dict")
    types.appendChild(type)
    fun field(key: String, value: String) {
        type.appendChild(element("key", key))
        type.appendChild(element("string", value))
    }
    field("CFBundleTypeName", "Audio and video")
    field("CFBundleTypeRole", "Viewer")
    field("LSHandlerRank", "Alternate")
    type.appendChild(element("key", "CFBundleTypeExtensions"))
    type.appendChild(element("array").apply {
        MediaTypes.playableExtensions.forEach { appendChild(element("string", it)) }
    })
    return StringWriter().also { output ->
        TransformerFactory.newInstance().newTransformer().apply {
            setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
        }.transform(DOMSource(fragment), StreamResult(output))
    }.toString()
}
