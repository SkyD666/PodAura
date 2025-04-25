package com.skyd.anivu.model.preference.behavior.media

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Title
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.datastore.preferences.core.Preferences
import com.skyd.anivu.model.preference.BasePreference
import org.jetbrains.compose.resources.getString
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.sort_date
import podaura.shared.generated.resources.sort_item_count
import podaura.shared.generated.resources.sort_name
import podaura.shared.generated.resources.unknown

abstract class BaseMediaListSortByPreference : BasePreference<String>() {
    companion object {
        const val DATE = "Date"
        const val NAME = "Name"
        const val FILE_COUNT = "FileCount"

        suspend fun toDisplayName(value: String): String = when (value) {
            DATE -> getString(Res.string.sort_date)
            NAME -> getString(Res.string.sort_name)
            FILE_COUNT -> getString(Res.string.sort_item_count)
            else -> getString(Res.string.unknown)
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
