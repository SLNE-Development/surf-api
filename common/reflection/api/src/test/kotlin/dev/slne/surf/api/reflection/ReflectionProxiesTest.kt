package dev.slne.surf.api.reflection

import java.io.IOException
import kotlin.test.*

class ReflectionProxiesTest {

    private val proxy = reflectionProxy<TargetProxy>()
    private val instance = proxy.create("alice")

    @AfterTest
    fun resetCounter() {
        proxy.setCounter(0)
    }

    @Test
    fun `proxy is a cached hidden class`() {
        assertSame(proxy, ReflectionProxies.get(TargetProxy::class.java))
        assertTrue(proxy.javaClass.isHidden)
    }

    @Test
    fun `private constructors create instances`() {
        assertEquals(TARGET, instance.javaClass.name)
        assertEquals(-1, proxy.fixed(instance))
        assertEquals(7, proxy.fixed(proxy.create("bob", 7)))
    }

    @Test
    fun `private instance methods are called on the receiver`() {
        assertEquals("alice greets bob", proxy.greet(instance, "bob"))
        assertEquals(5L, proxy.sum(instance, 2L, 3))
    }

    @Test
    fun `compatible parameter and return types are cast`() {
        assertEquals("alice greets bob", proxy.greetAsCharSequence(instance, "bob"))
    }

    @Test
    fun `overloads resolve by exact parameter types`() {
        assertEquals("string", proxy.overloadedString(instance, "x"))
    }

    @Test
    fun `private static methods are called without a receiver`() {
        assertEquals(42, proxy.twice(21))
    }

    @Test
    fun `members of superclasses are found`() {
        assertEquals("inherited", proxy.inherited(instance))
        assertEquals("base", proxy.baseValue(instance))
    }

    @Test
    fun `instance fields are read and written`() {
        proxy.setName(instance, "carol")

        assertEquals("carol", proxy.getName(instance))
    }

    @Test
    fun `static fields are read and written`() {
        proxy.setCounter(3)

        assertEquals(3, proxy.counter())
        assertEquals("constant", proxy.constant())
    }

    @Test
    fun `default methods keep their body`() {
        assertEquals("alice!", proxy.describe(instance))
    }

    @Test
    fun `exceptions of the member propagate unchanged`() {
        val failure = assertFailsWith<IOException> { proxy.fail(instance) }

        assertEquals("boom", failure.message)
    }

    @Test
    fun `property accessors bind through a delegating companion`() {
        assertEquals("constant", PropertyProxy.constant)
    }

    @Test
    fun `members of public classes return their exact types`() {
        val builders = reflectionProxy<StringBuilderProxy>()

        val builder = builders.append(builders.create("a"), "b")

        assertEquals("ab", builder.toString())
    }

    @Test
    fun `writing a final field fails on creation`() {
        val failure = assertFailsWith<ReflectionProxyException> {
            reflectionProxy<FinalWriteProxy>()
        }

        assertContains(failure.message.orEmpty(), "final writes are unsupported")
    }

    @Test
    fun `writing a static final field fails on creation`() {
        val failure = assertFailsWith<ReflectionProxyException> {
            reflectionProxy<StaticFinalWriteProxy>()
        }

        assertContains(failure.message.orEmpty(), "CONSTANT is final; final writes are unsupported")
    }

    @Test
    fun `every broken method is reported at once`() {
        val message = assertFailsWith<ReflectionProxyException> {
            reflectionProxy<BrokenProxy>()
        }.message.orEmpty()

        assertContains(message, "missing: $TARGET has no method missing")
        assertContains(message, "unannotated: annotate it with exactly one of")
        assertContains(message, "twoKinds: annotate it with exactly one of")
        assertContains(message, "ambiguous: method overloaded(java.lang.Object) is ambiguous")
        assertContains(message, "wrongReceiver: receiver type java.lang.String does not fit")
        assertContains(message, "noReceiver: expected 1 parameters, found 0")
        assertContains(message, "noMethodReceiver: an instance member takes the receiver first")
        assertContains(message, "staticConstructor: @ReflectedStatic does not apply")
        assertContains(message, "greet: suspend functions are not supported")
    }

    @Test
    fun `a missing target class fails`() {
        val failure = assertFailsWith<ReflectionProxyException> {
            reflectionProxy<MissingTargetProxy>()
        }

        assertContains(failure.message.orEmpty(), "fixture.Missing of")
    }

    @Test
    fun `target and targetName are mutually exclusive`() {
        val failure = assertFailsWith<ReflectionProxyException> {
            reflectionProxy<DoubleTargetProxy>()
        }

        assertContains(failure.message.orEmpty(), "exactly one of target and targetName")
    }

    @Test
    fun `interfaces without the annotation are rejected`() {
        val failure = assertFailsWith<ReflectionProxyException> {
            reflectionProxy<UnannotatedProxy>()
        }

        assertContains(failure.message.orEmpty(), "is not annotated with @ReflectionProxy")
    }

    @Test
    fun `classes are rejected`() {
        val failure = assertFailsWith<ReflectionProxyException> {
            ReflectionProxies.get(ClassProxy::class.java)
        }

        assertContains(failure.message.orEmpty(), "is not an interface")
    }
}
