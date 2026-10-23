package dev.slne.surf.nats.bytecode

import dev.slne.surf.nats.descriptor.NatsParameter
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import java.lang.reflect.Method
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

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
        suspend fun durations(duration: Duration, optional: Duration?): String
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

        override suspend fun durations(duration: Duration, optional: Duration?): String = "$duration|$optional"
    }

    private val impl = PrivateImpl()

    private fun method(name: String) = Example::class.java.methods.single { it.name == name || it.name.startsWith("$name-") }

    private fun parameter(name: String, type: Class<*>) = NatsParameter(name, type.kotlin, nullable = false)

    // kotlinTypes: the Kotlin parameter classes, as NatsParameter.type of a generated descriptor; default JVM types
    private fun invoker(name: String, vararg kotlinTypes: Class<*>): ServiceInvoker {
        val method = method(name)
        val types = if (kotlinTypes.isEmpty()) method.parameterTypes.dropLast(1) else kotlinTypes.toList()
        val parameters = types.mapIndexed { i, type -> parameter("p$i", type) }
        val problems = mutableListOf<String>()
        return ServiceInvokerFactory.create(impl, method, assertNotNull(ValueClassParameters.of(method, parameters, name, problems), "$problems"))
    }

    // The single problem ValueClassParameters reports for the parameters of method, without the label
    private fun problem(method: Method, parameters: List<NatsParameter>): String {
        val problems = mutableListOf<String>()
        assertNull(ValueClassParameters.of(method, parameters, "label", problems))
        return problems.single().removePrefix("label: ")
    }

    @Test
    fun `primitive arguments are unboxed from the array`() = runTest {
        assertEquals("1|2|3.5|true|x", invoker("mixed").call(arrayOf(1, 2L, 3.5, true, "x")))
    }

    @Test
    fun `nullable reference argument accepts null`() = runTest {
        assertEquals(
            "1|2|3.5|true|null",
            invoker("mixed").call(arrayOf(1, 2L, 3.5, true, null))
        )
    }

    @Test
    fun `boxed value class arguments are unboxed for the implementation`() = runTest {
        val invoker = invoker(
            "valueClasses",
            ULong::class.java,
            UInt::class.java,
            ULong::class.java,
            Tag::class.java,
            Tag::class.java,
        )
        assertEquals(
            "${ULong.MAX_VALUE}|7|3|a|b",
            invoker.call(arrayOf(ULong.MAX_VALUE, 7u, 3uL, Tag("a"), Tag("b")))
        )
        assertEquals("1|2|null|a|null", invoker.call(arrayOf(1uL, 2u, null, Tag("a"), null)))
    }

    @Test
    fun `duration arguments are unboxed when non-null and passed boxed when nullable`() = runTest {
        val invoker = invoker("durations", Duration::class.java, Duration::class.java)
        assertEquals("1.5s|2s", invoker.call(arrayOf(1.5.seconds, 2.seconds)))
        assertEquals("1s|null", invoker.call(arrayOf(1.seconds, null)))
    }

    @Test
    fun `parameter count that differs from the JVM method is rejected`() {
        assertEquals(
            "${method("noArguments").name} has 0 parameters, expected 1",
            problem(method("noArguments"), listOf(parameter("tag", Tag::class.java))),
        )
    }

    @Test
    fun `value class without box-impl for the JVM parameter type is rejected naming the parameter`() {
        val mixed = problem(
            method("mixed"),
            listOf(
                parameter("a", Tag::class.java),
                parameter("b", Long::class.java),
                parameter("factor", Double::class.java),
                parameter("flag", Boolean::class.java),
                parameter("label", String::class.java),
            ),
        )
        assertEquals("parameter 'a' (${Tag::class.java.name}) has no box-impl/unbox-impl for int", mixed)

        val mangled = method("valueClasses")
        assertTrue(mangled.name.startsWith("valueClasses-"), mangled.name)
        val valueClasses = problem(
            mangled,
            listOf(
                parameter("id", ULong::class.java),
                parameter("small", UInt::class.java),
                parameter("optional", ULong::class.java),
                parameter("tag", Duration::class.java),
                parameter("optionalTag", Tag::class.java),
            ),
        )
        assertEquals(
            "parameter 'tag' (${Duration::class.java.name}) has no box-impl/unbox-impl for ${String::class.java.name}",
            valueClasses,
        )
    }

    @Test
    fun `method without arguments takes an empty array`() = runTest {
        assertEquals(42, invoker("noArguments").call(arrayOf()))
    }

    @Test
    fun `suspending implementation resumes with its result`() = runTest {
        assertEquals("delayed:a", invoker("delayed").call(arrayOf("a")))
    }

    @Test
    fun `unit method returns Unit`() = runTest {
        assertEquals(Unit, invoker("unit").call(arrayOf("u")))
        assertEquals("u", impl.unitValue)
    }

    @Test
    fun `implementation exception is thrown unwrapped`() = runTest {
        val exception = assertFailsWith<IllegalStateException> {
            invoker("fails").call(arrayOf("boom"))
        }
        assertEquals("boom", exception.message)
    }

    @Test
    fun `null for a primitive parameter fails`() = runTest {
        assertFailsWith<NullPointerException> {
            invoker("mixed").call(arrayOf(null, 2L, 3.5, true, "x"))
        }
    }

    @Test
    fun `wrong argument count fails`() = runTest {
        assertFailsWith<IllegalArgumentException> {
            invoker("mixed").call(arrayOf(1, 2L))
        }
    }

    @Test
    fun `invokers for different methods are independent`() = runTest {
        val first = invoker("noArguments")
        val second = invoker("delayed")
        assertEquals(42, first.call(arrayOf()))
        assertEquals("delayed:b", second.call(arrayOf("b")))
        assertNull(impl.unitValue)
    }
}
