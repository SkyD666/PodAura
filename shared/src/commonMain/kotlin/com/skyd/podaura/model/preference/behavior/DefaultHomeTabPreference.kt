package com.skyd.podaura.model.preference.behavior

import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyd.ksp.annotation.Preference
import com.skyd.podaura.model.preference.BasePreference
import org.jetbrains.compose.resources.getString
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.calendar_screen_name
import podaura.shared.generated.resources.feed_screen_name
import podaura.shared.generated.resources.media_screen_name
import podaura.shared.generated.resources.more_screen_name

@Preference
object DefaultHomeTabPreference : BasePreference<String>() {
    const val FEED = "Feed"
    const val CALENDAR = "Calendar"
    const val MEDIA = "Media"
    const val MORE = "More"
    val values = arrayOf(FEED, CALENDAR, MEDIA, MORE)

    override val default = FEED
    override val key = stringPreferencesKey("defaultHomeTab")

    suspend fun toDisplayName(value: String): String = getString(
        when (value) {
            CALENDAR -> Res.string.calendar_screen_name
            MEDIA -> Res.string.media_screen_name
            MORE -> Res.string.more_screen_name
            else -> Res.string.feed_screen_name
        }
    )
}
