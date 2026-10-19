package com.skyd.podaura.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skyd.podaura.ext.getOrDefaultSuspend
import com.skyd.podaura.model.bean.playlist.PlaylistMediaWithArticleBean
import com.skyd.podaura.model.preference.behavior.calendar.CalendarHideMutedArticlePreference
import com.skyd.podaura.model.preference.behavior.playlist.ReverseLoadArticlePlaylistPreference
import com.skyd.podaura.model.preference.dataStore
import com.skyd.podaura.model.preference.rss.ParseLinkTagAsEnclosurePreference
import com.skyd.podaura.model.repository.player.PlayerRepository
import com.skyd.podaura.model.repository.playlist.IPlaylistMediaRepository
import com.skyd.podaura.ui.player.jumper.ArticlePlaylistSource
import com.skyd.podaura.ui.player.jumper.PlayDataMode
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
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

    fun handlePlayDataMode(
        playDataMode: PlayDataMode,
        requestId: String = Uuid.random().toString(),
        onPrepared: (PlayerLaunchData) -> Unit,
        onFailure: (Throwable) -> Unit,
    ) {
        loadJob?.cancel()
        loadJob = launchPreparation(onFailure) {
            publish(onPrepared = onPrepared) {
                when (playDataMode) {
                    is PlayDataMode.ArticleList -> PlayerLaunchData(
                        startPath = playDataMode.url,
                        playlist = when (val source = playDataMode.playlistSource) {
                            ArticlePlaylistSource.Subscription -> playerRepo.requestPlaylistByArticleId(
                                articleId = playDataMode.articleId,
                                reverse = dataStore.getOrDefaultSuspend(
                                    ReverseLoadArticlePlaylistPreference
                                ),
                            )

                            is ArticlePlaylistSource.CalendarDay -> playerRepo.requestPlaylistByCalendarDay(
                                day = source.day,
                                excludeMuted = dataStore.getOrDefaultSuspend(
                                    CalendarHideMutedArticlePreference
                                ),
                                includeMediaLinks = dataStore.getOrDefaultSuspend(
                                    ParseLinkTagAsEnclosurePreference
                                ),
                            )
                        },
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

    private fun launchPreparation(
        onFailure: (Throwable) -> Unit,
        prepare: suspend () -> Unit,
    ): Job = viewModelScope.launch(Dispatchers.IO) {
        try {
            prepare()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            withContext(Dispatchers.Main.immediate) { onFailure(error) }
        }
    }

    private suspend fun publish(
        batch: ExternalMediaBatch? = null,
        onPrepared: (PlayerLaunchData) -> Unit,
        prepare: suspend () -> PlayerLaunchData,
    ) {
        var transferred = false
        var preparedBatch = batch
        try {
            val prepared = prepare()
            val data = if (batch == null) preparePlatformPlayback(prepared) else prepared
            preparedBatch = data.externalBatch
            check(data.playlist.isNotEmpty()) { "No playable media" }
            withContext(Dispatchers.Main.immediate) {
                currentCoroutineContext().ensureActive()
                onPrepared(data)
                externalBatch?.release()
                externalBatch = preparedBatch
                transferred = true
            }
        } finally {
            if (!transferred) preparedBatch?.release()
        }
    }

    fun handlePlatformFiles(
        files: List<PlatformFile>,
        requestId: String = Uuid.random().toString(),
        onPrepared: (PlayerLaunchData) -> Unit,
        onFailure: (Throwable) -> Unit,
    ) {
        loadJob?.cancel()
        loadJob = launchPreparation(onFailure) {
            val batch = resolveExternalMediaBatch(files, { it.path }, ::resolveExternalMedia)
            publish(batch, onPrepared) {
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

    fun clearPendingPlayback() {
        loadJob?.cancel()
        externalBatch?.release()
        externalBatch = null
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

internal expect suspend fun preparePlatformPlayback(data: PlayerLaunchData): PlayerLaunchData
