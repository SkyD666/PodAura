package com.skyd.downloader.download

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.skyd.downloader.Status
import com.skyd.downloader.db.DownloadDatabase
import com.skyd.downloader.db.DownloadEntity
import com.skyd.downloader.db.instance
import com.skyd.downloader.util.FileUtil
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.writeString
import kotlinx.coroutines.test.runTest
import platform.Foundation.NSTemporaryDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class IosDownloadPathsTest {
    @Test
    fun relocatedDownloadsFindTheSameFilesAsTheMediaLibrary() = runTest {
        val directory = PlatformFile(NSTemporaryDirectory() + "ios-download-relocation-${Uuid.random()}")
            .apply { createDirectories() }
        val documents = PlatformFile(directory, "Documents").apply { createDirectories() }
        val feed = PlatformFile(documents, "播客 feed").apply { createDirectories() }
        val media = PlatformFile(feed, "episode 01.mp3").apply { writeString("local media") }
        val database = DownloadDatabase.instance(
            Room.databaseBuilder<DownloadDatabase>(name = PlatformFile(directory, "downloads.db").path)
                .setDriver(BundledSQLiteDriver())
        )
        try {
            val dao = database.downloadDao()
            val oldDocuments = "/private/var/mobile/Containers/Data/Application/" +
                    "00000000-0000-0000-0000-000000000001/Documents"
            val completed = DownloadEntity(
                id = "completed",
                path = "$oldDocuments/${feed.name}",
                fileName = media.name,
                status = Status.Success.name,
                downloadedBytes = 100,
                metadata = "article metadata",
            )
            val missing = completed.copy(id = "missing", fileName = "missing.mp3")
            val paused = completed.copy(id = "paused", status = Status.Paused.name)
            val simulator = completed.copy(
                id = "simulator",
                path = completed.path.replace(
                    "/private/var/mobile",
                    "/Users/test/Library/Developer/CoreSimulator/Devices/" +
                            "00000000-0000-0000-0000-000000000002/data",
                ),
            )
            val external = completed.copy(id = "external", path = "/external/Documents/feed")
            val sibling = completed.copy(id = "sibling", path = "${oldDocuments}Backup/feed")
            listOf(completed, missing, paused, simulator, external, sibling).forEach { dao.insert(it) }

            assertTrue(media.exists()) // The media library scans the current Documents directory.
            assertFalse(FileUtil.finalFileExists(completed.path, completed.fileName))

            relocateIosDownloadPaths(dao, documents.path)

            assertEquals(completed.copy(path = feed.path), dao.find(completed.id))
            assertTrue(FileUtil.finalFileExists(dao.find(completed.id)!!.path, completed.fileName))
            assertFalse(FileUtil.finalFileExists(dao.find(missing.id)!!.path, missing.fileName))
            assertEquals(paused.copy(path = feed.path), dao.find(paused.id))
            assertEquals(simulator.copy(path = feed.path), dao.find(simulator.id))
            assertEquals(external, dao.find(external.id))
            assertEquals(sibling, dao.find(sibling.id))

            relocateIosDownloadPaths(dao, documents.path)
            assertEquals(completed.copy(path = feed.path), dao.find(completed.id))
        } finally {
            database.close()
            directory.delete(recursively = true)
        }
    }
}
