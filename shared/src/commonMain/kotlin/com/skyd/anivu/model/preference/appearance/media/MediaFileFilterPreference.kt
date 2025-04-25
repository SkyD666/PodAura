package com.skyd.anivu.model.preference.appearance.media

import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyd.anivu.model.preference.BasePreference
import com.skyd.ksp.preference.Preference
import org.jetbrains.compose.resources.getString
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.media_display_filter_all
import podaura.shared.generated.resources.media_display_filter_audio
import podaura.shared.generated.resources.media_display_filter_media
import podaura.shared.generated.resources.media_display_filter_video

@Preference
object MediaFileFilterPreference : BasePreference<String>() {
    private const val MEDIA_FILE_FILTER = "mediaFileFilter"

    const val VIDEO_REGEX = ".*\\.(mp4|avi|mkv|mov|flv|wmv|webm|mpg|mpeg|3gp|rmvb|ts|mov|m3u8)$"
    const val AUDIO_REGEX = ".*\\.(mp3|wav|flac|aac|ogg|m4a|wma|opus|alac|aiff|aif)$"
    const val MEDIA_REGEX = "($VIDEO_REGEX)|($AUDIO_REGEX)"
    const val ALL_REGEX = ".*"

    val values = arrayOf(ALL_REGEX, MEDIA_REGEX, VIDEO_REGEX, AUDIO_REGEX)

    override val default = ALL_REGEX
    override val key = stringPreferencesKey(MEDIA_FILE_FILTER)

    suspend fun toDisplayName(value: String): String = when (value) {
        VIDEO_REGEX -> getString(Res.string.media_display_filter_video)
        AUDIO_REGEX -> getString(Res.string.media_display_filter_audio)
        MEDIA_REGEX -> getString(Res.string.media_display_filter_media)
        ALL_REGEX -> getString(Res.string.media_display_filter_all)
        else -> value
    }
}