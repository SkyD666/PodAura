package com.skyd.podaura.buildlogic

import com.skyd.podaura.media.MediaTypes
import org.w3c.dom.NodeList
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MediaFileAssociationsTest {
    @Test
    fun macOSAssociationsUseTheSharedCatalogWithoutClaimingDefaultOwnership() {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
            ("<dict>" + macOSMediaDocumentTypes() + "</dict>").byteInputStream(),
        )
        val xpath = XPathFactory.newInstance().newXPath()
        val nodes = xpath.evaluate(
            "/dict/array/dict/key[.='CFBundleTypeExtensions']/following-sibling::array[1]/string",
            document, XPathConstants.NODESET,
        ) as NodeList
        val extensions = (0 until nodes.length).map { nodes.item(it).textContent }
        assertEquals(MediaTypes.playableExtensions.toList(), extensions)
        assertTrue(extensions.none { it in MediaTypes.playlistExtensions })
        assertEquals("Viewer", xpath.evaluate(
            "/dict/array/dict/key[.='CFBundleTypeRole']/following-sibling::string[1]", document,
        ))
        assertEquals("Alternate", xpath.evaluate(
            "/dict/array/dict/key[.='LSHandlerRank']/following-sibling::string[1]", document,
        ))
    }
}
