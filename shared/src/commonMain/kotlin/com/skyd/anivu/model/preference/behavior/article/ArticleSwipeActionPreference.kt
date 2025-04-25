package com.skyd.anivu.model.preference.behavior.article

import androidx.datastore.preferences.core.Preferences
import com.skyd.anivu.model.preference.BasePreference
import org.jetbrains.compose.resources.getString
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.add_to_playlist
import podaura.shared.generated.resources.article_action_read
import podaura.shared.generated.resources.article_action_show_enclosures
import podaura.shared.generated.resources.article_action_switch_favorite_state
import podaura.shared.generated.resources.article_action_switch_read_state
import podaura.shared.generated.resources.none
import podaura.shared.generated.resources.open_link_in_browser
import podaura.shared.generated.resources.unknown

abstract class ArticleSwipeActionPreference : BasePreference<String>() {

    companion object {
        const val NONE = "None"
        const val READ = "Read"
        const val SHOW_ENCLOSURES = "ShowEnclosures"
        const val OPEN_LINK_IN_BROWSER = "OpenLinkInBrowser"
        const val SWITCH_READ_STATE = "SwitchReadState"
        const val SWITCH_FAVORITE_STATE = "SwitchFavoriteState"
        const val ADD_TO_PLAYLIST = "AddToPlaylist"

        suspend fun toDisplayName(value: String): String = getString(
            when (value) {
                NONE -> Res.string.none
                READ -> Res.string.article_action_read
                SHOW_ENCLOSURES -> Res.string.article_action_show_enclosures
                OPEN_LINK_IN_BROWSER -> Res.string.open_link_in_browser
                SWITCH_READ_STATE -> Res.string.article_action_switch_read_state
                SWITCH_FAVORITE_STATE -> Res.string.article_action_switch_favorite_state
                ADD_TO_PLAYLIST -> Res.string.add_to_playlist
                else -> Res.string.unknown
            }
        )
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
