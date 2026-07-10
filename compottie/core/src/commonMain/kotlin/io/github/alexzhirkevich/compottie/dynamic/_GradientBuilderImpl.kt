//package io.github.alexzhirkevich.compottie.dynamic
//
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.geometry.Rect
//import androidx.compose.ui.geometry.takeOrElse
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.Matrix
//import androidx.compose.ui.graphics.Shader
//import io.github.alexzhirkevich.compottie.internal.platform.CachedLinearGradient
//import io.github.alexzhirkevich.compottie.internal.platform.CachedRadialGradient
//import io.github.alexzhirkevich.compottie.internal.platform.GradientCache
//import kotlin.math.min
//
//internal class GradientBuilderImpl : GradientBuilder {
//
//    var shader: ((Rect, Matrix, GradientCache) -> Shader)? = null
//        private set
//
//    override fun linear(colorStops: List<Pair<Float, Color>>, start: Offset, end: Offset) {
//        shader = { rect, mat, cache ->
//            val s = start.takeOrElse { rect.topLeft }
//            val e = start.takeOrElse { rect.bottomRight }
//
//            CachedLinearGradient(
//                from = s,
//                to = e,
//                colors = colorStops.map(Pair<*, Color>::second),
//                colorStops = colorStops.map(Pair<Float, *>::first),
//                matrix = mat,
//                cache = cache
//            )
//        }
//    }
//
//    override fun radial( colorStops: List<Pair<Float, Color>>, center: Offset, radius: Float) {
//        shader = { rect, mat, cache ->
//            val c = center.takeOrElse { rect.center }
//            val r = if (radius.isNaN()) {
//                min(rect.width, rect.height)
//            } else {
//                radius
//            }
//
//            CachedRadialGradient(
//                center = c,
//                radius = r,
//                colors = colorStops.map(Pair<*, Color>::second),
//                colorStops = colorStops.map(Pair<Float, *>::first),
//                matrix = mat,
//                cache = cache
//            )
//        }
//    }
//}