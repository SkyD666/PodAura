package com.skyd.podaura.model.preference.appearance

import com.skyd.ksp.preference.Preference
import com.skyd.podaura.util.DynamicColorUtil

@Preference
actual object ThemePreference : BaseThemePreference() {
    actual override val default = if (DynamicColorUtil.isDynamicColorAvailable()) DYNAMIC else PINK
}
