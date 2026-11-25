package dev.slne.surf.api.core.server.impl.event

import dev.slne.surf.api.core.server.impl.event.invoker.SurfEventInvokerFactory
import dev.slne.surf.api.event.SurfAsyncEvent
import dev.slne.surf.api.event.SurfEventHandler
import dev.slne.surf.api.event.SurfSyncEvent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SurfEventBusImplTest {

    open class SyncEvent : SurfSyncEvent() {
        val received = mutableListOf<String>()
    }

    class SpecificSyncEvent : SyncEvent()

    open class AsyncEvent : SurfAsyncEvent() {
        val received = mutableListOf<String>()
    }

    class SpecificAsyncEvent : AsyncEvent()

    class Listener {
        @SurfEventHandler
        private fun onSync(event: SyncEvent) {
            event.received += "sync"
        }

        @SurfEventHandler
        private fun onSpecific(event: SpecificSyncEvent) {
            event.received += "specific"
        }

        @SurfEventHandler
        private fun onAsync(event: AsyncEvent) {
            event.received += "async"
        }

        @SurfEventHandler
        private fun onAsyncReturning(event: AsyncEvent): Boolean {
            return event.received.add("returning")
        }

        @SurfEventHandler
        private suspend fun onAsyncSuspending(event: AsyncEvent) {
            yield()
            event.received += "suspending"
        }
    }

    class SpecificAsyncListener {
        @SurfEventHandler
        private fun onSpecific(event: SpecificAsyncEvent) {
            event.received += "specific"
        }
    }

    class FailingListener {
        @SurfEventHandler
        fun onSync(event: SyncEvent) {
            throw IllegalStateException("handler failed for ${event.javaClass.simpleName}")
        }
    }

    private val bus = SurfEventBusImpl()

    @Test
    fun `private sync handlers receive the event and its subclasses`() {
        bus.registerListeners(Listener())

        val event = bus.callSync(SpecificSyncEvent()) as SyncEvent

        assertEquals(setOf("sync", "specific"), event.received.toSet())
    }

    @Test
    fun `sync handlers of a subclass ignore the base event`() {
        bus.registerListeners(Listener())

        val event = bus.callSync(SyncEvent()) as SyncEvent

        assertEquals(listOf("sync"), event.received)
    }

    @Test
    fun `plain value-returning and suspending async handlers complete`() = runTest {
        bus.registerListeners(Listener())

        val event = bus.callAsync(AsyncEvent()) as AsyncEvent

        assertEquals(setOf("async", "returning", "suspending"), event.received.toSet())
    }

    @Test
    fun `unregistered listeners receive nothing`() {
        val listener = Listener()
        bus.registerListeners(listener)
        bus.unregisterListeners(listener)

        val event = bus.callSync(SyncEvent()) as SyncEvent

        assertTrue(event.received.isEmpty())
    }

    @Test
    fun `sync invokers skip events of another type`() {
        val eventType = SpecificSyncEvent::class.java
        val method = Listener::class.java.getDeclaredMethod("onSpecific", eventType)
        val invoker = SurfEventInvokerFactory.createSync(Listener(), method, eventType)
        val event = SyncEvent()

        invoker.invoke(event)

        assertTrue(event.received.isEmpty())
    }

    @Test
    fun `async invokers skip events of another type`() = runTest {
        val eventType = SpecificAsyncEvent::class.java
        val method = SpecificAsyncListener::class.java.getDeclaredMethod("onSpecific", eventType)
        val invoker = SurfEventInvokerFactory.createAsync(
            SpecificAsyncListener(),
            method,
            eventType,
        )
        val event = AsyncEvent()

        invoker.invoke(event)

        assertTrue(event.received.isEmpty())
    }

    @Test
    fun `invokers rethrow the handler's exception`() {
        val eventType = SyncEvent::class.java
        val method = FailingListener::class.java.getDeclaredMethod("onSync", eventType)
        val invoker = SurfEventInvokerFactory.createSync(FailingListener(), method, eventType)

        val failure = assertFailsWith<IllegalStateException> { invoker.invoke(SyncEvent()) }

        assertEquals("handler failed for SyncEvent", failure.message)
    }
}
