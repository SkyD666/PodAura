package com.skyd.podaura.ui.component.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.PathEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

object MaterialSharedAxis {

    private val MotionEmphasizedEasing = PathEasing(
        Path().apply {
            moveTo(0f, 0f)
            cubicTo(0.05f, 0f, 0.133333f, 0.06f, 0.166666f, 0.4f)
            cubicTo(0.208333f, 0.82f, 0.25f, 1f, 1f, 1f)
        }
    )

    private const val DefaultDuration: Int = 450

    private object FadeThroughProvider {

        private const val ProgressThreshold: Float = 0.35f

        private val FadeInEasing = Easing { fraction ->
            ((MotionEmphasizedEasing.transform(fraction) - ProgressThreshold) / (1f - ProgressThreshold))
                .coerceIn(0f, 1f)
        }

        private val FadeOutEasing = Easing { fraction ->
            (MotionEmphasizedEasing.transform(fraction) / ProgressThreshold)
                .coerceIn(0f, 1f)
        }

        val EnterTransition: EnterTransition = fadeIn(
            animationSpec = tween(durationMillis = DefaultDuration, easing = FadeInEasing)
        )

        val ExitTransition: ExitTransition = fadeOut(
            animationSpec = tween(durationMillis = DefaultDuration, easing = FadeOutEasing)
        )
    }

    private object ScaleProvider {

        val EnterTransition: EnterTransition = scaleIn(
            initialScale = 0.8f,
            animationSpec = tween(
                durationMillis = DefaultDuration,
                easing = MotionEmphasizedEasing
            )
        )

        val ExitTransition: ExitTransition = scaleOut(
            targetScale = 1.1f,
            animationSpec = tween(
                durationMillis = DefaultDuration,
                easing = MotionEmphasizedEasing
            )
        )

        val PopEnterTransition: EnterTransition = scaleIn(
            initialScale = 1.1f,
            animationSpec = tween(
                durationMillis = DefaultDuration,
                easing = MotionEmphasizedEasing
            )
        )

        val PopExitTransition: ExitTransition = scaleOut(
            targetScale = 0.8f,
            animationSpec = tween(
                durationMillis = DefaultDuration,
                easing = MotionEmphasizedEasing
            )
        )
    }

    private object SlideDistanceProvider {

        private val SlideDistance = 30.dp

        val EnterTransition: EnterTransition
            @Composable get() {
                val slideDistancePx = with(LocalDensity.current) { SlideDistance.roundToPx() }
                val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
                return remember(slideDistancePx, isRtl) {
                    // Gravity.END appear: isRtl ? from left : from right
                    slideInHorizontally(
                        initialOffsetX = { if (isRtl) -slideDistancePx else slideDistancePx },
                        animationSpec = tween(DefaultDuration, easing = MotionEmphasizedEasing)
                    )
                }
            }

        val ExitTransition: ExitTransition
            @Composable get() {
                val slideDistancePx = with(LocalDensity.current) { SlideDistance.roundToPx() }
                val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
                return remember(slideDistancePx, isRtl) {
                    // Gravity.END disappear: isRtl ? to right : to left
                    slideOutHorizontally(
                        targetOffsetX = { if (isRtl) slideDistancePx else -slideDistancePx },
                        animationSpec = tween(DefaultDuration, easing = MotionEmphasizedEasing)
                    )
                }
            }

        val PopEnterTransition: EnterTransition
            @Composable get() {
                val slideDistancePx = with(LocalDensity.current) { SlideDistance.roundToPx() }
                val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
                return remember(slideDistancePx, isRtl) {
                    // Gravity.START appear: isRtl ? from right : from left
                    slideInHorizontally(
                        initialOffsetX = { if (isRtl) slideDistancePx else -slideDistancePx },
                        animationSpec = tween(DefaultDuration, easing = MotionEmphasizedEasing)
                    )
                }
            }

        val PopExitTransition: ExitTransition
            @Composable get() {
                val slideDistancePx = with(LocalDensity.current) { SlideDistance.roundToPx() }
                val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
                return remember(slideDistancePx, isRtl) {
                    // Gravity.START disappear: isRtl ? to left : to right
                    slideOutHorizontally(
                        targetOffsetX = { if (isRtl) -slideDistancePx else slideDistancePx },
                        animationSpec = tween(DefaultDuration, easing = MotionEmphasizedEasing)
                    )
                }
            }
    }

    object X {
        val EnterTransition: EnterTransition
            @Composable get() {
                val slideTransition = SlideDistanceProvider.EnterTransition
                val fadeThroughTransition = FadeThroughProvider.EnterTransition
                return remember(slideTransition, fadeThroughTransition) {
                    slideTransition + fadeThroughTransition
                }
            }

        val ExitTransition: ExitTransition
            @Composable get() {
                val slideTransition = SlideDistanceProvider.ExitTransition
                val fadeThroughTransition = FadeThroughProvider.ExitTransition
                return remember(slideTransition, fadeThroughTransition) {
                    slideTransition + fadeThroughTransition
                }
            }

        val PopEnterTransition: EnterTransition
            @Composable get() {
                val slideTransition = SlideDistanceProvider.PopEnterTransition
                val fadeThroughTransition = FadeThroughProvider.EnterTransition
                return remember(slideTransition, fadeThroughTransition) {
                    slideTransition + fadeThroughTransition
                }
            }

        val PopExitTransition: ExitTransition
            @Composable get() {
                val slideTransition = SlideDistanceProvider.PopExitTransition
                val fadeThroughTransition = FadeThroughProvider.ExitTransition
                return remember(slideTransition, fadeThroughTransition) {
                    slideTransition + fadeThroughTransition
                }
            }

        val TransitionSpec: ContentTransform
            @Composable get() {
                val enter = EnterTransition
                val exit = ExitTransition
                return remember(enter, exit) {
                    enter togetherWith exit
                }
            }

        val PopTransitionSpec: ContentTransform
            @Composable get() {
                val enter = PopEnterTransition
                val exit = PopExitTransition
                return remember(enter, exit) {
                    enter togetherWith exit
                }
            }
    }

    object Z {
        val EnterTransition: EnterTransition =
            ScaleProvider.EnterTransition + FadeThroughProvider.EnterTransition

        val ExitTransition: ExitTransition =
            ScaleProvider.ExitTransition + FadeThroughProvider.ExitTransition

        val PopEnterTransition: EnterTransition =
            ScaleProvider.PopEnterTransition + FadeThroughProvider.EnterTransition

        val PopExitTransition: ExitTransition =
            ScaleProvider.PopExitTransition + FadeThroughProvider.ExitTransition

        val TransitionSpec: ContentTransform = EnterTransition togetherWith ExitTransition

        val PopTransitionSpec: ContentTransform = PopEnterTransition togetherWith PopExitTransition
    }
}
