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
import com.skyd.anivu.ext.dataStore
import com.skyd.anivu.ext.put
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class BaseMediaListSortByPreference : BasePreference<String> {
    companion object {
        const val Date = "Date"
        const val Name = "Name"
        const val FileCount = "FileCount"

        fun toDisplayName(
            context: Context,
            value: String,
        ): String = when (value) {
            Date -> context.getString(R.string.sort_date)
            Name -> context.getString(R.string.sort_name)
            FileCount -> context.getString(R.string.sort_item_count)
            else -> context.getString(R.string.unknown)
        }

        fun toIcon(value: String): ImageVector? = when (value) {
            Date -> Icons.Outlined.DateRange
            Name -> Icons.Outlined.Title
            FileCount -> Icons.AutoMirrored.Outlined.InsertDriveFile
            else -> null
        }
    }

    abstract val key: Preferences.Key<String>

    fun put(context: Context, scope: CoroutineScope, value: String) {
        scope.launch(Dispatchers.IO) {
            context.dataStore.put(key, value)
        }
    }

    override fun fromPreferences(preferences: Preferences): String = preferences[key] ?: default
}
