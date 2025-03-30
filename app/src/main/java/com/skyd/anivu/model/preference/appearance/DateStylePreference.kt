package com.skyd.anivu.model.preference.appearance

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyd.anivu.R
import com.skyd.anivu.base.BasePreference
import com.skyd.anivu.ext.dataStore
import com.skyd.anivu.ext.getOrDefault
import com.skyd.ksp.preference.Preference

@Preference
object DateStylePreference : BasePreference<String> {
    private const val DATE_STYLE = "dateStyle"

    const val RELATIVE = "Relative"
    private const val FULL = "Full"
    val values = arrayOf(RELATIVE, FULL)

    override val default = RELATIVE
    override val key = stringPreferencesKey(DATE_STYLE)

    fun toDisplayName(
        context: Context,
        value: String = context.dataStore.getOrDefault(this),
    ): String = when (value) {
        RELATIVE -> context.getString(R.string.date_style_relative)
        FULL -> context.getString(R.string.date_style_full)
        else -> context.getString(R.string.unknown)
    }
}
