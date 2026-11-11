package dev.slne.surf.microservice.reliability.operation

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * Marks the coroutine of one handler run inside an atomic operation and collects the events the run publishes.
 *
 * Child coroutines inherit it and may add events concurrently. After [complete], [add] fails, so an event published
 * after the handler returned is rejected instead of being dropped silently.
 */
internal class ActiveOperation : AbstractCoroutineContextElement(Key) {
    private val events = ArrayList<PendingEvent>()
    private var completed = false

    /**
     * Holds [event] until the outcome of the operation is known.
     *
     * @throws IllegalStateException if the handler run has ended
     */
    fun add(event: PendingEvent): Unit = synchronized(this) {
        check(!completed) { "The transactional handler returned; it can no longer publish events" }
        events.add(event)
    }

    /**
     * Ends the handler run and returns its events in publish order.
     */
    fun complete(): List<PendingEvent> = synchronized(this) {
        completed = true
        events.toList()
    }

    companion object Key : CoroutineContext.Key<ActiveOperation>
}
