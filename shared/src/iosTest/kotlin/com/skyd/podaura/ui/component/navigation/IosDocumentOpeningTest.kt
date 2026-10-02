package com.skyd.podaura.ui.component.navigation

import com.skyd.podaura.ui.screen.settings.data.importexport.importopml.ImportOpmlDeepLinkRoute
import io.github.vinceglb.filekit.PlatformFile
import platform.Foundation.NSURL
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class IosDocumentOpeningTest {
    @Test
    fun queuesDocumentsBeforeNavigationAndPreservesTheirUrls() {
        while (iosDocumentRequests.tryReceive().isSuccess) Unit
        val urls = listOf("订阅 列表.OPML", "feeds.xml", "movie.mp4").map {
            NSURL.fileURLWithPath("/tmp/$it")
        }
        urls.forEach(::openIosDocument)
        urls.forEachIndexed { index, url ->
            val file = assertNotNull(iosDocumentRequests.tryReceive().getOrNull())
            assertEquals(url, file.nsUrl)
            if (index < 2) {
                assertSame(file, assertNotNull(file.opmlImportRoute()).opmlFile)
            } else {
                assertNull(file.opmlImportRoute())
            }
        }
        openIosDocument(NSURL(string = "https://example.com/feeds.opml"))
        assertTrue(iosDocumentRequests.tryReceive().isFailure)
        assertNull(PlatformFile(NSURL.fileURLWithPath("/tmp/feeds.opml.mp4")).opmlImportRoute())
    }

    @Test
    fun xmlAndOpmlDeepLinksKeepTheOriginalFileUri() {
        val uri = "content://example.provider/feeds%20list.opml"
        for (mimeType in listOf("text/xml", "application/xml", "text/x-opml", "application/x-opml+xml")) {
            val route = ExternalUrlHandler.UrlData(url = uri, mimeType = mimeType).toNavKey()
            assertEquals(uri, assertIs<ImportOpmlDeepLinkRoute>(route).opmlUrl)
        }
    }
}
