package com.skyd.anivu.model.preference.rss

import android.content.Context
import androidx.datastore.preferences.core.longPreferencesKey
import com.skyd.anivu.R
import com.skyd.anivu.base.BasePreference
import com.skyd.anivu.ext.dataStore
import com.skyd.anivu.ext.getOrDefault
import com.skyd.ksp.preference.Preference
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

@Preference
object RssSyncFrequencyPreference : BasePreference<Long> {
    private const val RSS_SYNC_FREQUENCY = "rssSyncFrequency"

    const val MANUAL = -1L
    val EVERY_15_MINUTE = 15.minutes.inWholeMilliseconds
    val EVERY_30_MINUTE = 30.minutes.inWholeMilliseconds
    val EVERY_1_HOUR = 1.hours.inWholeMilliseconds
    val EVERY_2_HOUR = 2.hours.inWholeMilliseconds
    val EVERY_3_HOUR = 3.hours.inWholeMilliseconds
    val EVERY_6_HOUR = 6.hours.inWholeMilliseconds
    val EVERY_12_HOUR = 12.hours.inWholeMilliseconds
    val EVERY_1_DAY = 1.days.inWholeMilliseconds

    val frequencies = listOf(
        MANUAL,
        EVERY_15_MINUTE,
        EVERY_30_MINUTE,
        EVERY_1_HOUR,
        EVERY_2_HOUR,
        EVERY_3_HOUR,
        EVERY_6_HOUR,
        EVERY_12_HOUR,
        EVERY_1_DAY,
    )

    override val default = MANUAL
    override val key = longPreferencesKey(RSS_SYNC_FREQUENCY)

    fun toDisplayName(
        context: Context,
        value: Long = context.dataStore.getOrDefault(this),
    ): String = when (value) {
        MANUAL -> context.getString(R.string.frequency_manual)
        EVERY_15_MINUTE, EVERY_30_MINUTE -> context.resources.getQuantityString(
            R.plurals.rss_sync_frequency_minute,
            value.milliseconds.inWholeMinutes.toInt(),
            value.milliseconds.inWholeMinutes.toInt(),
        )

        EVERY_1_HOUR, EVERY_2_HOUR, EVERY_3_HOUR, EVERY_6_HOUR, EVERY_12_HOUR -> {
            context.resources.getQuantityString(
                R.plurals.rss_sync_frequency_hour,
                value.milliseconds.inWholeHours.toInt(),
                value.milliseconds.inWholeHours.toInt(),
            )
        }

        EVERY_1_DAY -> context.resources.getQuantityString(
            R.plurals.frequency_day,
            value.milliseconds.inWholeDays.toInt(),
            value.milliseconds.inWholeDays.toInt(),
        )

        else -> context.getString(R.string.frequency_manual)
    }
}
