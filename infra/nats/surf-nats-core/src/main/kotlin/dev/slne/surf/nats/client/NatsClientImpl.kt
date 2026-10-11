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
import dev.slne.surf.nats.event.*
import dev.slne.surf.nats.rpc.RpcCallerImpl
import dev.slne.surf.nats.rpc.ServiceDescriptor
import dev.slne.surf.nats.rpc.ServiceRegistration
import io.nats.client.Connection
import io.nats.client.Dispatcher
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import kotlinx.coroutines.*
import kotlinx.serialization.modules.SerializersModule
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@InternalNatsApi
internal class NatsClientImpl(
    override val name: String,
    serializersModule: SerializersModule,
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

    val codec = NatsCodec(serializersModule)

    private val eventDescriptors = object : ClassValue<EventDescriptor>() {
        override fun computeValue(type: Class<*>): EventDescriptor {
            return EventDescriptor.of(type, name, codec)
        }
    }

    private val serviceDescriptors = object : ClassValue<ServiceDescriptor>() {
        override fun computeValue(type: Class<*>): ServiceDescriptor {
            return ServiceDescriptor.of(type, this@NatsClientImpl)
        }
    }

    private val serviceProxies = object : ClassValue<Any>() {
        override fun computeValue(type: Class<*>): Any {
            val descriptor = serviceDescriptor(type)
            return ServiceProxyFactory.create(
                descriptor,
                RpcCallerImpl(this@NatsClientImpl, descriptor)
            )
        }
    }

    private val services = ConcurrentHashMap<Class<*>, ServiceRegistration>()

    internal val eventSubscriptions = EventSubscriptions(this)

    private val closed = AtomicBoolean(false)
    override val isClosed get() = closed.get()

    private val replyDispatcherLock = Any()

    @Volatile
    private var replyDispatcher: Dispatcher? = null

    val origin = NatsHeaders.origin(name)

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

    override suspend fun registerListener(listener: Any): NatsRegistration {
        checkOpen()
        val methods = ListenerMethods.findListenerMethods(listener, this)
        val entries = methods.map { it to ListenerInvokerFactory.create(listener, it.method) }

        val added = ObjectArrayList<Pair<ListenerMethod, ListenerInvoker>>()
        try {
            for (entry in entries) {
                eventSubscriptions.add(entry.first, entry.second)
                added += entry
            }
            withContext(Dispatchers.IO) {
                NatsConnectionManager.connection().flush(5.seconds.toJavaDuration())
            }
        } catch (e: Exception) {
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
        checkOpen()
        require(type.java.isInstance(implementation)) {
            "${implementation.javaClass.name} does not implement ${type.java.name}"
        }
        val descriptor = serviceDescriptor(type.java)
        val registration = ServiceRegistration(this, descriptor, implementation)

        check(services.putIfAbsent(type.java, registration) == null) {
            "${type.java.name} is already registered on NatsClient[$name]"
        }

        try {
            registration.subscribe()
        } catch (e: Exception) {
            registration.close()
            throw e
        }

        registerRegistration(registration)
        return registration
    }

    @Suppress("UNCHECKED_CAST")
    override fun <S : Any> service(type: KClass<S>): S {
        checkOpen()
        return serviceProxies.get(type.java) as S
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

    internal fun unregisterService(registration: ServiceRegistration) {
        services.remove(registration.descriptor.type, registration)
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
        if (!closed.compareAndSet(false, true)) return
        closeRegistrations()
        closeReplyDispatcher()
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

    private fun closeReplyDispatcher() {
        synchronized(replyDispatcherLock) {
            replyDispatcher?.let { dispatcher ->
                try {
                    NatsConnectionManager.connection().closeDispatcher(dispatcher)
                } catch (e: Exception) {
                    logger.warn("Failed to close reply dispatcher", e)
                }
            }
            replyDispatcher = null
        }
    }

    private fun checkOpen() = check(!isClosed) { "NatsClient[$name] is closed" }
}