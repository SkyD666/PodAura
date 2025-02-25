package com.skyd.anivu.ui.activity.player

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skyd.anivu.appContext
import com.skyd.anivu.model.db.dao.ArticleDao
import com.skyd.anivu.model.db.dao.EnclosureDao
import com.skyd.anivu.model.repository.player.PlayerRepository
import com.skyd.anivu.ui.activity.player.PlayActivity.Companion.ARTICLE_ID_KEY
import com.skyd.anivu.ui.activity.player.PlayActivity.Companion.MEDIA_LIST_KEY
import com.skyd.anivu.ui.activity.player.PlayActivity.Companion.START_MEDIA_KEY
import com.skyd.anivu.ui.activity.player.PlayActivity.Companion.URL_KEY
import com.skyd.anivu.ui.activity.player.PlayActivity.PlayMediaListItem
import com.skyd.anivu.ui.mpv.resolveUri
import com.skyd.anivu.ui.mpv.service.CustomMediaData
import com.skyd.anivu.ui.mpv.service.PlaylistBean
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerRepo: PlayerRepository,
    private val articleDao: ArticleDao,
    private val enclosureDao: EnclosureDao,
) : ViewModel() {
    // Do not store data
    val mediaInfos = MutableSharedFlow<Pair<String?, List<PlaylistBean>>>(
        replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    fun handleIntent(intent: Intent?) {
        intent ?: return

        viewModelScope.launch(Dispatchers.IO) {
            val startMedia = intent.getStringExtra(START_MEDIA_KEY)
            if (startMedia != null) {
                val files: List<PlayMediaListItem> =
                    intent.getParcelableArrayListExtra(MEDIA_LIST_KEY) ?: return@launch
                val articleMap = articleDao.getArticleListByIds(files.mapNotNull { it.articleId })
                    .associateBy { it.articleWithEnclosure.article.articleId }
                mediaInfos.emit(
                    startMedia to files.map { playMediaListItem ->
                        PlaylistBean(
                            path = playMediaListItem.path,
                            customMediaData = articleMap[playMediaListItem.articleId]?.let {
                                CustomMediaData.fromArticleWithFeed(it)
                            } ?: CustomMediaData(
                                title = playMediaListItem.title,
                                thumbnail = playMediaListItem.thumbnail,
                            )
                        )
                    }
                )
            } else {
                val url = intent.getStringExtra(URL_KEY)
                val articleId = intent.getStringExtra(ARTICLE_ID_KEY) ?: url?.let {
                    enclosureDao.getMediaArticleId(it)
                }
                if (articleId == null) {
                    if (url != null) {
                        mediaInfos.emit(
                            url to listOf(
                                PlaylistBean(path = url, customMediaData = CustomMediaData())
                            )
                        )
                    } else {
                        val externalUri = intent.data
                        if (externalUri != null) {
                            externalUri.resolveUri(appContext)?.let { path ->
                                mediaInfos.emit(
                                    path to listOf(
                                        PlaylistBean(
                                            path = path,
                                            customMediaData = CustomMediaData()
                                        )
                                    )
                                )
                            }
                        }
                    }
                } else if (url != null) {
                    mediaInfos.emit(
                        url to playerRepo.requestPlaylist(articleId).map { articleWithFeed ->
                            val enclosures = articleWithFeed.articleWithEnclosure.enclosures
                            enclosures.map { enclosure ->
                                PlaylistBean(
                                    path = enclosure.url,
                                    customMediaData = CustomMediaData.fromArticleWithFeed(
                                        articleWithFeed
                                    ),
                                )
                            }
                        }.flatten()
                    )
                }
            }
        }
    }
}