package dev.slne.surf.api.core.util

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SurfTypeParameterMatcherTest {
    private interface First<T>
    private interface Second<T>
    private class Both : First<String>, Second<Int>

    @Test
    fun `cache distinguishes parameterized types with equal parameter names`() {
        val value = Both()
        val first = SurfTypeParameterMatcher.find(value, First::class.java, "T")
        val second = SurfTypeParameterMatcher.find(value, Second::class.java, "T")

        assertTrue(first.match("value"))
        assertFalse(first.match(1))
        assertTrue(second.match(1))
        assertFalse(second.match("value"))
    }
}
