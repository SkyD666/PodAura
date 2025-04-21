package com.skyd.anivu.model.preference.appearance.media.item

import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyd.ksp.preference.Preference

@Preference
object MediaListItemTypePreference : BaseMediaItemTypePreference() {
    private const val MEDIA_LIST_ITEM_TYPE = "mediaListItemType"

    val values = listOf(LIST, COMPACT_GRID, COMFORTABLE_GRID, COVER_GRID)

    override val default = LIST

    override val key = stringPreferencesKey(MEDIA_LIST_ITEM_TYPE)
}
