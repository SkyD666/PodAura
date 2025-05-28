package com.skyd.podaura.ui.component.settings.dsl

import androidx.compose.runtime.Composable
import com.skyd.podaura.ui.component.settings.dsl.Item.MultipleItem
import com.skyd.podaura.ui.component.settings.dsl.Item.SingleItem

@SettingsLazyScopeMarker
interface SettingsGroupScope {
    fun item(
        key: Any? = null,
        contentType: Any? = null,
        content: @Composable SettingsBaseItemScope.() -> Unit
    )

    fun items(
        count: Int,
        key: ((index: Int) -> Any)? = null,
        contentType: (index: Int) -> Any? = { null },
        isBaseItem: (Int) -> Boolean,
        itemContent: @Composable SettingsItemScope.(Int) -> Unit
    )

    fun otherItem(
        key: Any? = null,
        contentType: Any? = null,
        content: @Composable SettingsOtherItemScope.() -> Unit
    )
}

internal class SettingsGroupScopeImpl : SettingsGroupScope {
    internal val items = mutableListOf<Item>()

    override fun item(
        key: Any?,
        contentType: Any?,
        content: @Composable (SettingsBaseItemScope.() -> Unit),
    ) {
        items += SingleItem(
            key = key,
            contentType = contentType,
            content = SettingsSingleContent.SettingsBaseContent(content),
        )
    }

    override fun items(
        count: Int,
        key: ((Int) -> Any)?,
        contentType: (Int) -> Any?,
        isBaseItem: (Int) -> Boolean,
        itemContent: @Composable SettingsItemScope.(Int) -> Unit
    ) {
        items += MultipleItem(
            count = count,
            key = key,
            contentType = contentType,
            isBaseItem = isBaseItem,
            itemContent = itemContent,
        )
    }

    override fun otherItem(
        key: Any?,
        contentType: Any?,
        content: @Composable (SettingsOtherItemScope.() -> Unit)
    ) {
        items += SingleItem(
            key = key,
            contentType = contentType,
            content = SettingsSingleContent.SettingsOtherContent(content),
        )
    }
}