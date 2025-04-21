package com.skyd.anivu.model.preference.appearance.media.item

import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyd.ksp.preference.Preference

@Preference
object MediaSubListItemTypePreference : BaseMediaItemTypePreference() {
    private const val MEDIA_SUB_LIST_ITEM_TYPE = "mediaSubListItemType"

    val values = listOf(LIST, COMPACT_GRID, COMFORTABLE_GRID, COVER_GRID)

    override val default = LIST

    override val key = stringPreferencesKey(MEDIA_SUB_LIST_ITEM_TYPE)
}
