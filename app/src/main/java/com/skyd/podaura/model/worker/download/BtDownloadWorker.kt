package com.skyd.podaura.model.worker.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.skyd.podaura.BuildConfig
import com.skyd.podaura.R
import com.skyd.podaura.config.Const
import com.skyd.podaura.config.TEMP_TORRENT_DIR
import com.skyd.podaura.di.get
import com.skyd.podaura.ext.decodeURL
import com.skyd.podaura.ext.getAppName
import com.skyd.podaura.ext.getAppVersionName
import com.skyd.podaura.ext.getOrDefault
import com.skyd.podaura.ext.getString
import com.skyd.podaura.ext.ifNullOfBlank
import com.skyd.podaura.ext.toPercentage
import com.skyd.podaura.ext.validateFileName
import com.skyd.podaura.model.bean.download.bt.BtDownloadInfoBean
import com.skyd.podaura.model.bean.download.bt.PeerInfoBean
import com.skyd.podaura.model.bean.download.bt.from
import com.skyd.podaura.model.preference.data.medialib.MediaLibLocationPreference
import com.skyd.podaura.model.preference.dataStore
import com.skyd.podaura.model.preference.transmission.SeedingWhenCompletePreference
import com.skyd.podaura.model.preference.transmission.TorrentTrackersPreference
import com.skyd.podaura.model.repository.download.bt.BtDownloadManager
import com.skyd.podaura.model.repository.download.bt.BtDownloadManagerIntent
import com.skyd.podaura.ui.activity.MainActivity
import com.skyd.podaura.ui.component.blockString
import com.skyd.podaura.ui.component.showToast
import com.skyd.podaura.ui.screen.download.DownloadRoute
import com.skyd.podaura.util.uniqueInt
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.prepareGet
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.copyAndClose
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.libtorrent4j.AlertListener
import org.libtorrent4j.AnnounceEntry
import org.libtorrent4j.SessionManager
import org.libtorrent4j.SessionParams
import org.libtorrent4j.TorrentHandle
import org.libtorrent4j.TorrentInfo
import org.libtorrent4j.TorrentStatus
import org.libtorrent4j.alerts.AddTorrentAlert
import org.libtorrent4j.alerts.Alert
import org.libtorrent4j.alerts.FileErrorAlert
import org.libtorrent4j.alerts.FileRenamedAlert
import org.libtorrent4j.alerts.MetadataReceivedAlert
import org.libtorrent4j.alerts.PeerConnectAlert
import org.libtorrent4j.alerts.PeerDisconnectedAlert
import org.libtorrent4j.alerts.PeerInfoAlert
import org.libtorrent4j.alerts.SaveResumeDataAlert
import org.libtorrent4j.alerts.StateChangedAlert
import org.libtorrent4j.alerts.StorageMovedAlert
import org.libtorrent4j.alerts.StorageMovedFailedAlert
import org.libtorrent4j.alerts.TorrentAlert
import org.libtorrent4j.alerts.TorrentCheckedAlert
import org.libtorrent4j.alerts.TorrentErrorAlert
import org.libtorrent4j.alerts.TorrentFinishedAlert
import org.libtorrent4j.swig.settings_pack
import org.libtorrent4j.swig.torrent_flags_t
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.download_copyright_issues_warning
import podaura.shared.generated.resources.download_pause
import podaura.shared.generated.resources.downloading
import podaura.shared.generated.resources.torrent_download_channel_name
import java.io.File
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resumeWithException


class BtDownloadWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {
    private lateinit var torrentLinkUuid: String
    private lateinit var torrentLink: String
    private var saveDir = File(dataStore.getOrDefault(MediaLibLocationPreference))
    private var progress: Float = 0f
    private var name: String? = null
    private var description: String? = null

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val notificationId = uniqueInt()

    private val sessionManager = SessionManager(BuildConfig.DEBUG)

    private suspend fun initData(): Boolean {
        torrentLinkUuid = inputData.getString(TORRENT_LINK_UUID) ?: return false
        inputData.getString(SAVE_DIR)?.let { saveDir = File(it) }
        BtDownloadManager.apply {
            torrentLink = getDownloadLinkByUuid(torrentLinkUuid) ?: return false
            name = getDownloadName(link = torrentLink)
            progress = getDownloadProgress(link = torrentLink) ?: 0f
        }
        return true
    }

