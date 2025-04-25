package com.skyd.anivu.model.preference.behavior.article

import androidx.datastore.preferences.core.stringPreferencesKey
import com.skyd.anivu.model.preference.BasePreference
import com.skyd.ksp.preference.Preference
import org.jetbrains.compose.resources.getString
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.article_action_read
import podaura.shared.generated.resources.article_action_show_enclosures
import podaura.shared.generated.resources.unknown

@Preference
object ArticleTapActionPreference : BasePreference<String>() {
    private const val ARTICLE_TAP_ACTION = "articleTapAction"

    const val READ = "Read"
    const val SHOW_ENCLOSURES = "ShowEnclosures"

    val values = arrayOf(READ, SHOW_ENCLOSURES)

    override val default = READ
    override val key = stringPreferencesKey(ARTICLE_TAP_ACTION)

    suspend fun toDisplayName(value: String): String = getString(
        when (value) {
            READ -> Res.string.article_action_read
            SHOW_ENCLOSURES -> Res.string.article_action_show_enclosures
            else -> Res.string.unknown
        }
    )
}
