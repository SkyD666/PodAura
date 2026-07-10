package io.github.alexzhirkevich.compottie.internal.helpers.text

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
public value class TextJustify internal constructor(public val type: Byte) {

    public companion object {
        public val Left: TextJustify = TextJustify(0)
        public val Right: TextJustify = TextJustify(1)
        public val Center: TextJustify = TextJustify(2)
        internal val LastLineLeft: TextJustify = TextJustify(3)
        internal val LastLineRight: TextJustify = TextJustify(4)
        internal val LastLineCenter: TextJustify = TextJustify(5)
        internal val LastLineFull: TextJustify = TextJustify(6)
    }
}
