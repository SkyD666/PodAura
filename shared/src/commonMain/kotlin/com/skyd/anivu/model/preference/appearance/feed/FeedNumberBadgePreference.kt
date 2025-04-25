package com.skyd.anivu.model.preference.appearance.feed

import androidx.datastore.preferences.core.intPreferencesKey
import com.skyd.anivu.model.preference.BasePreference
import com.skyd.ksp.preference.Preference
import org.jetbrains.compose.resources.getString
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.feed_number_badge_all
import podaura.shared.generated.resources.feed_number_badge_unread
import podaura.shared.generated.resources.feed_number_badge_unread_all
import podaura.shared.generated.resources.unknown

@Preference
object FeedNumberBadgePreference : BasePreference<Int>() {
    private const val FEED_NUMBER_BADGE = "feedNumberBadge"

    const val UNREAD = 1
    const val ALL = 1 shl 1
    const val UNREAD_ALL = UNREAD + ALL
    val values = arrayOf(UNREAD, ALL, UNREAD_ALL)

    override val default = ALL
    override val key = intPreferencesKey(FEED_NUMBER_BADGE)

    suspend fun toDisplayName(value: Int): String = getString(
        when (value) {
            UNREAD -> Res.string.feed_number_badge_unread
            ALL -> Res.string.feed_number_badge_all
            UNREAD_ALL -> Res.string.feed_number_badge_unread_all
            else -> Res.string.unknown
        }
    )
}
