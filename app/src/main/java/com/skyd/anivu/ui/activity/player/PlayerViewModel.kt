package com.skyd.anivu.ui.activity.player

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skyd.anivu.model.bean.playlist.PlaylistMediaWithArticleBean
import com.skyd.anivu.model.repository.player.PlayDataMode
import com.skyd.anivu.model.repository.player.PlayerRepository
import com.skyd.anivu.model.repository.playlist.PlaylistMediaRepository
import com.skyd.anivu.ui.activity.player.PlayActivity.Companion.PLAY_DATA_MODE_KEY
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
) : ViewModel() {
    // Do not store data
    val mediaInfos = MutableSharedFlow<Pair<String?, List<PlaylistMediaWithArticleBean>>>(
        replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    fun handleIntent(intent: Intent?) {
        intent ?: return

        viewModelScope.launch(Dispatchers.IO) {
            when (val playDataMode = intent.getParcelableExtra<PlayDataMode>(PLAY_DATA_MODE_KEY)) {
                is PlayDataMode.ArticleList -> mediaInfos.emit(
                    playDataMode.url to playerRepo.requestPlaylistByArticleId(playDataMode.articleId)
                )

                is PlayDataMode.MediaLibraryList -> mediaInfos.emit(
                    playDataMode.startMediaPath to
                            playerRepo.requestPlaylistByMediaLibraryList(playDataMode.mediaList)
                )

                is PlayDataMode.Playlist -> {
                    val playlist =
                        playlistMediaRepo.requestPlaylistMediaList(playDataMode.playlistId).first()
                    val startUrl =
                        playDataMode.mediaUrl ?: playlist.firstOrNull()?.playlistMediaBean?.url
                    mediaInfos.emit(startUrl to playlist)
                }

                null -> {
                    val externalUri = intent.data
                    if (externalUri != null) {
                        val playlist = playerRepo.requestPlaylistByUri(externalUri)
                        if (playlist != null) {
                            mediaInfos.emit(playlist[0].playlistMediaBean.url to playlist)
                        }
                    }
                }
            }
        }
    }
}