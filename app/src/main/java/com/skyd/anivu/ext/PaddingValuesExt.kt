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
fun PaddingValues.withoutTop(): PaddingValues = PaddingValues(
    bottom = calculateBottomPadding(),
    start = calculateStartPadding(LocalLayoutDirection.current),
    end = calculateEndPadding(LocalLayoutDirection.current),
)

@Composable
fun PaddingValues.withoutBottom(): PaddingValues = PaddingValues(
    top = calculateTopPadding(),
    start = calculateStartPadding(LocalLayoutDirection.current),
    end = calculateEndPadding(LocalLayoutDirection.current),
)

@Composable
fun PaddingValues.withoutStart(): PaddingValues = PaddingValues(
    top = calculateTopPadding(),
    bottom = calculateBottomPadding(),
    end = calculateEndPadding(LocalLayoutDirection.current),
)

@Composable
fun PaddingValues.withoutEnd(): PaddingValues = PaddingValues(
    top = calculateTopPadding(),
    bottom = calculateBottomPadding(),
    start = calculateStartPadding(LocalLayoutDirection.current),
)

@Composable
fun PaddingValues.onlyHorizontal(): PaddingValues = PaddingValues(
    start = calculateStartPadding(LocalLayoutDirection.current),
    end = calculateEndPadding(LocalLayoutDirection.current),
)

fun PaddingValues.onlyVertical(): PaddingValues = PaddingValues(
    top = calculateTopPadding(),
    bottom = calculateBottomPadding(),
)