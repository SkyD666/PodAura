package io.github.alexzhirkevich.compottie.internal.utils

import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.isIdentity
import kotlin.math.absoluteValue

internal val IdentityMatrix = Matrix()

internal fun Matrix.preTranslate(x: Float, y: Float) {
    if (x.absoluteValue < Float.MIN_VALUE && y.absoluteValue < Float.MIN_VALUE) {
        return
    }

    return translate(x, y)
}

internal fun Matrix.preConcat(other: Matrix) {

    if (other.isIdentity()) {
        return
    }

    if (isIdentity()) {
        fastSetFrom(other)
        return
    }

    val v = values
    if (v.size < 16) return
    if (other.values.size < 16) return

    val v00 = dot(other, 0, this, 0)
    val v01 = dot(other, 0, this, 1)
    val v02 = dot(other, 0, this, 2)
    val v03 = dot(other, 0, this, 3)
    val v10 = dot(other, 1, this, 0)
    val v11 = dot(other, 1, this, 1)
    val v12 = dot(other, 1, this, 2)
    val v13 = dot(other, 1, this, 3)
    val v20 = dot(other, 2, this, 0)
    val v21 = dot(other, 2, this, 1)
    val v22 = dot(other, 2, this, 2)
    val v23 = dot(other, 2, this, 3)
    val v30 = dot(other, 3, this, 0)
    val v31 = dot(other, 3, this, 1)
    val v32 = dot(other, 3, this, 2)
    val v33 = dot(other, 3, this, 3)

    v[0] = v00
    v[1] = v01
    v[2] = v02
    v[3] = v03
    v[4] = v10
    v[5] = v11
    v[6] = v12
    v[7] = v13
    v[8] = v20
    v[9] = v21
    v[10] = v22
    v[11] = v23
    v[12] = v30
    v[13] = v31
    v[14] = v32
    v[15] = v33
}

private fun dot(m1: Matrix, row: Int, m2: Matrix, column: Int): Float {
    return m1[row, 0] * m2[0, column] +
            m1[row, 1] * m2[1, column] +
            m1[row, 2] * m2[2, column] +
            m1[row, 3] * m2[3, column]
}

internal fun Matrix.fastReset() {
    fastSetFrom(IdentityMatrix)
}

internal fun Matrix.fastSetFrom(other: Matrix) {
    other.values.copyInto(values)
}

internal fun Matrix.setValues(values: FloatArray) {
    this.values[Matrix.ScaleX] = values[0]
    this.values[Matrix.SkewX] = values[1]
    this.values[Matrix.TranslateX] = values[2]
    this.values[Matrix.SkewY] = values[3]
    this.values[Matrix.ScaleY] = values[4]
    this.values[Matrix.TranslateY] = values[5]
    this.values[Matrix.Perspective0] = values[6]
    this.values[Matrix.Perspective1] = values[7]
    this.values[Matrix.Perspective2] = values[8]
}

internal fun Matrix.preRotate(degree: Float) {
    if (degree.absoluteValue < Float.MIN_VALUE) {
        return
    }
    return rotateZ(degree)
}

internal fun Matrix.preRotateX(degree: Float, tempMatrix: Matrix) {
    if (degree.absoluteValue < Float.MIN_VALUE) {
        return
    }
    preConcat(tempMatrix.apply {
        fastReset()
        rotateX(degree)
    })
}

internal fun Matrix.preRotateY(degree: Float, tempMatrix: Matrix) {
    if (degree.absoluteValue < Float.MIN_VALUE) {
        return
    }
    preConcat(tempMatrix.apply {
        fastReset()
        rotateY(degree)
    })
}

internal fun Matrix.preRotateZ(degree: Float, tempMatrix: Matrix) {
    if (degree.absoluteValue < Float.MIN_VALUE) {
        return
    }
    preConcat(tempMatrix.apply {
        fastReset()
        rotateZ(degree)
    })
}

internal fun Matrix.preScale(x: Float, y: Float) {
    if (x == 1f && y == 1f) {
        return
    }
//    preConcat(tempMatrixTransform.apply {
//        fastReset()
//        scale(x, y)
//    })
    scale(x, y)
}
