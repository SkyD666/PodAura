package com.skyd.podaura

import android.Manifest
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import androidx.work.workDataOf
import com.skyd.podaura.ext.dataStore
import com.skyd.podaura.ext.put
import com.skyd.podaura.model.bean.download.bt.BtDownloadInfoBean.DownloadState
import com.skyd.podaura.model.bean.download.bt.DownloadLinkUuidMapBean
import com.skyd.podaura.model.bean.download.bt.PeerInfoBean
import com.skyd.podaura.model.db.AppDatabase
import com.skyd.podaura.model.preference.proxy.ProxyHostnamePreference
import com.skyd.podaura.model.preference.proxy.ProxyModePreference
import com.skyd.podaura.model.preference.proxy.ProxyTypePreference
import com.skyd.podaura.model.preference.proxy.UseProxyPreference
import com.skyd.podaura.model.repository.download.DownloadManager
import com.skyd.podaura.model.repository.download.DownloadRepository
import com.skyd.podaura.model.repository.download.DownloadStarter
import com.skyd.podaura.model.repository.download.bt.BtDownloadManager
import com.skyd.podaura.model.repository.download.bt.BtDownloadManager.setDownloadLinkUuidMap
import com.skyd.podaura.model.repository.download.bt.BtDownloadManagerIntent
import com.skyd.podaura.model.worker.download.BtDownloadWorker
import com.skyd.podaura.model.worker.download.BtDownloadWorker.Companion.TORRENT_LINK_UUID
import com.skyd.podaura.model.worker.download.doIfMagnetOrTorrentLink
import com.skyd.podaura.model.worker.download.ifMagnetLink
import com.skyd.podaura.model.worker.download.initProxySettings
import com.skyd.podaura.model.worker.download.isTorrentMimetype
import com.skyd.podaura.model.worker.download.readResumeData
import com.skyd.podaura.model.worker.download.serializeResumeData
import com.skyd.podaura.model.worker.download.toSettingsPackProxyType
import com.skyd.downloader.Status
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.libtorrent4j.AddTorrentParams
import org.libtorrent4j.SessionParams
import org.libtorrent4j.swig.settings_pack
import java.io.IOException
import java.util.UUID


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
class DownloadModule {
    private lateinit var workManager: WorkManager

    private val downloadUrl1 =
        "https://jt.ximalaya.com//GKwRIUEKWH__ATuPUALqSCn_.m4a?channel=rss&album_id=68370676&track_id=738449880&uid=334394113&jt=https://aod.cos.tx.xmcdn.com/storages/88ca-audiofreehighqps/90/02/GKwRIUEKWH__ATuPUALqSCn_.m4a"
    private val btDownloadUrl1 =
        "magnet:?xt=urn:btih:4LOYF55CKPUNEJ3WAEA3KRGX5NGLQLE6&dn=&tr=http%3A%2F%2F104.143.10.186%3A8000%2Fannounce&tr=udp%3A%2F%2F104.143.10.186%3A8000%2Fannounce&tr=http%3A%2F%2Ftracker.openbittorrent.com%3A80%2Fannounce&tr=http%3A%2F%2Ftracker3.itzmx.com%3A6961%2Fannounce&tr=http%3A%2F%2Ftracker4.itzmx.com%3A2710%2Fannounce&tr=http%3A%2F%2Ftracker.publicbt.com%3A80%2Fannounce&tr=http%3A%2F%2Ftracker.prq.to%2Fannounce&tr=http%3A%2F%2Fopen.acgtracker.com%3A1096%2Fannounce&tr=https%3A%2F%2Ft-115.rhcloud.com%2Fonly_for_ylbud&tr=http%3A%2F%2Ftracker1.itzmx.com%3A8080%2Fannounce&tr=http%3A%2F%2Ftracker2.itzmx.com%3A6961%2Fannounce&tr=udp%3A%2F%2Ftracker1.itzmx.com%3A8080%2Fannounce&tr=udp%3A%2F%2Ftracker2.itzmx.com%3A6961%2Fannounce&tr=udp%3A%2F%2Ftracker3.itzmx.com%3A6961%2Fannounce&tr=udp%3A%2F%2Ftracker4.itzmx.com%3A2710%2Fannounce&tr=http%3A%2F%2Fnyaa.tracker.wf%3A7777%2Fannounce&tr=http%3A%2F%2F208.67.16.113%3A8000%2Fannounce"

