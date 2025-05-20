package com.skyd.podaura

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        var cancel = false
        do {
            println("123")
            cancel = true
            if (cancel) continue
        } while (!cancel)
    }
}