package io.github.alexzhirkevich.compottie.internal.effects

import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.util.fastForEach
import io.github.alexzhirkevich.compottie.internal.AnimationState
import io.github.alexzhirkevich.compottie.internal.animation.ExpressionHolder
import io.github.alexzhirkevich.compottie.internal.animation.RawProperty
import io.github.alexzhirkevich.compottie.internal.animation.expressions.state
import io.github.alexzhirkevich.compottie.internal.animation.expressions.toJs
import io.github.alexzhirkevich.keight.Callable
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.JsAny
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
@JsonClassDiscriminator("ty")
internal sealed class LayerEffect : ExpressionHolder, Callable {

    abstract val enabled: Boolean
    abstract val name: String?
    abstract val matchName: String?
    abstract val index: Int?
    abstract val values: List<EffectValue<*>>

    private val valueByName by lazy {
        values.associateBy { it.name.orEmpty() }
    }

    private val valueByMatchName by lazy {
        values.associateBy { it.matchName.orEmpty() }
    }

    private val valueByIndex by lazy {
        values.associateBy { it.index ?: Int.MIN_VALUE }
    }

    override fun prepareExpressions(state: AnimationState) {
        values.fastForEach { it.prepareExpressions(state) }
    }

    abstract fun apply(
        paint: Paint,
        animationState: AnimationState,
        effectState: LayerEffectsState
    )

    abstract fun copy(): LayerEffect

    override suspend fun bind(
        thisArg: JsAny?,
        args: List<JsAny?>,
        runtime: ScriptRuntime
    ): Callable = this

    override suspend fun invoke(args: List<JsAny?>, runtime: ScriptRuntime): JsAny? {
        val index = args.getOrNull(0)?.toKotlin(runtime) ?: return null
        return if (index is Number) {
            valueByIndex[index.toInt()]
        } else {
            valueByName[index.toString()] ?: valueByMatchName[index.toString()]
        }
    }

    override suspend fun get(property: JsAny?, runtime: ScriptRuntime): JsAny? {
        return when (property?.toString()) {
            "param" -> Callable {
                val index = it[0]?.toKotlin(runtime) ?: return@Callable null
                if (index is Number) {
                    valueByIndex[index.toInt()]
                } else {
                    valueByName[index.toString()] ?: valueByMatchName[index.toString()]
                }?.value?.raw(runtime.state)?.toJs(runtime)
            }

            else -> super.get(property, runtime)
        }
    }

    @Serializable
    class Unsupported(
        @SerialName("nm")
        override val name: String? = null,

        @SerialName("mn")
        override val matchName: String? = null,

        @SerialName("ix")
        override val index: Int? = null,

        @SerialName("ef")
        override val values: List<EffectValue<@Contextual RawProperty<@Contextual Any>>> = emptyList()
    ) : LayerEffect() {

        override fun apply(
            paint: Paint,
            animationState: AnimationState,
            effectState: LayerEffectsState
        ) {
        }


        override val enabled: Boolean = true

        override fun copy(): LayerEffect {
            return Unsupported()
        }

        override fun prepareExpressions(state: AnimationState) {

        }
    }
}
