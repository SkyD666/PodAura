package com.skyd.podaura.ui.player

import androidx.datastore.preferences.core.mutablePreferencesOf
import com.skyd.podaura.ext.get
import com.skyd.podaura.model.preference.player.BackgroundPlayPreference
import com.skyd.podaura.model.preference.player.DirectBackgroundPlayPreference
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DirectBackgroundPlayTest {
    @Test
    fun disablingBackgroundPlaybackResetsTheOptionAndReenablingDoesNotRestoreIt() {
        val preferences = mutablePreferencesOf()
        assertFalse(preferences[DirectBackgroundPlayPreference])
        DirectBackgroundPlayPreference.update(preferences, true)
        assertTrue(preferences[DirectBackgroundPlayPreference])
        BackgroundPlayPreference.update(preferences, false)
        assertFalse(preferences[DirectBackgroundPlayPreference])
        DirectBackgroundPlayPreference.update(preferences, true)
        assertFalse(preferences[DirectBackgroundPlayPreference])
        BackgroundPlayPreference.update(preferences, true)
        assertFalse(preferences[DirectBackgroundPlayPreference])
        DirectBackgroundPlayPreference.update(preferences, true)
        assertTrue(preferences[DirectBackgroundPlayPreference])
    }
}
