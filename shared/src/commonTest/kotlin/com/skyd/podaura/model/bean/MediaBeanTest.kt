package com.skyd.podaura.model.bean

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MediaBeanTest {
    @Test
    fun nestedMkvFilesAreIncludedInThePlaybackList() {
        val lowerCase = media("电影.mkv")
        val upperCase = media("电影.MKV")
        val files = listOf(lowerCase, media("notes.pdf"), upperCase, media("cover.png"))

        assertEquals(listOf(lowerCase, upperCase), files.filter { it.isMedia })
    }

    @Test
    fun mediaLibraryContinuesToRecognizePlaylists() {
        assertTrue(media("stream.m3u8").isMedia)
    }

    private fun media(name: String) = MediaBean(
        filePath = "/media/library/folder/$name",
        parentPath = "/media/library/folder",
        fileCount = 0,
        articleWithEnclosure = null,
        feedBean = null,
    )
}
