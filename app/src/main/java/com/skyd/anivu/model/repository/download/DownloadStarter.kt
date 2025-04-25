package com.skyd.anivu.model.repository.download

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.core.net.toUri
import com.skyd.anivu.config.Const
import com.skyd.anivu.config.TEMP_TORRENT_DIR
import com.skyd.anivu.di.get
import com.skyd.anivu.ext.copyTo
import com.skyd.anivu.ext.fileName
import com.skyd.anivu.ext.getOrDefault
import com.skyd.anivu.ext.getString
import com.skyd.anivu.ext.isLocal
import com.skyd.anivu.ext.validateFileName
import com.skyd.anivu.model.db.dao.ArticleDao
import com.skyd.anivu.model.db.dao.EnclosureDao
import com.skyd.anivu.model.db.dao.GroupDao
import com.skyd.anivu.model.preference.data.medialib.MediaLibLocationPreference
import com.skyd.anivu.model.preference.dataStore
import com.skyd.anivu.model.repository.download.bt.BtDownloadManager
import com.skyd.anivu.model.repository.media.MediaRepository
import com.skyd.anivu.model.worker.download.isTorrentMimetype
import com.skyd.anivu.ui.component.showToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.io.files.Path
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.download_no_notification_permission_tip
import java.io.File

object DownloadStarter {
    private val scope = CoroutineScope(Dispatchers.IO)

    fun download(context: Context, url: String, type: String? = null) = scope.launch {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted =
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            if (granted == PermissionChecker.PERMISSION_DENIED) {
                context.getString(Res.string.download_no_notification_permission_tip).showToast()
                return@launch
            }
        }
        val articleId = get<EnclosureDao>().getMediaArticleId(url)
        val article = articleId?.let { get<ArticleDao>().getArticleWithFeed(it).first() }
        val group = article?.feed?.groupId?.let { get<GroupDao>().getGroupById(it) }
        val saveDir = get<MediaRepository>().getFolder(
            parentFile = Path(dataStore.getOrDefault(MediaLibLocationPreference)),
            groupName = group?.name,
            feedUrl = article?.feed?.url,
            displayName = article?.feed?.title,
        ).first().toString()
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