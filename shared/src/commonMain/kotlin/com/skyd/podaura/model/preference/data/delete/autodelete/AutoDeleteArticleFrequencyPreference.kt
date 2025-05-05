package com.skyd.podaura.model.preference.data.delete.autodelete

import androidx.datastore.preferences.core.longPreferencesKey
import com.skyd.ksp.preference.Preference
import com.skyd.podaura.model.preference.BasePreference
import org.jetbrains.compose.resources.getPluralString
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.frequency_day
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

@Preference
object AutoDeleteArticleFrequencyPreference : BasePreference<Long>() {
    private const val AUTO_DELETE_ARTICLE_FREQUENCY = "autoDeleteArticleFrequency"

    override val default = 14.days.inWholeMilliseconds
    override val key = longPreferencesKey(AUTO_DELETE_ARTICLE_FREQUENCY)

    suspend fun toDisplayNameMilliseconds(
        milliseconds: Long,
    ): String = getPluralString(
        Res.plurals.frequency_day,
        milliseconds.milliseconds.inWholeDays.toInt(),
        milliseconds.milliseconds.inWholeDays.toInt(),
    )

    suspend fun toDisplayNameDays(days: Long): String = getPluralString(
        Res.plurals.frequency_day,
        days.toInt(),
        days.toInt(),
    )
}
