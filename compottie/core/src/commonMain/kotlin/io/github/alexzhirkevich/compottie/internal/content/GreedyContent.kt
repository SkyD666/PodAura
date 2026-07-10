package io.github.alexzhirkevich.compottie.internal.content

internal interface GreedyContent {
    fun absorbContent(contents: MutableList<Content>)
}