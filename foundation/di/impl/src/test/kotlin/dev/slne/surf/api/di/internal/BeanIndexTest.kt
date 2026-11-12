package dev.slne.surf.api.di.internal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BeanIndexTest {

    @Test
    fun `a bean is resolvable under every type it provides`() {
        val index = indexOf(definition("Impl", provides = listOf("Impl", "Contract")))

        assertEquals("Impl", index.resolveSingle(needs("Contract"))?.id)
        assertEquals("Impl", index.resolveSingle(needs("Impl"))?.id)
    }

    @Test
    fun `an unprovided type resolves to null rather than failing`() {
        val index = indexOf(definition("Impl"))

        assertNull(index.resolveSingle(needs("Missing")))
        assertTrue(index.resolveAll(needsAll("Missing")).isEmpty())
    }

    @Test
    fun `a qualifier picks between beans of the same type`() {
        val index = indexOf(
            definition("Primary", provides = listOf("Storage"), qualifier = "primary"),
            definition("Fallback", provides = listOf("Storage"), qualifier = "fallback"),
        )

        assertEquals("Fallback", index.resolveSingle(needs("Storage", qualifier = "fallback"))?.id)
    }

    @Test
    fun `a qualifier that matches nothing resolves to null`() {
        val index = indexOf(definition("Primary", provides = listOf("Storage"), qualifier = "primary"))

        assertNull(index.resolveSingle(needs("Storage", qualifier = "absent")))
    }

    @Test
    fun `primary wins when the injection point names nothing`() {
        val index = indexOf(
            definition("Chosen", provides = listOf("Storage"), primary = true),
            definition("Other", provides = listOf("Storage")),
        )

        assertEquals("Chosen", index.resolveSingle(needs("Storage"))?.id)
    }

    @Test
    fun `ambiguity fails and names the candidates`() {
        val index = indexOf(
            definition("First", provides = listOf("Storage")),
            definition("Second", provides = listOf("Storage")),
        )

        val failure = assertFailsWith<AmbiguousBeanException> { index.resolveSingle(needs("Storage")) }

        assertTrue(failure.message!!.contains("First"), failure.message)
        assertTrue(failure.message!!.contains("Second"), failure.message)
        assertTrue(failure.message!!.contains("@Primary"), failure.message)
    }

    @Test
    fun `two primaries for one type fail instead of one being picked`() {
        val index = indexOf(
            definition("First", provides = listOf("Storage"), primary = true),
            definition("Second", provides = listOf("Storage"), primary = true),
        )

        val failure = assertFailsWith<AmbiguousBeanException> { index.resolveSingle(needs("Storage")) }

        assertTrue(failure.message!!.contains("several beans marked @Primary"), failure.message)
    }

    @Test
    fun `a collection takes every provider ordered by priority then id`() {
        val index = indexOf(
            definition("Zulu", provides = listOf("Handler"), priority = 10),
            definition("Alpha", provides = listOf("Handler"), priority = 10),
            definition("First", provides = listOf("Handler"), priority = 0),
        )

        assertEquals(listOf("First", "Alpha", "Zulu"), index.resolveAll(needsAll("Handler")).ids())
    }

    @Test
    fun `a factory is resolvable under its return type`() {
        val index = indexOf(
            definition(
                "RedisConnection",
                factories = listOf(factory("api", "RedisApi")),
            )
        )

        assertEquals("RedisConnection#api", index.resolveSingle(needs("RedisApi"))?.id)
        assertEquals("RedisConnection", index.resolveSingle(needs("RedisConnection"))?.id)
    }

    @Test
    fun `a factory competes with a class providing the same type`() {
        val index = indexOf(
            definition("DirectApi", provides = listOf("RedisApi")),
            definition("RedisConnection", factories = listOf(factory("api", "RedisApi"))),
        )

        val failure = assertFailsWith<AmbiguousBeanException> { index.resolveSingle(needs("RedisApi")) }

        assertTrue(failure.message!!.contains("RedisConnection#api"), failure.message)
        assertTrue(failure.message!!.contains("DirectApi"), failure.message)
    }
}
