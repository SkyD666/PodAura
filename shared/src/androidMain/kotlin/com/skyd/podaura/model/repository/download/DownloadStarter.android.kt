package com.skyd.podaura.model.repository.download

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.skyd.fundation.di.get
import com.skyd.podaura.ext.getOrDefault
import com.skyd.podaura.model.db.dao.ArticleDao
import com.skyd.podaura.model.db.dao.EnclosureDao
import com.skyd.podaura.model.db.dao.GroupDao
import com.skyd.podaura.model.preference.data.medialib.MediaLibLocationPreference
import com.skyd.podaura.model.preference.dataStore
import com.skyd.podaura.model.repository.media.MediaRepository
import com.skyd.podaura.ui.component.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import org.jetbrains.compose.resources.getString
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.download_no_notification_permission_tip

class AndroidDownloadStarter(private val context: Context) : DownloadStarter() {
    override suspend fun download(url: String, type: String?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            )
            if (granted == PermissionChecker.PERMISSION_DENIED) {
                getString(Res.string.download_no_notification_permission_tip).showToast()
                return
            }
        }
        withContext(Dispatchers.IO) {
            val articleId = get<EnclosureDao>().getMediaArticleId(url)
            val article =
                articleId?.let { get<ArticleDao>().getArticleWithFeed(it).first() }
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
                get<IDownloadManager>().download(
                    url = url,
                    path = saveDir,
                )
            }
        }
    }
}

@Composable
actual fun rememberDownloadStarter(): DownloadStarter {
    val context = LocalContext.current
    return remember(context) { AndroidDownloadStarter(context) }
}