    override suspend fun doWork(): Result {
        withContext(Dispatchers.IO) {
            coroutineContext.job.invokeOnCompletion {
                if (it is CancellationException) {
                    pauseWorker(handle = null)
                }
            }
            if (!initData()) return@withContext Result.failure()
            updateNotification()
            // 如果数据库中没有下载信息，就添加新的下载信息（为新下载任务添加信息）
            addNewDownloadInfoToDbIfNotExists(
                link = torrentLink,
                name = name,
                path = saveDir.path,
                progress = progress,
                size = sessionManager.stats().totalDownload(),
                downloadRequestId = id.toString(),
            )
            workerDownload(saveDir)
        }
        BtDownloadManager.removeWorkerFromFlow(id.toString())
        return Result.success(
            workDataOf(
                STATE to (BtDownloadManager.getDownloadState(link = torrentLink)
                    ?.ordinal ?: 0),
                TORRENT_LINK_UUID to torrentLinkUuid,
            )
        )
    }

    private var sessionIsStopping: Boolean = false
    private suspend fun workerDownload(
        saveDir: File,
    ) = suspendCancellableCoroutine { continuation ->
        if (!saveDir.exists() && !saveDir.mkdirs()) {
            continuation.resumeWithException(RuntimeException("Mkdirs failed: $saveDir"))
        }

        sessionManager.addListener(object : AlertListener {
            override fun types(): IntArray? = null         // 监听所有类型的警报
            override fun alert(alert: Alert<*>?) {
                if (alert == null) return

                onAlert(continuation, alert)

                if (isStopped && !sessionIsStopping) {
                    val handle = (alert as? TorrentAlert)?.handle() ?: return
                    pauseWorker(handle = handle)
                    continuation.resume(Unit, null)
                }
            }
        })

        // 这里不是挂起函数，因此外面的job.invokeOnCompletion不能捕获到异常，需要手动runCatching
        runCatching {
            howToDownload(saveDir = saveDir)
        }.onFailure {
            it.printStackTrace()
            pauseWorker(handle = null, state = BtDownloadInfoBean.DownloadState.ErrorPaused)
            continuation.resumeWithException(it)
        }
    }

    private fun howToDownload(saveDir: File) = runBlocking(Dispatchers.IO) {
        sessionManager.apply {
            val lastSessionParams = BtDownloadManager.getSessionParams(link = torrentLink)
            val sessionParams = if (lastSessionParams == null) SessionParams()
            else SessionParams(lastSessionParams.data)

            sessionParams.settings = initProxySettings(
                context = applicationContext,
                settings = sessionParams.settings,
            ).setString(
                settings_pack.string_types.user_agent.swigValue(),
                "${applicationContext.getAppName() ?: "PodAura"}/${applicationContext.getAppVersionName()}"
            )

            start(sessionParams)
            startDht()

            if (BtDownloadManager.containsDownloadInfo(link = torrentLink)) {
                BtDownloadManager.sendIntent(
                    BtDownloadManagerIntent.UpdateDownloadInfoRequestId(
                        link = torrentLink,
                        downloadRequestId = id.toString(),
                    )
                )
            }
            var newDownloadState: BtDownloadInfoBean.DownloadState? = null
            when (BtDownloadManager.getDownloadState(link = torrentLink)) {
                null,
                    // 重新下载
                BtDownloadInfoBean.DownloadState.Seeding,
                BtDownloadInfoBean.DownloadState.SeedingPaused,
                BtDownloadInfoBean.DownloadState.Completed -> {
                    val resumeData = readResumeData(id.toString())
                    if (resumeData != null) {
                        swig().async_add_torrent(resumeData)
                    }
                    newDownloadState = BtDownloadInfoBean.DownloadState.Seeding
                }

                BtDownloadInfoBean.DownloadState.Init -> {
                    downloadByMagnetOrTorrent(torrentLink, saveDir)
                    newDownloadState = BtDownloadInfoBean.DownloadState.Downloading
                }

                BtDownloadInfoBean.DownloadState.Downloading,
                BtDownloadInfoBean.DownloadState.ErrorPaused,
                BtDownloadInfoBean.DownloadState.StorageMovedFailed,
                BtDownloadInfoBean.DownloadState.Paused -> {
                    downloadByMagnetOrTorrent(torrentLink, saveDir)
                    newDownloadState = BtDownloadInfoBean.DownloadState.Downloading
                }
            }
            updateDownloadState(
                link = torrentLink,
                downloadState = newDownloadState,
            )
        }
    }

