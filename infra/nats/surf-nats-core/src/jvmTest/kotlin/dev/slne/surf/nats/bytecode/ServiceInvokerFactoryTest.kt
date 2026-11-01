package dev.slne.surf.nats.bytecode

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.milliseconds

class ServiceInvokerFactoryTest {

    data class Request(val value: String)

    interface Example {
        suspend fun echo(request: Request): String
        suspend fun noRequest(): Int
        suspend fun delayed(request: Request): String
        suspend fun unit(request: Request)
        suspend fun fails(request: Request): String
    }

    private class PrivateImpl : Example {
        var unitValue: String? = null

        override suspend fun echo(request: Request) = "echo:${request.value}"

        override suspend fun noRequest() = 42

        override suspend fun delayed(request: Request): String {
            delay(10.milliseconds)
            return "delayed:${request.value}"
        }

        override suspend fun unit(request: Request) {
            unitValue = request.value
        }

        override suspend fun fails(request: Request): String = throw IllegalStateException(request.value)
    }

    private val impl = PrivateImpl()

    private fun invoker(name: String): ServiceInvoker {
        return ServiceInvokerFactory.create(impl, Example::class.java.methods.single { it.name == name })
    }

    @Test
    fun `request is passed to the implementation`() = runTest {
        assertEquals("echo:a", invoker("echo").call(Request("a")))
    }

    @Test
    fun `method without a request ignores the passed value`() = runTest {
        assertEquals(42, invoker("noRequest").call(Unit))
    }

    @Test
    fun `suspending implementation resumes with its result`() = runTest {
        assertEquals("delayed:a", invoker("delayed").call(Request("a")))
    }

    @Test
    fun `unit method returns Unit`() = runTest {
        assertEquals(Unit, invoker("unit").call(Request("u")))
        assertEquals("u", impl.unitValue)
    }

    @Test
    fun `implementation exception is thrown unwrapped`() = runTest {
        val exception = assertFailsWith<IllegalStateException> {
            invoker("fails").call(Request("boom"))
        }
        assertEquals("boom", exception.message)
    }

    @Test
    fun `request of another type fails`() = runTest {
        assertFailsWith<ClassCastException> {
            invoker("echo").call("not a request")
        }
    }

    @Test
    fun `invokers for different methods are independent`() = runTest {
        val first = invoker("noRequest")
        val second = invoker("delayed")
        assertEquals(42, first.call(Unit))
        assertEquals("delayed:b", second.call(Request("b")))
        assertNull(impl.unitValue)
    }
}
