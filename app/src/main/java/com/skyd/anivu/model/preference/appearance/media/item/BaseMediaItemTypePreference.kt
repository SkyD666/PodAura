package com.skyd.anivu.model.preference.appearance.media.item

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.Preferences
import com.skyd.anivu.R
import com.skyd.anivu.base.BasePreference
import com.skyd.generated.preference.LocalMediaItemGridTypeMinWidth
import com.skyd.generated.preference.LocalMediaItemListTypeMinWidth

abstract class BaseMediaItemTypePreference : BasePreference<String> {
    companion object {
        const val LIST = "List"
        const val COMPACT_GRID = "CompactGrid"
        const val COMFORTABLE_GRID = "ComfortableGrid"
        const val COVER_GRID = "CoverGrid"

        fun toDisplayName(
            context: Context,
            value: String,
        ): String = when (value) {
            LIST -> context.getString(R.string.list_item_type_list)
            COMPACT_GRID -> context.getString(R.string.list_item_type_compact_grid)
            COMFORTABLE_GRID -> context.getString(R.string.list_item_type_comfortable_grid)
            COVER_GRID -> context.getString(R.string.list_item_type_cover_grid)
            else -> context.getString(R.string.unknown)
        }

        @Composable
        fun toMinWidth(
            value: String,
        ): Float = when (value) {
            LIST -> LocalMediaItemListTypeMinWidth.current
            COMPACT_GRID,
            COMFORTABLE_GRID,
            COVER_GRID -> LocalMediaItemGridTypeMinWidth.current

            else -> MediaItemListTypeMinWidthPreference.default
        }
    }

    abstract override val key: Preferences.Key<String>
}
