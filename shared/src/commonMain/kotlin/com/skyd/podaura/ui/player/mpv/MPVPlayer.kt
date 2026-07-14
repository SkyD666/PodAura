package com.skyd.podaura.ui.player.mpv

import androidx.compose.ui.input.key.KeyEvent
import co.touchlab.kermit.Logger
import coil3.Bitmap
import com.skyd.compone.component.blockString
import com.skyd.fundation.config.Const
import com.skyd.fundation.config.MPV_FONT_DIR
import com.skyd.fundation.config.TEMP_PICTURES_DIR
import com.skyd.podaura.ext.asPlatformFile
import com.skyd.podaura.ext.getOrDefault
import com.skyd.podaura.model.preference.dataStore
import com.skyd.podaura.model.preference.player.HardwareDecodePreference
import com.skyd.podaura.model.preference.player.MpvCacheDirPreference
import com.skyd.podaura.model.preference.player.MpvConfigDirPreference
import com.skyd.podaura.model.preference.player.PlayerMaxBackCacheSizePreference
import com.skyd.podaura.model.preference.player.PlayerMaxCacheSizePreference
import com.skyd.podaura.model.preference.player.PlayerSeekOptionPreference
import com.skyd.podaura.ui.player.DefaultEventObserver
import com.skyd.podaura.ui.player.Track
import com.skyd.podaura.ui.player.land.controller.bar.toDurationString
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.exists
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.track_off
import podaura.shared.generated.resources.ui_track
import podaura.shared.generated.resources.ui_track_text
import podaura.shared.generated.resources.ui_track_title_lang
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.math.log
import kotlin.random.Random
import kotlin.reflect.KProperty
import kotlin.time.Duration.Companion.milliseconds

class MPVPlayer : DefaultEventObserver() {
    companion object {
        private const val TAG = "MPVPlayer"

        // resolution (px) of the thumbnail
        private const val THUMB_SIZE = 512

        private val initialized = AtomicBoolean(false)
        val instance: MPVPlayer by lazy {
            MPVPlayer().apply {
                initialize(
                    configDir = dataStore.getOrDefault(MpvConfigDirPreference),
                    cacheDir = dataStore.getOrDefault(MpvCacheDirPreference),
                    fontDir = Const.MPV_FONT_DIR,
                )
            }
        }
    }

    private val logger = Logger.withTag(TAG)
    lateinit var mpv: MPV
        private set
    private val scope = CoroutineScope(Dispatchers.IO)
    val initialized = Companion.initialized.load()

    fun initialize(
        configDir: String,
        cacheDir: String,
        fontDir: String,
        vo: String = "gpu",
    ) {
        if (!Companion.initialized.compareAndSet(expectedValue = false, newValue = true)) {
            return
        }
        copyAssetsForMpv(configDir)

        mpv = platformMPV()
        mpv.initialize()
        mpv.option("config", "yes")
        mpv.option("config-dir", configDir)
        for (opt in arrayOf("gpu-shader-cache-dir", "icc-cache-dir")) {
            mpv.option(opt, cacheDir)
        }
        initOptions(vo)
        /* Hardcoded options: */
        // we need to call write-watch-later manually
        mpv.option("save-position-on-quit", "no")
        // would crash before the surface is attached
        mpv.option("force-window", "no")
        // "no" wouldn't work and "yes" is not intended by the UI
        mpv.option("idle", "yes")
        mpv.setPropertyString("sub-fonts-dir", fontDir)
        mpv.setPropertyString("osd-fonts-dir", fontDir)

        observeProperties()

        mpv.addEventListener(this)
    }

    var voInUse: String = ""
        private set

    private fun initOptions(vo: String) {
        // apply phone-optimized defaults
        mpv.option("profile", "fast")

        // vo
        voInUse = vo

        mpv.initOptionsPlatform(logger)
        mpv.option("video-sync", "audio")

        mpv.option("opengl-es", "yes")
        mpv.option(
            "hwdec",
            if (dataStore.getOrDefault(HardwareDecodePreference)) "auto" else "no"
        )
        mpv.option("input-default-bindings", "yes")
        // Limit demuxer cache since the defaults are too high for mobile devices
        mpv.option(
            "demuxer-max-bytes",
            dataStore.getOrDefault(PlayerMaxCacheSizePreference).toString(),
        )
        mpv.option(
            "demuxer-max-back-bytes",
            dataStore.getOrDefault(PlayerMaxBackCacheSizePreference).toString(),
        )

        mpv.option("screenshot-directory", Const.TEMP_PICTURES_DIR)
    }

