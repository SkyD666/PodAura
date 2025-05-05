package com.skyd.podaura.model.preference.rss

import androidx.datastore.preferences.core.longPreferencesKey
import com.skyd.ksp.preference.Preference
import com.skyd.podaura.ext.getOrDefault
import com.skyd.podaura.model.preference.BasePreference
import com.skyd.podaura.model.preference.dataStore
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.frequency_day
import podaura.shared.generated.resources.frequency_manual
import podaura.shared.generated.resources.rss_sync_frequency_hour
import podaura.shared.generated.resources.rss_sync_frequency_minute
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

@Preference
object RssSyncFrequencyPreference : BasePreference<Long>() {
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

    suspend fun toDisplayName(
        value: Long = dataStore.getOrDefault(this),
    ): String = when (value) {
        MANUAL -> getString(Res.string.frequency_manual)
        EVERY_15_MINUTE, EVERY_30_MINUTE -> getPluralString(
            Res.plurals.rss_sync_frequency_minute,
            value.milliseconds.inWholeMinutes.toInt(),
            value.milliseconds.inWholeMinutes.toInt(),
        )

        EVERY_1_HOUR, EVERY_2_HOUR, EVERY_3_HOUR, EVERY_6_HOUR, EVERY_12_HOUR -> {
            getPluralString(
                Res.plurals.rss_sync_frequency_hour,
                value.milliseconds.inWholeHours.toInt(),
                value.milliseconds.inWholeHours.toInt(),
            )
        }

        EVERY_1_DAY -> getPluralString(
            Res.plurals.frequency_day,
            value.milliseconds.inWholeDays.toInt(),
            value.milliseconds.inWholeDays.toInt(),
        )

        else -> getString(Res.string.frequency_manual)
    }
}
