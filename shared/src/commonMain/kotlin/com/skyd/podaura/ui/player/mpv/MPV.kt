package com.skyd.podaura.ui.player.mpv

import coil3.Bitmap
import com.skyd.podaura.ui.PlatformSurfaceHolder

enum class MPVFormat {
    MPV_FORMAT_NONE,
    MPV_FORMAT_STRING,
    MPV_FORMAT_OSD_STRING,
    MPV_FORMAT_FLAG,
    MPV_FORMAT_INT64,
    MPV_FORMAT_DOUBLE,
    MPV_FORMAT_NODE,
    MPV_FORMAT_NODE_ARRAY,
    MPV_FORMAT_NODE_MAP,
    MPV_FORMAT_BYTE_ARRAY,
}

object MPVEvent {
    const val NONE: Int = 0
    const val SHUTDOWN: Int = 1
    const val LOG_MESSAGE: Int = 2
    const val GET_PROPERTY_REPLY: Int = 3
    const val SET_PROPERTY_REPLY: Int = 4
    const val COMMAND_REPLY: Int = 5
    const val START_FILE: Int = 6
    const val END_FILE: Int = 7
    const val FILE_LOADED: Int = 8
    const val CLIENT_MESSAGE: Int = 16
    const val VIDEO_RECONFIG: Int = 17
    const val AUDIO_RECONFIG: Int = 18
    const val SEEK: Int = 20
    const val PLAYBACK_RESTART: Int = 21
    const val PROPERTY_CHANGE: Int = 22
    const val QUEUE_OVERFLOW: Int = 24
    const val HOOK: Int = 25
}

expect class MPV {
    fun initialize()
    fun destroy()
    fun attachSurface(surfaceHolder: PlatformSurfaceHolder)
    fun detachSurface()
    fun addEventListener(listener: EventListener)
    fun removeEventListener(listener: EventListener)
    fun command(vararg command: String)
    fun option(key: String, value: String)
    fun getPropertyInt(name: String): Int
    fun getPropertyBoolean(name: String): Boolean
    fun getPropertyDouble(name: String): Double
    fun getPropertyString(name: String): String?
    fun setPropertyInt(name: String, value: Int)
    fun setPropertyBoolean(name: String, value: Boolean)
    fun setPropertyDouble(name: String, value: Double)
    fun setPropertyString(name: String, value: String)
    fun observeProperty(name: String, format: MPVFormat)
    fun grabThumbnail(dimension: Int): Bitmap?
}

expect fun platformMPV(): MPV