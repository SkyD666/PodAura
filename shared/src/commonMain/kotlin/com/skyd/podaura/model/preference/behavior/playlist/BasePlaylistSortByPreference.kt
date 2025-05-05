package com.skyd.podaura.model.preference.behavior.playlist

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.SwipeVertical
import androidx.compose.material.icons.outlined.Title
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.datastore.preferences.core.Preferences
import com.skyd.podaura.model.preference.BasePreference
import org.jetbrains.compose.resources.getString
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.sort_create_time
import podaura.shared.generated.resources.sort_item_count
import podaura.shared.generated.resources.sort_manual
import podaura.shared.generated.resources.sort_name
import podaura.shared.generated.resources.unknown

abstract class BasePlaylistSortByPreference : BasePreference<String>() {
    companion object {
        const val NAME = "Name"
        const val MEDIA_COUNT = "MediaCount"
        const val MANUAL = "Manual"
        const val CREATE_TIME = "CreateTime"

        suspend fun toDisplayName(value: String): String = when (value) {
            NAME -> getString(Res.string.sort_name)
            MEDIA_COUNT -> getString(Res.string.sort_item_count)
            MANUAL -> getString(Res.string.sort_manual)
            CREATE_TIME -> getString(Res.string.sort_create_time)
            else -> getString(Res.string.unknown)
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
