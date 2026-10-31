package dev.slne.surf.nats.client

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.NatsClient
import dev.slne.surf.nats.NatsRegistration
import dev.slne.surf.nats.bytecode.ListenerInvoker
import dev.slne.surf.nats.bytecode.ListenerInvokerFactory
import dev.slne.surf.nats.bytecode.ServiceProxyFactory
import dev.slne.surf.nats.codec.NatsCodec
import dev.slne.surf.nats.codec.NatsHeaders
import dev.slne.surf.nats.connection.NatsConnectionManager
import dev.slne.surf.nats.descriptor.NatsEvent
import dev.slne.surf.nats.event.*
import dev.slne.surf.nats.rpc.RpcCallerImpl
import dev.slne.surf.nats.rpc.ServiceDescriptor
import dev.slne.surf.nats.rpc.ServiceRegistration
import dev.slne.surf.nats.runtime.AbstractNatsClient
import io.nats.client.Dispatcher
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@InternalNatsApi
internal class NatsClientImpl(
    name: String,
    internal val clientOrigin: ClientOrigin,
    private val onClose: (NatsClientImpl) -> Unit,
) : AbstractNatsClient<NatsRegistration>(name), NatsClient {

    internal val clientScope = CoroutineScope(
        Dispatchers.Default +
                CoroutineName("surf-nats/$name") +
                SupervisorJob() +
                CoroutineExceptionHandler { context, throwable ->
                    val coroutine = context[CoroutineName]?.name ?: "surf-nats/$name"
                    logger.error("Unhandled exception in coroutine '$coroutine'", throwable)
                }
    )

    private val serviceProxies = ConcurrentHashMap<Class<*>, Any>()

    private val services = ConcurrentHashMap<String, ServiceRegistration>()

    internal val eventSubscriptions = EventSubscriptions(this)

    private val replyDispatcherLock = Any()

    @Volatile
    private var replyDispatcher: Dispatcher? = null

    val origin = NatsHeaders.origin(name)

    internal fun headers() = NatsHeaders.forOrigin(origin)

    override fun publish(event: NatsEvent) {
        checkOpen()
        val descriptor = eventDescriptor(event.javaClass)
        val data = NatsCodec.encode(descriptor.serializer, event)
        val header = headers()
        val connection = NatsConnectionManager.connection()

        requireEventFits(
            descriptor.type.name,
            descriptor.subject,
            data.size + header.serializedLength(),
            connection.serverInfo.maxPayload,
        )

        connection.publish(descriptor.subject, header, data)
    }

    override suspend fun registerListener(listener: Any): NatsRegistration {
        lifecycle.requireAccepting()
        val methods = ListenerMethods.findListenerMethods(listener, this)
        val entries = methods.map { it to ListenerInvokerFactory.create(listener, it.method) }

        val added = ObjectArrayList<Pair<ListenerMethod, ListenerInvoker>>()
        try {
            for (entry in entries) {
                eventSubscriptions.add(entry.first, entry.second)
                added += entry
            }
            flush()
        } catch (e: Throwable) {
            for ((method, invoker) in added) {
                try {
                    eventSubscriptions.remove(method, invoker)
                } catch (cleanup: Exception) {
                    e.addSuppressed(cleanup)
                }
            }
            throw e
        }

        return EventListenerRegistration(this, entries).also { registerRegistration(it) }
    }

    override suspend fun <S : Any> registerService(
        type: KClass<S>,
        implementation: S
    ): NatsRegistration {
        lifecycle.requireAccepting()
        require(type.java.isInstance(implementation)) {
            "${implementation.javaClass.name} does not implement ${type.java.name}"
        }
        val descriptor = serviceDescriptor(type.java)
        services[descriptor.name]?.let { existing ->
            error(alreadyRegistered(descriptor, existing))
        }

        val registration = ServiceRegistration(this, descriptor, implementation)
        services.putIfAbsent(descriptor.name, registration)?.let { existing ->
            registration.discard()
            error(alreadyRegistered(descriptor, existing))
        }

        try {
            registration.subscribe()
        } catch (e: Throwable) {
            registration.close()
            throw e
        }

        registerRegistration(registration)
        return registration
    }

    private fun alreadyRegistered(descriptor: ServiceDescriptor, existing: ServiceRegistration): String {
        val type = descriptor.type.name
        val existingType = existing.descriptor.type.name
        return if (existingType == type) {
            "$type is already registered on NatsClient[$name]"
        } else {
            "Service ${descriptor.name} of $type is already registered on " +
                    "NatsClient[$name] by $existingType"
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <S : Any> service(type: KClass<S>): S {
        checkOpen()
        return (serviceProxies[type.java] ?: createProxy(type.java)) as S
    }

    private fun createProxy(type: Class<*>): Any {
        lifecycle.requireAccepting()
        var proxy: Any? = null
        lifecycle.admit(
            add = {
                proxy = serviceProxies.computeIfAbsent(type) { serviceType ->
                    val descriptor = serviceDescriptor(serviceType)
                    val caller = RpcCallerImpl(this, descriptor)
                    ServiceProxyFactory.create<Any>(descriptor, caller).also { caller.start() }
                }
            },
            undo = { proxy?.let { serviceProxies.remove(type, it) } },
        )
        return checkNotNull(proxy)
    }

    override fun closeRegistration(registration: NatsRegistration) = registration.close()

    internal fun unregisterService(registration: ServiceRegistration) {
        services.remove(registration.descriptor.name, registration)
    }

    internal fun eventDescriptor(type: Class<*>): EventDescriptor {
        return EVENT_DESCRIPTORS.get(type)
    }

    internal fun serviceDescriptor(type: Class<*>): ServiceDescriptor {
        return SERVICE_DESCRIPTORS.get(type)
    }

    internal fun replyDispatcher(): Dispatcher {
        replyDispatcher?.let { return it }
        synchronized(replyDispatcherLock) {
            checkOpen()
            return replyDispatcher
                ?: NatsConnectionManager.connection().createDispatcher()
                    .also { replyDispatcher = it }
        }
    }

    override fun close() {
        if (!lifecycle.beginClose()) return
        // Running handlers may still publish and call through this client while their registrations drain
        closeRegistrations()
        lifecycle.markClosed()
        closeReplyDispatcher()
        serviceProxies.clear()
        eventSubscriptions.close()
        clientScope.cancel("${lifecycle.label} closed")
        onClose(this)
    }

    private fun closeReplyDispatcher() {
        synchronized(replyDispatcherLock) {
            replyDispatcher?.let { closeDispatcher(it, "reply dispatcher") }
            replyDispatcher = null
        }
    }

    internal fun closeDispatcher(dispatcher: Dispatcher, what: String) {
        try {
            NatsConnectionManager.connection().closeDispatcher(dispatcher)
        } catch (e: Exception) {
            logger.warn("Failed to close $what", e)
        }
    }

    internal suspend fun flush() {
        NatsConnectionManager.flush(5.seconds.toJavaDuration())
    }

    private companion object {
        private val EVENT_DESCRIPTORS = object : ClassValue<EventDescriptor>() {
            override fun computeValue(type: Class<*>): EventDescriptor = EventDescriptor.of(type)
        }

        private val SERVICE_DESCRIPTORS = object : ClassValue<ServiceDescriptor>() {
            override fun computeValue(type: Class<*>): ServiceDescriptor = ServiceDescriptor.of(type)
        }
    }
}
