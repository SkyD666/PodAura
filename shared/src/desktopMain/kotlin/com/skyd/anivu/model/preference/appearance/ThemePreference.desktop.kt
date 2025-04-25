package com.skyd.anivu.model.preference.appearance

import com.skyd.ksp.preference.Preference

@Preference
actual object ThemePreference : BaseThemePreference() {
    actual override val default = PINK
}