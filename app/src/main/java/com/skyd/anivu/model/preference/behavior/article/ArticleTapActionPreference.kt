package com.skyd.anivu.model.preference.behavior.article

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyd.anivu.R
import com.skyd.anivu.base.BasePreference
import com.skyd.anivu.ext.dataStore
import com.skyd.anivu.ext.getOrDefault
import com.skyd.ksp.preference.Preference

@Preference
object ArticleTapActionPreference : BasePreference<String> {
    private const val ARTICLE_TAP_ACTION = "articleTapAction"

    const val READ = "Read"
    const val SHOW_ENCLOSURES = "ShowEnclosures"

    val values = arrayOf(READ, SHOW_ENCLOSURES)

    override val default = READ
    override val key = stringPreferencesKey(ARTICLE_TAP_ACTION)

    fun toDisplayName(
        context: Context,
        value: String = context.dataStore.getOrDefault(this),
    ): String = when (value) {
        READ -> context.getString(R.string.article_action_read)
        SHOW_ENCLOSURES -> context.getString(R.string.article_action_show_enclosures)
        else -> context.getString(R.string.unknown)
    }
}
