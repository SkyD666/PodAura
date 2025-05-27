package com.skyd.podaura.ui.component.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.skyd.podaura.ext.plus
import com.skyd.podaura.ui.component.settings.dsl.SettingsLazyListScope
import com.skyd.podaura.ui.component.settings.dsl.SettingsLazyListScopeImpl

@Composable
fun SettingsLazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: SettingsLazyListScope.() -> Unit
) {
    LazyColumn(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding + PaddingValues(vertical = 16.dp),
        content = {
            val scope = SettingsLazyListScopeImpl(this)
            scope.content()
        },
    )
}