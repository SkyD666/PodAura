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

    internal val LocalItemTopBottomSpace = compositionLocalOf { 16.dp }
    internal val LocalItemHorizontalSpace = compositionLocalOf { 16.dp }
    internal val LocalItemVerticalSpace = compositionLocalOf { 1.dp }

    internal val LocalBaseItemVerticalPadding = compositionLocalOf { 14.dp }
    internal val LocalBaseItemBackground = compositionLocalOf<Color?> { null }
    internal val LocalBaseItemEnabled = compositionLocalOf { true }
    internal val LocalBaseItemRoundTop = compositionLocalOf { false }
    internal val LocalBaseItemRoundBottom = compositionLocalOf { false }
    internal val LocalBaseItemUseColorfulIcon = compositionLocalOf { false }

    val itemTopBottomSpace: Dp
        @Composable get() = LocalItemTopBottomSpace.current
    val itemHorizontalSpace: Dp
        @Composable get() = LocalItemHorizontalSpace.current
    val itemVerticalSpace: Dp
        @Composable get() = LocalItemVerticalSpace.current

    val baseItemVerticalPadding: Dp
        @Composable get() = LocalBaseItemVerticalPadding.current
    val baseItemBackground: Color
        @Composable get() = LocalBaseItemBackground.current
            ?: MaterialTheme.colorScheme.surfaceContainerHighest
    val baseItemEnabled: Boolean
        @Composable get() = LocalBaseItemEnabled.current
    val baseItemRoundTop: Boolean
        @Composable get() = LocalBaseItemRoundTop.current
    val baseItemRoundBottom: Boolean
        @Composable get() = LocalBaseItemRoundBottom.current
    val baseItemTopRound: Dp
        @Composable get() = if (baseItemRoundTop) itemRoundLarge else itemRoundSmall
    val baseItemBottomRound: Dp
        @Composable get() = if (baseItemRoundBottom) itemRoundLarge else itemRoundSmall
    val baseItemUseColorfulIcon: Boolean
        @Composable get() = LocalBaseItemUseColorfulIcon.current
}