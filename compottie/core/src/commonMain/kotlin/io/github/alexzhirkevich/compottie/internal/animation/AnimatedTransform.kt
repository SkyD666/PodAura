package io.github.alexzhirkevich.compottie.internal.animation

import androidx.compose.ui.graphics.Matrix
import io.github.alexzhirkevich.compottie.dynamic.DynamicTransformProvider
import io.github.alexzhirkevich.compottie.internal.AnimationState
import io.github.alexzhirkevich.compottie.internal.utils.degreeToRadians
import io.github.alexzhirkevich.compottie.internal.utils.fastReset
import io.github.alexzhirkevich.compottie.internal.utils.preConcat
import io.github.alexzhirkevich.compottie.internal.utils.preRotate
import io.github.alexzhirkevich.compottie.internal.utils.preRotateX
import io.github.alexzhirkevich.compottie.internal.utils.preRotateY
import io.github.alexzhirkevich.compottie.internal.utils.preRotateZ
import io.github.alexzhirkevich.compottie.internal.utils.preScale
import io.github.alexzhirkevich.compottie.internal.utils.preTranslate
import io.github.alexzhirkevich.compottie.internal.utils.radiansToDegree
import io.github.alexzhirkevich.compottie.internal.utils.setValues
import io.github.alexzhirkevich.keight.ScriptRuntime
import io.github.alexzhirkevich.keight.js.JsAny
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

internal abstract class AnimatedTransform : ExpressionHolder, PropertyGroup {

    abstract val anchorPoint: AnimatedVector2
    abstract val position: AnimatedVector2
    abstract val scale: AnimatedVector2
    abstract val rotation: AnimatedNumber
    open val rotationX: AnimatedNumber? = null
    open val rotationY: AnimatedNumber? = null
    open val rotationZ: AnimatedNumber? = null
    abstract val opacity: AnimatedNumber
    abstract val skew: AnimatedNumber
    abstract val skewAxis: AnimatedNumber

    override val group: PropertyGroup? = null

    var isAutoOrient: Boolean = false

    override fun prepareExpressions(state: AnimationState) {

        anchorPoint.group = this
        position.group = this
        scale.group = this
        rotation.group = this
        rotationX?.group = this
        rotationY?.group = this
        rotationZ?.group = this
        opacity.group = this
        skew.group = this
        skewAxis.group = this

        anchorPoint.prepareExpressions(state)
        position.prepareExpressions(state)
        scale.prepareExpressions(state)
        rotation.prepareExpressions(state)
        rotationX?.prepareExpressions(state)
        rotationY?.prepareExpressions(state)
        rotationZ?.prepareExpressions(state)
        opacity.prepareExpressions(state)
        skew.prepareExpressions(state)
        skewAxis.prepareExpressions(state)
    }

    var dynamic: DynamicTransformProvider? = null
        set(value) {
            if (field !== value) {
                field = value
                position.dynamicOffset(value?.offset)
                scale.dynamicScale(value?.scale)
                rotation.dynamic = value?.rotation
                opacity.dynamicNorm(value?.opacity)
                skew.dynamic = value?.skew
                skewAxis.dynamic = value?.skewAxis
            }
        }

    open fun isHidden(state: AnimationState): Boolean = false

    protected val matrix: Matrix = Matrix()

    private val skewMatrix1: Matrix = Matrix()

    private val skewMatrix2: Matrix = Matrix()

    private val skewMatrix3: Matrix = Matrix()

    private val skewValues: FloatArray = FloatArray(9)

    fun matrix(state: AnimationState): Matrix {
        matrix.fastReset()

        if (isHidden(state)) {
            return matrix
        }

        val curPos = Vec2(position.interpolatedVec(state))
        matrix.preTranslate(curPos.x, curPos.y)

        val angle = if (isAutoOrient) {
            // 1) Find the next position value.
            // 2) Create a vector from the current position to the next position.
            // 3) Find the angle of that vector to the X axis (0 degrees).
            val vector = state.onFrame(state.frame + 0.01f) {
                Vec2(position.interpolatedVec(it))
            } - curPos

            radiansToDegree(atan2(vector.y, vector.x)) +
                    rotation.interpolatedFloat(state)
        } else {
            rotation.interpolatedFloat(state)
        }

        matrix.preRotate(angle)

        rotationX?.let {
            matrix.preRotateX(it.interpolatedFloat(state), skewMatrix1)
        }

        rotationY?.let {
            matrix.preRotateY(it.interpolatedFloat(state), skewMatrix1)
        }

        rotationZ?.let {
            matrix.preRotateZ(it.interpolatedFloat(state), skewMatrix1)
        }

        val sk = skew.interpolatedFloat(state)
        if (sk != 0f) {

            val skewAngle = skewAxis.interpolatedFloat(state)

            val mCos = if (skewAngle == 0f)
                0f
            else cos(degreeToRadians(-skewAngle + 90))

            val mSin = if (skewAngle == 0f)
                1f
            else sin(degreeToRadians(-skewAngle + 90))

            val aTan = tan(degreeToRadians(sk))

            clearSkewValues()
            skewValues[0] = mCos
            skewValues[1] = mSin
            skewValues[3] = -mSin
            skewValues[4] = mCos
            skewValues[8] = 1f
            skewMatrix1.setValues(skewValues)
            clearSkewValues()
            skewValues[0] = 1f
            skewValues[3] = aTan
            skewValues[4] = 1f
            skewValues[8] = 1f
            skewMatrix2.setValues(skewValues)
            clearSkewValues()
            skewValues[0] = mCos
            skewValues[1] = -mSin
            skewValues[3] = mSin
            skewValues[4] = mCos
            skewValues[8] = 1f

            skewMatrix3.setValues(skewValues)
            skewMatrix2.preConcat(skewMatrix1)
            skewMatrix3.preConcat(skewMatrix2)

            matrix.preConcat(skewMatrix3)
        }

        val sc = Vec2(scale.interpolatedNorm(state))
        matrix.preScale(sc.x, sc.y)
        val ap = Vec2(anchorPoint.interpolatedVec(state))
        matrix.preTranslate(-ap.x, -ap.y)

        return matrix
    }

    override suspend fun get(property: JsAny?, runtime: ScriptRuntime): JsAny? {
        return when (property?.toString()) {
            "anchorPoint" -> anchorPoint
            "position" -> position
            "scale" -> scale
            "rotation" -> rotation
            "rotationX" -> rotationX
            "rotationY" -> rotationY
            "rotationZ" -> rotationZ
            "opacity" -> opacity
            "skew" -> skew
            "skewAxis" -> skewAxis
            else -> super.get(property, runtime)
        }
    }

    private fun clearSkewValues() {
        skewValues.fill(0f)
    }
}
