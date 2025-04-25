package com.skyd.anivu.model.preference.appearance

import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyd.anivu.model.preference.BasePreference
import com.skyd.anivu.ui.component.PodAuraTextFieldStyle
import com.skyd.ksp.preference.Preference
import org.jetbrains.compose.resources.getString
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.normal_text_field_style
import podaura.shared.generated.resources.outlined_text_field_style
import podaura.shared.generated.resources.unknown

@Preference
object TextFieldStylePreference : BasePreference<String>() {
    private const val TEXT_FIELD_STYLE = "textFieldStyle"

    val values = PodAuraTextFieldStyle.entries.map { it.value }

    override val default = PodAuraTextFieldStyle.Normal.value
    override val key = stringPreferencesKey(TEXT_FIELD_STYLE)

    suspend fun toDisplayName(value: String): String = getString(
        when (value) {
            PodAuraTextFieldStyle.Normal.value -> Res.string.normal_text_field_style
            PodAuraTextFieldStyle.Outlined.value -> Res.string.outlined_text_field_style
            else -> Res.string.unknown
        }
    )
}
