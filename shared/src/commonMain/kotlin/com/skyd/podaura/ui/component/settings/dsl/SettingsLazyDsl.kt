package com.skyd.podaura.ui.component.settings.dsl

import androidx.compose.runtime.Composable

internal sealed interface SettingsSingleContent {
    data class SettingsBaseContent(
        val content: @Composable SettingsBaseItemScope.() -> Unit,
    ) : SettingsSingleContent

    data class SettingsOtherContent(
        val content: @Composable SettingsOtherItemScope.() -> Unit,
    ) : SettingsSingleContent
}

internal sealed interface Item {
    data class SingleItem(
        val key: Any? = null,
        val contentType: Any? = null,
        val content: SettingsSingleContent,
    ) : Item

    data class MultipleItem(
        val count: Int,
        val key: ((index: Int) -> Any)?,
        val contentType: (index: Int) -> Any?,
        val isBaseItem: (index: Int) -> Boolean,
        val itemContent: @Composable SettingsItemScope.(Int) -> Unit,
    ) : Item
}