package com.skyd.anivu.ui.activity.player

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skyd.anivu.appContext
import com.skyd.anivu.model.bean.playlist.PlaylistMediaBean
import com.skyd.anivu.model.bean.playlist.PlaylistMediaWithArticleBean
import com.skyd.anivu.model.db.dao.ArticleDao
import com.skyd.anivu.model.repository.player.PlayerRepository
import com.skyd.anivu.model.repository.playlist.PlaylistMediaRepository
import com.skyd.anivu.ui.activity.player.PlayActivity.Companion.PLAY_DATA_MODE_KEY
import com.skyd.anivu.ui.mpv.resolveUri
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerRepo: PlayerRepository,
    private val playlistMediaRepo: PlaylistMediaRepository,
    private val articleDao: ArticleDao,
) : ViewModel() {
    // Do not store data
    val mediaInfos = MutableSharedFlow<Pair<String?, List<PlaylistMediaWithArticleBean>>>(
        replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    fun handleIntent(intent: Intent?) {
        intent ?: return

        viewModelScope.launch(Dispatchers.IO) {
            val playDataMode =
                intent.getParcelableExtra<PlayActivity.PlayDataMode>(PLAY_DATA_MODE_KEY)
            when (playDataMode) {
                is PlayActivity.PlayDataMode.ArticleList -> {
                    val url = playDataMode.url
                    val articleId = playDataMode.articleId
                    mediaInfos.emit(
                        url to playerRepo.requestPlaylist(articleId).map { articleWithFeed ->
                            val enclosures = articleWithFeed.articleWithEnclosure.enclosures
                            enclosures.mapIndexed { index, enclosure ->
                                PlaylistMediaWithArticleBean(
                                    playlistMediaBean = PlaylistMediaBean(
                                        playlistId = "",
                                        url = enclosure.url,
                                        articleId = articleWithFeed.articleWithEnclosure.article.articleId,
                                        orderPosition = index.toDouble(),
                                        createTime = System.currentTimeMillis(),
                                    ),
                                    article = articleWithFeed,
                                )
                            }
                        }.flatten()
                    )
                }

                is PlayActivity.PlayDataMode.MediaLibraryList -> {
                    val files = playDataMode.mediaList
                    val startMedia = playDataMode.startMediaPath
                    val articleMap =
                        articleDao.getArticleListByIds(files.mapNotNull { it.articleId })
                            .associateBy { it.articleWithEnclosure.article.articleId }
                    mediaInfos.emit(
                        startMedia to files.mapIndexed { index, playMediaListItem ->
                            PlaylistMediaWithArticleBean(
                                playlistMediaBean = PlaylistMediaBean(
                                    playlistId = "",
                                    url = playMediaListItem.path,
                                    articleId = articleMap[playMediaListItem.articleId]?.articleWithEnclosure?.article?.articleId,
                                    orderPosition = index.toDouble(),
                                    createTime = System.currentTimeMillis(),
                                ).apply { updateLocalMediaMetadata() },
                                article = articleMap[playMediaListItem.articleId],
                            )
                        }
                    )
                }

                is PlayActivity.PlayDataMode.Playlist -> {
                    val playlist =
                        playlistMediaRepo.requestPlaylistMediaList(playDataMode.playlistId).first()
                    val startUrl =
                        playDataMode.mediaUrl ?: playlist.firstOrNull()?.playlistMediaBean?.url
                    mediaInfos.emit(startUrl to playlist)
                }

                null -> {
                    val externalUri = intent.data
                    if (externalUri != null) {
                        externalUri.resolveUri(appContext)?.let { path ->
                            mediaInfos.emit(
                                path to listOf(
                                    PlaylistMediaWithArticleBean.fromUrl(
                                        playlistId = "",
                                        url = path,
                                        orderPosition = 1.0,
                                    )
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}