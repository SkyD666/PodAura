package io.github.alexzhirkevich.compottie

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

/**
 * Use subclasses of [LottieClipSpec] to set min/max bounds on the animation playback.
 *
 * @see rememberLottieAnimatable
 * @see animateLottieCompositionAsState
 */

@Stable
public sealed class LottieClipSpec {

    internal abstract fun getMinProgress(composition: LottieComposition): Float

    internal abstract fun getMaxProgress(composition: LottieComposition): Float

    /**
     * Play the animation between these two frames. [maxInclusive] determines whether the animation
     * should play the max frame or stop one frame before it.
     */
    @Immutable
    public class Frame(
        public val min: Int?,
        public val max: Int?,
        public val maxInclusive: Boolean = true,
    ) : LottieClipSpec() {

        private val actualMaxFrame = when {
            max == null -> null
            maxInclusive -> max
            else -> max - 1
        }

        override fun getMinProgress(composition: LottieComposition): Float {
            return when (min) {
                null -> 0f
                else -> (min / composition.endFrame).coerceIn(0f, 1f)
            }
        }

        override fun getMaxProgress(composition: LottieComposition): Float {
            return when (actualMaxFrame) {
                null -> 1f
                else -> (actualMaxFrame / composition.endFrame).coerceIn(0f, 1f)
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Frame

            if (min != other.min) return false
            if (max != other.max) return false
            if (maxInclusive != other.maxInclusive) return false
            if (actualMaxFrame != other.actualMaxFrame) return false

            return true
        }

        override fun hashCode(): Int {
            var result = min ?: 0
            result = 31 * result + (max ?: 0)
            result = 31 * result + maxInclusive.hashCode()
            result = 31 * result + (actualMaxFrame ?: 0)
            return result
        }

        override fun toString(): String {
            return "Frame(min=$min, max=$max, maxInclusive=$maxInclusive, actualMaxFrame=$actualMaxFrame)"
        }
    }

    /**
     * Play the animation between these two progress values.
     */

    @Immutable
    public class Progress(
        public val min: Float,
        public val max: Float,
    ) : LottieClipSpec() {
        override fun getMinProgress(composition: LottieComposition): Float {
            return min
        }

        override fun getMaxProgress(composition: LottieComposition): Float {
            return max
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Progress

            if (min != other.min) return false
            if (max != other.max) return false

            return true
        }

        override fun hashCode(): Int {
            var result = min.hashCode()
            result = 31 * result + max.hashCode()
            return result
        }

        override fun toString(): String {
            return "Progress(min=$min, max=$max)"
        }

    }

    /**
     * Play the animation from minMarker until maxMarker. If maxMarker represents the end of your animation,
     * set [maxInclusive] to true. If the marker represents the beginning of the next section, set
     * it to false to stop the animation at the frame before maxMarker.
     */
    @Immutable
    public class Markers(
        public val min: String?,
        public val max: String?,
        public val maxInclusive: Boolean
    ) : LottieClipSpec() {
        override fun getMinProgress(composition: LottieComposition): Float {
            return when (min) {
                null -> 0f
                else -> ((composition.marker(min)?.startFrame ?: 0f) / composition.endFrame).coerceIn(0f, 1f)
            }
        }

        override fun getMaxProgress(composition: LottieComposition): Float {
            return when (max) {
                null -> 1f
                else -> {
                    val offset = if (maxInclusive) 0 else -1
                    ((composition.marker(max)?.startFrame?.plus(offset) ?: 0f) / composition.endFrame).coerceIn(0f, 1f)
                }
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Markers

            if (min != other.min) return false
            if (max != other.max) return false
            if (maxInclusive != other.maxInclusive) return false

            return true
        }

        override fun hashCode(): Int {
            var result = min?.hashCode() ?: 0
            result = 31 * result + (max?.hashCode() ?: 0)
            result = 31 * result + maxInclusive.hashCode()
            return result
        }

        override fun toString(): String {
            return "Markers(min=$min, max=$max, maxInclusive=$maxInclusive)"
        }
    }

    /**
     * Play the animation from the beginning of the marker for the duration of the marker itself.
     * The duration can be set in After Effects.
     */
    public class Marker(public val marker: String) : LottieClipSpec() {
        override fun getMinProgress(composition: LottieComposition): Float {
            return ((composition.marker(marker)?.startFrame ?: 0f) / composition.endFrame).coerceIn(0f, 1f)
        }

        override fun getMaxProgress(composition: LottieComposition): Float {
            val marker = composition.marker(marker) ?: return 1f
            return ((marker.startFrame + marker.durationFrames) / composition.endFrame).coerceIn(0f, 1f)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Marker

            if (marker != other.marker) return false

            return true
        }

        override fun hashCode(): Int {
            return marker.hashCode()
        }

        override fun toString(): String {
            return "Marker(marker='$marker')"
        }
    }
}
