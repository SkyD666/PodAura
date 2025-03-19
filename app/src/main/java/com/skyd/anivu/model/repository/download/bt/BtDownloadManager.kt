package com.skyd.anivu.model.repository.download.bt

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.accompanist.permissions.rememberPermissionState
import com.skyd.anivu.R
import com.skyd.anivu.appContext
import com.skyd.anivu.ext.longestCommonPrefix
import com.skyd.anivu.ext.sampleWithoutFirst
import com.skyd.anivu.model.bean.download.bt.BtDownloadInfoBean
import com.skyd.anivu.model.bean.download.bt.BtDownloadInfoBean.DownloadState
import com.skyd.anivu.model.bean.download.bt.DownloadLinkUuidMapBean
import com.skyd.anivu.model.bean.download.bt.PeerInfoBean
import com.skyd.anivu.model.bean.download.bt.SessionParamsBean
import com.skyd.anivu.model.bean.download.bt.TorrentFileBean
import com.skyd.anivu.model.db.dao.ArticleDao
import com.skyd.anivu.model.db.dao.DownloadInfoDao
import com.skyd.anivu.model.db.dao.EnclosureDao
import com.skyd.anivu.model.db.dao.SessionParamsDao
import com.skyd.anivu.model.db.dao.TorrentFileDao
import com.skyd.anivu.model.repository.MediaRepository
import com.skyd.anivu.model.worker.download.BtDownloadWorker
import com.skyd.anivu.model.worker.download.BtDownloadWorker.Companion.SAVE_DIR
import com.skyd.anivu.model.worker.download.BtDownloadWorker.Companion.TORRENT_LINK_UUID
import com.skyd.anivu.model.worker.download.getWhatPausedState
import com.skyd.anivu.model.worker.download.updateDownloadState
import com.skyd.anivu.ui.component.showToast
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.libtorrent4j.TorrentStatus
import java.io.File
import java.util.UUID

object BtDownloadManager {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DownloadManagerPoint {
        val downloadInfoDao: DownloadInfoDao
        val sessionParamsDao: SessionParamsDao
        val torrentFileDao: TorrentFileDao
        val enclosureDao: EnclosureDao
        val articleDao: ArticleDao
        val mediaRepository: MediaRepository
    }

    private val hiltEntryPoint = EntryPointAccessors.fromApplication(
        appContext, DownloadManagerPoint::class.java
    )

    private val downloadInfoDao = hiltEntryPoint.downloadInfoDao
    private val sessionParamsDao = hiltEntryPoint.sessionParamsDao
    private val torrentFileDao = hiltEntryPoint.torrentFileDao
    private val scope = CoroutineScope(Dispatchers.IO)
    private val intentFlow = MutableSharedFlow<BtDownloadManagerIntent>()
    private lateinit var downloadInfoMap: LinkedHashMap<String, BtDownloadInfoBean>
    private lateinit var downloadInfoListFlow: MutableStateFlow<List<BtDownloadInfoBean>>

    val peerInfoMapFlow = MutableStateFlow(mutableMapOf<String, List<PeerInfoBean>>())
    val torrentStatusMapFlow = MutableStateFlow(mutableMapOf<String, TorrentStatus>())

    init {
        intentFlow
            .onEachIntent()
            .launchIn(scope)
    }

    fun interface BtDownloadWorkStarter {
        fun start(torrentLink: String, saveDir: String, requestId: String?)
    }

    @Composable
    fun rememberBtDownloadWorkStarter(): BtDownloadWorkStarter {
        val context = LocalContext.current
        var currentTorrentLink: String? by rememberSaveable { mutableStateOf(null) }
        var currentSaveDir: String? by rememberSaveable { mutableStateOf(null) }
        var currentRequestId: String? by rememberSaveable { mutableStateOf(null) }
        val starter = {
            download(context, currentTorrentLink!!, currentSaveDir!!, currentRequestId)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val storagePermissionState = rememberPermissionState(
                Manifest.permission.POST_NOTIFICATIONS
            ) {
                if (it) {
                    starter()
                } else {
                    context.getString(R.string.download_no_notification_permission_tip)
                        .showToast()
                }
            }
            return remember {
                BtDownloadWorkStarter { torrentLink, saveDir, requestId ->
                    currentTorrentLink = torrentLink
                    currentSaveDir = saveDir
                    currentRequestId = requestId
                    storagePermissionState.launchPermissionRequest()
                }
            }
        } else {
            return remember {
                BtDownloadWorkStarter { torrentLink, saveDir, requestId ->
                    download(context, torrentLink, saveDir, requestId)
                }
            }
        }
    }