    // Called when back button is pressed, or app is shutting down
    fun destroy() {
        if (Companion.initialized.compareAndSet(expectedValue = true, newValue = false)) {
            mpv.destroy()
            mpv.removeEventListener(this)
        }
    }

    fun onKey(event: KeyEvent): Boolean {
        return mpv.onKey(event, logger)
    }

    private fun observeProperties() {
        // This observes all properties needed by MPVView, MPVActivity or other classes
        data class Property(val name: String, val format: MPVFormat = MPVFormat.MPV_FORMAT_NONE)

        val p = arrayOf(
            Property("time-pos", MPVFormat.MPV_FORMAT_INT64),
            Property("duration", MPVFormat.MPV_FORMAT_INT64),
            Property("demuxer-cache-time", MPVFormat.MPV_FORMAT_INT64),
            Property("video-rotate", MPVFormat.MPV_FORMAT_INT64),
            Property("paused-for-cache", MPVFormat.MPV_FORMAT_FLAG),
            Property("core-idle", MPVFormat.MPV_FORMAT_FLAG),
            Property("demuxer-cache-idle", MPVFormat.MPV_FORMAT_FLAG),
            Property("seeking", MPVFormat.MPV_FORMAT_FLAG),
            Property("pause", MPVFormat.MPV_FORMAT_FLAG),
            Property("eof-reached", MPVFormat.MPV_FORMAT_FLAG),
            Property("idle-active", MPVFormat.MPV_FORMAT_FLAG),
            Property("aid", MPVFormat.MPV_FORMAT_INT64),
            Property("sid", MPVFormat.MPV_FORMAT_INT64),
            Property("track-list"),
            Property("video-zoom", MPVFormat.MPV_FORMAT_DOUBLE),
            Property("video-params/aspect", MPVFormat.MPV_FORMAT_DOUBLE),
            Property("video-pan-x", MPVFormat.MPV_FORMAT_DOUBLE),
            Property("video-pan-y", MPVFormat.MPV_FORMAT_DOUBLE),
            Property("audio-delay", MPVFormat.MPV_FORMAT_DOUBLE),
            Property("sub-delay", MPVFormat.MPV_FORMAT_DOUBLE),
            Property("speed", MPVFormat.MPV_FORMAT_DOUBLE),
            Property("demuxer-cache-duration", MPVFormat.MPV_FORMAT_DOUBLE),
            Property("playlist"),
            Property("playlist-pos", MPVFormat.MPV_FORMAT_INT64),
            Property("playlist-count", MPVFormat.MPV_FORMAT_INT64),
            Property("video-format"),
            Property("media-title", MPVFormat.MPV_FORMAT_STRING),
            Property("metadata"),
            Property("loop-playlist"),
            Property("loop-file"),
            Property("shuffle", MPVFormat.MPV_FORMAT_FLAG),
            Property("hwdec-current")
        )

        for ((name, format) in p) {
            mpv.observeProperty(name, format)
        }
    }

    private var tracks = mapOf<String, MutableList<Track>>(
        "audio" to mutableListOf(),
        "video" to mutableListOf(),
        "sub" to mutableListOf()
    )

    val subtitleTrack: List<Track>
        get() = tracks["sub"].orEmpty().toList()
    val audioTrack: List<Track>
        get() = tracks["audio"].orEmpty().toList()
    val videoTrack: List<Track>
        get() = tracks["video"].orEmpty().toList()

    private fun getTrackDisplayName(mpvId: Int, lang: String?, title: String?): String {
        return if (!lang.isNullOrEmpty() && !title.isNullOrEmpty()) {
            blockString(Res.string.ui_track_title_lang, mpvId, title, lang)
        } else if (!lang.isNullOrEmpty() || !title.isNullOrEmpty()) {
            blockString(Res.string.ui_track_text, mpvId, lang.orEmpty() + title.orEmpty())
        } else {
            blockString(Res.string.ui_track, mpvId)
        }
    }

