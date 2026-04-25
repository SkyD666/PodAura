package com.skyd.podaura.model.preference.appearance

import com.skyd.ksp.annotation.Preference

@Preference
actual object DarkModePreference : BaseDarkModePreference() {

    actual val values: List<Int> = listOf(
        MODE_NIGHT_FOLLOW_SYSTEM,
        MODE_NIGHT_NO,
        MODE_NIGHT_YES
    )

    actual override val default: Int = MODE_NIGHT_FOLLOW_SYSTEM

    actual override fun onChangeNightMode(mode: Int) {}
}
