package io.github.alexzhirkevich.compottie.internal.platform

internal actual fun CharSequence.codePointAt(index: Int) = Character.codePointAt(this, index)

internal actual fun charCount(codePoint: Int): Int = Character.charCount(codePoint)

internal actual fun StringBuilder.addCodePoint(codePoint: Int) {
    this.appendCodePoint(codePoint)
}
