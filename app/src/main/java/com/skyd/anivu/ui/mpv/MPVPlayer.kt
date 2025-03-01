package com.skyd.anivu.ui.mpv


import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.SurfaceHolder
import androidx.core.content.ContextCompat
import com.skyd.anivu.config.Const
import com.skyd.anivu.ext.dataStore
import com.skyd.anivu.ext.getOrDefault
import com.skyd.anivu.model.preference.player.HardwareDecodePreference
import com.skyd.anivu.model.preference.player.PlayerMaxBackCacheSizePreference
import com.skyd.anivu.model.preference.player.PlayerMaxCacheSizePreference
import com.skyd.anivu.model.preference.player.PlayerSeekOptionPreference
import com.skyd.anivu.ui.mpv.land.controller.bar.toDurationString
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.MPVLib.mpvFormat.MPV_FORMAT_DOUBLE
import `is`.xyz.mpv.MPVLib.mpvFormat.MPV_FORMAT_FLAG
import `is`.xyz.mpv.MPVLib.mpvFormat.MPV_FORMAT_INT64
import `is`.xyz.mpv.MPVLib.mpvFormat.MPV_FORMAT_NONE
import `is`.xyz.mpv.MPVLib.mpvFormat.MPV_FORMAT_STRING
import `is`.xyz.mpv.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.io.File
import kotlin.math.log
import kotlin.random.Random
import kotlin.reflect.KProperty

