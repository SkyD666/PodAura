package com.skyd.podaura.model.preference.appearance

import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import com.skyd.ksp.preference.Preference

@Preference
actual object DarkModePreference : BaseDarkModePreference() {
    actual val values: List<Int> = buildList {
        add(MODE_NIGHT_NO)
        add(MODE_NIGHT_YES)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            add(MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    actual override val default = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MODE_NIGHT_FOLLOW_SYSTEM
    } else {
        MODE_NIGHT_NO
    }

    private fun toAndroidNightMode(mode: Int) = when (mode) {
        MODE_NIGHT_NO -> AppCompatDelegate.MODE_NIGHT_NO
        MODE_NIGHT_YES -> AppCompatDelegate.MODE_NIGHT_YES
        MODE_NIGHT_FOLLOW_SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }

    override fun onChangeNightMode(mode: Int) {
        AppCompatDelegate.setDefaultNightMode(toAndroidNightMode(mode))
    }
}