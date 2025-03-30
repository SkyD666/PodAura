package com.skyd.anivu.model.preference.behavior.article

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyd.anivu.ext.dataStore
import com.skyd.anivu.ext.getOrDefault
import com.skyd.ksp.preference.Preference

@Preference
object ArticleSwipeRightActionPreference : ArticleSwipeActionPreference() {
    private const val ARTICLE_SWIPE_RIGHT_ACTION = "articleSwipeRightAction"

    override val default = SWITCH_FAVORITE_STATE

    override val key = stringPreferencesKey(ARTICLE_SWIPE_RIGHT_ACTION)

    fun toDisplayName(
        context: Context,
        value: String = context.dataStore.getOrDefault(this),
    ): String = ArticleSwipeActionPreference.toDisplayName(context, value)
}
