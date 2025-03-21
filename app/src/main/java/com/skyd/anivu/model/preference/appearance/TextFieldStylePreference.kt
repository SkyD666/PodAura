package com.skyd.anivu.model.preference.appearance

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyd.anivu.R
import com.skyd.anivu.base.BasePreference
import com.skyd.anivu.ext.dataStore
import com.skyd.anivu.ext.getOrDefault
import com.skyd.anivu.ui.component.PodAuraTextFieldStyle

object TextFieldStylePreference : BasePreference<String> {
    private const val TEXT_FIELD_STYLE = "textFieldStyle"

    val values = PodAuraTextFieldStyle.entries.map { it.value }

    override val default = PodAuraTextFieldStyle.Normal.value
    override val key = stringPreferencesKey(TEXT_FIELD_STYLE)

    fun toDisplayName(
        context: Context,
        value: String = context.dataStore.getOrDefault(this),
    ): String = when (value) {
        PodAuraTextFieldStyle.Normal.value -> context.getString(R.string.normal_text_field_style)
        PodAuraTextFieldStyle.Outlined.value -> context.getString(R.string.outlined_text_field_style)
        else -> context.getString(R.string.unknown)
    }
}
