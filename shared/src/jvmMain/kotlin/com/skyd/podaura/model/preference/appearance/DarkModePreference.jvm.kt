package com.skyd.podaura.model.preference.appearance

import com.skyd.ksp.preference.Preference

@Preference
actual object DarkModePreference : BaseDarkModePreference() {
    actual val values: List<Int> = buildList {
        add(MODE_NIGHT_NO)
        add(MODE_NIGHT_YES)
    }

    actual override val default: Int = MODE_NIGHT_NO

    override fun onChangeNightMode(mode: Int) {
    }
}