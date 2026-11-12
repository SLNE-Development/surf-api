package dev.slne.surf.nats.event

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.bytecode.ListenerInvoker
import dev.slne.surf.nats.bytecode.call
import dev.slne.surf.nats.client.NatsClientImpl
import dev.slne.surf.nats.client.awaitRunningHandlers
import dev.slne.surf.nats.codec.NatsCodec
import dev.slne.surf.nats.codec.header
import dev.slne.surf.nats.connection.NatsConnectionManager
import dev.slne.surf.nats.protocol.ProtocolHeaders
import dev.slne.surf.nats.runtime.ListenerRules
import io.nats.client.Dispatcher
import io.nats.client.Message
import io.nats.client.MessageHandler
import io.nats.client.Subscription
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.jetbrains.annotations.Blocking
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * Manages the event subscriptions of one client on a dispatcher of its own.
 *
 * Listener methods with the same subject and queue group share one subscription. `ANY_INSTANCE` listeners subscribe
 * in a queue group named after the client, so the instances of the same client share the load. One subject accepts
 * listeners of a single event type per client.
 *
 * Every message is decoded once and each listener runs in its own coroutine. A message that cannot be decoded,
 * including one without a `Content-Type: application/protobuf` header, is dropped with a warning. A failing listener
 * is logged and does not affect the others. Thread-safe.
 */
@InternalNatsApi
internal class EventSubscriptions(private val client: NatsClientImpl) {

    private data class Key(val subject: String, val queueGroup: String?)
    private class Target(val invoker: ListenerInvoker, val ignoreFromThisClient: Boolean)

    private class Entry(
        val descriptor: EventDescriptor,
        val subscription: Subscription,
        val targets: CopyOnWriteArrayList<Target>,
    )

    private val lock = Any()
    private val entries = HashMap<Key, Entry>()
    private var dispatcher: Dispatcher? = null
    private var closed = false

    private val handlerScope = client.clientScope + SupervisorJob(client.clientScope.coroutineContext.job)

    /**
     * Adds [invoker] as a target of [method]'s subject and subscribes when no subscription exists yet.
     *
     * Throws [IllegalStateException] after [drain] or [close], or when the subject is used by another event type.
     */
    fun add(method: ListenerMethod, invoker: ListenerInvoker): Unit = synchronized(lock) {
        check(!closed) { "NatsClient[${client.name}] is closed" }

        val key = keyOf(method)

        for ((otherKey, other) in entries) {
            if (otherKey.subject != key.subject) continue
            check(other.descriptor.type == method.descriptor.type) {
                "Subject '${key.subject}' is already used by ${other.descriptor.type.name}, " +
                        "cannot register a listener for ${method.descriptor.type.name}"
            }
        }

        val target = Target(invoker, method.ignoreFromThisClient)

        val existing = entries[key]
        if (existing != null) {
            existing.targets.add(target)
            return
        }

        val targets = CopyOnWriteArrayList(listOf(target))
        val descriptor = method.descriptor
        val name = CoroutineName("surf-nats/${client.name}/listener/${descriptor.type.simpleName}")
        val handler = MessageHandler { message -> dispatch(descriptor, name, targets, message) }

        val dispatcher = dispatcher
            ?: NatsConnectionManager.connection().createDispatcher().also { dispatcher = it }

        val subscription = if (key.queueGroup == null) {
            dispatcher.subscribe(key.subject, handler)
        } else {
            dispatcher.subscribe(key.subject, key.queueGroup, handler)
        }

        entries[key] = Entry(descriptor, subscription, targets)
    }

    /**
     * Removes [invoker] from [method]'s subject and unsubscribes once no target is left.
     */
    fun remove(method: ListenerMethod, invoker: ListenerInvoker): Unit = synchronized(lock) {
        val key = keyOf(method)
        val entry = entries[key] ?: return

        entry.targets.removeIf { it.invoker === invoker }

        if (entry.targets.isEmpty()) {
            entries.remove(key)
            dispatcher?.unsubscribe(entry.subscription)
        }
    }

    /**
     * Stops every subscription after the messages it already received are dispatched, then waits for the running
     * handlers until [timeout] passes and cancels those still running. Later listeners are rejected, handlers launched
     * after the drain never run and [close] has nothing left to do.
     */
    @Blocking
    fun drain(timeout: Duration) {
        val deadline = TimeSource.Monotonic.markNow() + timeout
        val draining = synchronized(lock) {
            if (closed) return
            closed = true
            entries.clear()
            dispatcher.also { dispatcher = null }
        }
        try {
            draining?.let { client.drainDispatcher(it, timeout, "event dispatcher") }
            val stillActive = awaitRunningHandlers(handlerScope, deadline)
            if (stillActive != null) {
                client.logger.warn(
                    "$stillActive listener handlers did not finish within the drain timeout of $timeout " +
                            "and are cancelled"
                )
            }
        } finally {
            handlerScope.cancel("Listener handlers of NatsClient[${client.name}] drained")
        }
    }

    /**
     * Closes the dispatcher without draining it. Running handlers keep running until [NatsClientImpl.close] cancels
     * the client scope.
     */
    fun close(): Unit = synchronized(lock) {
        if (closed) return
        closed = true
        entries.clear()

        val dispatcher = dispatcher ?: return
        this.dispatcher = null
        client.closeDispatcher(dispatcher, "event dispatcher")
    }

    private fun dispatch(
        descriptor: EventDescriptor,
        name: CoroutineName,
        targets: List<Target>,
        message: Message
    ) {
        val origin = message.header(ProtocolHeaders.ORIGIN)
        val contentType = message.header(ProtocolHeaders.CONTENT_TYPE)

        val fromThisClient = origin == client.origin

        if (fromThisClient && targets.all { it.ignoreFromThisClient }) return

        val event = try {
            NatsCodec.decode(descriptor.serializer, message.data, contentType)
        } catch (e: Exception) {
            client.logger.warn(
                "Dropping undecodable ${descriptor.type.simpleName} on ${message.subject}",
                e
            )
            return
        }

        val context = NatsEventContext(origin, fromThisClient, message.header(ProtocolHeaders.EVENT_ID)) + name

        for (target in targets) {
            if (fromThisClient && target.ignoreFromThisClient) continue
            handlerScope.launch(context) {
                try {
                    target.invoker.call(event)
                } catch (e: CancellationException) {
                    ensureActive()
                    logListenerFailure(descriptor, message, e)
                } catch (e: Exception) {
                    logListenerFailure(descriptor, message, e)
                }
            }
        }
    }

    private fun logListenerFailure(descriptor: EventDescriptor, message: Message, e: Exception) {
        client.logger.error("Listener for ${descriptor.type.name} failed on ${message.subject}", e)
    }

    private fun keyOf(method: ListenerMethod): Key {
        return Key(method.descriptor.subject, ListenerRules.queueGroup(method.recipients, client.name))
    }
}