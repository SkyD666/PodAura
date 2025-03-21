package com.skyd.anivu.config

import android.os.Environment
import com.skyd.anivu.appContext
import java.io.File

object Const {
    const val GITHUB_REPO = "https://github.com/SkyD666/PodAura"
    const val GITHUB_LATEST_RELEASE = "https://api.github.com/repos/SkyD666/PodAura/releases/latest"
    const val GITHUB_NEW_ISSUE_URL = "https://github.com/SkyD666/PodAura/issues/new"
    const val TELEGRAM_GROUP = "https://t.me/SkyD666Chat"
    const val DISCORD_SERVER = "https://discord.gg/pEWEjeJTa3"

    const val TRANSLATION_URL = "https://crowdin.com/project/anivu"

    const val AFADIAN_LINK = "https://afdian.com/a/SkyD666"
    const val BUY_ME_A_COFFEE_LINK = "https://www.buymeacoffee.com/SkyD666"

    const val RAYS_ANDROID_URL = "https://github.com/SkyD666/Rays-Android"
    const val RACA_ANDROID_URL = "https://github.com/SkyD666/Raca-Android"
    const val NIGHT_SCREEN_URL = "https://github.com/SkyD666/NightScreen"

    const val BASE_URL = "https://github.com/SkyD666/"

    val FEED_ICON_DIR = File(appContext.filesDir.path, "Pictures/FeedIcon").apply {
        if (!exists()) mkdirs()
    }

    val TEMP_TORRENT_DIR = File(appContext.cacheDir.path, "Torrent").apply {
        if (!exists()) mkdirs()
    }

    val VIDEO_DIR = File(appContext.filesDir.path, "Video").apply {
        if (!exists()) mkdirs()
    }

    val TORRENT_RESUME_DATA_DIR = File(appContext.filesDir.path, "TorrentResumeData").apply {
        if (!exists()) mkdirs()
    }

    val MPV_CONFIG_DIR = File("${appContext.filesDir.path}/Mpv", "Config")
        .apply { if (!exists()) mkdirs() }
    val MPV_CACHE_DIR = File("${appContext.cacheDir.path}/Mpv", "Cache")
        .apply { if (!exists()) mkdirs() }
    val MPV_FONT_DIR = File(MPV_CONFIG_DIR, "Font")
        .apply { if (!exists()) mkdirs() }

    val PICTURES_DIR = appContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
    val PODAURA_PICTURES_DIR = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
        "PodAura"
    ).apply { if (!exists()) mkdirs() }
    val TEMP_PICTURES_DIR = File(appContext.cacheDir.path, "Pictures").apply {
        if (!exists()) mkdirs()
    }

    val INTERNAL_STORAGE: String = Environment.getExternalStorageDirectory().absolutePath
}