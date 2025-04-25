package com.skyd.anivu.model.preference.appearance.media.item

import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.Preferences
import com.skyd.anivu.model.preference.BasePreference
import org.jetbrains.compose.resources.getString
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.list_item_type_comfortable_grid
import podaura.shared.generated.resources.list_item_type_compact_grid
import podaura.shared.generated.resources.list_item_type_cover_grid
import podaura.shared.generated.resources.list_item_type_list
import podaura.shared.generated.resources.unknown

abstract class BaseMediaItemTypePreference : BasePreference<String>() {
    companion object {
        const val LIST = "List"
        const val COMPACT_GRID = "CompactGrid"
        const val COMFORTABLE_GRID = "ComfortableGrid"
        const val COVER_GRID = "CoverGrid"

        suspend fun toDisplayName(value: String): String = getString(
            when (value) {
                LIST -> Res.string.list_item_type_list
                COMPACT_GRID -> Res.string.list_item_type_compact_grid
                COMFORTABLE_GRID -> Res.string.list_item_type_comfortable_grid
                COVER_GRID -> Res.string.list_item_type_cover_grid
                else -> Res.string.unknown
            }
        )

        @Composable
        fun toMinWidth(value: String): Float = when (value) {
            LIST -> MediaItemListTypeMinWidthPreference.current
            COMPACT_GRID,
            COMFORTABLE_GRID,
            COVER_GRID -> MediaItemGridTypeMinWidthPreference.current

            else -> MediaItemListTypeMinWidthPreference.default
        }
    }

    abstract override val key: Preferences.Key<String>
}
