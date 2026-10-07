package dev.slne.surf.nats.client

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.NatsClient
import dev.slne.surf.nats.NatsRegistration
import dev.slne.surf.nats.bytecode.ListenerInvoker
import dev.slne.surf.nats.bytecode.ListenerInvokerFactory
import dev.slne.surf.nats.codec.NatsCodec
import dev.slne.surf.nats.codec.NatsHeaders
import dev.slne.surf.nats.connection.NatsConnectionManager
import dev.slne.surf.nats.event.EventDescriptor
import dev.slne.surf.nats.event.EventListenerRegistration
import dev.slne.surf.nats.event.EventSubscriptions
import dev.slne.surf.nats.event.ListenerMethod
import dev.slne.surf.nats.event.ListenerMethods
import io.nats.client.Connection
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import kotlinx.coroutines.*
import kotlinx.serialization.modules.SerializersModule
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.KClass

@InternalNatsApi
class NatsClientImpl(
    override val name: String,
    serializersModule: SerializersModule,
    private val onClose: (NatsClientImpl) -> Unit,
) : NatsClient {

    internal val logger = ComponentLogger.logger("NatsClient[$name]")

    internal val clientScope = CoroutineScope(
        Dispatchers.Default +
                CoroutineName("surf-nats/$name") +
                SupervisorJob() +
                CoroutineExceptionHandler { context, throwable ->
                    val coroutine = context[CoroutineName]?.name ?: "surf-nats/$name"
                    logger.error("Unhandled exception in coroutine '$coroutine'", throwable)
                }
    )

    private val registrations = ConcurrentHashMap.newKeySet<NatsRegistration>()

    val codec = NatsCodec(serializersModule)

    private val eventDescriptors = object : ClassValue<EventDescriptor>() {
        override fun computeValue(type: Class<*>): EventDescriptor {
            return EventDescriptor.of(type, name, codec)
        }
    }

    internal val eventSubscriptions = EventSubscriptions(this)

    private val closed = AtomicBoolean(false)
    override val isClosed get() = closed.get()

    override fun publish(event: Any) {
        checkOpen()
        val descriptor = eventDescriptor(event.javaClass)
        val data = codec.encode(descriptor.serializer, event, descriptor.format)
        val header = NatsHeaders.base(name, descriptor.format)
        val connection = NatsConnectionManager.connection()

        checkPayloadSize(
            descriptor,
            data.size + header.serializedLength(),
            connection
        )

        connection.publish(descriptor.subject, header, data)
    }

    private fun checkPayloadSize(descriptor: EventDescriptor, size: Int, connection: Connection) {
        val max = connection.serverInfo.maxPayload
        require(size <= max) {
            "Event ${descriptor.type.name} is $size bytes, exceeding the NATS max payload of $max " +
                    "bytes (subject ${descriptor.subject}). Large events are not offloaded to the object store."
        }
    }

    override fun registerListener(listener: Any): NatsRegistration {
        checkOpen()
        val methods = ListenerMethods.findListenerMethods(listener, this)
        val entries = methods.map { it to ListenerInvokerFactory.create(listener, it.method) }

        val added = ObjectArrayList<Pair<ListenerMethod, ListenerInvoker>>()
        try {
            for (entry in entries) {
                eventSubscriptions.add(entry.first, entry.second)
                added += entry
            }
        } catch (e: Exception) {
            added.forEach { (method, invoker) -> eventSubscriptions.remove(method, invoker) }
            throw e
        }

        return EventListenerRegistration(this, entries).also { registerRegistration(it) }
    }

    override fun <S : Any> registerService(
        type: KClass<S>,
        implementation: S
    ): NatsRegistration {
        checkOpen()
        TODO("Not yet implemented")
    }

    override fun <S : Any> service(type: KClass<S>): S {
        checkOpen()
        TODO("Not yet implemented")
    }

    internal fun registerRegistration(registration: NatsRegistration) {
        registrations.add(registration)
        if (isClosed) {
            registrations.remove(registration)
            registration.close()
            checkOpen()
        }
    }

    internal fun unregisterRegistration(registration: NatsRegistration) {
        registrations.remove(registration)
    }

    internal fun eventDescriptor(type: Class<*>): EventDescriptor {
        return eventDescriptors.get(type)
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        closeRegistrations()
        eventSubscriptions.close()
        clientScope.cancel("NatsClient[$name] closed")
        onClose(this)
    }

    private fun closeRegistrations() {
        for (registration in registrations.toList()) {
            try {
                registration.close()
            } catch (e: Exception) {
                logger.error("Failed to close registration $registration", e)
            }
        }

        if (registrations.isNotEmpty()) {
            logger.warn("Some registrations were not closed properly: ${registrations.size} remaining")
        }
    }

    private fun checkOpen() = check(!isClosed) { "NatsClient[$name] is closed" }
}