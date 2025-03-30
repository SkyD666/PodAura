package com.skyd.anivu.model.preference.appearance.feed

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import com.skyd.anivu.R
import com.skyd.anivu.base.BasePreference
import com.skyd.anivu.ext.dataStore
import com.skyd.anivu.ext.getOrDefault
import com.skyd.ksp.preference.Preference

@Preference
object FeedNumberBadgePreference : BasePreference<Int> {
    private const val FEED_NUMBER_BADGE = "feedNumberBadge"

    const val UNREAD = 1
    const val ALL = 1 shl 1
    const val UNREAD_ALL = UNREAD + ALL
    val values = arrayOf(UNREAD, ALL, UNREAD_ALL)

    override val default = ALL
    override val key = intPreferencesKey(FEED_NUMBER_BADGE)

    fun toDisplayName(
        context: Context,
        value: Int = context.dataStore.getOrDefault(this),
    ): String = when (value) {
        UNREAD -> context.getString(R.string.feed_number_badge_unread)
        ALL -> context.getString(R.string.feed_number_badge_all)
        UNREAD_ALL -> context.getString(R.string.feed_number_badge_unread_all)
        else -> context.getString(R.string.unknown)
    }
}
