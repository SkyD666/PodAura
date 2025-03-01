package com.skyd.anivu.ext

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp

@Composable
operator fun PaddingValues.plus(other: PaddingValues): PaddingValues = PaddingValues(
    top = calculateTopPadding() + other.calculateTopPadding(),
    bottom = calculateBottomPadding() + other.calculateBottomPadding(),
    start = calculateStartPadding(LocalLayoutDirection.current) +
            other.calculateStartPadding(LocalLayoutDirection.current),
    end = calculateEndPadding(LocalLayoutDirection.current) +
            other.calculateEndPadding(LocalLayoutDirection.current)
)

@Composable
operator fun PaddingValues.plus(other: Dp): PaddingValues = this + PaddingValues(other)

@Composable
fun PaddingValues.topPaddingRemoved(): PaddingValues = PaddingValues(
    bottom = calculateBottomPadding(),
    start = calculateStartPadding(LocalLayoutDirection.current),
    end = calculateEndPadding(LocalLayoutDirection.current),
)