    fun loadTracks() {
        loadTrack("sub")
        loadTrack("audio")
        loadTrack("video")
    }

    fun loadSubtitleTrack() = loadTrack("sub")
    fun loadAudioTrack() = loadTrack("audio")

    private fun loadTrack(trackType: String) {
        tracks[trackType]!!.apply {
            clear()
            add(
                Track(
                    trackId = -1,
                    name = blockString(Res.string.track_off),
                    isAlbumArt = false,
                )
            )
        }
        val count = mpv.getPropertyInt("track-list/count")
        // Note that because events are async, properties might disappear at any moment
        // so use ?: continue instead of !!
        for (i in 0 until count) {
            val type = mpv.getPropertyString("track-list/$i/type") ?: continue
            if (type == trackType) {
                val mpvId = mpv.getPropertyInt("track-list/$i/id")
                val lang = mpv.getPropertyString("track-list/$i/lang")
                val title = mpv.getPropertyString("track-list/$i/title")
                val isAlbumArt = mpv.getPropertyBoolean("track-list/$i/albumart")

                tracks.getValue(type).add(
                    Track(
                        trackId = mpvId,
                        name = getTrackDisplayName(mpvId, lang, title),
                        isAlbumArt = isAlbumArt,
                    )
                )
            }
        }
    }

    fun loadPlaylist(): MutableList<String> {
        val playlist = mutableListOf<String>()
        val count = mpv.getPropertyInt("playlist-count")
        for (i in 0 until count) {
            val filename = mpv.getPropertyString("playlist/$i/filename")!!
            playlist.add(filename)
        }
        return playlist
    }

    data class Chapter(val index: Int, val title: String?, val time: Double)

    fun loadChapters(): MutableList<Chapter> {
        val chapters = mutableListOf<Chapter>()
        val count = mpv.getPropertyInt("chapter-list/count")
        for (i in 0 until count) {
            val title = mpv.getPropertyString("chapter-list/$i/title")
            val time = mpv.getPropertyDouble("chapter-list/$i/time")
            chapters.add(
                Chapter(
                    index = i,
                    title = title,
                    time = time
                )
            )
        }
        return chapters
    }

    // Property getters/setters
    val filename: String?
        get() = mpv.getPropertyString("filename")
    val path: String?
        get() = mpv.getPropertyString("path")
    val mediaTitle: String?
        get() = mpv.getPropertyString("media-title")
    var paused: Boolean
        get() = mpv.getPropertyBoolean("pause")
        set(paused) {
            mpv.setPropertyBoolean("pause", paused)
        }
    val playlistCount: Int
        get() = mpv.getPropertyInt("playlist-count")
    var playlistPos: Int
        get() = mpv.getPropertyInt("playlist-pos")
        set(value) = mpv.setPropertyInt("playlist-pos", value)
    val isIdling: Boolean
        get() = mpv.getPropertyBoolean("idle-active")
    val eofReached: Boolean
        get() = mpv.getPropertyBoolean("eof-reached")
    val keepOpen: Boolean
        get() = mpv.getPropertyBoolean("keep-open")

    val shuffle: Boolean
        get() = mpv.getPropertyBoolean("shuffle")

    val coreIdle: Boolean
        get() = mpv.getPropertyBoolean("core-idle")
    val pausedForCache: Boolean
        get() = mpv.getPropertyBoolean("paused-for-cache")
    val demuxerCacheIdle: Boolean
        get() = mpv.getPropertyBoolean("demuxer-cache-idle")

    val loopPlaylist: Boolean
        get() = mpv.getPropertyString("loop-playlist") in arrayOf("inf", "force")
    val loopOne: Boolean
        get() = mpv.getPropertyString("loop-file") in arrayOf("inf")

    val duration: Int
        get() = mpv.getPropertyInt("duration")

    var timePos: Int
        get() = mpv.getPropertyInt("time-pos")
        set(progress) = mpv.setPropertyInt("time-pos", progress)

