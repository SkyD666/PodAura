package io.github.alexzhirkevich.compottie.statemachine

internal enum class SMGuardCondition {
    Equal {
        override fun check(a: Any, b: Any): Boolean {
            return a == b
        }
    },
    NotEqual {
        override fun check(a: Any, b: Any): Boolean {
            return a != b
        }
    },
    GreaterThan {
        override fun check(a: Any, b: Any): Boolean {
            return a is Float && b is Float && a > b
        }
    },
    GreaterThanOrEqual {
        override fun check(a: Any, b: Any): Boolean {
            return a is Float && b is Float && a >= b
        }
    },
    LessThan {
        override fun check(a: Any, b: Any): Boolean {
            return a is Float && b is Float && a < b
        }
    },
    LessThanOrEqual {
        override fun check(a: Any, b: Any): Boolean {
            return a is Float && b is Float && a <= b
        }
    };

    abstract fun check(a: Any, b: Any): Boolean
}
