package com.skyd.podaura.ui.component.settings.dsl

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.skyd.podaura.ui.component.settings.CategorySettingsItem
import com.skyd.podaura.ui.component.settings.LocalItemEnabled
import com.skyd.podaura.ui.component.settings.SettingsDefaults
import com.skyd.podaura.ui.component.suspendString

internal enum class ItemType {
    Base, Other
}

internal data class Item(
    val key: Any? = null,
    val contentType: Any? = null,
    val content: @Composable LazyItemScope.() -> Unit,
    val type: ItemType,
) {
    val isBaseType: Boolean
        get() = type == ItemType.Base
}

@SettingsLazyScopeMarker
interface SettingsLazyListScope {
    fun item(
        key: Any? = null,
        contentType: Any? = null,
        content: @Composable LazyItemScope.() -> Unit
    )

    fun group(
        enabled: Boolean = true,
        content: SettingsGroupScope.() -> Unit
    )

    fun group(
        category: @Composable () -> Unit,
        enabled: Boolean = true,
        content: SettingsGroupScope.() -> Unit
    )

    fun group(
        text: suspend () -> String,
        enabled: Boolean = true,
        content: SettingsGroupScope.() -> Unit
    )
}

@SettingsLazyScopeMarker
interface SettingsGroupScope {
    fun item(
        key: Any? = null,
        contentType: Any? = null,
        content: @Composable LazyItemScope.() -> Unit
    )

    fun otherItem(
        key: Any? = null,
        contentType: Any? = null,
        content: @Composable LazyItemScope.() -> Unit
    )
}

internal class SettingsLazyListScopeImpl(
    internal val lazyListScope: LazyListScope,
) : SettingsLazyListScope {
    private var isFirstItem = true

    override fun item(
        key: Any?,
        contentType: Any?,
        content: @Composable (LazyItemScope.() -> Unit),
    ) {
        lazyListScope.item(
            key = key,
            contentType = contentType,
            content = content,
        )
        isFirstItem = false
    }

    override fun group(
        enabled: Boolean,
        content: SettingsGroupScope.() -> Unit
    ) = groupImpl(
        category = null,
        enabled = enabled,
        content = content,
    )

    override fun group(
        category: @Composable (() -> Unit),
        enabled: Boolean,
        content: SettingsGroupScope.() -> Unit
    ) = groupImpl(
        category = category,
        enabled = enabled,
        content = content,
    )

    private fun groupImpl(
        category: @Composable (() -> Unit)?,
        enabled: Boolean,
        content: SettingsGroupScope.() -> Unit
    ) {
        if (!isFirstItem) {
            lazyListScope.item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        isFirstItem = false
        if (category != null) {
            lazyListScope.item {
                category()
            }
        }
        val groupScope = SettingsGroupScopeImpl()
        groupScope.content()
        val items = groupScope.items
        items.forEachIndexed { index, itemData ->
            val lazyContent = itemData.content
            lazyListScope.item(
                key = itemData.key,
                contentType = itemData.contentType,
            ) {
                if (itemData.type == ItemType.Base) {
                    val prevIsBase = items.getOrNull(index - 1)?.isBaseType == true
                    val nextIsBase = items.getOrNull(index + 1)?.isBaseType == true
                    CompositionLocalProvider(
                        LocalItemEnabled provides enabled,
                        SettingsDefaults.LocalBaseItemRoundTop provides !prevIsBase,
                        SettingsDefaults.LocalBaseItemRoundBottom provides !nextIsBase,
                    ) {
                        lazyContent()
                    }
                } else {
                    lazyContent()
                }
            }
        }
    }

    override fun group(
        text: suspend () -> String,
        enabled: Boolean,
        content: SettingsGroupScope.() -> Unit
    ) = groupImpl(
        category = { CategorySettingsItem(suspendString { text() }) },
        enabled = enabled,
        content = content,
    )
}

internal class SettingsGroupScopeImpl : SettingsGroupScope {
    internal val items = mutableListOf<Item>()

    override fun item(
        key: Any?,
        contentType: Any?,
        content: @Composable (LazyItemScope.() -> Unit),
    ) {
        items += Item(
            key = key,
            contentType = contentType,
            content = content,
            type = ItemType.Base,
        )
    }

    override fun otherItem(
        key: Any?,
        contentType: Any?,
        content: @Composable (LazyItemScope.() -> Unit)
    ) {
        items += Item(
            key = key,
            contentType = contentType,
            content = content,
            type = ItemType.Other,
        )
    }
}