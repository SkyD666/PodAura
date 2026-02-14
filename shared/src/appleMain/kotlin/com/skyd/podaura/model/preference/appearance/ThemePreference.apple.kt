package com.skyd.podaura.model.preference.appearance

import com.skyd.ksp.annotation.Preference

@Preference
actual object ThemePreference : BaseThemePreference() {
    actual override val default = PINK
}
