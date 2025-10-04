package com.skyd.podaura

import android.Manifest
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.skyd.downloader.Status
import com.skyd.podaura.model.db.AppDatabase
import com.skyd.podaura.model.db.builder
import com.skyd.podaura.model.db.instance
import com.skyd.podaura.model.preference.createDataStore
import com.skyd.podaura.model.repository.download.DownloadManager
import com.skyd.podaura.model.repository.download.DownloadRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.IOException


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
class DownloadModule {
    private lateinit var workManager: WorkManager

    private val downloadUrl1 =
        "https://jt.ximalaya.com//GKwRIUEKWH__ATuPUALqSCn_.m4a?channel=rss&album_id=68370676&track_id=738449880&uid=334394113&jt=https://aod.cos.tx.xmcdn.com/storages/88ca-audiofreehighqps/90/02/GKwRIUEKWH__ATuPUALqSCn_.m4a"
    private val btDownloadUrl1 =
        "magnet:?xt=urn:btih:4LOYF55CKPUNEJ3WAEA3KRGX5NGLQLE6&dn=&tr=http%3A%2F%2F104.143.10.186%3A8000%2Fannounce&tr=udp%3A%2F%2F104.143.10.186%3A8000%2Fannounce&tr=http%3A%2F%2Ftracker.openbittorrent.com%3A80%2Fannounce&tr=http%3A%2F%2Ftracker3.itzmx.com%3A6961%2Fannounce&tr=http%3A%2F%2Ftracker4.itzmx.com%3A2710%2Fannounce&tr=http%3A%2F%2Ftracker.publicbt.com%3A80%2Fannounce&tr=http%3A%2F%2Ftracker.prq.to%2Fannounce&tr=http%3A%2F%2Fopen.acgtracker.com%3A1096%2Fannounce&tr=https%3A%2F%2Ft-115.rhcloud.com%2Fonly_for_ylbud&tr=http%3A%2F%2Ftracker1.itzmx.com%3A8080%2Fannounce&tr=http%3A%2F%2Ftracker2.itzmx.com%3A6961%2Fannounce&tr=udp%3A%2F%2Ftracker1.itzmx.com%3A8080%2Fannounce&tr=udp%3A%2F%2Ftracker2.itzmx.com%3A6961%2Fannounce&tr=udp%3A%2F%2Ftracker3.itzmx.com%3A6961%2Fannounce&tr=udp%3A%2F%2Ftracker4.itzmx.com%3A2710%2Fannounce&tr=http%3A%2F%2Fnyaa.tracker.wf%3A7777%2Fannounce&tr=http%3A%2F%2F208.67.16.113%3A8000%2Fannounce"

    private lateinit var context: Context
    private lateinit var dataStore: DataStore<Preferences>

    private val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.POST_NOTIFICATIONS)
    } else arrayOf()

    @get:Rule
    val runtimePermissionRule = GrantPermissionRule.grant(*permissions)

    private lateinit var db: AppDatabase
    private lateinit var downloadRepository: DownloadRepository

    /**
     * Pass
     * DownloadStarter.download
     * DownloadManager.getInstance(context).downloadInfoListFlow
     */
    @Test
    fun test1() = runTest {
        DownloadStarter.download(context, downloadUrl1)
        Thread.sleep(1000)
        assertNotNull(
            DownloadManager.getInstance(context).downloadInfoListFlow.first()
                .firstOrNull { it.url == downloadUrl1 }
        )
    }

    /**
     * Pass
     * DownloadStarter.download
     * BtDownloadManager.getDownloadInfoList
     */
    @Test
    fun test2() = runTest {
        DownloadStarter.download(context, btDownloadUrl1)
        Thread.sleep(1000)
        assertNull(
            DownloadManager.getInstance(context).downloadInfoListFlow.first()
                .firstOrNull { it.url == btDownloadUrl1 })
    }

    /**
     * Pass
     * downloadRepository.requestDownloadTasksList
     */
    @Test
    fun test3() = runTest {
        DownloadStarter.download(context, downloadUrl1)
        Thread.sleep(1000)

        assertNotNull(
            downloadRepository.requestDownloadTasksList().first()
                .firstOrNull { it.url == downloadUrl1 })
    }


    /**
     * Pass
     * DownloadManager.getInstance(context).pause
     */
    @Test
    fun test6() = runTest {
        DownloadStarter.download(context, downloadUrl1)
        Thread.sleep(1000)

        val task =
            downloadRepository.requestDownloadTasksList().first().first { it.url == downloadUrl1 }
        DownloadManager.getInstance(context).pause(task.id)
        Thread.sleep(5000)

        assertEquals(
            DownloadManager.getInstance(context).downloadInfoListFlow.first()
                .first { it.url == downloadUrl1 }.status, Status.Paused
        )
    }

    /**
     * Pass
     * DownloadManager.getInstance(context).resume
     */
    @Test
    fun test7() = runTest {
        db.clearAllTables()
        DownloadStarter.download(context, downloadUrl1)
        Thread.sleep(1000)

        val task =
            downloadRepository.requestDownloadTasksList().first().first { it.url == downloadUrl1 }
        DownloadManager.getInstance(context).pause(task.id)
        Thread.sleep(5000)

        assertEquals(
            DownloadManager.getInstance(context).downloadInfoListFlow.first()
                .first { it.url == downloadUrl1 }.status, Status.Paused
        )

        DownloadManager.getInstance(context).resume(task.id)
        Thread.sleep(5000)

        assertTrue(
            DownloadManager.getInstance(context).downloadInfoListFlow.first()
                .first { it.url == downloadUrl1 }.status.run { this == Status.Queued || this == Status.Downloading || this == Status.Started || this == Status.Success }
        )
    }

    /**
     * Pass
     * DownloadManager.getInstance(context).pause
     * DownloadManager.getInstance(context).retry
     */
    @Test
    fun test32() = runTest {
        DownloadStarter.download(context, downloadUrl1)
        Thread.sleep(5000)
        val id = DownloadManager.getInstance(context).downloadInfoListFlow.first()
            .first { it.url == downloadUrl1 }.id
        DownloadManager.getInstance(context).pause(id)
        Thread.sleep(5000)
        DownloadManager.getInstance(context).retry(id)
        Thread.sleep(5000)
        assertTrue(
            DownloadManager.getInstance(context).downloadInfoListFlow.first()
                .first { it.url == downloadUrl1 }
                .status.run { this == Status.Queued || this == Status.Downloading || this == Status.Started || this == Status.Success })
    }

    @Before
    fun init() {
        context = ApplicationProvider.getApplicationContext()
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .build()

        // Initialize WorkManager for instrumentation tests.
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        workManager = WorkManager.getInstance(context)

        dataStore = createDataStore(context)

        db = AppDatabase.instance(AppDatabase.builder())
        db.clearAllTables()

        downloadRepository = DownloadRepository()
    }

    @After
    @Throws(IOException::class)
    fun destroy() {
//        db.close()
    }
}