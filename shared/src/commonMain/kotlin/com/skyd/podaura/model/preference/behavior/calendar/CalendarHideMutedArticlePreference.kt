package com.skyd.podaura.model.preference.behavior.calendar

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.skyd.ksp.annotation.Preference
import com.skyd.podaura.model.preference.BasePreference

@Preference
object CalendarHideMutedArticlePreference : BasePreference<Boolean>() {
    private const val CALENDAR_HIDE_MUTED_FEED = "calendarHideMutedFeed"

    override val default = true
    override val key = booleanPreferencesKey(CALENDAR_HIDE_MUTED_FEED)
}
