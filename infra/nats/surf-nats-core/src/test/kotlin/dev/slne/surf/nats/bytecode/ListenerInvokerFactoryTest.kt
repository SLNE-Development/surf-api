package dev.slne.surf.nats.bytecode

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds

class ListenerInvokerFactoryTest {

    class Event(val value: String)
    class OtherEvent

    @Suppress("unused", "RedundantSuspendModifier")
    class RecordingListener {
        val received = mutableListOf<String>()

        fun onPublic(event: Event) {
            received += "public:${event.value}"
        }

        internal fun onInternal(event: Event) {
            received += "internal:${event.value}"
        }

        private fun onPrivate(event: Event) {
            received += "private:${event.value}"
        }

        suspend fun onSuspendImmediate(event: Event) {
            received += "suspend-immediate:${event.value}"
        }

        suspend fun onSuspendDelayed(event: Event) {
            delay(10.milliseconds)
            received += "suspend-delayed:${event.value}"
        }

        fun onThrow(event: Event): Unit = throw IllegalStateException("boom ${event.value}")

        suspend fun onSuspendThrow(event: Event) {
            yield()
            throw IllegalStateException("suspend boom ${event.value}")
        }

        fun returnsValue(event: Event): Int = event.value.length

        companion object {
            val staticReceived = mutableListOf<String>()

            @JvmStatic
            fun onStatic(event: Event) {
                staticReceived += "static:${event.value}"
            }
        }
    }

    object ObjectListener {
        val received = mutableListOf<String>()

        fun onEvent(event: Event) {
            received += "object:${event.value}"
        }
    }

    @Test
    fun `invokes public method`() = runTest {
        val listener = RecordingListener()
        invokerFor(listener, "onPublic").call(Event("a"))
        assertEquals(listOf("public:a"), listener.received)
    }

    @Test
    fun `invokes internal method`() = runTest {
        val listener = RecordingListener()
        invokerFor(listener, "onInternal").call(Event("a"))
        assertEquals(listOf("internal:a"), listener.received)
    }

    @Test
    fun `invokes private method`() = runTest {
        val listener = RecordingListener()
        invokerFor(listener, "onPrivate").call(Event("a"))
        assertEquals(listOf("private:a"), listener.received)
    }

    @Test
    fun `invokes method of an object`() = runTest {
        ObjectListener.received.clear()
        invokerFor(ObjectListener, "onEvent").call(Event("a"))
        assertEquals(listOf("object:a"), ObjectListener.received)
    }

    @Test
    fun `invokes JvmStatic method without target`() = runTest {
        RecordingListener.staticReceived.clear()
        val method = RecordingListener::class.java.declaredMethods
            .single { it.name == "onStatic" && Modifier.isStatic(it.modifiers) }

        ListenerInvokerFactory.create(null, method).call(Event("a"))

        assertEquals(listOf("static:a"), RecordingListener.staticReceived)
    }

    @Test
    fun `normal method returns Unit without suspending`() {
        val invoker = invokerFor(RecordingListener(), "onPublic")
        assertSame(Unit, invoker.invoke(Event("a"), NoopContinuation))
    }

    @Test
    fun `suspend method that does not suspend returns immediately`() = runTest {
        val listener = RecordingListener()
        val invoker = invokerFor(listener, "onSuspendImmediate")

        assertSame(Unit, invoker.invoke(Event("a"), NoopContinuation))
        assertEquals(listOf("suspend-immediate:a"), listener.received)
    }

    @Test
    fun `suspend method that suspends completes after resumption`() = runTest {
        val listener = RecordingListener()
        val invoker = invokerFor(listener, "onSuspendDelayed")

        assertSame(COROUTINE_SUSPENDED, invoker.invoke(Event("direct"), NoopContinuation))

        invoker.call(Event("a"))
        assertTrue("suspend-delayed:a" in listener.received, listener.received.toString())
    }

    @Test
    fun `exception from normal method propagates unchanged`() = runTest {
        val exception = assertFailsWith<IllegalStateException> {
            invokerFor(RecordingListener(), "onThrow").call(Event("a"))
        }
        assertEquals("boom a", exception.message)
    }

    @Test
    fun `exception from suspend method propagates unchanged`() = runTest {
        val exception = assertFailsWith<IllegalStateException> {
            invokerFor(RecordingListener(), "onSuspendThrow").call(Event("a"))
        }
        assertEquals("suspend boom a", exception.message)
    }

    @Test
    fun `wrong event type fails with ClassCastException`() = runTest {
        val invoker = invokerFor(RecordingListener(), "onPublic")
        assertFailsWith<ClassCastException> { invoker.call(OtherEvent()) }
    }

    @Test
    fun `methods of the same class get separate invokers`() = runTest {
        val listener = RecordingListener()
        val public = invokerFor(listener, "onPublic")
        val private = invokerFor(listener, "onPrivate")

        assertNotSame(public::class.java, private::class.java)

        private.call(Event("1"))
        public.call(Event("2"))
        assertEquals(listOf("private:1", "public:2"), listener.received)
    }

    @Test
    fun `normal method with return value is rejected`() {
        assertFailsWith<IllegalArgumentException> { invokerFor(RecordingListener(), "returnsValue") }
    }

    @Test
    fun `instance method without target is rejected`() {
        val method = method(RecordingListener::class.java, "onPublic")
        assertFailsWith<IllegalArgumentException> { ListenerInvokerFactory.create(null, method) }
    }

    private fun invokerFor(target: Any, name: String): ListenerInvoker {
        return ListenerInvokerFactory.create(target, method(target.javaClass, name))
    }

    private fun method(type: Class<*>, name: String): Method {
        return type.declaredMethods.single { it.name == name || it.name.startsWith("$name$") }
    }

    private suspend fun ListenerInvoker.call(event: Any) {
        suspendCoroutineUninterceptedOrReturn { invoke(event, it) }
    }

    private object NoopContinuation : Continuation<Unit> {
        override val context = EmptyCoroutineContext
        override fun resumeWith(result: Result<Unit>) = Unit
    }
}
