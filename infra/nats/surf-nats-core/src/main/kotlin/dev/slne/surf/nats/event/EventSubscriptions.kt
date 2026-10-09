package dev.slne.surf.nats.event

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.NatsEventContext
import dev.slne.surf.nats.bytecode.ListenerInvoker
import dev.slne.surf.nats.bytecode.call
import dev.slne.surf.nats.client.NatsClientImpl
import dev.slne.surf.nats.codec.NatsHeaders
import dev.slne.surf.nats.connection.NatsConnectionManager
import io.nats.client.Dispatcher
import io.nats.client.Message
import io.nats.client.MessageHandler
import io.nats.client.Subscription
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.cancellation.CancellationException

@InternalNatsApi
internal class EventSubscriptions(private val client: NatsClientImpl) {

    private data class Key(val subject: String, val queueGroup: String?)
    private class Target(val invoker: ListenerInvoker, val ignoreOwn: Boolean)

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
        val existing = entries[key]

        val target = Target(invoker, method.ignoreOwn)

        if (existing != null) {
            check(existing.descriptor.type == method.descriptor.type) {
                "Subject '${key.subject}' is already used by ${existing.descriptor.type.name}, " +
                        "cannot register a listener for ${method.descriptor.type.name}"
            }
            existing.targets.add(target)
            return
        }

        val targets = CopyOnWriteArrayList(listOf(target))
        val descriptor = method.descriptor
        val handler = MessageHandler { message -> dispatch(descriptor, targets, message) }

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
            if (!closed) {
                dispatcher?.unsubscribe(entry.subscription)
            }
        }
    }

    fun close(): Unit = synchronized(lock) {
        if (closed) return
        closed = true
        entries.clear()

        val dispatcher = dispatcher ?: return
        this.dispatcher = null
        try {
            NatsConnectionManager.connection().closeDispatcher(dispatcher)
        } catch (e: Exception) {
            client.logger.warn("Failed to close event dispatcher", e)
        }
    }

    private fun dispatch(
        descriptor: EventDescriptor,
        targets: List<Target>,
        message: Message
    ) {
        val headers = if (message.hasHeaders()) message.headers else null
        val origin = headers?.getFirst(NatsHeaders.ORIGIN)
        val contentType = headers?.getFirst(NatsHeaders.CONTENT_TYPE)

        val own = origin == client.origin

        if (own && targets.all { it.ignoreOwn }) return

        val event = try {
            client.codec.decode(descriptor.serializer, message.data, contentType)
        } catch (e: Exception) {
            client.logger.warn(
                "Dropping undecodable ${descriptor.type.simpleName} on ${message.subject}",
                e
            )
            return
        }

        val context = NatsEventContext(origin, own) +
                CoroutineName("surf-nats/${client.name}/listener/${descriptor.type.simpleName}")

        for (target in targets) {
            if (own && target.ignoreOwn) continue
            client.clientScope.launch(context) {
                try {
                    target.invoker.call(event)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    client.logger.error(
                        "Listener for ${descriptor.type.name} failed on ${message.subject}",
                        e
                    )
                }
            }
        }
    }

    private fun keyOf(method: ListenerMethod): Key {
        return Key(method.descriptor.subject, if (method.loadBalanced) client.name else null)
    }
}