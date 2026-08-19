package com.skyd.podaura.model.repository.download

import androidx.compose.runtime.Composable
import com.skyd.fundation.di.get
import com.skyd.podaura.ext.getOrDefault
import com.skyd.podaura.model.db.dao.ArticleDao
import com.skyd.podaura.model.db.dao.EnclosureDao
import com.skyd.podaura.model.db.dao.FeedDao
import com.skyd.podaura.model.db.dao.GroupDao
import com.skyd.podaura.model.download.ArticleDownloadSource
import com.skyd.podaura.model.preference.data.medialib.MediaLibLocationPreference
import com.skyd.podaura.model.preference.dataStore
import com.skyd.podaura.model.repository.media.MediaRepository
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

abstract class DownloadStarter {
    open suspend fun download(
        url: String,
        type: String? = null,
        articleDownloadSource: ArticleDownloadSource? = null,
    ) {
        withContext(Dispatchers.IO) {
            val articleId = articleDownloadSource?.articleId
                ?: get<EnclosureDao>().getMediaArticleId(url)
            val article =
                articleId?.let { get<ArticleDao>().getArticleWithFeed(it).first() }
            val feed = article?.feed
                ?: articleDownloadSource?.feedUrl?.let { get<FeedDao>().getFeed(it) }
            val group = feed?.groupId?.let { get<GroupDao>().getGroupById(it) }
            val saveDir = get<MediaRepository>().getFolder(
                parentFile = PlatformFile(dataStore.getOrDefault(MediaLibLocationPreference)),
                groupName = group?.name,
                feedUrl = feed?.url,
                displayName = feed?.title,
            ).first().path
            if (url.startsWith("magnet:")) {
                // todo open link
            } else {
                get<IDownloadManager>().download(
                    url = url,
                    path = saveDir,
                    articleDownloadSource = articleDownloadSource,
                )
            }
        }
    }
}

@Composable
expect fun rememberDownloadStarter(): DownloadStarter
