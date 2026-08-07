package com.skyd.podaura.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skyd.podaura.ext.getOrDefaultSuspend
import com.skyd.podaura.model.bean.playlist.PlaylistMediaWithArticleBean
import com.skyd.podaura.model.preference.behavior.playlist.ReverseLoadArticlePlaylistPreference
import com.skyd.podaura.model.preference.dataStore
import com.skyd.podaura.model.repository.player.PlayerRepository
import com.skyd.podaura.model.repository.playlist.IPlaylistMediaRepository
import com.skyd.podaura.ui.player.jumper.PlayDataMode
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

class PlayerViewModel(
    private val playerRepo: PlayerRepository,
    private val playlistMediaRepo: IPlaylistMediaRepository,
) : ViewModel() {
    // Do not store data
    val mediaInfos = MutableSharedFlow<PlayerLaunchData>(
        replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    fun handlePlayDataMode(
        playDataMode: PlayDataMode,
        requestId: String = Uuid.random().toString(),
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            when (playDataMode) {
                is PlayDataMode.ArticleList -> mediaInfos.emit(
                    PlayerLaunchData(
                        startPath = playDataMode.url,
                        playlist = playerRepo.requestPlaylistByArticleId(
                            articleId = playDataMode.articleId,
                            reverse = dataStore.getOrDefaultSuspend(ReverseLoadArticlePlaylistPreference),
                        ),
                        startPositionSeconds = playDataMode.startPositionSeconds,
                        requestId = requestId,
                    )
                )

                is PlayDataMode.MediaLibraryList -> mediaInfos.emit(
                    PlayerLaunchData(
                        startPath = playDataMode.startMediaPath,
                        playlist = playerRepo.requestPlaylistByMediaLibraryList(playDataMode.mediaList),
                        requestId = requestId,
                    )
                )

                is PlayDataMode.Playlist -> {
                    val playlist =
                        playlistMediaRepo.requestPlaylistMediaList(playDataMode.playlistId).first()
                    val startUrl =
                        playDataMode.mediaUrl ?: playlist.firstOrNull()?.playlistMediaBean?.url
                    mediaInfos.emit(
                        PlayerLaunchData(
                            startPath = startUrl,
                            playlist = playlist,
                            requestId = requestId,
                        )
                    )
                }
            }
        }
    }

    fun handlePlatformFile(
        file: PlatformFile,
        requestId: String = Uuid.random().toString(),
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val playlist =
                playerRepo.requestPlaylistByPlatformFile(file)
            if (playlist != null) {
                mediaInfos.emit(
                    PlayerLaunchData(
                        startPath = playlist[0].playlistMediaBean.url,
                        playlist = playlist,
                        requestId = requestId,
                    )
                )
            }
        }
    }
}

data class PlayerLaunchData(
    val startPath: String?,
    val playlist: List<PlaylistMediaWithArticleBean>,
    val startPositionSeconds: Long? = null,
    val requestId: String,
)
