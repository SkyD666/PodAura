package com.skyd.htmlrender.base.css.model

internal enum class StyleOrigin(val value: Int) {
    EXTERNAL(0),
    INTERNAL(1),
    INLINE(2)
}