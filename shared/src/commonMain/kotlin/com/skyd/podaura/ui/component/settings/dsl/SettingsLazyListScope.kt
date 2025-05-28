package com.skyd.podaura.ui.component.settings.dsl

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.skyd.podaura.ui.component.settings.CategorySettingsItem
import com.skyd.podaura.ui.component.settings.SettingsDefaults
import com.skyd.podaura.ui.component.suspendString

@SettingsLazyScopeMarker
interface SettingsLazyListScope {
    fun item(
        key: Any? = null,
        contentType: Any? = null,
        content: @Composable SettingsItemScope.() -> Unit
    )

    fun items(
        count: Int,
        key: ((index: Int) -> Any)? = null,
        contentType: (index: Int) -> Any? = { null },
        itemContent: @Composable SettingsItemScope.(index: Int) -> Unit
    )

    fun group(
        enabled: Boolean = true,
        content: SettingsGroupScope.() -> Unit
    )

    fun group(
        category: @Composable SettingsOtherItemScope.() -> Unit,
        enabled: Boolean = true,
        content: SettingsGroupScope.() -> Unit
    )

    fun group(
        text: suspend () -> String,
        enabled: Boolean = true,
        content: SettingsGroupScope.() -> Unit
    )
}

internal class SettingsLazyListScopeImpl(
    internal val lazyListScope: LazyListScope,
) : SettingsLazyListScope {
    private var isFirstItem = true

    override fun item(
        key: Any?,
        contentType: Any?,
        content: @Composable (SettingsItemScope.() -> Unit),
    ) {
        lazyListScope.item(
            key = key,
            contentType = contentType,
            content = { remember(this) { SettingsItemScopeImpl(this) }.content() },
        )
        if (isFirstItem) isFirstItem = false
    }

    override fun items(
        count: Int,
        key: ((Int) -> Any)?,
        contentType: (Int) -> Any?,
        itemContent: @Composable (SettingsItemScope.(Int) -> Unit)
    ) {
        lazyListScope.items(
            count = count,
            key = key,
            contentType = contentType,
            itemContent = { remember(this) { SettingsItemScopeImpl(this) }.itemContent(it) },
        )
        if (isFirstItem) isFirstItem = false
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
        category: @Composable (SettingsOtherItemScope.() -> Unit),
        enabled: Boolean,
        content: SettingsGroupScope.() -> Unit
    ) = groupImpl(
        category = category,
        enabled = enabled,
        content = content,
    )

    private fun groupImpl(
        category: @Composable (SettingsOtherItemScope.() -> Unit)?,
        enabled: Boolean,
        content: SettingsGroupScope.() -> Unit
    ) {
        if (!isFirstItem) {
            lazyListScope.item {
                Spacer(modifier = Modifier.Companion.height(16.dp))
            }
        }
        if (isFirstItem) isFirstItem = false
        if (category != null) {
            lazyListScope.item {
                remember(this) { SettingsOtherItemScopeImpl(this) }.category()
            }
        }
        val groupScope = SettingsGroupScopeImpl()
        groupScope.content()
        val items = groupScope.items
        items.forEachIndexed { index, itemData ->
            val prevIsBase = isBase(items = items, index = index - 1)
            val nextIsBase = isBase(items = items, index = index + 1)
            when (itemData) {
                is Item.MultipleItem -> {
                    val itemContent = itemData.itemContent
                    val isBaseItem = itemData.isBaseItem(index)
                    lazyListScope.items(
                        count = itemData.count,
                        key = itemData.key,
                        contentType = itemData.contentType,
                    ) {
                        if (isBaseItem) {
                            CompositionLocalProvider(
                                SettingsDefaults.LocalBaseItemEnabled provides enabled,
                                SettingsDefaults.LocalBaseItemRoundTop provides !prevIsBase,
                                SettingsDefaults.LocalBaseItemRoundBottom provides !nextIsBase,
                            ) {
                                remember(this) { SettingsItemScopeImpl(this) }.itemContent(index)
                            }
                        } else {
                            remember(this) { SettingsItemScopeImpl(this) }.itemContent(index)
                        }
                    }
                }

                is Item.SingleItem -> {
                    val lazyContent = itemData.content
                    lazyListScope.item(
                        key = itemData.key,
                        contentType = itemData.contentType,
                    ) {
                        when (lazyContent) {
                            is SettingsSingleContent.SettingsBaseContent -> {
                                CompositionLocalProvider(
                                    SettingsDefaults.LocalBaseItemEnabled provides enabled,
                                    SettingsDefaults.LocalBaseItemRoundTop provides !prevIsBase,
                                    SettingsDefaults.LocalBaseItemRoundBottom provides !nextIsBase,
                                ) {
                                    lazyContent.content(remember(this) {
                                        SettingsBaseItemScopeImpl(
                                            this
                                        )
                                    })
                                }
                            }

                            is SettingsSingleContent.SettingsOtherContent -> {
                                lazyContent.content(remember(this) { SettingsOtherItemScopeImpl(this) })
                            }
                        }
                    }
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

    private fun isBase(items: MutableList<Item>, index: Int): Boolean {
        val item = items.getOrNull(index)
        return when (item) {
            is Item.MultipleItem -> item.isBaseItem(index)
            is Item.SingleItem -> item.content is SettingsSingleContent.SettingsBaseContent
            null -> false
        }
    }
}

inline fun <T> SettingsLazyListScope.items(
    items: List<T>,
    noinline key: ((item: T) -> Any)? = null,
    noinline contentType: (item: T) -> Any? = { null },
    crossinline itemContent: @Composable SettingsItemScope.(item: T) -> Unit
) = items(
    count = items.size,
    key = if (key != null) { index: Int -> key(items[index]) } else null,
    contentType = { index: Int -> contentType(items[index]) }
) {
    itemContent(items[it])
}