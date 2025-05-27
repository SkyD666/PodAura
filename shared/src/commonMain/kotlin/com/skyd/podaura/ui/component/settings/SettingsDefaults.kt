package com.skyd.podaura.ui.component.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object SettingsDefaults {
    internal val itemRoundLarge = 20.dp
    internal val itemRoundSmall = 5.dp

    internal val LocalItemHorizontalSpace = compositionLocalOf { 16.dp }
    internal val LocalItemVerticalSpace = compositionLocalOf { 1.dp }
    internal val LocalBaseItemBackground = compositionLocalOf<Color?> { null }
    internal val LocalBaseItemRoundTop = compositionLocalOf { false }
    internal val LocalBaseItemRoundBottom = compositionLocalOf { false }

    val itemHorizontalSpace: Dp
        @Composable get() = LocalItemHorizontalSpace.current
    val itemVerticalSpace: Dp
        @Composable get() = LocalItemVerticalSpace.current
    val baseItemBackground: Color
        @Composable get() = LocalBaseItemBackground.current
            ?: MaterialTheme.colorScheme.surfaceContainerHighest
    val baseItemRoundTop: Boolean
        @Composable get() = LocalBaseItemRoundTop.current
    val baseItemRoundBottom: Boolean
        @Composable get() = LocalBaseItemRoundBottom.current

    val topRound: Dp
        @Composable get() = if (baseItemRoundTop) itemRoundLarge else itemRoundSmall
    val bottomRound: Dp
        @Composable get() = if (baseItemRoundBottom) itemRoundLarge else itemRoundSmall
}