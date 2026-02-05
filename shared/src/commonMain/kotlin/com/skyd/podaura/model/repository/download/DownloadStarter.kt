package com.skyd.podaura.model.repository.download

import androidx.compose.runtime.Composable
import com.skyd.fundation.di.get
import com.skyd.podaura.ext.getOrDefault
import com.skyd.podaura.model.db.dao.ArticleDao
import com.skyd.podaura.model.db.dao.EnclosureDao
import com.skyd.podaura.model.db.dao.GroupDao
import com.skyd.podaura.model.preference.data.medialib.MediaLibLocationPreference
import com.skyd.podaura.model.preference.dataStore
import com.skyd.podaura.model.repository.media.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path

abstract class DownloadStarter {
    open suspend fun download(url: String, type: String? = null) {
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
expect fun rememberDownloadStarter(): DownloadStarter