    val hwdecActive: String
        get() = mpv.getPropertyString("hwdec-current") ?: "no"

    var playbackSpeed: Double
        get() = mpv.getPropertyDouble("speed")
        set(speed) = mpv.setPropertyDouble("speed", speed)

    val videoRotate: Int
        get() = mpv.getPropertyInt("video-rotate")

    val videoZoom: Double
        get() = mpv.getPropertyDouble("video-zoom")

    val videoPanX: Double
        get() = mpv.getPropertyDouble("video-pan-x")

    val videoPanY: Double
        get() = mpv.getPropertyDouble("video-pan-y")

    val estimatedVfFps: Double
        get() = mpv.getPropertyDouble("estimated-vf-fps")

    val videoW: Int
        get() = mpv.getPropertyInt("video-params/w")

    val videoH: Int
        get() = mpv.getPropertyInt("video-params/h")

    val videoDW: Int
        get() = mpv.getPropertyInt("video-params/dw")

    val videoDH: Int
        get() = mpv.getPropertyInt("video-params/dh")

    val videoAspect: Double
        get() = mpv.getPropertyDouble("video-params/aspect")

    val videoFormat: String?
        get() = mpv.getPropertyString("video-format")

    val demuxerCacheDuration: Double
        get() = mpv.getPropertyDouble("demuxer-cache-duration")

    val artist: String
        get() = mpv.getPropertyString("metadata/by-key/Artist").orEmpty()

    val album: String
        get() = mpv.getPropertyString("metadata/by-key/Album").orEmpty()

    var thumbnail: Bitmap? = null
        private set
        get() {
            field = if (videoFormat.isNullOrEmpty()) {
                null
            } else {
                mpv.grabThumbnail(THUMB_SIZE)
            }
            return field
        }

