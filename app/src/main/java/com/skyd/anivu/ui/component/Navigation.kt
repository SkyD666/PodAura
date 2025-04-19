package com.skyd.anivu.ui.component

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.AnimatedPaneScope
import androidx.compose.material3.adaptive.layout.ExtendedPaneScaffoldPaneScope
import androidx.compose.material3.adaptive.layout.PaneMotionDefaults
import androidx.compose.material3.adaptive.layout.PaneScaffoldValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntRect
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost

val EnterTransition = fadeIn(animationSpec = tween(220, delayMillis = 30)) + scaleIn(
    animationSpec = tween(220, delayMillis = 30),
    initialScale = 0.92f,
)

val ExitTransition = fadeOut(animationSpec = tween(90))

val PopEnterTransition = fadeIn(animationSpec = tween(220)) + scaleIn(
    animationSpec = tween(220),
    initialScale = 0.92f,
)

val PopExitTransition = fadeOut(animationSpec = tween(220)) + scaleOut(
    animationSpec = tween(220),
    targetScale = 0.92f,
)

@Composable
fun PodAuraNavHost(
    navController: NavHostController,
    startDestination: Any,
    builder: NavGraphBuilder.() -> Unit
) = NavHost(
    modifier = Modifier.background(MaterialTheme.colorScheme.background),
    navController = navController,
    startDestination = startDestination,
    enterTransition = { EnterTransition },
    exitTransition = { ExitTransition },
    popEnterTransition = { PopEnterTransition },
    popExitTransition = { PopExitTransition },
    builder = builder,
)

@Composable
fun <S, T : PaneScaffoldValue<S>> ExtendedPaneScaffoldPaneScope<S, T>.PodAuraAnimatedPane(
    modifier: Modifier = Modifier,
    boundsAnimationSpec: FiniteAnimationSpec<IntRect> = PaneMotionDefaults.AnimationSpec,
    content: (@Composable AnimatedPaneScope.() -> Unit),
) = AnimatedPane(
    modifier = modifier,
    enterTransition = EnterTransition,
    exitTransition = ExitTransition,
    boundsAnimationSpec = boundsAnimationSpec,
    content = content,
)