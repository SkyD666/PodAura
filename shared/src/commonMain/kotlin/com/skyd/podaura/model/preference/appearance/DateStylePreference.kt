package com.skyd.podaura.model.preference.appearance

import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyd.ksp.preference.Preference
import com.skyd.podaura.model.preference.BasePreference
import org.jetbrains.compose.resources.getString
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.date_style_full
import podaura.shared.generated.resources.date_style_relative
import podaura.shared.generated.resources.unknown

@Preference
object DateStylePreference : BasePreference<String>() {
    private const val DATE_STYLE = "dateStyle"

    const val RELATIVE = "Relative"
    private const val FULL = "Full"
    val values = arrayOf(RELATIVE, FULL)

    override val default = RELATIVE
    override val key = stringPreferencesKey(DATE_STYLE)

    suspend fun toDisplayName(value: String): String = getString(
        when (value) {
            RELATIVE -> Res.string.date_style_relative
            FULL -> Res.string.date_style_full
            else -> Res.string.unknown
        }
    )
}
