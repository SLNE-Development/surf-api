package dev.slne.surf.nats.bytecode

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.milliseconds

class ServiceInvokerFactoryTest {

    @JvmInline
    value class Tag(val value: String)

    interface Example {
        suspend fun mixed(a: Int, b: Long, factor: Double, flag: Boolean, label: String?): String
        suspend fun noArguments(): Int
        suspend fun delayed(value: String): String
        suspend fun unit(value: String)
        suspend fun fails(message: String): String
        suspend fun valueClasses(id: ULong, small: UInt, optional: ULong?, tag: Tag, optionalTag: Tag?): String
    }

    private class PrivateImpl : Example {
        var unitValue: String? = null

        override suspend fun mixed(
            a: Int,
            b: Long,
            factor: Double,
            flag: Boolean,
            label: String?
        ): String {
            return "$a|$b|$factor|$flag|$label"
        }

        override suspend fun noArguments() = 42

        override suspend fun delayed(value: String): String {
            delay(10.milliseconds)
            return "delayed:$value"
        }

        override suspend fun unit(value: String) {
            unitValue = value
        }

        override suspend fun fails(message: String): String = throw IllegalStateException(message)

        override suspend fun valueClasses(
            id: ULong,
            small: UInt,
            optional: ULong?,
            tag: Tag,
            optionalTag: Tag?
        ): String = "$id|$small|$optional|${tag.value}|${optionalTag?.value}"
    }

    private val impl = PrivateImpl()

    private fun invoker(method: KFunction<*>): ServiceInvoker {
        return ServiceInvokerFactory.create(impl, method.javaMethod!!)
    }

    @Test
    fun `primitive arguments are unboxed from the array`() = runTest {
        assertEquals("1|2|3.5|true|x", invoker(Example::mixed).call(arrayOf(1, 2L, 3.5, true, "x")))
    }

    @Test
    fun `nullable reference argument accepts null`() = runTest {
        assertEquals(
            "1|2|3.5|true|null",
            invoker(Example::mixed).call(arrayOf(1, 2L, 3.5, true, null))
        )
    }

    @Test
    fun `boxed value class arguments are unboxed for the implementation`() = runTest {
        val invoker = invoker(Example::valueClasses)
        assertEquals(
            "${ULong.MAX_VALUE}|7|3|a|b",
            invoker.call(arrayOf(ULong.MAX_VALUE, 7u, 3uL, Tag("a"), Tag("b")))
        )
        assertEquals("1|2|null|a|null", invoker.call(arrayOf(1uL, 2u, null, Tag("a"), null)))
    }

    @Test
    fun `method without arguments takes an empty array`() = runTest {
        assertEquals(42, invoker(Example::noArguments).call(arrayOf()))
    }

    @Test
    fun `suspending implementation resumes with its result`() = runTest {
        assertEquals("delayed:a", invoker(Example::delayed).call(arrayOf("a")))
    }

    @Test
    fun `unit method returns Unit`() = runTest {
        assertEquals(Unit, invoker(Example::unit).call(arrayOf("u")))
        assertEquals("u", impl.unitValue)
    }

    @Test
    fun `implementation exception is thrown unwrapped`() = runTest {
        val exception = assertFailsWith<IllegalStateException> {
            invoker(Example::fails).call(arrayOf("boom"))
        }
        assertEquals("boom", exception.message)
    }

    @Test
    fun `null for a primitive parameter fails`() = runTest {
        assertFailsWith<NullPointerException> {
            invoker(Example::mixed).call(arrayOf(null, 2L, 3.5, true, "x"))
        }
    }

    @Test
    fun `wrong argument count fails`() = runTest {
        assertFailsWith<IllegalArgumentException> {
            invoker(Example::mixed).call(arrayOf(1, 2L))
        }
    }

    @Test
    fun `invokers for different methods are independent`() = runTest {
        val first = invoker(Example::noArguments)
        val second = invoker(Example::delayed)
        assertEquals(42, first.call(arrayOf()))
        assertEquals("delayed:b", second.call(arrayOf("b")))
        assertNull(impl.unitValue)
    }
}
