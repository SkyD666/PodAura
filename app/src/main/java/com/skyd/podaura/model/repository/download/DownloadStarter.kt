package com.skyd.podaura.model.repository.download

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.skyd.podaura.di.get
import com.skyd.podaura.ext.getOrDefault
import com.skyd.podaura.ext.getString
import com.skyd.podaura.model.db.dao.ArticleDao
import com.skyd.podaura.model.db.dao.EnclosureDao
import com.skyd.podaura.model.db.dao.GroupDao
import com.skyd.podaura.model.preference.data.medialib.MediaLibLocationPreference
import com.skyd.podaura.model.preference.dataStore
import com.skyd.podaura.model.repository.media.MediaRepository
import com.skyd.podaura.ui.component.showToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.io.files.Path
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.download_no_notification_permission_tip

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
        if (url.startsWith("magnet:")) {
            // todo open link
        } else {
            DownloadManager.getInstance(context).download(
                url = url,
                path = saveDir,
            )
        }
    }
}