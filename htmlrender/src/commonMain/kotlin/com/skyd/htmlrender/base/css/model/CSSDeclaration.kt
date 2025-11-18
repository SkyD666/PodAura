package com.skyd.htmlrender.base.css.model


interface CSSDeclaration {
    val property: String
    val value: String
}

internal data class CSSDeclarationWithImportant(
    override val property: String,
    override val value: String,
    val isImportant: Boolean
) : CSSDeclaration

internal data class CSSDeclarationWithPriority(
    override val property: String,
    override val value: String,
    val priority: CSSPriority
) : CSSDeclaration, Comparable<CSSDeclarationWithPriority> {

    constructor(
        src: CSSDeclarationWithImportant,
        origin: StyleOrigin,
        selector: String? = null,
        order: Int = 0
    ) : this(
        src.property,
        src.value,
        CSSPriority.calculate(origin, src.isImportant, selector, order)
    )

    override fun compareTo(other: CSSDeclarationWithPriority): Int =
        priority.compareTo(other.priority)
}