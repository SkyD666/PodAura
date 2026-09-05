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
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.uuid.Uuid

class PlayerViewModel(
    private val playerRepo: PlayerRepository,
    private val playlistMediaRepo: IPlaylistMediaRepository,
) : ViewModel() {
    private var loadJob: Job? = null
    private var externalBatch: ExternalMediaBatch? = null

    // Do not store data
    val mediaInfos = MutableSharedFlow<PlayerLaunchData>(
        replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    fun handlePlayDataMode(
        playDataMode: PlayDataMode,
        requestId: String = Uuid.random().toString(),
    ) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch(Dispatchers.IO) {
            publish {
                when (playDataMode) {
                    is PlayDataMode.ArticleList -> PlayerLaunchData(
                        startPath = playDataMode.url,
                        playlist = playerRepo.requestPlaylistByArticleId(
                            articleId = playDataMode.articleId,
                            reverse = dataStore.getOrDefaultSuspend(
                                ReverseLoadArticlePlaylistPreference
                            ),
                        ),
                        startPositionSeconds = playDataMode.startPositionSeconds,
                        requestId = requestId,
                    )

                    is PlayDataMode.MediaLibraryList -> {
                        val playlist =
                            playerRepo.requestPlaylistByMediaLibraryList(playDataMode.mediaList)
                        val startPath = playlist.firstOrNull {
                            it.playlistMediaBean.stableUrl == playDataMode.startMediaPath
                        }?.playlistMediaBean?.url
                        PlayerLaunchData(
                            startPath = startPath,
                            playlist = playlist,
                            requestId = requestId,
                        )
                    }

                    is PlayDataMode.Playlist -> {
                        val playlist = playlistMediaRepo
                            .requestPlaylistMediaList(playDataMode.playlistId).first()
                        val startUrl =
                            playDataMode.mediaUrl ?: playlist.firstOrNull()?.playlistMediaBean?.url
                        PlayerLaunchData(
                            startPath = startUrl,
                            playlist = playlist,
                            requestId = requestId,
                        )
                    }
                }
            }
        }
    }

    private suspend fun publish(
        batch: ExternalMediaBatch? = null,
        prepare: suspend () -> PlayerLaunchData,
    ) {
        var transferred = false
        try {
            val data = prepare()
            withContext(Dispatchers.Main.immediate) {
                currentCoroutineContext().ensureActive()
                mediaInfos.emit(data)
                externalBatch?.release()
                externalBatch = batch
                transferred = true
            }
        } finally {
            if (!transferred) batch?.release()
        }
    }

    fun handlePlatformFiles(
        files: List<PlatformFile>,
        requestId: String = Uuid.random().toString(),
    ) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch(Dispatchers.IO) {
            val batch = resolveExternalMediaBatch(files, { it.path }, ::resolveExternalMedia)
            publish(batch) {
                val playlist = batch.media.mapIndexed { index, media ->
                    PlaylistMediaWithArticleBean.fromUrl(
                        playlistId = "",
                        url = media.playbackUrl,
                        orderPosition = index.toDouble(),
                    ).apply { playlistMediaBean.sourceUrl = media.source }
                }
                PlayerLaunchData(
                    startPath = playlist.firstOrNull()?.playlistMediaBean?.url,
                    playlist = playlist,
                    requestId = requestId,
                    externalBatch = batch,
                )
            }
        }
    }

    override fun onCleared() {
        clearPendingPlayback()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun clearPendingPlayback() {
        loadJob?.cancel()
        externalBatch?.release()
        externalBatch = null
        mediaInfos.resetReplayCache()
    }
}

data class PlayerLaunchData(
    val startPath: String?,
    val playlist: List<PlaylistMediaWithArticleBean>,
    val startPositionSeconds: Long? = null,
    val requestId: String,
    val externalBatch: ExternalMediaBatch? = null,
) {
    fun toLoadCommand() = PlayerCommand.LoadList(
        playlist = playlist,
        startPath = startPath,
        startPositionSeconds = startPositionSeconds,
        requestId = requestId,
        externalBatch = externalBatch,
    )
}
