package dev.slne.surf.nats.client

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.NatsClient
import dev.slne.surf.nats.NatsRegistration
import dev.slne.surf.nats.codec.NatsCodec
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

    private val logger = ComponentLogger.logger("NatsClient[$name]")

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

    private val closed = AtomicBoolean(false)
    override val isClosed get() = closed.get()

    override fun publish(event: Any) {
        checkOpen()
        TODO("Not yet implemented")
    }

    override fun registerListener(listener: Any): NatsRegistration {
        checkOpen()
        TODO("Not yet implemented")
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

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        closeRegistrations()
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