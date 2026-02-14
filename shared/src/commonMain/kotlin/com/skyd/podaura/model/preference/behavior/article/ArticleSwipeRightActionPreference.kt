package com.skyd.podaura.model.preference.behavior.article

import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyd.ksp.annotation.Preference

@Preference
object ArticleSwipeRightActionPreference : ArticleSwipeActionPreference() {
    private const val ARTICLE_SWIPE_RIGHT_ACTION = "articleSwipeRightAction"

    override val default = SWITCH_FAVORITE_STATE

    override val key = stringPreferencesKey(ARTICLE_SWIPE_RIGHT_ACTION)
}