class MPVPlayer(private val context: Application) : SurfaceHolder.Callback, MPVLib.EventObserver {
    companion object {
        private const val TAG = "MPVPlayer"

        // resolution (px) of the thumbnail
        private const val THUMB_SIZE = 1024

        @Volatile
        private var initialized = false

        @Volatile
        private var instance: MPVPlayer? = null

        fun getInstance(context: Application): MPVPlayer {
            if (instance == null) {
                synchronized(MPVPlayer::class.java) {
                    if (instance == null) {
                        instance = MPVPlayer(context)
                    }
                }
            }
            instance?.initialize(
                configDir = Const.MPV_CONFIG_DIR.path,
                cacheDir = Const.MPV_CACHE_DIR.path,
                fontDir = Const.MPV_FONT_DIR.path,
            )
            return instance!!
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    fun initialize(
        configDir: String,
        cacheDir: String,
        fontDir: String,
        logLvl: String = "v",
        vo: String = "gpu",
    ) {
        if (initialized) return
        synchronized(MPVPlayer::class.java) {
            if (initialized) return
            initialized = true
        }

        MPVLib.create(context, logLvl)
        MPVLib.setOptionString("config", "yes")
        MPVLib.setOptionString("config-dir", configDir)
        for (opt in arrayOf("gpu-shader-cache-dir", "icc-cache-dir"))
            MPVLib.setOptionString(opt, cacheDir)
        initOptions(vo) // do this before init() so user-supplied config can override our choices
        MPVLib.init()
        /* Hardcoded options: */
        // we need to call write-watch-later manually
        MPVLib.setOptionString("save-position-on-quit", "no")
        // would crash before the surface is attached
        MPVLib.setOptionString("force-window", "no")
        MPVLib.setOptionString("vo", "null")
        // "no" wouldn't work and "yes" is not intended by the UI
        MPVLib.setOptionString("idle", "yes")
        MPVLib.setPropertyString("sub-fonts-dir", fontDir)
        MPVLib.setPropertyString("osd-fonts-dir", fontDir)

        observeProperties()

        MPVLib.addObserver(this)
    }

    private var voInUse: String = ""

    private fun initOptions(vo: String) {
        // apply phone-optimized defaults
        MPVLib.setOptionString("profile", "fast")

        // vo
        voInUse = vo

        // vo: set display fps as reported by android
        val disp = ContextCompat.getDisplayOrDefault(context)
        val refreshRate = disp.mode.refreshRate

        val dataStore = context.dataStore
        Log.v(TAG, "Display ${disp.displayId} reports FPS of $refreshRate")
        MPVLib.setOptionString("display-fps-override", refreshRate.toString())
        MPVLib.setOptionString("video-sync", "audio")

        MPVLib.setOptionString("gpu-context", "android")
        MPVLib.setOptionString("opengl-es", "yes")
        MPVLib.setOptionString(
            "hwdec",
            if (dataStore.getOrDefault(HardwareDecodePreference)) "auto" else "no"
        )
        MPVLib.setOptionString("ao", "audiotrack,opensles")
        MPVLib.setOptionString("input-default-bindings", "yes")
        // Limit demuxer cache since the defaults are too high for mobile devices
        MPVLib.setOptionString(
            "demuxer-max-bytes",
            dataStore.getOrDefault(PlayerMaxCacheSizePreference).toString(),
        )
        MPVLib.setOptionString(
            "demuxer-max-back-bytes",
            dataStore.getOrDefault(PlayerMaxBackCacheSizePreference).toString(),
        )

        MPVLib.setOptionString("screenshot-directory", Const.PICTURES_DIR.path)
    }

    // Called when back button is pressed, or app is shutting down
    fun destroy() {
        if (initialized) {
            MPVLib.destroy()
            MPVLib.removeObserver(this)
            initialized = false
        }
    }

    fun onKey(event: KeyEvent): Boolean {
        if (KeyEvent.isModifierKey(event.keyCode)) {
            return false
        }

        var mapped = KeyMapping.map.get(event.keyCode)
        if (mapped == null) {
            // Fallback to produced glyph
            if (!event.isPrintingKey) {
                if (event.repeatCount == 0) {
                    Log.d(TAG, "Unmapped non-printable key ${event.keyCode}")
                }
                return false
            }

            val ch = event.unicodeChar
            if (ch.and(KeyCharacterMap.COMBINING_ACCENT) != 0) {
                return false // dead key
            }
            mapped = ch.toChar().toString()
        }

        if (event.repeatCount > 0) {
            return true // consume event but ignore it, mpv has its own key repeat
        }

        val mod = mutableListOf<String>().apply {
            event.isShiftPressed && add("shift")
            event.isCtrlPressed && add("ctrl")
            event.isAltPressed && add("alt")
            event.isMetaPressed && add("meta")
            add(mapped)
        }

        val action = if (event.action == KeyEvent.ACTION_DOWN) "keydown" else "keyup"
        MPVLib.command(arrayOf(action, mod.joinToString("+")))

        return true
    }

    private fun observeProperties() {
        // This observes all properties needed by MPVView, MPVActivity or other classes
        data class Property(val name: String, val format: Int = MPV_FORMAT_NONE)

        val p = arrayOf(
            Property("time-pos", MPV_FORMAT_INT64),
            Property("duration", MPV_FORMAT_INT64),
            Property("demuxer-cache-time", MPV_FORMAT_INT64),
            Property("video-rotate", MPV_FORMAT_INT64),
            Property("paused-for-cache", MPV_FORMAT_FLAG),
            Property("seeking", MPV_FORMAT_FLAG),
            Property("pause", MPV_FORMAT_FLAG),
            Property("eof-reached", MPV_FORMAT_FLAG),
            Property("paused-for-cache", MPV_FORMAT_FLAG),
            Property("idle-active", MPV_FORMAT_FLAG),
            Property("aid", MPV_FORMAT_INT64),
            Property("sid", MPV_FORMAT_INT64),
            Property("track-list"),
            // observing double properties is not hooked up in the JNI code, but doing this
            // will restrict updates to when it actually changes
            Property("video-zoom", MPV_FORMAT_DOUBLE),
            Property("video-params/aspect", MPV_FORMAT_DOUBLE),
            Property("video-pan-x", MPV_FORMAT_DOUBLE),
            Property("video-pan-y", MPV_FORMAT_DOUBLE),
            Property("speed", MPV_FORMAT_DOUBLE),
            Property("demuxer-cache-duration", MPV_FORMAT_DOUBLE),
            Property("playlist"),
            Property("playlist-pos", MPV_FORMAT_INT64),
            Property("playlist-count", MPV_FORMAT_INT64),
            Property("video-format"),
            Property("media-title", MPV_FORMAT_STRING),
            Property("metadata"),
            Property("loop-playlist"),
            Property("loop-file"),
            Property("shuffle", MPV_FORMAT_FLAG),
            Property("hwdec-current")
        )

        for ((name, format) in p) {
            MPVLib.observeProperty(name, format)
        }
    }

    data class Track(val trackId: Int, val name: String, val isAlbumArt: Boolean)

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
            context.getString(R.string.ui_track_title_lang, mpvId, title, lang)
        } else if (!lang.isNullOrEmpty() || !title.isNullOrEmpty()) {
            context.getString(R.string.ui_track_text, mpvId, lang.orEmpty() + title.orEmpty())
        } else {
            context.getString(R.string.ui_track, mpvId)
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
                    name = context.getString(R.string.track_off),
                    isAlbumArt = false,
                )
            )
        }
        val count = MPVLib.getPropertyInt("track-list/count")
        // Note that because events are async, properties might disappear at any moment
        // so use ?: continue instead of !!
        for (i in 0 until count) {
            val type = MPVLib.getPropertyString("track-list/$i/type") ?: continue
            if (type == trackType) {
                val mpvId = MPVLib.getPropertyInt("track-list/$i/id") ?: continue
                val lang = MPVLib.getPropertyString("track-list/$i/lang")
                val title = MPVLib.getPropertyString("track-list/$i/title")
                val isAlbumArt = MPVLib.getPropertyBoolean("track-list/$i/albumart")

                tracks.getValue(type).add(
                    Track(
                        trackId = mpvId,
                        name = getTrackDisplayName(mpvId, lang, title),
                        isAlbumArt = isAlbumArt ?: false,
                    )
                )
            }
        }
    }

    fun loadPlaylist(): MutableList<String> {
        val playlist = mutableListOf<String>()
        val count = MPVLib.getPropertyInt("playlist-count")!!
        for (i in 0 until count) {
            val filename = MPVLib.getPropertyString("playlist/$i/filename")!!
            playlist.add(filename)
        }
        return playlist
    }

    data class Chapter(val index: Int, val title: String?, val time: Double)

    fun loadChapters(): MutableList<Chapter> {
        val chapters = mutableListOf<Chapter>()
        val count = MPVLib.getPropertyInt("chapter-list/count")!!
        for (i in 0 until count) {
            val title = MPVLib.getPropertyString("chapter-list/$i/title")
            val time = MPVLib.getPropertyDouble("chapter-list/$i/time")!!
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
        get() = MPVLib.getPropertyString("filename")
    val path: String?
        get() = MPVLib.getPropertyString("path")
    val mediaTitle: String?
        get() = MPVLib.getPropertyString("media-title")
    var paused: Boolean
        get() = MPVLib.getPropertyBoolean("pause")
        set(paused) = MPVLib.setPropertyBoolean("pause", paused)
    val playlistCount: Int
        get() = MPVLib.getPropertyInt("playlist-count") ?: 0
    var playlistPos: Int
        get() = MPVLib.getPropertyInt("playlist-pos") ?: -1
        set(value) = MPVLib.setPropertyInt("playlist-pos", value)
    val isIdling: Boolean
        get() = MPVLib.getPropertyBoolean("idle-active")
    val eofReached: Boolean
        get() = MPVLib.getPropertyBoolean("eof-reached")
    val keepOpen: Boolean
        get() = MPVLib.getPropertyBoolean("keep-open") ?: false

    val shuffle: Boolean
        get() = MPVLib.getPropertyBoolean("shuffle") ?: false

    val loopPlaylist: Boolean
        get() = MPVLib.getPropertyString("loop-playlist") in arrayOf("inf", "force")
    val loopOne: Boolean
        get() = MPVLib.getPropertyString("loop-file") in arrayOf("inf")

    val duration: Int
        get() = MPVLib.getPropertyInt("duration") ?: 0

    var timePos: Int
        get() = MPVLib.getPropertyInt("time-pos") ?: 0
        set(progress) = MPVLib.setPropertyInt("time-pos", progress)

    val hwdecActive: String
        get() = MPVLib.getPropertyString("hwdec-current") ?: "no"

    var playbackSpeed: Double
        get() = MPVLib.getPropertyDouble("speed")
        set(speed) = MPVLib.setPropertyDouble("speed", speed)

    val videoRotate: Int
        get() = MPVLib.getPropertyInt("video-rotate")

    val videoZoom: Double
        get() = MPVLib.getPropertyDouble("video-zoom")

    val videoPanX: Double
        get() = MPVLib.getPropertyDouble("video-pan-x")

    val videoPanY: Double
        get() = MPVLib.getPropertyDouble("video-pan-y")

    val estimatedVfFps: Double?
        get() = MPVLib.getPropertyDouble("estimated-vf-fps")

    val videoW: Int?
        get() = MPVLib.getPropertyInt("video-params/w")

    val videoH: Int?
        get() = MPVLib.getPropertyInt("video-params/h")

    val videoDW: Int?
        get() = MPVLib.getPropertyInt("video-params/dw")

    val videoDH: Int?
        get() = MPVLib.getPropertyInt("video-params/dh")

    val videoAspect: Double?
        get() = MPVLib.getPropertyDouble("video-params/aspect")

    val videoFormat: String?
        get() = MPVLib.getPropertyString("video-format")

    val demuxerCacheDuration: Double
        get() = MPVLib.getPropertyDouble("demuxer-cache-duration") ?: 0.0

    val artist: String
        get() = MPVLib.getPropertyString("metadata/by-key/Artist").orEmpty()

    val album: String
        get() = MPVLib.getPropertyString("metadata/by-key/Album").orEmpty()

    var thumbnail: Bitmap? = null
        private set
        get() {
            field = if (videoFormat.isNullOrEmpty()) {
                null
            } else {
                MPVLib.grabThumbnail(THUMB_SIZE)
            }
            return field
        }

    class TrackDelegate(private val name: String) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): Int {
            val v = MPVLib.getPropertyString(name)
            // we can get null here for "no" or other invalid value
            return v?.toIntOrNull() ?: -1
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
            MPVLib.setPropertyString(name, if (value == -1) "no" else value.toString())
        }
    }

    var vid: Int by TrackDelegate("vid")
    var sid: Int by TrackDelegate("sid")
    var secondarySid: Int by TrackDelegate("secondary-sid")
    var aid: Int by TrackDelegate("aid")

    fun resetAid() = MPVLib.setPropertyString("aid", "auto")
    fun resetVid() = MPVLib.setPropertyString("vid", "auto")
    fun resetSid() = MPVLib.setPropertyString("sid", "auto")

    // Commands

    fun cyclePause() {
        if (keepOpen && eofReached) {
            seek(0)
        } else if (!isIdling) {
            MPVLib.command(arrayOf("cycle", "pause"))
        } else if (playlistCount > 0) {
            playMediaAtIndex(playlistCount - 1)
        }
    }

    fun cycleHwdec() = MPVLib.command(arrayOf("cycle-values", "hwdec", "auto", "no"))

    fun loopPlaylist() {
        MPVLib.setPropertyString("loop-playlist", "inf")
        MPVLib.setPropertyString("loop-file", "no")
    }

    fun loopFile() {
        MPVLib.setPropertyString("loop-playlist", "no")
        MPVLib.setPropertyString("loop-file", "inf")
    }

    fun loopNo() {
        MPVLib.setPropertyString("loop-playlist", "no")
        MPVLib.setPropertyString("loop-file", "no")
    }

    fun shuffle(newShuffle: Boolean) {
        // Use the 'shuffle' property to store the shuffled state, since changing
        // it at runtime doesn't do anything.
        val current = shuffle
        if (current == newShuffle) return
        MPVLib.command(arrayOf(if (newShuffle) "playlist-shuffle" else "playlist-unshuffle"))
        MPVLib.setPropertyBoolean("shuffle", newShuffle)
    }

    fun loadFile(filePath: String) {
        if (path != filePath) {
            MPVLib.command(arrayOf("loadfile", filePath))
        }
        paused = false
    }

    fun loadList(files: List<String>, startFile: String?) {
        val realFiles = files.filter { it.isNotBlank() }
        if (realFiles.isNotEmpty()) {
            val index = if (startFile == null) 0
            else realFiles.indexOf(startFile).takeIf { it >= 0 } ?: 0

            val currentPlaylist = loadPlaylist()
            if (currentPlaylist.size != realFiles.size ||
                currentPlaylist.zip(realFiles).any { (old, new) -> old != new }
            ) {
                val playlistFile = File(Const.MPV_CACHE_DIR, "playlist")
                if (playlistFile.exists() || playlistFile.createNewFile()) {
                    playlistFile.writeText(realFiles.joinToString("\n"))
                    MPVLib.command(arrayOf("loadlist", playlistFile.path, "replace"))
                    MPVLib.command(arrayOf("playlist-play-index", index.toString()))
                }
            } else if (path != startFile && playlistCount > 0) {
                MPVLib.command(arrayOf("playlist-play-index", index.toString()))
            }
            paused = false
        }
    }

    fun removeFromList(files: List<String>) {
        val currentList = loadPlaylist()
        files.forEach { file ->
            val index = currentList.indexOfFirst { it == file }
            if (index != -1) {
                MPVLib.command(arrayOf("playlist-remove", index.toString()))
            }
        }
    }

    fun playlistPrev() {
        if (isIdling && playlistCount > 1) {
            playMediaAtIndex(playlistCount - 2)
        } else {
            MPVLib.command(arrayOf("playlist-prev"))
        }
        paused = false
    }

    fun playlistNext() {
        MPVLib.command(arrayOf("playlist-next"))
        paused = false
    }

    fun playFileInPlaylist(path: String) {
        val index = loadPlaylist().indexOf(path)
        if (index >= 0 && playlistPos != index) {
            MPVLib.command(arrayOf("playlist-play-index", index.toString()))
            paused = false
        }
    }

    fun stop() {
        MPVLib.command(arrayOf("stop"))
    }

    fun seek(position: Int, precise: Boolean = PlayerSeekOptionPreference.isPrecise(context)) {
        if (precise) {
            timePos = position
        } else {
            // seek faster than assigning to timePos but less precise
            MPVLib.command(arrayOf("seek", position.toString(), "absolute+keyframes"))
        }
    }

    fun zoom(value: Float) {
        MPVLib.setOptionString("video-zoom", log(value.coerceAtMost(60f), 2f).toString())
    }

    fun rotate(value: Int) {
        var scaledValue = value % 360
        scaledValue = if (scaledValue >= 0) scaledValue else scaledValue + 360
        MPVLib.setOptionString("video-rotate", scaledValue.toString())
    }

    fun offset(x: Int, y: Int) {
        val dw = videoDW
        val dh = videoDH
        if (dw == null || dh == null) return
        MPVLib.setOptionString("video-pan-x", (x.toFloat() / dw).toString())
        MPVLib.setOptionString("video-pan-y", (y.toFloat() / dh).toString())
    }

    fun playMediaAtIndex(index: Int? = null) {
        when (index) {
            null -> MPVLib.command(arrayOf("playlist-play-index", "none"))
            -1 -> MPVLib.command(arrayOf("playlist-play-index", "current"))
            else -> MPVLib.command(arrayOf("playlist-play-index", index.toString()))
        }
        paused = false
    }

    fun screenshot(onSaveScreenshot: (File) -> Unit) {
        val format = "jpg"
        val filename =
            "$filename-(${timePos.toLong().toDurationString(splitter = "-")})-${Random.nextInt()}"
        MPVLib.setOptionString("screenshot-format", format)
        MPVLib.setOptionString("screenshot-template", filename)
        MPVLib.command(arrayOf("screenshot"))

        scope.launch {
            val picture = File(Const.PICTURES_DIR, "$filename.$format")
            try {
                withTimeout(10000) {
                    while (!picture.exists()) delay(100)
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Failed to save screenshot")
                return@launch
            }
            onSaveScreenshot(picture)
        }
    }

    fun addSubtitle(filePath: String) {
        MPVLib.command(arrayOf("sub-add", filePath, "cached"))
        loadSubtitleTrack()
    }

    fun addAudio(filePath: String) {
        MPVLib.command(arrayOf("audio-add", filePath, "cached"))
        loadAudioTrack()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        MPVLib.setPropertyString("android-surface-size", "${width}x$height")
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.w(TAG, "attaching surface")
        MPVLib.attachSurface(holder.surface)
        // This forces mpv to render subs/osd/whatever into our surface even if it would ordinarily not
        MPVLib.setOptionString("force-window", "yes")
        // We disable video output when the context disappears, enable it back
        MPVLib.setPropertyString("vo", voInUse)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        if (initialized) {
            Log.w(TAG, "detaching surface")
            MPVLib.setPropertyString("vo", "null")
            MPVLib.setOptionString("force-window", "no")
            MPVLib.detachSurface()
        }
    }

    override fun eventProperty(property: String) {
        when (property) {
            "track-list" -> loadTracks()
        }
    }

    override fun eventProperty(property: String, value: Long) {
    }

    override fun eventProperty(property: String, value: Boolean) {
    }

    override fun eventProperty(property: String, value: String) {
    }

    override fun event(eventId: Int) {
        when (eventId) {
            MPVLib.mpvEventId.MPV_EVENT_FILE_LOADED -> {
                resetAid()
                resetVid()
                resetSid()
            }
        }
    }

    override fun efEvent(err: String?) {
        Log.e(TAG, "efEvent: $err")
    }
}
