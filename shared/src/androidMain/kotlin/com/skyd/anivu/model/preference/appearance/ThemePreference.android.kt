package com.skyd.anivu.model.preference.appearance

import com.google.android.material.color.DynamicColors
import com.skyd.ksp.preference.Preference

@Preference
actual object ThemePreference : BaseThemePreference() {
    actual override val default = if (DynamicColors.isDynamicColorAvailable()) DYNAMIC else PINK
}
