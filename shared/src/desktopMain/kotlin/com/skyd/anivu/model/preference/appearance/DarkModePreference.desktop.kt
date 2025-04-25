package com.skyd.anivu.model.preference.appearance

import com.skyd.ksp.preference.Preference

@Preference
actual object DarkModePreference : BaseDarkModePreference() {
    actual val values: List<Int> = TODO()

    actual override val default: Int = TODO()

    override fun onChangeNightMode(mode: Int) {
        TODO()
    }
}