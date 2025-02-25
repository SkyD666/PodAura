package com.skyd.anivu

import android.Manifest
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.skyd.anivu.model.bean.MediaBean
import com.skyd.anivu.model.bean.MediaGroupBean
import com.skyd.anivu.model.bean.MediaGroupBean.Companion.isDefaultGroup
import com.skyd.anivu.model.db.AppDatabase
import com.skyd.anivu.model.repository.FilePickerRepository
import com.skyd.anivu.model.repository.MediaRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File
import java.io.IOException


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
class MediaModule {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val permissions = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
    )

    @get:Rule
    val runtimePermissionRule = GrantPermissionRule.grant(*permissions)

    private lateinit var context: Context

    private lateinit var db: AppDatabase
    private lateinit var mediaRepository: MediaRepository
    private lateinit var filePickerRepository: FilePickerRepository

    private val path = "/storage/emulated/0/TestMediaLib"

    val file1 = File(path, "File1")
    val file2 = File(path, "File2")
    val file3 = File(path, "File3")
    val file4 = File(path, "File4")
    val file5 = File(path, "File5")

    private fun initLib() {
        File(path).apply {
            deleteRecursively()
            mkdirs()
        }
        file1.createNewFile()
        file2.createNewFile()
        file3.createNewFile()
        file4.createNewFile()
        file5.createNewFile()
        File(path, MediaRepository.MEDIA_LIB_JSON_NAME).delete()
    }

    /**
     * Pass
     * mediaRepository.requestGroups
     */
    @Test
    fun test1() = runTest {
        initLib()
        assertTrue(mediaRepository.requestGroups(path).first()
            .run { size == 1 && first().isDefaultGroup() })
    }

    /**
     * Pass
     * mediaRepository.createGroup
     */
    @Test
    fun test2() = runTest {
        initLib()
        mediaRepository.createGroup(path, MediaGroupBean("TestGroup")).first()
        assertTrue(mediaRepository.requestGroups(path).first().run {
            size == 2 && get(1).name == "TestGroup"
        })
    }

    /**
     * Pass
     * mediaRepository.createGroup
     * mediaRepository.renameGroup
     */
    @Test
    fun test3() = runTest {
        initLib()
        val group = MediaGroupBean("TestGroup")
        mediaRepository.createGroup(path, group).first()
        assertTrue(mediaRepository.requestGroups(path).first().run {
            size == 2 && get(1).name == "TestGroup"
        })
        mediaRepository.renameGroup(path, group, "NewTestGroup").first()
        assertTrue(mediaRepository.requestGroups(path).first().run {
            size == 2 && get(1).name == "NewTestGroup"
        })
    }

    /**
     * Pass
     * mediaRepository.requestFiles
     */
    @Test
    fun test4() = runTest {
        initLib()
        assertTrue(
            mediaRepository.requestFiles(path, MediaGroupBean.DefaultMediaGroup).first().size == 5
        )
    }

    /**
     * Pass
     * mediaRepository.createGroup
     * mediaRepository.requestGroups
     * mediaRepository.changeMediaGroup
     * mediaRepository.requestFiles
     */
    @Test
    fun test5() = runTest {
        initLib()
        val group = MediaGroupBean("TestGroup")
        mediaRepository.createGroup(path, group).first()
        assertTrue(mediaRepository.requestGroups(path).first().run {
            size == 2 && get(1).name == "TestGroup"
        })
        mediaRepository.changeMediaGroup(
            path,
            MediaBean(file = file1, articleWithEnclosure = null, feedBean = null),
            group
        ).first()
        assertTrue(mediaRepository.requestFiles(path, group).first().first().file == file1)
    }

    /**
     * Pass
     * mediaRepository.createGroup
     * mediaRepository.requestGroups
     * mediaRepository.moveFilesToGroup
     * mediaRepository.requestFiles
     */
    @Test
    fun test6() = runTest {
        initLib()
        val group = MediaGroupBean("TestGroup")
        mediaRepository.createGroup(path, group).first()
        assertTrue(mediaRepository.requestGroups(path).first().run {
            size == 2 && get(1).name == "TestGroup"
        })
        mediaRepository.moveFilesToGroup(path, MediaGroupBean.DefaultMediaGroup, group).first()
        assertTrue(mediaRepository.requestFiles(path, group).first().size == 5)
    }

    /**
     * Pass
     * mediaRepository.createGroup
     * mediaRepository.requestGroups
     * mediaRepository.moveFilesToGroup
     * mediaRepository.deleteFile
     * mediaRepository.requestFiles
     */
    @Test
    fun test7() = runTest {
        initLib()
        val group = MediaGroupBean("TestGroup")
        mediaRepository.createGroup(path, group).first()
        assertTrue(mediaRepository.requestGroups(path).first().run {
            size == 2 && get(1).name == "TestGroup"
        })
        mediaRepository.moveFilesToGroup(path, MediaGroupBean.DefaultMediaGroup, group).first()
        mediaRepository.deleteFile(file1).first()
        assertNull(
            mediaRepository.requestFiles(path, group).first().firstOrNull { it.file == file1 })
    }

    /**
     * Pass
     * mediaRepository.createGroup
     * mediaRepository.requestGroups
     * mediaRepository.moveFilesToGroup
     * mediaRepository.renameFile
     * mediaRepository.requestFiles
     */
    @Test
    fun test8() = runTest {
        initLib()
        val group = MediaGroupBean("TestGroup")
        mediaRepository.createGroup(path, group).first()
        assertTrue(mediaRepository.requestGroups(path).first().run {
            size == 2 && get(1).name == "TestGroup"
        })
        mediaRepository.moveFilesToGroup(path, MediaGroupBean.DefaultMediaGroup, group).first()
        mediaRepository.renameFile(file1, "NewFile1").first()
        assertNotNull(mediaRepository.requestFiles(path, group).first()
            .firstOrNull { it.file.name == "NewFile1" })
    }

    /**
     * Pass
     * mediaRepository.createGroup
     * mediaRepository.requestGroups
     * mediaRepository.moveFilesToGroup
     * mediaRepository.renameGroup
     * mediaRepository.requestFiles
     */
    @Test
    fun test9() = runTest {
        initLib()
        val group = MediaGroupBean("TestGroup")
        mediaRepository.createGroup(path, group).first()
        assertTrue(mediaRepository.requestGroups(path).first().run {
            size == 2 && get(1).name == "TestGroup"
        })
        mediaRepository.moveFilesToGroup(path, MediaGroupBean.DefaultMediaGroup, group).first()
        val newGroup = mediaRepository.renameGroup(path, group, "NewGroup").first()
        assertNotNull(mediaRepository.requestFiles(path, newGroup).first().size == 5)
    }

    /**
     * Pass
     * mediaRepository.createGroup
     * mediaRepository.requestGroups
     * mediaRepository.moveFilesToGroup
     * mediaRepository.setFileDisplayName
     * mediaRepository.requestFiles
     */
    @Test
    fun test10() = runTest {
        initLib()
        val group = MediaGroupBean("TestGroup")
        mediaRepository.createGroup(path, group).first()
        assertTrue(mediaRepository.requestGroups(path).first().run {
            size == 2 && get(1).name == "TestGroup"
        })
        mediaRepository.moveFilesToGroup(path, MediaGroupBean.DefaultMediaGroup, group).first()
        val displayName = ":/*-\\`~"
        mediaRepository.setFileDisplayName(
            MediaBean(file = file2, articleWithEnclosure = null, feedBean = null),
            displayName
        ).first()
        assertNotNull(mediaRepository.requestFiles(path, group).first()
            .firstOrNull { it.displayName == displayName })
    }

    /**
     * Pass
     * mediaRepository.createGroup
     * mediaRepository.requestGroups
     * mediaRepository.moveFilesToGroup
     * mediaRepository.deleteGroup
     * mediaRepository.requestFiles
     */
    @Test
    fun test11() = runTest {
        initLib()
        val group = MediaGroupBean("TestGroup")
        mediaRepository.createGroup(path, group).first()
        assertTrue(mediaRepository.requestGroups(path).first().run {
            size == 2 && get(1).name == "TestGroup"
        })
        mediaRepository.moveFilesToGroup(path, MediaGroupBean.DefaultMediaGroup, group).first()
        mediaRepository.deleteGroup(path, group).first()
        assertNotNull(
            mediaRepository.requestFiles(path, MediaGroupBean.DefaultMediaGroup).first().size == 5
        )
    }

    /**
     * Pass
     * mediaRepository.createGroup
     * mediaRepository.requestGroups
     * mediaRepository.moveFilesToGroup
     * mediaRepository.requestFiles
     */
    @Test
    fun test12() = runTest {
        initLib()
        val group = MediaGroupBean("TestGroup")
        mediaRepository.createGroup(path, group).first()
        assertTrue(mediaRepository.requestGroups(path).first().run {
            size == 2 && get(1).name == "TestGroup"
        })
        val notExistsGroup = MediaGroupBean("NotExistsGroup")
        mediaRepository.moveFilesToGroup(path, group, notExistsGroup).first()
        assertTrue(mediaRepository.requestGroups(path).first().contains(notExistsGroup))
        assertNotNull(mediaRepository.requestFiles(path, notExistsGroup).first().size == 5)
    }

    /**
     * Pass
     * mediaRepository.createGroup
     * mediaRepository.requestGroups
     * mediaRepository.moveFilesToGroup
     * mediaRepository.renameFile
     * mediaRepository.requestFiles
     */
    @Test
    fun test13() = runTest {
        initLib()
        val group = MediaGroupBean("TestGroup")
        mediaRepository.createGroup(path, group).first()
        assertTrue(mediaRepository.requestGroups(path).first().run {
            size == 2 && get(1).name == "TestGroup"
        })
        mediaRepository.moveFilesToGroup(path, MediaGroupBean.DefaultMediaGroup, group).first()
        assertNotNull(mediaRepository.renameFile(file1, "/*\\*?1Test").first())
        assertNotNull(mediaRepository.requestFiles(path, group).first()
            .firstOrNull { it.file.name == "1Test" })
    }

    /**
     * Pass
     * filePickerRepository.requestFiles
     */
    @Test
    fun test14() = runTest {
        initLib()
        assertEquals(filePickerRepository.requestFiles(path).first().size, 5)
    }

    /**
     * Pass
     * filePickerRepository.requestFiles
     */
    @Test
    fun test15() = runTest {
        initLib()
        file1.renameTo(File(file1.parentFile, file1.name + ".txt"))
        assertEquals(filePickerRepository.requestFiles(path, extensionName = "txt").first().size, 1)
    }

    /**
     * Pass
     * mediaRepository.requestGroups
     * filePickerRepository.requestFiles
     */
    @Test
    fun test16() = runTest {
        initLib()
        file1.renameTo(File(file1.parentFile, file1.name + ".txt"))
        mediaRepository.requestGroups(path).first()
        assertTrue(File(path, MediaRepository.MEDIA_LIB_JSON_NAME)
            .run { exists() || createNewFile() })
        assertEquals(
            1, filePickerRepository.requestFiles(path, extensionName = "json").first().size
        )
    }

    /**
     * Pass
     * mediaRepository.moveFilesToGroup
     * mediaRepository.requestFiles
     */
    @Test
    fun test17() = runTest {
        initLib()
        mediaRepository.moveFilesToGroup(
            path, MediaGroupBean.DefaultMediaGroup, MediaGroupBean.DefaultMediaGroup
        ).first()
        assertEquals(
            5,
            mediaRepository.requestFiles(path, MediaGroupBean.DefaultMediaGroup).first().size
        )
    }

    /**
     * Pass
     * mediaRepository.createGroup
     * mediaRepository.requestGroups
     */
    @Test
    fun test18() = runTest {
        initLib()
        val group = MediaGroupBean("TestGroup")
        mediaRepository.createGroup(path, group).first()
        mediaRepository.createGroup(path, group).first()
        assertEquals(2, mediaRepository.requestGroups(path).first().size)
    }

    /**
     * Pass
     * mediaRepository.createGroup
     * mediaRepository.deleteGroup
     * mediaRepository.requestGroups
     */
    @Test
    fun test19() = runTest {
        initLib()
        val group = MediaGroupBean("TestGroup")
        mediaRepository.createGroup(path, group).first()
        mediaRepository.deleteGroup(path, MediaGroupBean.DefaultMediaGroup).first()
        assertEquals(2, mediaRepository.requestGroups(path).first().size)
    }

    /**
     * Pass
     * mediaRepository.createGroup
     * mediaRepository.requestGroups
     */
    @Test
    fun test20() = runTest {
        initLib()
        val group = MediaGroupBean(MediaGroupBean.DefaultMediaGroup.name)
        mediaRepository.createGroup(path, group).first()
        assertEquals(
            2, mediaRepository.requestGroups(path).first()
                .filter { it.name == MediaGroupBean.DefaultMediaGroup.name }.size
        )
    }

    /**
     * Pass
     * mediaRepository.renameGroup
     * mediaRepository.requestGroups
     */
    @Test
    fun test21() = runTest {
        initLib()
        mediaRepository.renameGroup(path, MediaGroupBean.DefaultMediaGroup, "NewGroup").first()
        assertTrue(mediaRepository.requestGroups(path).first()
            .run { size == 1 && first().name == MediaGroupBean.DefaultMediaGroup.name }
        )
    }

    @Before
    fun init() {
        context = ApplicationProvider.getApplicationContext()

        db = AppDatabase.getInstance(context)
        db.clearAllTables()

        mediaRepository = MediaRepository(json, db.feedDao(), db.articleDao())
        filePickerRepository = FilePickerRepository()
    }

    @After
    @Throws(IOException::class)
    fun destroy() {
//        db.close()
    }
}