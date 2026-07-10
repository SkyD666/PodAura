package io.github.alexzhirkevich.compottie.internal.content


internal interface Content {

    val name: String?

    fun setContents(
        contentsBefore: List<Content>,
        contentsAfter: List<Content>
    )
}

internal val Content.nameOrDefault: String
    get() = name ?: ("__${this::class.simpleName.orEmpty()}_${hashCode()}")