    inner class TrackDelegate(private val name: String) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): Int {
            val v = mpv.getPropertyString(name)
            // we can get null here for "no" or other invalid value
            return v?.toIntOrNull() ?: -1
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
            mpv.setPropertyString(name, if (value == -1) "no" else value.toString())
        }
    }

    var vid: Int by TrackDelegate("vid")
    var sid: Int by TrackDelegate("sid")
    var secondarySid: Int by TrackDelegate("secondary-sid")
    var aid: Int by TrackDelegate("aid")

    fun resetAid() = mpv.setPropertyString("aid", "auto")
    fun resetVid() = mpv.setPropertyString("vid", "auto")
    fun resetSid() = mpv.setPropertyString("sid", "auto")

    // Commands

    fun cyclePause() {
        if (keepOpen && eofReached) {
            seek(0)
        } else if (!isIdling) {
            mpv.command("cycle", "pause")
        } else if (playlistCount > 0) {
            playMediaAtIndex(playlistCount - 1)
        }
    }

    fun cycleHwdec() = mpv.command("cycle-values", "hwdec", "auto", "no")

    fun loopPlaylist() {
        mpv.setPropertyString("loop-playlist", "inf")
        mpv.setPropertyString("loop-file", "no")
    }

    fun loopFile() {
        mpv.setPropertyString("loop-playlist", "no")
        mpv.setPropertyString("loop-file", "inf")
    }

    fun loopNo() {
        mpv.setPropertyString("loop-playlist", "no")
        mpv.setPropertyString("loop-file", "no")
    }

    fun shuffle(newShuffle: Boolean) {
        // Use the 'shuffle' property to store the shuffled state, since changing
        // it at runtime doesn't do anything.
        val current = shuffle
        if (current == newShuffle) return
        mpv.command(if (newShuffle) "playlist-shuffle" else "playlist-unshuffle")
        mpv.setPropertyBoolean("shuffle", newShuffle)
    }

    fun loading(): Boolean {
        return (pausedForCache || (coreIdle && !demuxerCacheIdle)) && !paused
    }

    fun loadFile(filePath: String) {
        if (path != filePath) {
            mpv.command("loadfile", filePath)
        }
        paused = false
    }

    fun loadList(files: List<String>, startFile: String?) {
        val realFiles = files.filter { it.isNotBlank() }
        if (realFiles.isNotEmpty()) {
            val index = if (startFile == null) 0
            else realFiles.indexOf(startFile).takeIf { it >= 0 } ?: 0
            val currentPlaylist = loadPlaylist()
            if (currentPlaylist != realFiles) {
                paused = true
                mpv.command("stop")
                realFiles.forEachIndexed { index, path ->
                    val mode = if (index == 0) "replace" else "append"
                    mpv.command("loadfile", path, mode)
                }
                mpv.command("playlist-play-index", index.toString())
                paused = false
            } else if (path != startFile && playlistCount > 0) {
                mpv.command("playlist-play-index", index.toString())
                paused = false
            }
        }
    }

    fun removeFromList(files: List<String>) {
        val currentList = loadPlaylist()
        files.forEach { file ->
            val index = currentList.indexOfFirst { it == file }
            if (index != -1) {
                mpv.command("playlist-remove", index.toString())
            }
        }
    }

    fun playlistPrev() {
        if (isIdling && playlistCount > 1) {
            playMediaAtIndex(playlistCount - 2)
        } else {
            mpv.command("playlist-prev")
        }
        paused = false
    }

    fun playlistNext() {
        mpv.command("playlist-next")
        paused = false
    }

    fun playFileInPlaylist(path: String) {
        val index = loadPlaylist().indexOf(path)
        if (index >= 0 && playlistPos != index) {
            mpv.command("playlist-play-index", index.toString())
            paused = false
        }
    }

    fun stop() {
        mpv.command("stop")
    }

    fun seek(
        position: Int,
        precise: Boolean = PlayerSeekOptionPreference.isPrecise(
            dataStore.getOrDefault(PlayerSeekOptionPreference)
        )
    ) {
        if (precise) {
            timePos = position
        } else {
            // seek faster than assigning to timePos but less precise
            mpv.command("seek", position.toString(), "absolute+keyframes")
        }
    }

    fun zoom(value: Float) {
        mpv.option("video-zoom", log(value.coerceAtMost(60f), 2f).toString())
    }

    fun rotate(value: Int) {
        var scaledValue = value % 360
        scaledValue = if (scaledValue >= 0) scaledValue else scaledValue + 360
        mpv.option("video-rotate", scaledValue.toString())
    }

    fun offset(x: Int, y: Int) {
        val dw = videoDW
        val dh = videoDH
        mpv.option("video-pan-x", (x.toFloat() / dw).toString())
        mpv.option("video-pan-y", (y.toFloat() / dh).toString())
    }

    fun audioDelay(delayMillis: Long) {
        mpv.option("audio-delay", (delayMillis / 1000.0).toString())
    }

    fun subtitleDelay(delayMillis: Long) {
        mpv.option("sub-delay", (delayMillis / 1000.0).toString())
    }

    fun playMediaAtIndex(index: Int? = null) {
        when (index) {
            null -> mpv.command("playlist-play-index", "none")
            -1 -> mpv.command("playlist-play-index", "current")
            else -> mpv.command("playlist-play-index", index.toString())
        }
        paused = false
    }

    fun screenshot(onSaveScreenshot: (PlatformFile) -> Unit) {
        val format = "jpg"
        val filename =
            "$filename-(${
                timePos.toLong().toDurationString(splitter = "-")
            })-${Random.Default.nextInt()}"
        mpv.option("screenshot-format", format)
        mpv.option("screenshot-template", filename)
        mpv.command("screenshot")

        scope.launch {
            val picture =
                PlatformFile(Const.TEMP_PICTURES_DIR.asPlatformFile(), "$filename.$format")
            try {
                withTimeout(10000.milliseconds) {
                    while (!picture.exists()) delay(100.milliseconds)
                }
            } catch (_: TimeoutCancellationException) {
                logger.e("Failed to save screenshot")
                return@launch
            }
            onSaveScreenshot(picture)
        }
    }

    fun addSubtitle(filePath: String) {
        mpv.command("sub-add", filePath, "cached")
        loadSubtitleTrack()
    }

    fun addAudio(filePath: String) {
        mpv.command("audio-add", filePath, "cached")
        loadAudioTrack()
    }

    override fun onPropertyChange(name: String) {
        when (name) {
            "track-list" -> {
                loadTracks()
            }
        }
    }
}