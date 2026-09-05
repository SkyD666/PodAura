package com.skyd.podaura.ui.activity.player

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import com.skyd.podaura.ui.player.PlayerOpenRequest
import com.skyd.podaura.ui.player.jumper.PLAY_DATA_MODE_KEY
import com.skyd.podaura.ui.player.jumper.PlayDataMode
import io.github.vinceglb.filekit.AndroidFile
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class PlayerOpenIntentTest {
    @Test
    fun opensDataOnly() {
        val uri = uri()
        assertFiles(listOf(uri), intent(data = uri))
    }

    @Test
    fun opensClipDataOnlyInItemOrder() {
        val first = uri()
        val second = uri()
        assertFiles(listOf(first, second), intent(clipUris = listOf(first, second)))
    }

    @Test
    fun combinesDataAndClipDataWithoutDuplicatesInFirstOccurrenceOrder() {
        val first = uri()
        val second = uri()
        val third = uri()
        assertFiles(
            listOf(second, first, third),
            intent(data = second, clipUris = listOf(first, second, third, first)),
        )
    }

    @Test
    fun ignoresClipItemsWithoutUris() {
        val uri = uri()
        assertFiles(listOf(uri), intent(clipUris = listOf(null, uri, null)))
    }

    @Test
    fun resumesOnlyWhenThereIsNoInternalRequestOrUri() {
        assertEquals(PlayerOpenRequest.Resume, (null as Intent?).toPlayerOpenRequest("request"))
        assertEquals(PlayerOpenRequest.Resume, intent().toPlayerOpenRequest("request"))
        assertEquals(PlayerOpenRequest.Resume, intent(clipUris = listOf(null)).toPlayerOpenRequest("request"))
    }

    @Test
    fun internalPlaybackParametersTakePriorityOverExternalUris() {
        val mode = PlayDataMode.Playlist("playlist", "episode.mp3")
        val intent = intent(data = uri(), clipUris = listOf(uri()))
        whenever(intent.getStringExtra(PLAY_DATA_MODE_KEY)).thenReturn(mode.encodeToString())

        assertEquals(PlayerOpenRequest.Media(mode, "request"), intent.toPlayerOpenRequest("request"))
    }

    private fun assertFiles(expected: List<Uri>, intent: Intent) {
        val request = intent.toPlayerOpenRequest("request") as PlayerOpenRequest.Files
        assertEquals("request", request.requestId)
        assertEquals(expected, request.files.map { (it.androidFile as AndroidFile.UriWrapper).uri })
    }

    private fun uri(): Uri = mock<Uri>().also {
        whenever(it.scheme).thenReturn("content")
    }

    private fun intent(data: Uri? = null, clipUris: List<Uri?> = emptyList()): Intent {
        val intent = mock<Intent>()
        whenever(intent.data).thenReturn(data)
        if (clipUris.isNotEmpty()) {
            val clip = mock<ClipData>()
            whenever(clip.itemCount).thenReturn(clipUris.size)
            clipUris.forEachIndexed { index, uri ->
                val item = mock<ClipData.Item>()
                whenever(item.uri).thenReturn(uri)
                whenever(clip.getItemAt(index)).thenReturn(item)
            }
            whenever(intent.clipData).thenReturn(clip)
        }
        return intent
    }
}
