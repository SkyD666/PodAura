package com.skyd.anivu.model.preference.behavior.media

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Title
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.datastore.preferences.core.Preferences
import com.skyd.anivu.R
import com.skyd.anivu.base.BasePreference

abstract class BaseMediaListSortByPreference : BasePreference<String> {
    companion object {
        const val DATE = "Date"
        const val NAME = "Name"
        const val FILE_COUNT = "FileCount"

        fun toDisplayName(
            context: Context,
            value: String,
        ): String = when (value) {
            DATE -> context.getString(R.string.sort_date)
            NAME -> context.getString(R.string.sort_name)
            FILE_COUNT -> context.getString(R.string.sort_item_count)
            else -> context.getString(R.string.unknown)
        }

        fun toIcon(value: String): ImageVector? = when (value) {
            DATE -> Icons.Outlined.DateRange
            NAME -> Icons.Outlined.Title
            FILE_COUNT -> Icons.AutoMirrored.Outlined.InsertDriveFile
            else -> null
        }
    }

    abstract override val key: Preferences.Key<String>
}