    fun download(
        context: Context,
        torrentLink: String,
        saveDir: String,
        requestId: String? = null
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                context.getString(R.string.download_no_notification_permission_tip).showToast()
                return
            }
        }
        scope.launch {
            var torrentLinkUuid = getDownloadUuidByLink(torrentLink)
            if (torrentLinkUuid == null) {
                torrentLinkUuid = UUID.randomUUID().toString()
                setDownloadLinkUuidMap(
                    DownloadLinkUuidMapBean(
                        link = torrentLink,
                        uuid = torrentLinkUuid,
                    )
                )
            }

            val workRequest = OneTimeWorkRequestBuilder<BtDownloadWorker>()
                .run { if (requestId != null) setId(UUID.fromString(requestId)) else this }
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setInputData(workDataOf(TORRENT_LINK_UUID to torrentLinkUuid, SAVE_DIR to saveDir))
                .build()

            WorkManager.getInstance(context).apply {
                // ENQUEUED generally means in queue, but not running. So we replace it to start
                val existingWorkPolicy = if (getWorkInfoById(workRequest.id)
                        .get()?.state == WorkInfo.State.ENQUEUED
                ) {
                    ExistingWorkPolicy.REPLACE
                } else {
                    ExistingWorkPolicy.KEEP
                }
                enqueueUniqueWork(
                    torrentLinkUuid,
                    existingWorkPolicy,
                    workRequest
                )

                getWorkInfoByIdFlow(workRequest.id)
                    .take(1)
                    .filter { it == null || it.state.isFinished }
                    .onEach {
                        removeWorkerFromFlow(workRequest.id.toString())
                    }.collect {
                        cancel()
                    }
            }
        }
    }

    fun pause(
        context: Context,
        requestId: String,
        link: String,
    ) {
        val requestUuid = UUID.fromString(requestId)
        WorkManager.getInstance(context).apply {
            val workerState = getWorkInfoById(requestUuid).get()?.state
            if (workerState == null || workerState.isFinished) {
                scope.launch {
                    val state = getDownloadState(link)
                    updateDownloadState(
                        link = link,
                        downloadState = getWhatPausedState(state),
                    )
                }
            } else {
                cancelWorkById(requestUuid)
            }
        }
    }

    fun delete(
        context: Context,
        requestId: String,
        link: String,
    ) {
        val requestUuid = UUID.fromString(requestId)
        val worker = WorkManager.getInstance(context)
        // 在worker结束后删除数据库中的下载任务信息
        scope.launch {
            worker.cancelWorkById(requestUuid)
            worker.getWorkInfoByIdFlow(requestUuid)
                .filter { it == null || it.state.isFinished }
                .flatMapConcat {
                    BtDownloadWorker.hiltEntryPoint.downloadRepository.deleteBtDownloadTaskInfo(
                        link = link
                    )
                }.take(1)
                .collect()
        }
    }

    internal fun updatePeerInfoMapFlow(requestId: String, list: List<PeerInfoBean>) {
        peerInfoMapFlow.tryEmit(peerInfoMapFlow.value.toMutableMap().apply {
            put(requestId, list)
        })
    }

    internal fun updateTorrentStatusMapFlow(requestId: String, status: TorrentStatus) {
        torrentStatusMapFlow.tryEmit(torrentStatusMapFlow.value.toMutableMap().apply {
            put(requestId, status)
        })
    }

    internal fun removeWorkerFromFlow(requestId: String) {
        peerInfoMapFlow.tryEmit(
            peerInfoMapFlow.value.toMutableMap().apply { remove(requestId) }
        )
        torrentStatusMapFlow.tryEmit(
            torrentStatusMapFlow.value.toMutableMap().apply { remove(requestId) }
        )
    }

    private fun Flow<BtDownloadManagerIntent>.onEachIntent(): Flow<BtDownloadManagerIntent> {
        return merge(
            filterIsInstance<BtDownloadManagerIntent.UpdateDownloadInfo>()
                .sampleWithoutFirst(100)
                .onEach { intent ->
                    downloadInfoDao.updateDownloadInfo(intent.btDownloadInfoBean)
                    putDownloadInfoToMap(
                        link = intent.btDownloadInfoBean.link,
                        newInfo = intent.btDownloadInfoBean,
                    )
                }.catch { it.printStackTrace() },

            filterIsInstance<BtDownloadManagerIntent.UpdateSessionParams>()
                .onEach { intent ->
                    sessionParamsDao.updateSessionParams(
                        SessionParamsBean(
                            link = intent.link,
                            data = intent.sessionStateData,
                        )
                    )
                }.catch { it.printStackTrace() },

            filterIsInstance<BtDownloadManagerIntent.UpdateDownloadProgress>()
                .sampleWithoutFirst(1000)
                .onEach { intent ->
                    val result = downloadInfoDao.updateDownloadProgress(
                        link = intent.link,
                        progress = intent.progress,
                    )
                    if (result > 0) {
                        updateDownloadInfoMap(intent.link) { copy(progress = intent.progress) }
                    }
                }.catch { it.printStackTrace() },

            filterIsInstance<BtDownloadManagerIntent.UpdateDownloadState>()
                .onEach { intent ->
                    val result = downloadInfoDao.updateDownloadState(
                        link = intent.link,
                        downloadState = intent.downloadState,
                    )
                    if (result > 0) {
                        updateDownloadInfoMap(intent.link) { copy(downloadState = intent.downloadState) }
                        if (intent.downloadState == DownloadState.Completed) {
                            addFileToMediaLibrary(intent.link)
                        }
                    }
                }.catch { it.printStackTrace() },

            filterIsInstance<BtDownloadManagerIntent.UpdateDownloadSize>()
                .sampleWithoutFirst(1000)
                .onEach { intent ->
                    val result = downloadInfoDao.updateDownloadSize(
                        link = intent.link, size = intent.size,
                    )
                    if (result > 0) {
                        updateDownloadInfoMap(intent.link) { copy(size = intent.size) }
                    }
                }.catch { it.printStackTrace() },

            filterIsInstance<BtDownloadManagerIntent.UpdateDownloadName>()
                .sampleWithoutFirst(200)
                .onEach { intent ->
                    if (intent.name.isNullOrBlank()) return@onEach
                    val result = downloadInfoDao.updateDownloadName(
                        link = intent.link,
                        name = intent.name,
                    )
                    if (result > 0) {
                        updateDownloadInfoMap(intent.link) { copy(name = intent.name) }
                    }
                }.catch { it.printStackTrace() },

            filterIsInstance<BtDownloadManagerIntent.UpdateDownloadInfoRequestId>()
                .onEach { intent ->
                    val result = downloadInfoDao.updateDownloadInfoRequestId(
                        link = intent.link,
                        downloadRequestId = intent.downloadRequestId,
                    )
                    if (result > 0) {
                        updateDownloadInfoMap(intent.link) {
                            copy(downloadRequestId = intent.downloadRequestId)
                        }
                    }
                }.catch { it.printStackTrace() },

            filterIsInstance<BtDownloadManagerIntent.UpdateTorrentFiles>()
                .onEach { intent -> torrentFileDao.updateTorrentFiles(intent.files) }
                .catch { it.printStackTrace() },

            filterIsInstance<BtDownloadManagerIntent.UpdateDownloadDescription>()
                .sampleWithoutFirst(500)
                .onEach { intent ->
                    val result = downloadInfoDao.updateDownloadDescription(
                        link = intent.link,
                        description = intent.description,
                    )
                    if (result > 0) {
                        updateDownloadInfoMap(intent.link) { copy(description = intent.description) }
                    }
                }
                .catch { it.printStackTrace() },
        )
    }

    fun sendIntent(intent: BtDownloadManagerIntent) = scope.launch {
        intentFlow.emit(intent)
    }

    suspend fun getDownloadInfoList(): Flow<List<BtDownloadInfoBean>> {
        checkDownloadInfoMapInitialized()
        return downloadInfoListFlow
    }

    suspend fun getDownloadInfo(link: String): BtDownloadInfoBean? {
        checkDownloadInfoMapInitialized()
        return downloadInfoMap[link]
    }

    fun getDownloadLinkByUuid(uuid: String): String? {
        return downloadInfoDao.getDownloadLinkByUuid(uuid)
    }

    fun getDownloadUuidByLink(link: String): String? {
        return downloadInfoDao.getDownloadUuidByLink(link)
    }

    fun setDownloadLinkUuidMap(bean: DownloadLinkUuidMapBean) {
        return downloadInfoDao.setDownloadLinkUuidMap(bean)
    }

    suspend fun getDownloadName(link: String): String? {
        checkDownloadInfoMapInitialized()
        return downloadInfoMap[link]?.name
    }

    suspend fun getDownloadProgress(link: String): Float? {
        checkDownloadInfoMapInitialized()
        return downloadInfoMap[link]?.progress
    }

    suspend fun getDownloadState(link: String): DownloadState? {
        checkDownloadInfoMapInitialized()
        return downloadInfoMap[link]?.downloadState
    }

    suspend fun containsDownloadInfo(link: String): Boolean {
        checkDownloadInfoMapInitialized()
        return downloadInfoMap.containsKey(link)
    }

    fun getSessionParams(link: String): SessionParamsBean? {
        return sessionParamsDao.getSessionParams(link)
    }

    fun getTorrentFilesByLink(link: String): List<TorrentFileBean> {
        return torrentFileDao.getTorrentFilesByLink(link = link)
    }

    suspend fun deleteDownloadInfo(link: String): Int {
        checkDownloadInfoMapInitialized()
        removeDownloadInfoFromMap(link)
        return downloadInfoDao.deleteDownloadInfo(link)
    }

    fun deleteSessionParams(link: String): Int {
        return sessionParamsDao.deleteSessionParams(link)
    }

    fun removeDownloadLinkUuidMap(link: String): Int {
        return downloadInfoDao.removeDownloadLinkUuidMap(link)
    }

    // update ---------------------------

    private suspend fun checkDownloadInfoMapInitialized() {
        withContext(Dispatchers.IO) {
            if (!this@BtDownloadManager::downloadInfoMap.isInitialized) {
                initDownloadInfoList()
            }
        }
    }

    private suspend fun initDownloadInfoList() {
        val workManager = WorkManager.getInstance(appContext)
        downloadInfoMap = downloadInfoDao.getAllDownloadListFlow().first().let { list ->
            val newList = list.map {
                when (it.downloadState) {
                    DownloadState.Downloading -> {
                        if (workManager.getWorkInfoById(UUID.fromString(it.downloadRequestId))
                                .get()?.state?.isFinished == true
                        ) return@map it
                        val result = downloadInfoDao.updateDownloadState(
                            link = it.link,
                            downloadState = DownloadState.Paused,
                        )
                        if (result > 0) {
                            it.copy(downloadState = DownloadState.Paused)
                        } else it
                    }

                    DownloadState.Seeding -> {
                        if (workManager.getWorkInfoById(UUID.fromString(it.downloadRequestId))
                                .get()?.state?.isFinished == true
                        ) return@map it
                        val result = downloadInfoDao.updateDownloadState(
                            link = it.link,
                            downloadState = DownloadState.SeedingPaused,
                        )
                        if (result > 0) {
                            it.copy(downloadState = DownloadState.SeedingPaused)
                        } else it
                    }

                    else -> it
                }
            }

            linkedMapOf(*(newList.map { it.link to it }.toTypedArray()))
        }
        downloadInfoListFlow = MutableStateFlow(downloadInfoMap.values.toList())
    }

    private suspend fun updateFlow() {
        checkDownloadInfoMapInitialized()
        downloadInfoListFlow.emit(downloadInfoMap.values.toList())
    }

    private suspend fun putDownloadInfoToMap(
        link: String,
        newInfo: BtDownloadInfoBean,
    ) {
        checkDownloadInfoMapInitialized()
        downloadInfoMap[link] = newInfo
        updateFlow()
    }

    private suspend fun updateDownloadInfoMap(
        link: String,
        newInfo: BtDownloadInfoBean.() -> BtDownloadInfoBean,
    ) {
        checkDownloadInfoMapInitialized()
        val downloadInfo = downloadInfoMap[link] ?: return
        downloadInfoMap[link] = downloadInfo.newInfo()
        updateFlow()
    }

    private suspend fun removeDownloadInfoFromMap(link: String) {
        checkDownloadInfoMapInitialized()
        downloadInfoMap.remove(link)
        updateFlow()
    }

    private suspend fun addFileToMediaLibrary(link: String) {
        val torrents = torrentFileDao.getTorrentFilesByLink(link)
        val savePathDir = torrents.map { it.path }.longestCommonPrefix()
        val needsAdd = torrents.map { File(it.path) }.filter { it.parent == savePathDir }
        needsAdd.forEach { file ->
            with(hiltEntryPoint) {
                val articleId = enclosureDao.getMediaArticleId(link)
                if (articleId != null) {
                    val article = articleDao.getArticleWithFeed(articleId).first()
                    mediaRepository.addNewFile(
                        file = file,
                        groupName = null,
                        articleId = articleId,
                        displayName = article?.articleWithEnclosure?.article?.title
                    ).collect()
                }
            }
        }
    }
}