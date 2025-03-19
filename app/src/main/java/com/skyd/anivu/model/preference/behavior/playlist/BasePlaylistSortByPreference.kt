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
        const val NAME = "Name"
        const val MEDIA_COUNT = "MediaCount"
        const val MANUAL = "Manual"
        const val CREATE_TIME = "CreateTime"

        fun toDisplayName(
            context: Context,
            value: String,
        ): String = when (value) {
            NAME -> context.getString(R.string.sort_name)
            MEDIA_COUNT -> context.getString(R.string.sort_item_count)
            MANUAL -> context.getString(R.string.sort_manual)
            CREATE_TIME -> context.getString(R.string.sort_create_time)
            else -> context.getString(R.string.unknown)
        }

        fun toIcon(value: String): ImageVector? = when (value) {
            NAME -> Icons.Outlined.Title
            MEDIA_COUNT -> Icons.Outlined.Subscriptions
            MANUAL -> Icons.Outlined.SwipeVertical
            CREATE_TIME -> Icons.Outlined.DateRange
            else -> null
        }
    }

    abstract override val key: Preferences.Key<String>
}
