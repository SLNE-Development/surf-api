package dev.slne.surf.api.common.invoker

import java.lang.classfile.ClassFile
import java.lang.constant.ClassDesc
import java.lang.constant.ConstantDescs.CD_String
import java.lang.constant.ConstantDescs.CD_double
import java.lang.constant.MethodTypeDesc
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.invoke.WrongMethodTypeException
import kotlin.coroutines.Continuation
import kotlin.test.*

class InvokerFactoryTest {

    fun interface Adder {
        fun add(a: Long, b: Int): Long
    }

    interface StringAndDouble {
        fun first(value: String): String
        fun second(value: Double): Double
    }

    interface TwoMethods {
        fun a()
        fun b()
    }

    private val lookup = MethodHandles.lookup()

    @Test
    fun `generated template passes the verifier`() {
        val factory = InvokerFactory(Adder::class.java, lookup)

        val errors = ClassFile.of().verify(ClassFile.of().parse(factory.classBytes))

        assertTrue(errors.isEmpty(), errors.joinToString("\n"))
    }

    @Test
    fun `invokers call their own handle with wide primitives`() {
        val factory = InvokerFactory(Adder::class.java, lookup)
        val long = Long::class.javaPrimitiveType
        val int = Int::class.javaPrimitiveType
        val sum = lookup.findStatic(
            InvokerFactoryTest::class.java,
            "sum",
            MethodType.methodType(long, long, int),
        )
        val constant = MethodHandles.dropArguments(MethodHandles.constant(long, -1L), 0, long, int)

        val adder = factory.create(sum)
        val other = factory.create(constant)

        assertEquals(5_000_000_003L, adder.add(5_000_000_000L, 3))
        assertEquals(-1L, other.add(1L, 1))
        assertNotSame(adder.javaClass, other.javaClass)
        assertTrue(adder.javaClass.isHidden)
    }

    @Test
    fun `handles of another type are adapted`() {
        val factory = InvokerFactory(Adder::class.java, lookup)
        val boxed = MethodHandles.dropArguments(
            MethodHandles.constant(Any::class.java, 9L),
            0,
            Any::class.java,
            Any::class.java,
        )

        assertEquals(9L, factory.create(boxed).add(0L, 0))
    }

    @Test
    fun `incompatible handles are rejected`() {
        val factory = InvokerFactory(Adder::class.java, lookup)

        assertFailsWith<WrongMethodTypeException> {
            factory.create(MethodHandles.constant(String::class.java, "x"))
        }
    }

    @Test
    fun `interfaces with several abstract methods are rejected`() {
        assertFailsWith<IllegalArgumentException> {
            InvokerFactory(TwoMethods::class.java, lookup)
        }
    }

    @Test
    fun `generated classes implement several methods with one handle each`() {
        val bytes = HandleInvokerClasses.generate(
            ClassDesc.of(InvokerFactoryTest::class.java.packageName, "StringAndDoubleImpl"),
            StringAndDouble::class.java.describeConstable().orElseThrow(),
            listOf(
                InvokerMethod("first", MethodTypeDesc.of(CD_String, CD_String)),
                InvokerMethod("second", MethodTypeDesc.of(CD_double, CD_double)),
            ),
        )
        val first = MethodHandles.identity(String::class.java)
        val second = MethodHandles.identity(Double::class.javaPrimitiveType)

        val handles = listOf(first, second)
        val invoker = HandleInvokerClasses.instantiate(lookup, bytes, handles) as StringAndDouble

        assertEquals("a", invoker.first("a"))
        assertEquals(2.5, invoker.second(2.5))
    }

    @Test
    fun `suspend functions are detected by their continuation parameter`() {
        val suspending = Fixtures::class.java.getMethod("suspending", Continuation::class.java)
        val plain = Fixtures::class.java.getMethod("plain")

        assertTrue(InvokerMethods.isSuspendFunction(suspending))
        assertFalse(InvokerMethods.isSuspendFunction(plain))
    }

    @Test
    fun `private methods are accessible and bound to their target`() {
        val method = Fixtures::class.java.getDeclaredMethod("secret")

        assertTrue(InvokerMethods.canAccess(method, lookup))
        assertEquals("secret", InvokerMethods.bind(method, Fixtures(), lookup).invoke())
        assertFailsWith<IllegalArgumentException> { InvokerMethods.bind(method, null, lookup) }
    }

    class Fixtures {
        suspend fun suspending() = Unit
        fun plain() = Unit

        @Suppress("unused")
        private fun secret() = "secret"
    }

    companion object {
        @JvmStatic
        fun sum(a: Long, b: Int): Long = a + b
    }
}
