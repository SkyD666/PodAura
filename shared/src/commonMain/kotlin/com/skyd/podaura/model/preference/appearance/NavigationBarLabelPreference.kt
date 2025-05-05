package com.skyd.podaura.model.preference.appearance

import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyd.ksp.preference.Preference
import com.skyd.podaura.model.preference.BasePreference
import org.jetbrains.compose.resources.getString
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.navigation_bar_label_none
import podaura.shared.generated.resources.navigation_bar_label_show
import podaura.shared.generated.resources.navigation_bar_label_show_on_active
import podaura.shared.generated.resources.unknown

@Preference
object NavigationBarLabelPreference : BasePreference<String>() {
    private const val NAVIGATION_BAR_LABEL = "navigationBarLabel"

    const val SHOW = "Show"
    const val SHOW_ON_ACTIVE = "ShowOnActive"
    const val NONE = "None"
    val values = arrayOf(SHOW, SHOW_ON_ACTIVE, NONE)

    override val default = SHOW
    override val key = stringPreferencesKey(NAVIGATION_BAR_LABEL)

    suspend fun toDisplayName(value: String): String = getString(
        when (value) {
            SHOW -> Res.string.navigation_bar_label_show
            SHOW_ON_ACTIVE -> Res.string.navigation_bar_label_show_on_active
            NONE -> Res.string.navigation_bar_label_none
            else -> Res.string.unknown
        }
    )
}