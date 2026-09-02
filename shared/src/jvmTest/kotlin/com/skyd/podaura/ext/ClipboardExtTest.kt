package com.skyd.podaura.ext

import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import kotlinx.coroutines.test.runTest
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ClipboardExtTest {
    @Test
    fun readsTextFromClipboardTransferableThatIsNotStringSelection() = runTest {
        val url = "https://example.com/feed.xml"
        val clipboard = TestClipboard { ClipEntry(TextTransferable(url)) }

        assertEquals(url, clipboard.readText())
        assertEquals(1, clipboard.readCount)
    }

    @Test
    fun retriesWhenWindowsClipboardIsTemporarilyBusy() = runTest {
        val url = "https://example.com/feed.xml"
        val clipboard = TestClipboard {
            if (readCount < 3) throw IllegalStateException("Clipboard is busy")
            ClipEntry(TextTransferable(url))
        }

        assertEquals(url, clipboard.readText())
        assertEquals(3, clipboard.readCount)
    }

    @Test
    fun returnsNullWhenTransferableDoesNotContainText() {
        assertNull(UnsupportedTransferable.getPlainText())
    }
}

private class TestClipboard(private val entry: TestClipboard.() -> ClipEntry?) : Clipboard {
    var readCount = 0
        private set

    override suspend fun getClipEntry(): ClipEntry? {
        readCount++
        return entry()
    }

    override suspend fun setClipEntry(clipEntry: ClipEntry?) = Unit
}

private class TextTransferable(private val text: String) : Transferable {
    override fun getTransferDataFlavors() = arrayOf(DataFlavor.stringFlavor)

    override fun isDataFlavorSupported(flavor: DataFlavor) = flavor == DataFlavor.stringFlavor

    override fun getTransferData(flavor: DataFlavor): Any {
        if (!isDataFlavorSupported(flavor)) throw UnsupportedFlavorException(flavor)
        return text
    }
}

private data object UnsupportedTransferable : Transferable {
    override fun getTransferDataFlavors() = emptyArray<DataFlavor>()

    override fun isDataFlavorSupported(flavor: DataFlavor) = false

    override fun getTransferData(flavor: DataFlavor): Any = throw UnsupportedFlavorException(flavor)
}
