package com.skyd.htmlrender.base.css.model

internal class CSSPriority(private val priority: Long) {
    operator fun compareTo(other: CSSPriority): Int = priority.compareTo(other.priority)

    companion object {
        private val idRegex by lazy { Regex("""#\w+""") }
        private val classRegex by lazy { Regex("""\.\w+""") }
        private val attributeRegex by lazy { Regex("""\[\w+(?:\W*=\W*".+?")?""") }
        private val pseudoClassRegex by lazy { Regex("""(?<!::):\w+""") }
        private val typeRegex by lazy { Regex("""(^|\s|[\[>+~])\w+""") }
        private val pseudoElementRegex by lazy { Regex("""::\w+""") }

        fun calculate(
            origin: StyleOrigin,
            isImportant: Boolean,
            selector: String? = null,
            order: Int = 0
        ): CSSPriority {
            var idCount = 0
            var classCount = 0
            var typeCount = 0

            if (selector != null) {
                idCount = idRegex.findAll(selector).count()

                classCount = classRegex.findAll(selector).count()
                classCount += attributeRegex.findAll(selector).count()
                classCount += pseudoClassRegex.findAll(selector).count()

                typeCount = typeRegex.findAll(selector).count()
                typeCount += pseudoElementRegex.findAll(selector).count()
            }

            return calculate(origin, isImportant, idCount, classCount, typeCount, order)
        }

        private fun calculate(
            origin: StyleOrigin,
            isImportant: Boolean,
            idCount: Int,
            classCount: Int,
            typeCount: Int,
            order: Int
        ): CSSPriority {
            var priority = 0L

            // !important flag
            priority = priority or ((if (isImportant) 1L else 0L) shl 62)

            // StyleOrigin
            priority = priority or (origin.value.toLong() shl 60)

            // Number of ID selectors
            priority = priority or ((idCount.toLong() and 0x3FF) shl 49)

            // Number of class/attribute/pseudo-class selectors
            priority = priority or ((classCount.toLong() and 0x3FF) shl 39)

            // Number of type selectors and pseudo-element selectors
            priority = priority or ((typeCount.toLong() and 0x3FF) shl 29)

            // Source order
            priority = priority or (order.toLong() and 0x1FFFFFFF)

            return CSSPriority(priority)
        }
    }
}