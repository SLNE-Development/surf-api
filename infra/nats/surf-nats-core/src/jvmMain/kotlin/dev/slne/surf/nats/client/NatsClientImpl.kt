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
import dev.slne.surf.nats.protocol.ProtocolDefaults
import dev.slne.surf.nats.rpc.RpcCallerImpl
import dev.slne.surf.nats.rpc.ServiceDescriptor
import dev.slne.surf.nats.rpc.ServiceRegistration
import dev.slne.surf.nats.runtime.ClientLifecycle
import io.nats.client.Dispatcher
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import kotlinx.coroutines.*
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@InternalNatsApi
internal class NatsClientImpl(
    override val name: String,
    internal val clientOrigin: ClientOrigin,
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

    private val eventDescriptors = object : ClassValue<EventDescriptor>() {
        override fun computeValue(type: Class<*>): EventDescriptor {
            return EventDescriptor.of(type, name)
        }
    }

    private val serviceDescriptors = object : ClassValue<ServiceDescriptor>() {
        override fun computeValue(type: Class<*>): ServiceDescriptor {
            return ServiceDescriptor.of(type, name)
        }
    }

    private val serviceProxies = ConcurrentHashMap<Class<*>, Any>()

    private val services = ConcurrentHashMap<String, ServiceRegistration>()

    internal val eventSubscriptions = EventSubscriptions(this)

    private val lifecycle = ClientLifecycle(name)
    override val isClosed get() = lifecycle.isClosed

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

        val size = data.size + header.serializedLength()
        val max = connection.serverInfo.maxPayload
        require(size <= max) {
            ProtocolDefaults.eventTooLargeMessage(descriptor.type.name, size, max, descriptor.subject)
        }

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
        services[descriptor.registrationKey]?.let { existing ->
            error(alreadyRegistered(descriptor, existing))
        }

        val registration = ServiceRegistration(this, descriptor, implementation)
        services.putIfAbsent(descriptor.registrationKey, registration)?.let { existing ->
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
            "Service '${descriptor.name}' v${descriptor.majorVersion} of $type is already registered on " +
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

    internal fun registerRegistration(registration: NatsRegistration) {
        lifecycle.admit(
            add = { registrations.add(registration) },
            undo = {
                registrations.remove(registration)
                registration.close()
            },
        )
    }

    internal fun unregisterRegistration(registration: NatsRegistration) {
        registrations.remove(registration)
    }

    internal fun unregisterService(registration: ServiceRegistration) {
        services.remove(registration.descriptor.registrationKey, registration)
    }

    internal fun eventDescriptor(type: Class<*>): EventDescriptor {
        return eventDescriptors.get(type)
    }

    internal fun serviceDescriptor(type: Class<*>): ServiceDescriptor {
        return serviceDescriptors.get(type)
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
        withContext(Dispatchers.IO) {
            NatsConnectionManager.connection().flush(5.seconds.toJavaDuration())
        }
    }

    internal fun checkOpen() = lifecycle.requireOpen()
}