    private suspend fun downloadByMagnetOrTorrent(
        link: String,
        saveDir: File,
        flags: torrent_flags_t = torrent_flags_t(),
    ) {
        if (link.startsWith("magnet:")) {
            sessionManager.download(link, saveDir, flags)
        } else {
            val tempTorrentFile = if (link.startsWith("file:") || link.startsWith("/")) {
                File(link)
            } else {
                File(
                    Const.TEMP_TORRENT_DIR,
                    link.substringAfterLast('/').decodeURL().validateFileName()
                ).apply {
                    if (parentFile?.exists() == false) parentFile?.mkdirs()
                    if (!exists()) createNewFile()
                    // May throw exceptions
                    get<HttpClient>().prepareGet(link).execute { httpResponse ->
                        val channel: ByteReadChannel = httpResponse.body()
                        channel.copyAndClose(this.writeChannel())
                    }
                }
            }
            sessionManager.download(
                TorrentInfo(tempTorrentFile), saveDir,
                null, null, null,
                flags
            )
        }
    }

    override suspend fun getForegroundInfo() = createForegroundInfo()

    private suspend fun updateNotification() {
        runCatching {
            setForeground(createForegroundInfo())
        }.onFailure { it.printStackTrace() }
    }

    private fun updateNotificationAsync() {
        runCatching {
            setForegroundAsync(createForegroundInfo())
        }.onFailure { it.printStackTrace() }
    }

    // Creates an instance of ForegroundInfo which can be used to update the ongoing notification.
    private fun createForegroundInfo(): ForegroundInfo {
        val title = name.ifNullOfBlank { applicationContext.getString(Res.string.downloading) }
        // This PendingIntent can be used to cancel the worker
        val cancelIntent = WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)
        val contentIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(
                Intent.ACTION_VIEW,
                DownloadRoute.BASE_PATH.toUri(),
                applicationContext,
                MainActivity::class.java
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )

