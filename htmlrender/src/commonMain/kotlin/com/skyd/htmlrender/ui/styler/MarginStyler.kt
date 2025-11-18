package com.skyd.htmlrender.ui.styler

import com.skyd.htmlrender.core.styler.IStringAnnotationStyler
import com.skyd.htmlrender.core.util.TextUnitParser


interface MarginStyler : IStringAnnotationStyler {
    val name: String
    val value: String

    override fun getTag(): String = name

    override fun getAnnotation(): String = value

    class Top(override val value: String) : MarginStyler {
        override val name: String = TOP
    }

    class Right(override val value: String) : MarginStyler {
        override val name: String = RIGHT
    }

    class Bottom(override val value: String) : MarginStyler {
        override val name: String = BOTTOM
    }

    class Left(override val value: String) : MarginStyler {
        override val name: String = LEFT
    }


    companion object {
        const val TOP = "margin-top"
        const val RIGHT = "margin-right"
        const val BOTTOM = "margin-bottom"
        const val LEFT = "margin-left"

        fun cumulativeValue(parent: String, value: String): String {
            val parentSize = TextUnitParser.parse(parent)
            val size = TextUnitParser.parse(value)
            return if (parentSize == null && size == null) {
                value
            } else if (parentSize == null) {
                value
            } else if (size == null) {
                parent
            } else {
                if (parentSize.type == size.type) {
                    buildString {
                        append("${parentSize.value + size.value}")
                        when {
                            size.isEm -> {
                                append(TextUnitParser.EM)
                            }

                            size.isSp -> {
                                append(TextUnitParser.PX)
                            }

                            else -> {
                                return value
                            }
                        }
                    }
                } else {
                    value
                }
            }
        }
    }
}