package io.github.alexzhirkevich.compottie.internal

import androidx.compose.ui.graphics.ImageBitmap
import io.github.alexzhirkevich.compottie.internal.animation.AnimatedColor
import io.github.alexzhirkevich.compottie.internal.animation.AnimatedGradient
import io.github.alexzhirkevich.compottie.internal.animation.AnimatedNumber
import io.github.alexzhirkevich.compottie.internal.animation.AnimatedShape
import io.github.alexzhirkevich.compottie.internal.animation.AnimatedVector2
import io.github.alexzhirkevich.compottie.internal.animation.RawProperty
import io.github.alexzhirkevich.compottie.internal.assets.ImageAsset
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonElement

internal interface SlotResolver {
    fun number(sid: String, state: AnimationState): AnimatedNumber?
    fun vector(sid: String, state: AnimationState): AnimatedVector2?
    fun color(sid: String, state: AnimationState): AnimatedColor?
    fun gradient(sid: String, state: AnimationState): AnimatedGradient?
    fun shape(sid: String, state: AnimationState): AnimatedShape?
    fun image(sid: String, state: AnimationState): ImageBitmap?
}

internal class AnimationSlots(
    private val slots: Map<String, JsonElement>,
) : SlotResolver {
    private val cache = mutableMapOf<String, RawProperty<*>>()

    override fun number(sid: String, state: AnimationState): AnimatedNumber? =
        property(sid, AnimatedNumber.serializer())

    override fun vector(sid: String, state: AnimationState): AnimatedVector2? =
        property(sid, AnimatedVector2.serializer())

    override fun color(sid: String, state: AnimationState): AnimatedColor? =
        property(sid, AnimatedColor.serializer())

    override fun gradient(sid: String, state: AnimationState): AnimatedGradient? =
        property(sid, AnimatedGradient.serializer())

    override fun shape(sid: String, state: AnimationState): AnimatedShape? =
        property(sid, AnimatedShape.serializer())

    override fun image(sid: String, state: AnimationState): ImageBitmap? =
        (state.assets[sid] as? ImageAsset)?.bitmap

    @Suppress("UNCHECKED_CAST")
    private fun <T : RawProperty<*>> property(
        sid: String,
        deserializer: DeserializationStrategy<T>,
    ): T? {
        val json = slots[sid] ?: return null
        val cached = cache[sid]
        if (cached != null) {
            return cached as T
        }
        val new = LottieJson.decodeFromJsonElement(deserializer, json)
        cache[sid] = new
        return new
    }
}

public class AnimationTheme(
    public val rules: Map<String, RawProperty<*>>,
    public val images: Map<String, ImageAsset>
) : SlotResolver {

    override fun number(sid: String, state: AnimationState): AnimatedNumber? =
        rules[sid] as AnimatedNumber?

    override fun vector(sid: String, state: AnimationState): AnimatedVector2? =
        rules[sid] as AnimatedVector2?

    override fun color(sid: String, state: AnimationState): AnimatedColor? =
        rules[sid] as AnimatedColor?

    override fun gradient(sid: String, state: AnimationState): AnimatedGradient? =
        rules[sid] as AnimatedGradient?

    override fun shape(sid: String, state: AnimationState): AnimatedShape? =
        rules[sid] as AnimatedShape?

    override fun image(sid: String, state: AnimationState): ImageBitmap? =
        images[sid]?.bitmap
}

internal class CombinedSlotResolver(
    val first: (AnimationState) -> SlotResolver?,
    val second: (AnimationState) -> SlotResolver?,
) : SlotResolver {

    override fun number(sid: String, state: AnimationState): AnimatedNumber? =
        first(state)?.number(sid, state) ?: second(state)?.number(sid, state)

    override fun vector(sid: String, state: AnimationState): AnimatedVector2? =
        first(state)?.vector(sid, state) ?: second(state)?.vector(sid, state)

    override fun color(sid: String, state: AnimationState): AnimatedColor? =
        first(state)?.color(sid, state) ?: second(state)?.color(sid, state)

    override fun gradient(sid: String, state: AnimationState): AnimatedGradient? =
        first(state)?.gradient(sid, state) ?: second(state)?.gradient(sid, state)

    override fun shape(sid: String, state: AnimationState): AnimatedShape? =
        first(state)?.shape(sid, state) ?: second(state)?.shape(sid, state)

    override fun image(sid: String, state: AnimationState): ImageBitmap? =
        first(state)?.image(sid, state) ?: second(state)?.image(sid, state)
}
