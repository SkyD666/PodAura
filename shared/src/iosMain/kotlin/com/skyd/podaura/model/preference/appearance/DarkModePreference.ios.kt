package com.skyd.podaura.model.preference.appearance

import com.skyd.ksp.annotation.Preference

@Preference
actual object DarkModePreference : BaseDarkModePreference() {
    actual val values: List<Int> = buildList {
        add(MODE_NIGHT_FOLLOW_SYSTEM)
        add(MODE_NIGHT_NO)
        add(MODE_NIGHT_YES)
    }

    actual override val default: Int = MODE_NIGHT_FOLLOW_SYSTEM

    actual override fun onChangeNightMode(mode: Int) {}
}
