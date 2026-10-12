package dev.slne.surf.nats.event

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.bytecode.ListenerInvoker
import dev.slne.surf.nats.bytecode.call
import dev.slne.surf.nats.client.NatsClientImpl
import dev.slne.surf.nats.codec.NatsHeaders
import dev.slne.surf.nats.codec.header
import dev.slne.surf.nats.connection.NatsConnectionManager
import dev.slne.surf.nats.rpc.Recipients
import io.nats.client.Dispatcher
import io.nats.client.Message
import io.nats.client.MessageHandler
import io.nats.client.Subscription
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.cancellation.CancellationException

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
    private val entries = Object2ObjectOpenHashMap<Key, Entry>()
    private var dispatcher: Dispatcher? = null
    private var closed = false

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

    fun remove(method: ListenerMethod, invoker: ListenerInvoker): Unit = synchronized(lock) {
        val key = keyOf(method)
        val entry = entries[key] ?: return

        entry.targets.removeIf { it.invoker === invoker }

        if (entry.targets.isEmpty()) {
            entries.remove(key)
            dispatcher?.unsubscribe(entry.subscription)
        }
    }

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
        val origin = message.header(NatsHeaders.ORIGIN)
        val contentType = message.header(NatsHeaders.CONTENT_TYPE)

        val fromThisClient = origin == client.origin

        if (fromThisClient && targets.all { it.ignoreFromThisClient }) return

        val event = try {
            client.codec.decode(descriptor.serializers, message.data, contentType)
        } catch (e: Exception) {
            client.logger.warn(
                "Dropping undecodable ${descriptor.type.simpleName} on ${message.subject}",
                e
            )
            return
        }

        val context = NatsEventContext(origin, fromThisClient) + name

        for (target in targets) {
            if (fromThisClient && target.ignoreFromThisClient) continue
            client.clientScope.launch(context) {
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
        return Key(method.descriptor.subject, if (method.recipients == Recipients.ANY_INSTANCE) client.name else null)
    }
}