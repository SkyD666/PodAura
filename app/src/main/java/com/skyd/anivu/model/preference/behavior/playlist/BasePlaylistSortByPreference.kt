package com.skyd.anivu.model.preference.behavior.playlist

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.SwipeVertical
import androidx.compose.material.icons.outlined.Title
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.datastore.preferences.core.Preferences
import com.skyd.anivu.R
import com.skyd.anivu.base.BasePreference

abstract class BasePlaylistSortByPreference : BasePreference<String> {
    companion object {
        const val Name = "Name"
        const val MediaCount = "MediaCount"
        const val Manual = "Manual"
        const val CreateTime = "CreateTime"

        fun toDisplayName(
            context: Context,
            value: String,
        ): String = when (value) {
            Name -> context.getString(R.string.sort_name)
            MediaCount -> context.getString(R.string.sort_item_count)
            Manual -> context.getString(R.string.sort_manual)
            CreateTime -> context.getString(R.string.sort_create_time)
            else -> context.getString(R.string.unknown)
        }

        fun toIcon(value: String): ImageVector? = when (value) {
            Name -> Icons.Outlined.Title
            MediaCount -> Icons.Outlined.Subscriptions
            Manual -> Icons.Outlined.SwipeVertical
            CreateTime -> Icons.Outlined.DateRange
            else -> null
        }
    }

    abstract override val key: Preferences.Key<String>
}
