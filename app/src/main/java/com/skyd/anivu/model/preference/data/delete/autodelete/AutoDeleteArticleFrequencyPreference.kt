package com.skyd.anivu.model.preference.data.delete.autodelete

import android.content.Context
import androidx.datastore.preferences.core.longPreferencesKey
import com.skyd.anivu.R
import com.skyd.anivu.base.BasePreference
import com.skyd.anivu.ext.dataStore
import com.skyd.anivu.ext.getOrDefault
import com.skyd.ksp.preference.Preference
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

@Preference
object AutoDeleteArticleFrequencyPreference : BasePreference<Long> {
    private const val AUTO_DELETE_ARTICLE_FREQUENCY = "autoDeleteArticleFrequency"

    override val default = 14.days.inWholeMilliseconds
    override val key = longPreferencesKey(AUTO_DELETE_ARTICLE_FREQUENCY)

    fun toDisplayNameMilliseconds(
        context: Context,
        milliseconds: Long = context.dataStore.getOrDefault(this),
    ): String = context.resources.getQuantityString(
        R.plurals.frequency_day,
        milliseconds.milliseconds.inWholeDays.toInt(),
        milliseconds.milliseconds.inWholeDays.toInt(),
    )

    fun toDisplayNameDays(
        context: Context,
        days: Long = context.dataStore.getOrDefault(this).milliseconds.inWholeDays,
    ): String = context.resources.getQuantityString(
        R.plurals.frequency_day,
        days.toInt(),
        days.toInt(),
    )
}