    private lateinit var context: Context

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
        assertNull(DownloadManager.getInstance(context).downloadInfoListFlow.first()
            .firstOrNull { it.url == btDownloadUrl1 })
        assertNotNull(BtDownloadManager.getDownloadInfoList().first()
            .firstOrNull { it.link == btDownloadUrl1 })
    }

    /**
     * Pass
     * downloadRepository.requestDownloadTasksList
     */
    @Test
    fun test3() = runTest {
        DownloadStarter.download(context, downloadUrl1)
        Thread.sleep(1000)

        assertNotNull(downloadRepository.requestDownloadTasksList().first()
            .firstOrNull { it.url == downloadUrl1 })
    }

    /**
     * Pass
     * downloadRepository.requestBtDownloadTasksList
     */
    @Test
    fun test4() = runTest {
        DownloadStarter.download(context, btDownloadUrl1)
        Thread.sleep(1000)

        assertNotNull(downloadRepository.requestBtDownloadTasksList().first()
            .firstOrNull { it.link == btDownloadUrl1 })
    }

    /**
     * Pass
     * downloadRepository.requestBtDownloadTasksList
     * downloadRepository.deleteBtDownloadTaskInfo
     */
    @Test
    fun test5() = runTest {
        DownloadStarter.download(context, btDownloadUrl1)
        Thread.sleep(1000)

        assertNotNull(downloadRepository.requestBtDownloadTasksList().first()
            .firstOrNull { it.link == btDownloadUrl1 })
        downloadRepository.deleteBtDownloadTaskInfo(btDownloadUrl1).first()
        assertNull(downloadRepository.requestBtDownloadTasksList().first()
            .firstOrNull { it.link == btDownloadUrl1 })
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
     * BtDownloadManager.getDownloadInfo
     * BtDownloadManager.delete
     */
    @Test
    fun test8() = runTest {
        DownloadStarter.download(context, btDownloadUrl1)
        Thread.sleep(1000)

        assertNotEquals(
            BtDownloadManager.getDownloadInfo(btDownloadUrl1)!!.downloadState,
            DownloadState.Init,
        )
        BtDownloadManager.delete(
            context,
            BtDownloadManager.getDownloadInfo(btDownloadUrl1)!!.downloadRequestId,
            btDownloadUrl1
        )
        Thread.sleep(5000)

        assertNull(BtDownloadManager.getDownloadInfo(btDownloadUrl1))
    }

    /**
     * Pass
     * BtDownloadManager.getDownloadInfo
     * BtDownloadManager.pause
     */
    @Test
    fun test9() = runTest {
        DownloadStarter.download(context, btDownloadUrl1)
        Thread.sleep(1000)

        assertNotEquals(
            BtDownloadManager.getDownloadInfo(btDownloadUrl1)!!.downloadState,
            DownloadState.Init,
        )
        BtDownloadManager.pause(
            context,
            BtDownloadManager.getDownloadInfo(btDownloadUrl1)!!.downloadRequestId,
            btDownloadUrl1
        )
        Thread.sleep(5000)

        assertTrue(BtDownloadManager.getDownloadInfo(btDownloadUrl1)!!.downloadState
            .run { this == DownloadState.Paused || this == DownloadState.SeedingPaused })
    }

    /**
     * Pass
     * BtDownloadManager.containsDownloadInfo
     */
    @Test
    fun test10() = runTest {
        DownloadStarter.download(context, btDownloadUrl1)
        Thread.sleep(1000)

        assertTrue(BtDownloadManager.containsDownloadInfo(btDownloadUrl1))
    }

    /**
     * Pass
     * BtDownloadManager.containsDownloadInfo
     */
    @Test
    fun test11() = runTest {
        DownloadStarter.download(context, btDownloadUrl1)
        Thread.sleep(1000)

        BtDownloadManager.delete(
            context,
            BtDownloadManager.getDownloadInfo(btDownloadUrl1)!!.downloadRequestId,
            btDownloadUrl1
        )
        Thread.sleep(500)

        assertFalse(BtDownloadManager.containsDownloadInfo(btDownloadUrl1))
    }

    /**
     * Pass
     * BtDownloadManager.getDownloadUuidByLink
     * BtDownloadManager.getDownloadLinkByUuid
     */
    @Test
    fun test12() = runTest {
        DownloadStarter.download(context, btDownloadUrl1)
        Thread.sleep(1000)

        val uuid = BtDownloadManager.getDownloadUuidByLink(btDownloadUrl1)!!
        assertEquals(
            BtDownloadManager.getDownloadLinkByUuid(uuid),
            btDownloadUrl1
        )
    }

    /**
     * Pass
     * BtDownloadManager.getDownloadProgress
     */
    @Test
    fun test13() = runTest {
        DownloadStarter.download(context, btDownloadUrl1)
        Thread.sleep(1000)

        assertNotNull(BtDownloadManager.getDownloadProgress(btDownloadUrl1))
    }

    /**
     * Pass
     * BtDownloadManager.getDownloadName
     */
    @Test
    fun test14() = runTest {
        DownloadStarter.download(context, btDownloadUrl1)
        Thread.sleep(1000)

        assertNotNull(BtDownloadManager.getDownloadName(btDownloadUrl1))
    }

    /**
     * Pass
     * BtDownloadManager.getDownloadState
     */
    @Test
    fun test15() = runTest {
        DownloadStarter.download(context, btDownloadUrl1)
        Thread.sleep(1000)

        assertNotNull(BtDownloadManager.getDownloadState(btDownloadUrl1))
    }

    /**
     * Pass
     * BtDownloadManager.pause
     * BtDownloadManager.getSessionParams
     */
    @Test
    fun test16() = runTest {
        db.clearAllTables()
        DownloadStarter.download(context, btDownloadUrl1)
        Thread.sleep(1000)
        BtDownloadManager.pause(
            context,
            BtDownloadManager.getDownloadInfo(btDownloadUrl1)!!.downloadRequestId,
            btDownloadUrl1
        )
        Thread.sleep(5000)
        assertNotNull(BtDownloadManager.getSessionParams(btDownloadUrl1))
    }

    /**
     * Pass
     * BtDownloadManager.delete
     * BtDownloadManager.getSessionParams
     */
    @Test
    fun test17() = runTest {
        DownloadStarter.download(context, btDownloadUrl1)
        Thread.sleep(1000)
        BtDownloadManager.delete(
            context,
            BtDownloadManager.getDownloadInfo(btDownloadUrl1)!!.downloadRequestId,
            btDownloadUrl1
        )
        Thread.sleep(1000)
        assertNull(BtDownloadManager.getSessionParams(btDownloadUrl1))
    }

    /**
     * Pass
     * BtDownloadManager.deleteDownloadInfo
     * BtDownloadManager.getDownloadInfo
     */
    @Test
    fun test18() = runTest {
        DownloadStarter.download(context, btDownloadUrl1)
        Thread.sleep(1000)

        BtDownloadManager.deleteDownloadInfo(btDownloadUrl1)
        assertNull(BtDownloadManager.getDownloadInfo(btDownloadUrl1))
    }

    /**
     * Pass
     * BtDownloadManager.deleteSessionParams
     * BtDownloadManager.getSessionParams
     */
    @Test
    fun test19() = runTest {
        DownloadStarter.download(context, btDownloadUrl1)
        Thread.sleep(1000)

        BtDownloadManager.deleteSessionParams(btDownloadUrl1)
        assertNull(BtDownloadManager.getSessionParams(btDownloadUrl1))
    }

    /**
     * Pass
     * BtDownloadWorker.doWork
     */
    @Test
    fun test20() = runTest {
        val torrentLinkUuid = UUID.randomUUID().toString()
        setDownloadLinkUuidMap(
            DownloadLinkUuidMapBean(
                link = btDownloadUrl1,
                uuid = torrentLinkUuid,
            )
        )
        val worker = TestListenableWorkerBuilder<BtDownloadWorker>(context)
            .setInputData(workDataOf(TORRENT_LINK_UUID to torrentLinkUuid))
            .build()
        withContext(Dispatchers.IO) {
            withTimeoutOrNull(5000) {
                worker.doWork()
            }
            assertTrue(BtDownloadManager.containsDownloadInfo(btDownloadUrl1))
        }
    }

    /**
     * Pass
     * isTorrentMimetype
     */
    @Test
    fun test21() = runTest {
        assertFalse(isTorrentMimetype("application/x-bittorrents"))
    }

    /**
     * Pass
     * isTorrentMimetype
     */
    @Test
    fun test22() = runTest {
        assertTrue(isTorrentMimetype("applications/x-bittorrent"))
    }

    /**
     * Pass
     * doIfMagnetOrTorrentLink
     */
    @Test
    fun test23() = runTest {
        doIfMagnetOrTorrentLink(
            link = btDownloadUrl1,
            onTorrent = { assertTrue(false) },
            onUnsupported = { assertTrue(false) },
        )
    }

    /**
     * Pass
     * doIfMagnetOrTorrentLink
     */
    @Test
    fun test24() = runTest {
        doIfMagnetOrTorrentLink(
            link = downloadUrl1,
            onMagnet = { assertTrue(false) },
            onTorrent = { assertTrue(false) },
            onSupported = { assertTrue(false) },
        )
    }

    /**
     * Pass
     * ifMagnetLink
     */
    @Test
    fun test25() = runTest {
        ifMagnetLink(
            link = downloadUrl1,
            onMagnet = { assertTrue(false) },
        )
    }

    /**
     * Pass
     * ifMagnetLink
     */
    @Test
    fun test26() = runTest {
        ifMagnetLink(
            link = btDownloadUrl1,
            onUnsupported = { assertTrue(false) },
        )
    }

    /**
     * Pass
     * initProxySettings
     * toSettingsPackProxyType
     */
    @Test
    fun test27() = runTest {
        context.dataStore.apply {
            put(UseProxyPreference.key, true)
            put(ProxyModePreference.key, ProxyModePreference.MANUAL_MODE)
            put(ProxyTypePreference.key, ProxyTypePreference.HTTP)
            put(ProxyHostnamePreference.key, "localhost")
        }
        val sessionParams = SessionParams()
        sessionParams.settings = initProxySettings(
            context = context,
            settings = sessionParams.settings,
        ).setString(
            settings_pack.string_types.user_agent.swigValue(),
            "Test/1.0"
        )
        with(sessionParams.settings) {
            assertEquals(getString(settings_pack.string_types.user_agent.swigValue()), "Test/1.0")
            assertEquals(
                getInteger(settings_pack.int_types.proxy_type.swigValue()),
                toSettingsPackProxyType(ProxyTypePreference.HTTP).swigValue()
            )
            assertEquals(
                getString(settings_pack.string_types.proxy_hostname.swigValue()),
                "localhost"
            )
        }
    }

    /**
     * Pass
     * initProxySettings
     */
    @Test
    fun test28() = runTest {
        context.dataStore.put(UseProxyPreference.key, false)
        val sessionParams = SessionParams()
        sessionParams.settings = initProxySettings(
            context = context,
            settings = sessionParams.settings,
        )
        assertEquals(
            sessionParams.settings.getInteger(settings_pack.int_types.proxy_type.swigValue()),
            0
        )
    }

    /**
     * Pass
     * BtDownloadManager.sendIntent
     */
    @Test
    fun test29() = runTest {
        db.clearAllTables()
        DownloadStarter.download(context, btDownloadUrl1)
        Thread.sleep(1000)
        BtDownloadManager.sendIntent(
            BtDownloadManagerIntent.UpdateDownloadDescription(btDownloadUrl1, "TestTest")
        )
        Thread.sleep(5000)
        assertEquals(BtDownloadManager.getDownloadInfo(btDownloadUrl1)!!.description, "TestTest")
    }

    /**
     * Pass
     * BtDownloadManager.updatePeerInfoMapFlow
     * BtDownloadManager.peerInfoMapFlow
     */
    @Test
    fun test30() = runTest {
        val requestId = UUID.randomUUID().toString()
        BtDownloadManager.updatePeerInfoMapFlow(
            requestId, listOf(PeerInfoBean(client = "TestClient")),
        )
        assertEquals(
            BtDownloadManager.peerInfoMapFlow.value[requestId]!!.first().client, "TestClient"
        )
    }

    /**
     * Pass
     * BtDownloadManager.updatePeerInfoMapFlow
     * BtDownloadManager.peerInfoMapFlow
     * BtDownloadManager.removeWorkerFromFlow
     */
    @Test
    fun test31() = runTest {
        val requestId = UUID.randomUUID().toString()
        BtDownloadManager.updatePeerInfoMapFlow(
            requestId, listOf(PeerInfoBean(client = "TestClient2")),
        )
        assertEquals(
            BtDownloadManager.peerInfoMapFlow.value[requestId]!!.first().client, "TestClient2"
        )
        BtDownloadManager.removeWorkerFromFlow(requestId)
        assertNull(BtDownloadManager.peerInfoMapFlow.value[requestId])
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
        assertTrue(DownloadManager.getInstance(context).downloadInfoListFlow.first()
            .first { it.url == downloadUrl1 }
            .status.run { this == Status.Queued || this == Status.Downloading || this == Status.Started || this == Status.Success })
    }

    /**
     * Pass
     * serializeResumeData
     * readResumeData
     */
    @Test
    fun test33() = runTest {
        serializeResumeData("TestResumeData", AddTorrentParams())
        assertNotNull(readResumeData("TestResumeData"))
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

        db = AppDatabase.getInstance(context)
        db.clearAllTables()

        downloadRepository = DownloadRepository()
    }

    @After
    @Throws(IOException::class)
    fun destroy() {
//        db.close()
    }
}