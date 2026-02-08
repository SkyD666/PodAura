package com.skyd.fundation.config

import com.skyd.fundation.BuildKonfig
import com.skyd.fundation.util.Directories
import com.skyd.fundation.util.ensureDirectoryExists
import com.skyd.fundation.util.joinPath

actual val Const.DB_DIR: String
    get() = joinPath(Directories.applicationSupport, BuildKonfig.packageName, "Database")
        .ensureDirectoryExists()
actual val Const.DATA_STORE_DIR: String
    get() = joinPath(Directories.applicationSupport, BuildKonfig.packageName, "DataStore")
        .ensureDirectoryExists()
actual val Const.FEED_ICON_DIR: String
    get() = joinPath(Directories.applicationSupport, BuildKonfig.packageName, "Pictures", "FeedIcon")
        .ensureDirectoryExists()
actual val Const.MPV_CACHE_DIR: String
    get() = joinPath(Directories.applicationSupport, BuildKonfig.packageName, "Mpv", "Cache")
        .ensureDirectoryExists()
actual val Const.MPV_CONFIG_DIR: String
    get() = joinPath(Directories.applicationSupport, BuildKonfig.packageName, "Mpv", "Config")
        .ensureDirectoryExists()
actual val Const.VIDEO_DIR: String
    get() = joinPath(Directories.applicationSupport, BuildKonfig.packageName, "Video")
        .ensureDirectoryExists()
actual val Const.DEFAULT_FILE_PICKER_PATH: String
    get() = Directories.documents
actual val Const.TEMP_PICTURES_DIR: String
    get() = joinPath(Directories.caches, BuildKonfig.packageName, "Pictures")
        .ensureDirectoryExists()
actual val Const.PICTURES_DIR: String
    get() = joinPath(Directories.applicationSupport, BuildKonfig.packageName, "Pictures")
        .ensureDirectoryExists()
actual val Const.MPV_FONT_DIR: String
    get() = joinPath(MPV_CONFIG_DIR, "Font")
        .ensureDirectoryExists()
actual val Const.PODAURA_PICTURES_DIR: String
    get() = joinPath(Directories.documents, "Pictures")
        .ensureDirectoryExists()
