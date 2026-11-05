package dev.slne.surf.microservice.processor.declaration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JvmBinaryNameTest {

    @Test
    fun `joins package and top-level class with a dot`() {
        assertEquals("com.example.Service", jvmBinaryName("com.example", listOf("Service")))
    }

    @Test
    fun `joins nested classes with a dollar sign`() {
        assertEquals("com.example.Outer\$Middle\$Inner", jvmBinaryName("com.example", listOf("Outer", "Middle", "Inner")))
    }

    @Test
    fun `omits the default package`() {
        assertEquals("Outer\$Inner", jvmBinaryName("", listOf("Outer", "Inner")))
    }

    @Test
    fun `rejects an empty class list`() {
        assertFailsWith<IllegalArgumentException> { jvmBinaryName("com.example", emptyList()) }
    }
}
