package com.skyd.anivu.model.repository.download

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.core.net.toUri
import com.skyd.anivu.R
import com.skyd.anivu.appContext
import com.skyd.anivu.config.Const
import com.skyd.anivu.ext.copyTo
import com.skyd.anivu.ext.dataStore
import com.skyd.anivu.ext.fileName
import com.skyd.anivu.ext.getOrDefault
import com.skyd.anivu.ext.isLocal
import com.skyd.anivu.ext.validateFileName
import com.skyd.anivu.model.db.dao.ArticleDao
import com.skyd.anivu.model.db.dao.EnclosureDao
import com.skyd.anivu.model.db.dao.GroupDao
import com.skyd.anivu.model.preference.data.medialib.MediaLibLocationPreference
import com.skyd.anivu.model.repository.download.bt.BtDownloadManager
import com.skyd.anivu.model.repository.media.IMediaRepository
import com.skyd.anivu.model.worker.download.isTorrentMimetype
import com.skyd.anivu.ui.component.showToast
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

object DownloadStarter {
    private val scope = CoroutineScope(Dispatchers.IO)

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerEntryPoint {
        val groupDao: GroupDao
        val articleDao: ArticleDao
        val enclosureDao: EnclosureDao
        val mediaRepository: IMediaRepository
    }

    private val hiltEntryPoint = EntryPointAccessors.fromApplication(
        appContext, WorkerEntryPoint::class.java
    )

    fun download(context: Context, url: String, type: String? = null) = scope.launch {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted =
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            if (granted == PermissionChecker.PERMISSION_DENIED) {
                context.getString(R.string.download_no_notification_permission_tip)
                    .showToast()
                return@launch
            }
        }
        val articleId = hiltEntryPoint.enclosureDao.getMediaArticleId(url)
        val article = articleId?.let { hiltEntryPoint.articleDao.getArticleWithFeed(it).first() }
        val group = article?.feed?.groupId?.let { hiltEntryPoint.groupDao.getGroupById(it) }
        val saveDir = hiltEntryPoint.mediaRepository.getFolder(
            parentFile = File(context.dataStore.getOrDefault(MediaLibLocationPreference)),
            groupName = group?.name,
            feedUrl = article?.feed?.url,
            displayName = article?.feed?.title,
        ).first().path
        val isMagnetOrTorrent =
            url.startsWith("magnet:") || isTorrentMimetype(type) ||
                    Regex("^(((http|https|file|content)://)|/).*\\.torrent$").matches(url)
        if (isMagnetOrTorrent) {
            var uri = url.toUri()
            if (url.startsWith("/")) {
                uri = uri.buildUpon().scheme("file").build()
            }
            if (uri.isLocal()) {
                val newUrl = File(Const.TEMP_TORRENT_DIR, uri.fileName() ?: url.validateFileName())
                if (uri.scheme == "content") {
                    if (uri.copyTo(newUrl) > 0) {
                        BtDownloadManager.download(context, newUrl.path, saveDir, requestId = null)
                    }
                } else {
                    File(url).copyTo(newUrl)
                    BtDownloadManager.download(context, newUrl.path, saveDir, requestId = null)
                }
            } else {
                BtDownloadManager.download(context, url, saveDir, requestId = null)
            }
        } else {
            DownloadManager.getInstance(context).download(
                url = url,
                path = saveDir,
            )
        }
    }
}