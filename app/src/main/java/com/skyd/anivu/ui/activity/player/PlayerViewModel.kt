package com.skyd.anivu.ui.activity.player

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skyd.anivu.appContext
import com.skyd.anivu.model.db.dao.EnclosureDao
import com.skyd.anivu.model.repository.player.PlayerRepository
import com.skyd.anivu.ui.activity.player.PlayActivity.Companion.ARTICLE_ID_KEY
import com.skyd.anivu.ui.activity.player.PlayActivity.Companion.FILES_KEY
import com.skyd.anivu.ui.activity.player.PlayActivity.Companion.START_FILE_KEY
import com.skyd.anivu.ui.activity.player.PlayActivity.Companion.URL_KEY
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
    private val enclosureDao: EnclosureDao,
) : ViewModel() {
    // Do not store data
    val mediaInfos = MutableSharedFlow<Pair<String?, List<PlaylistBean>>>(
        replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    fun handleIntent(intent: Intent?) {
        intent ?: return

        viewModelScope.launch(Dispatchers.IO) {
            val url = intent.getStringExtra(URL_KEY)
            val articleId = intent.getStringExtra(ARTICLE_ID_KEY) ?: url?.let {
                enclosureDao.getMediaArticleId(it)
            }
            if (articleId == null) {
                val externalUri = intent.data
                if (externalUri == null) {
                    val startFilePath = intent.getStringExtra(START_FILE_KEY)
                    if (startFilePath != null) {
                        val files =
                            intent.getStringArrayListExtra(FILES_KEY) ?: listOf(startFilePath)
                        mediaInfos.emit(
                            startFilePath to files.map {
                                PlaylistBean(path = it, customMediaData = CustomMediaData())
                            }
                        )
                    } else if (url != null) {
                        mediaInfos.emit(
                            url to listOf(
                                PlaylistBean(
                                    path = url,
                                    customMediaData = CustomMediaData()
                                )
                            )
                        )
                    }
                } else {
                    externalUri.resolveUri(appContext)?.let { path ->
                        mediaInfos.emit(
                            path to listOf(
                                PlaylistBean(path = path, customMediaData = CustomMediaData())
                            )
                        )
                    }
                }
            } else if (url != null) {
                mediaInfos.emit(
                    url to playerRepo.requestPlaylist(articleId).map { articleWithFeed ->
                        val articleWithEnclosure = articleWithFeed.articleWithEnclosure
                        val enclosures = articleWithEnclosure.enclosures
                        val article = articleWithFeed.articleWithEnclosure.article
                        enclosures.map { enclosure ->
                            PlaylistBean(
                                path = enclosure.url,
                                customMediaData = CustomMediaData(
                                    articleId = article.articleId,
                                    title = article.title,
                                    thumbnail = articleWithEnclosure.media?.image
                                        ?: articleWithFeed.feed.icon,
                                    artist = article.author.orEmpty().ifEmpty {
                                        articleWithFeed.feed.title
                                    }
                                ),
                            )
                        }
                    }.flatten()
                )
            }
        }
    }
}