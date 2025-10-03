package com.skyd.podaura.ui.activity.player

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skyd.podaura.ext.getOrDefaultSuspend
import com.skyd.podaura.model.bean.playlist.PlaylistMediaWithArticleBean
import com.skyd.podaura.model.preference.behavior.playlist.ReverseLoadArticlePlaylistPreference
import com.skyd.podaura.model.preference.dataStore
import com.skyd.podaura.model.repository.player.PlayerRepository
import com.skyd.podaura.model.repository.playlist.IPlaylistMediaRepository
import com.skyd.podaura.ui.player.jumper.PLAY_DATA_MODE_KEY
import com.skyd.podaura.ui.player.jumper.PlayDataMode
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val playerRepo: PlayerRepository,
    private val playlistMediaRepo: IPlaylistMediaRepository,
) : ViewModel() {
    // Do not store data
    val mediaInfos = MutableSharedFlow<Pair<String?, List<PlaylistMediaWithArticleBean>>>(
        replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    fun handleIntent(intent: Intent?) {
        intent ?: return

        viewModelScope.launch(Dispatchers.IO) {
            when (val playDataMode = intent.getStringExtra(PLAY_DATA_MODE_KEY)?.let {
                PlayDataMode.decodeFromString(it)
            }) {
                is PlayDataMode.ArticleList -> mediaInfos.emit(
                    playDataMode.url to playerRepo.requestPlaylistByArticleId(
                        articleId = playDataMode.articleId,
                        reverse = dataStore.getOrDefaultSuspend(ReverseLoadArticlePlaylistPreference),
                    )
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
                        val playlist =
                            playerRepo.requestPlaylistByPlatformFile(PlatformFile(externalUri))
                        if (playlist != null) {
                            mediaInfos.emit(playlist[0].playlistMediaBean.url to playlist)
                        }
                    }
                }
            }
        }
    }
}