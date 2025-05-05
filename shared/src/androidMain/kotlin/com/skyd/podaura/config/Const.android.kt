package com.skyd.podaura.config

import android.content.Context
import android.os.Environment
import com.skyd.podaura.di.get
import java.io.File

actual val Const.FEED_ICON_DIR: String
    get() = File(get<Context>().filesDir.path, "Pictures/FeedIcon")
        .apply { if (!exists()) mkdirs() }.path
actual val Const.MPV_CACHE_DIR: String
    get() = File("${get<Context>().filesDir.path}/Mpv", "Config")
        .apply { if (!exists()) mkdirs() }.path
actual val Const.MPV_CONFIG_DIR: String
    get() = File("${get<Context>().filesDir.path}/Mpv", "Config")
        .apply { if (!exists()) mkdirs() }.path
actual val Const.VIDEO_DIR: String
    get() = File(get<Context>().filesDir.path, "Video")
        .apply { if (!exists()) mkdirs() }.path
actual val Const.DEFAULT_FILE_PICKER_PATH: String
    get() = Environment.getExternalStorageDirectory().absolutePath
actual val Const.TEMP_PICTURES_DIR: String
    get() = File(get<Context>().cacheDir.path, "Pictures")
        .apply { if (!exists()) mkdirs() }.path
actual val Const.TEMP_TORRENT_DIR: String
    get() = File(get<Context>().cacheDir.path, "Torrent")
        .apply { if (!exists()) mkdirs() }.path
actual val Const.TORRENT_RESUME_DATA_DIR: String
    get() = File(get<Context>().filesDir.path, "TorrentResumeData")
        .apply { if (!exists()) mkdirs() }.path
actual val Const.MPV_FONT_DIR: String
    get() = File(MPV_CONFIG_DIR, "Font").apply { if (!exists()) mkdirs() }.path
actual val Const.PICTURES_DIR: String
    get() = get<Context>().getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!.path
actual val Const.PODAURA_PICTURES_DIR: String
    get() = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
        "PodAura"
    ).apply { if (!exists()) mkdirs() }.path