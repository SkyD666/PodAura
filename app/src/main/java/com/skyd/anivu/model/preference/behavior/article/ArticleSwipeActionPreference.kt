package com.skyd.anivu.model.preference.behavior.article

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import com.skyd.anivu.R
import com.skyd.anivu.base.BasePreference

abstract class ArticleSwipeActionPreference : BasePreference<String> {

    companion object {
        const val NONE = "None"
        const val READ = "Read"
        const val SHOW_ENCLOSURES = "ShowEnclosures"
        const val OPEN_LINK_IN_BROWSER = "OpenLinkInBrowser"
        const val SWITCH_READ_STATE = "SwitchReadState"
        const val SWITCH_FAVORITE_STATE = "SwitchFavoriteState"
        const val ADD_TO_PLAYLIST = "AddToPlaylist"

        fun toDisplayName(
            context: Context,
            value: String,
        ): String = when (value) {
            NONE -> context.getString(R.string.none)
            READ -> context.getString(R.string.article_action_read)
            SHOW_ENCLOSURES -> context.getString(R.string.article_action_show_enclosures)
            OPEN_LINK_IN_BROWSER -> context.getString(R.string.open_link_in_browser)
            SWITCH_READ_STATE -> context.getString(R.string.article_action_switch_read_state)
            SWITCH_FAVORITE_STATE -> context.getString(R.string.article_action_switch_favorite_state)
            ADD_TO_PLAYLIST -> context.getString(R.string.add_to_playlist)
            else -> context.getString(R.string.unknown)
        }
    }

    val values = arrayOf(
        NONE,
        READ,
        SHOW_ENCLOSURES,
        OPEN_LINK_IN_BROWSER,
        SWITCH_READ_STATE,
        SWITCH_FAVORITE_STATE,
        ADD_TO_PLAYLIST,
    )

    abstract override val key: Preferences.Key<String>
}
