package com.skyd.podaura.model.preference.appearance

import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyd.compone.component.ComponeTextFieldStyle
import com.skyd.ksp.annotation.Preference
import com.skyd.podaura.model.preference.BasePreference
import org.jetbrains.compose.resources.getString
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.normal_text_field_style
import podaura.shared.generated.resources.outlined_text_field_style
import podaura.shared.generated.resources.unknown

@Preference
object TextFieldStylePreference : BasePreference<String>() {
    private const val TEXT_FIELD_STYLE = "textFieldStyle"

    val values = ComponeTextFieldStyle.entries.map { it.value }

    override val default = ComponeTextFieldStyle.Normal.value
    override val key = stringPreferencesKey(TEXT_FIELD_STYLE)

    suspend fun toDisplayName(value: String): String = getString(
        when (value) {
            ComponeTextFieldStyle.Normal.value -> Res.string.normal_text_field_style
            ComponeTextFieldStyle.Outlined.value -> Res.string.outlined_text_field_style
            else -> Res.string.unknown
        }
    )
}
