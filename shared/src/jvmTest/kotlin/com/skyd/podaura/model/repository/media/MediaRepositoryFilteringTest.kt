package com.skyd.podaura.model.repository.media

import com.skyd.fundation.util.Platform
import com.skyd.fundation.util.platform
import com.skyd.podaura.model.bean.MediaGroupBean
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.lang.reflect.Proxy
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MediaRepositoryFilteringTest {
    @Test
    fun macosHidesFinderMetadataFromIndexesSearchAndFolderCounts() = runBlocking<Unit> {
        if (platform != Platform.macOS_Jvm) return@runBlocking
        val root = Files.createTempDirectory("podaura-library-filtering")
        try {
            val folder = Files.createDirectory(root.resolve("episodes"))
            Files.writeString(folder.resolve("episode.mp3"), "media")
            Files.writeString(folder.resolve(".DS_Store"), "Finder metadata")
            Files.writeString(root.resolve(".DS_Store"), "Finder metadata")
            val index = root.resolve(MediaRepository.MEDIA_LIB_JSON_NAME)
            Files.writeString(index, """{"files":[{"fileName":".DS_Store","isFile":true}]}""")
            val repo = MediaRepository(Json, emptyDao(), emptyDao())

            val results = repo.search(root.toString(), query = "", recursive = true).first()
            assertEquals(setOf("episodes", "episode.mp3"), results.map { it.name }.toSet())
            assertEquals(1, results.single { it.name == "episodes" }.fileCount)
            assertTrue(repo.search(root.toString(), query = ".DS_Store", recursive = true).first().isEmpty())

            // Refresh the cached index after Finder recreates its metadata.
            Files.delete(root.resolve(".DS_Store"))
            Files.writeString(root.resolve(".ds_store"), "Finder metadata")
            repo.createGroup(root.toString(), MediaGroupBean("Podcasts")).first()
            val saved = Json.decodeFromString<MediaRepository.MediaLibJson>(Files.readString(index))
            assertEquals(listOf("episodes"), saved.files.map { it.fileName })
            assertTrue(Files.exists(folder.resolve(".DS_Store")))
            assertTrue(Files.exists(root.resolve(".ds_store")))
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    private inline fun <reified T> emptyDao(): T = Proxy.newProxyInstance(
        T::class.java.classLoader, arrayOf(T::class.java),
    ) { _, method, _ ->
        when (method.name) {
            "getArticleWithFeedListByIds", "getFeedsIn" -> emptyList<Any>()
            else -> error("Unexpected database access: ${method.name}")
        }
    } as T
}