        // Create a Notification channel if necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel()
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setTicker(title)
            .setContentText(progress.toPercentage())
            .setSmallIcon(R.drawable.ic_icon_24)
            .setOngoing(true)
            .setProgress(100, (progress * 100).toInt(), false)
            // Add the cancel action to the notification which can be used to cancel the worker
            .addAction(
                R.drawable.ic_pause_24,
                applicationContext.getString(Res.string.download_pause),
                cancelIntent,
            )
            .setContentIntent(contentIntent)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ForegroundInfo(
                notificationId,
                notification,
                FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            applicationContext.getString(Res.string.torrent_download_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        notificationManager.createNotificationChannel(channel)
    }

    private fun onAlert(continuation: CancellableContinuation<Unit>, alert: Alert<*>) {
        when (alert) {
            is AddTorrentAlert -> {
                addTrackers(alert.handle())
            }

            is SaveResumeDataAlert -> {
                serializeResumeData(id.toString(), alert.params())
            }

            is TorrentErrorAlert -> {
                // Download error
                pauseWorker(
                    handle = alert.handle(),
                    state = BtDownloadInfoBean.DownloadState.ErrorPaused,
                )
                continuation.resumeWithException(RuntimeException(alert.message()))
            }
            // If the storage fails to read or write files that it needs access to,
            // this alert is generated and the torrent is paused.
            is FileErrorAlert -> {
                // 文件错误，例如存储空间已满
                pauseWorker(
                    handle = alert.handle(),
                    state = BtDownloadInfoBean.DownloadState.ErrorPaused,
                )
                continuation.resumeWithException(RuntimeException(alert.message()))
            }

            is StorageMovedAlert -> {
                alert.handle().saveResumeData()
                updateNotificationAsync()     // update Notification
                updateDownloadStateAndSessionParams(
                    link = torrentLink,
                    sessionStateData = sessionManager.saveState() ?: byteArrayOf(),
                    downloadState = BtDownloadInfoBean.DownloadState.Seeding,
                )
            }

            is StorageMovedFailedAlert -> {
                Log.e(
                    TAG,
                    "StorageMovedFailedAlert: " +
                            "Message: ${alert.message()}\n" +
                            "${alert.error()}\n" +
                            "Path: ${alert.filePath()}"
                )
                // 文件移动，例如存储空间已满
                alert.handle().saveResumeData()
                pauseWorker(
                    handle = alert.handle(),
                    state = BtDownloadInfoBean.DownloadState.StorageMovedFailed,
                )
            }

            is TorrentFinishedAlert -> {
                // Torrent download finished
                val handle = alert.handle()
                progress = 1f
                name = handle.name
                handle.saveResumeData()
                updateNotificationAsync()     // update Notification
                updateDownloadStateAndSessionParams(
                    link = torrentLink,
                    sessionStateData = sessionManager.saveState() ?: byteArrayOf(),
                    downloadState = BtDownloadInfoBean.DownloadState.Completed,
                )
                // Do not seeding when complete
                if (!dataStore.getOrDefault(SeedingWhenCompletePreference)) {
                    BtDownloadManager.pause(
                        context = applicationContext,
                        requestId = id.toString(),
                        link = torrentLink
                    )
                }
            }

            is FileRenamedAlert -> {

            }

            // a torrent completes checking. ready to start downloading
            is TorrentCheckedAlert -> {
                val handle = alert.handle()
                name = handle.name
                if (!BtFileNameChecker.check(handle.name)) {
                    sessionManager.remove(handle)
                    blockString(Res.string.download_copyright_issues_warning).showToast()
                    return
                }
                updateTorrentFilesToDb(
                    link = torrentLink,
                    savePath = handle.savePath(),
                    files = handle.torrentFile().files(),
                )
                updateNotificationAsync()     // update Notification
                updateNameInfoToDb(link = torrentLink, name = name)
            }

            is MetadataReceivedAlert -> {
                // 元数据更新
                // save the torrent file in order to load it back up again
                // when the session is restarted
            }

            is StateChangedAlert -> {
                // Download state update
                if (alert.state == TorrentStatus.State.SEEDING) {
                    updateDownloadStateAndSessionParams(
                        link = torrentLink,
                        sessionStateData = sessionManager.saveState() ?: byteArrayOf(),
                        downloadState = BtDownloadInfoBean.DownloadState.Seeding,
                    )
                }
                description = alert.state.toDisplayString(context = applicationContext)
                updateDescriptionInfoToDb(link = torrentLink, description = description!!)
                val handle = alert.handle()
                if (handle.isValid) {
                    progress = handle.status().progress()
                    updateNotificationAsync()     // update Notification
                    updateProgressInfoToDb(link = torrentLink, progress = progress)
                }
            }

            is PeerConnectAlert,
            is PeerDisconnectedAlert,
            is PeerInfoAlert -> {
                val peerInfo = alert.handle()?.peerInfo()?.map { PeerInfoBean.from(it) }
//                Log.e("TAG", "onAlert: ${peerInfo?.size}")
                if (!peerInfo.isNullOrEmpty()) {
                    BtDownloadManager.updatePeerInfoMapFlow(id.toString(), peerInfo)
                }
            }

            is TorrentAlert<*> -> {
//                Log.e("TAG", "onAlert: ${alert}")
                // Download progress update
                val handle = alert.handle()
                if (handle.isValid) {
                    BtDownloadManager.updateTorrentStatusMapFlow(id.toString(), handle.status())
                    if (progress != handle.status().progress()) {
                        progress = handle.status().progress()
                        updateNotificationAsync()     // update Notification
                        updateProgressInfoToDb(link = torrentLink, progress = progress)
                        updateSizeInfoToDb(
                            link = torrentLink,
                            size = sessionManager.stats().totalDownload()
                        )
                    }
                }
            }
        }
    }

    private fun addTrackers(handle: TorrentHandle) {
        dataStore.getOrDefault(TorrentTrackersPreference).forEach { track ->
            handle.addTracker(AnnounceEntry(track))
        }
    }

    private fun pauseWorker(
        handle: TorrentHandle?,
        state: BtDownloadInfoBean.DownloadState = getWhatPausedState(
            runBlocking { BtDownloadManager.getDownloadState(link = torrentLink) }
        )
    ) {
        if (!sessionManager.isRunning || sessionIsStopping) {
            return
        }
        sessionIsStopping = true
        updateDownloadStateAndSessionParams(
            link = torrentLink,
            sessionStateData = sessionManager.saveState() ?: byteArrayOf(),
            downloadState = state
        )
        if (handle != null) {
            handle.saveResumeData()
            sessionManager.remove(handle)
        }

        sessionManager.pause()
        sessionManager.stopDht()
        sessionManager.stop()

        BtDownloadManager.removeWorkerFromFlow(id.toString())
    }

    companion object {
        const val TAG = "DownloadTorrentWorker"
        const val STATE = "state"
        const val TORRENT_LINK_UUID = "torrentLinkUuid"
        const val SAVE_DIR = "saveDir"
        const val CHANNEL_ID = "downloadTorrent"
    }
}