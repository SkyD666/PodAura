package com.skyd.podaura.model.repository.media

import com.skyd.podaura.model.bean.MediaBean
import com.skyd.podaura.model.bean.MediaGroupBean
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import kotlinx.serialization.json.Json
import java.lang.reflect.Proxy
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MediaRepositoryConcurrencyTest {
    @Test
    fun multipleInstancesPreserveUpdatesAndReadersAlwaysSeeCompleteJson() = runBlocking<Unit> {
        val root = Files.createTempDirectory("podaura-library-concurrency")
        val index = root.resolve(MediaRepository.MEDIA_LIB_JSON_NAME)
        Files.writeString(index, """{"allGroups":[],"files":[]}""")
        val repos = List(8) { MediaRepository(Json, unusedDao(), unusedDao()) }
        try {
            withTimeout(30_000) {
                coroutineScope {
                    val reader = launch(Dispatchers.IO) {
                        while (isActive) {
                            Json.decodeFromString<MediaRepository.MediaLibJson>(Files.readString(index))
                            yield()
                        }
                    }
                    try {
                        (0 until 100).map { i ->
                            async(Dispatchers.IO) {
                                val repo = repos[i % repos.size]
                                repo.createGroup(root.toString(), MediaGroupBean("group-$i")).first()
                                val groups = repo.requestGroups(root.toString()).first()
                                assertEquals(groups.size, groups.map { it.name }.distinct().size)
                            }
                        }.awaitAll()
                    } finally {
                        reader.cancel()
                        reader.join()
                    }
                }
                val saved = Json.decodeFromString<MediaRepository.MediaLibJson>(Files.readString(index))
                assertEquals((0 until 100).map { "group-$it" }.toSet(), saved.allGroups.toSet())
                assertTrue(saved.files.isEmpty())

                // These operations used to invoke nested mutation flows. They must not deadlock.
                val file = Files.writeString(root.resolve("episode.mp3"), "fixture")
                val repo = repos.first()
                repo.addNewFile(PlatformFile(file.toFile()), PlatformFile(root.toFile()), "new", null, "Episode").first()
                val media = MediaBean(filePath = file.toString(), parentPath = root.toString(),
                    fileCount = 0, articleWithEnclosure = null, feedBean = null)
                repo.changeMediaGroup(root.toString(), media, MediaGroupBean("moved")).first()
                repo.moveFilesToGroup(root.toString(), MediaGroupBean("moved"), MediaGroupBean("final")).first()
                repo.renameGroup(root.toString(), MediaGroupBean("final"), "renamed").first()
                val final = Json.decodeFromString<MediaRepository.MediaLibJson>(Files.readString(index))
                assertEquals("renamed", final.files.single().groupName)
                assertEquals("Episode", final.files.single().displayName)
                assertEquals(setOf("episode.mp3", MediaRepository.MEDIA_LIB_JSON_NAME),
                    root.toFile().list()!!.toSet())
            }
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    private inline fun <reified T> unusedDao(): T = Proxy.newProxyInstance(
        T::class.java.classLoader, arrayOf(T::class.java),
    ) { _, method, _ -> error("Unexpected database access: ${method.name}") } as T
}
