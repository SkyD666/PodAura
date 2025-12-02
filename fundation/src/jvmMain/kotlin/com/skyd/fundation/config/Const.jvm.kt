package com.skyd.fundation.config

import java.io.File

val Const.DB_DIR: String
    get() = getAppDirectories().dataDir + File.separator + "Database"
val Const.DATA_STORE_DIR: String
    get() = getAppDirectories().dataDir + File.separator + "DataStore"
actual val Const.FEED_ICON_DIR: String
    get() = File(getAppDirectories().dataDir, "Pictures/FeedIcon")
        .apply { if (!exists()) mkdirs() }.path
actual val Const.MPV_CACHE_DIR: String
    get() = File(getAppDirectories().dataDir, "Mpv/Cache")
        .apply { if (!exists()) mkdirs() }.path
actual val Const.MPV_CONFIG_DIR: String
    get() = File(getAppDirectories().dataDir, "Mpv/Config")
        .apply { if (!exists()) mkdirs() }.path
actual val Const.VIDEO_DIR: String
    get() = File(getAppDirectories().dataDir, "Video")
        .apply { if (!exists()) mkdirs() }.path
actual val Const.DEFAULT_FILE_PICKER_PATH: String
    get() = getAppDirectories().homeDir
actual val Const.TEMP_PICTURES_DIR: String
    get() = File(getAppDirectories().cacheDir, "Pictures")
        .apply { if (!exists()) mkdirs() }.path
actual val Const.PICTURES_DIR: String
    get() = File(getAppDirectories().dataDir, "Pictures")
        .apply { if (!exists()) mkdirs() }.path
actual val Const.MPV_FONT_DIR: String
    get() = File(MPV_CONFIG_DIR, "Font").apply { if (!exists()) mkdirs() }.path
actual val Const.PODAURA_PICTURES_DIR: String
    get() = File(
        getAppDirectories().homeDir + File.separator + "Pictures",
        "PodAura"
    ).apply { if (!exists()) mkdirs() }.path