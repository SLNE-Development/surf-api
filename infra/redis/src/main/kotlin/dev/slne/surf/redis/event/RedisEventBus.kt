package dev.slne.surf.redis.event

import com.google.common.flogger.StackSize
import dev.slne.surf.redis.RedisApi
import dev.slne.surf.redis.util.KotlinSerializerCache
import dev.slne.surf.redis.util.asDeferred
import dev.slne.surf.surfapi.core.api.util.logger
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import org.jetbrains.annotations.Blocking
import reactor.core.Disposable
import java.lang.invoke.LambdaMetafactory
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.InaccessibleObjectException
import java.lang.reflect.Method
import org.redisson.client.codec.StringCodec.INSTANCE as StringCodec

/**
 * Redis-backed event bus based on Redis Pub/Sub.
 *
 * This event bus:
 * - Publishes events to Redis using JSON serialization
 * - Subscribes to a single Redis channel
 * - Dispatches events to locally registered handlers
 *
 * Event handlers are discovered via the [OnRedisEvent] annotation
 * and are invoked using JVM-generated lambdas for minimal dispatch overhead.
 *
 * Registration must happen before the owning [RedisApi] is frozen.
 */
class RedisEventBus internal constructor(private val api: RedisApi) {

    /**
     * Registered event handlers indexed by exact event type.
     *
     * Dispatch is exact-type only for maximum performance.
     */
    private val eventHandlers =
        Object2ObjectOpenHashMap<Class<out RedisEvent>, ObjectArrayList<RedisEventConsumer>>()

    /**
     * Registry mapping serialized event type identifiers to event classes.
     */
    private val eventTypeRegistry = Object2ObjectOpenHashMap<String, Class<out RedisEvent>>()

    /**
     * Cache for event serializers, resolved once per event class.
     */
    private val serializerCache = KotlinSerializerCache<RedisEvent>(api.json.serializersModule)

    private val topic by lazy { api.redissonReactive.getTopic(REDIS_CHANNEL, StringCodec) }
    private lateinit var subscription: Disposable

    companion object {
        private val log = logger()
        private const val REDIS_CHANNEL = "surf-redis:events"
        private val lookup = MethodHandles.lookup()
    }

    /**
     * Initializes the event bus by subscribing to the Redis event channel.
     *
     * This method is blocking and must be called during startup.
     */
    @Blocking
    internal fun init() {
        setupSubscription()
    }

    /**
     * Sets up the Redis Pub/Sub subscription and installs the message listener.
     *
     * Incoming messages are dispatched synchronously on a Redisson/Reactor thread.
     */
    @Blocking
    private fun setupSubscription() {
        subscription = topic.getMessages(String::class.java)
            .onErrorContinue { t, message ->
                log.atSevere()
                    .withCause(t)
                    .log("Error receiving Redis Pub/Sub message: ${message.toString().replace("{", "[").replace("}", "]")}")
            }
            .subscribe(this::handleIncomingMessage)
    }

    fun close() {
        subscription.dispose()
    }

    /**
     * Handles an incoming Redis Pub/Sub message.
     *
     * The message is deserialized, validated, and dispatched to all
     * registered handlers for the corresponding event type.
     */
    private fun handleIncomingMessage(message: String) {
        val envelope = try {
            api.json.decodeFromString<EventEnvelope>(message)
        } catch (e: SerializationException) {
            log.atFine()
                .withCause(e)
                .log("Unable to deserialize event envelope: ${e.message}")
            return
        }

        val eventClass = eventTypeRegistry[envelope.eventClass]

        if (eventClass == null) {
            log.atFine()
                .log("No registered event class for name: ${envelope.eventClass} - ignoring event.")
            return
        }

        val event = deserializeEvent(eventClass, envelope.eventData) ?: return
        val handler = eventHandlers[eventClass]
        if (handler.isNullOrEmpty()) return

        for (redisEventConsumer in handler) {
            try {
                redisEventConsumer.accept(event)
            } catch (e: Throwable) {
                log.atSevere()
                    .withCause(e)
                    .log("Error handling event ${event::class.simpleName}: ${e.message}")
            }
        }
    }

    /**
     * Publishes an event to Redis asynchronously.
     *
     * @return a [Deferred] containing the number of receiving subscribers,
     *         or `0` if the event could not be serialized
     */
    fun publish(event: RedisEvent): Deferred<Long> {
        val eventData = serializeEvent(event) ?: return CompletableDeferred(0L)
        val envelope = EventEnvelope.forEvent(event, eventData)

        val message = api.json.encodeToString(envelope)

        return topic.publish(message).asDeferred()
    }

    /**
     * Registers all event handler methods on the given listener instance.
     *
     * Methods annotated with [OnRedisEvent] must:
     * - Have exactly one parameter
     * - Accept a subtype of [RedisEvent]
     *
     * @throws IllegalStateException if the [RedisApi] has already been frozen
     */
    fun registerListener(listener: Any) {
        require(!api.isFrozen()) { "Cannot register listener after RedisApi has been frozen." }

        val methods = listener.javaClass.declaredMethods

        for (method in methods) {
            if (method.isAnnotationPresent(OnRedisEvent::class.java)) {
                if (method.parameterCount != 1) {
                    log.atSevere()
                        .withStackTrace(StackSize.MEDIUM)
                        .log("Method ${method.name} has invalid parameter count - cannot register as event handler.")
                    continue
                }

                val firstParamType = method.parameterTypes.first()

                if (!RedisEvent::class.java.isAssignableFrom(firstParamType)) {
                    log.atSevere()
                        .withStackTrace(StackSize.MEDIUM)
                        .log("Method ${method.name} parameter is not a RedisEvent - cannot register as event handler.")
                    continue
                }

                @Suppress("UNCHECKED_CAST")
                firstParamType as Class<out RedisEvent>

                eventTypeRegistry[firstParamType.name] = firstParamType

                try {
                    method.isAccessible = true
                } catch (e: InaccessibleObjectException) {
                    log.atWarning()
                        .withCause(e)
                        .log("Unable to set accessible flag for method ${method.name}.")
                    continue
                }

                val eventConsumer = createRedisEventConsumer(listener, method, firstParamType)
                eventHandlers.computeIfAbsent(firstParamType) { ObjectArrayList() }
                    .add(eventConsumer)
            }
        }
    }

    /**
     * Creates a highly optimized event consumer for the given handler method.
     *
     * The method is converted into a JVM-generated lambda via [LambdaMetafactory],
     * resulting in near-direct invocation performance.
     */
    private fun createRedisEventConsumer(
        listener: Any,
        method: Method,
        eventType: Class<out RedisEvent>
    ): RedisEventConsumer {
        val listenerClass = listener.javaClass
        val listenerLookup = MethodHandles.privateLookupIn(listenerClass, lookup)

        val samMethodType = MethodType.methodType(Void.TYPE, Any::class.java)
        val implMethod = listenerLookup.unreflect(method)
        val instantiatedMethodType = MethodType.methodType(Void.TYPE, eventType)
        val invokedType = MethodType.methodType(RedisEventConsumer::class.java, listenerClass)


        val callSite = LambdaMetafactory.metafactory(
            lookup,
            "accept",
            invokedType,
            samMethodType,
            implMethod,
            instantiatedMethodType
        )

        val factory = callSite.target
        return factory.invoke(listener) as RedisEventConsumer
    }

    /**
     * Serializes the given event to JSON.
     *
     * @return the serialized event, or `null` if no serializer is available
     */
    private fun serializeEvent(event: RedisEvent): String? {
        val serializer = serializerCache.get(event.javaClass)

        if (serializer == null) {
            log.atWarning()
                .log("No serializer found for event ${event::class.simpleName} — cannot serialize.")
            return null
        }

        try {
            return api.json.encodeToString(serializer, event)
        } catch (e: SerializationException) {
            log.atWarning()
                .withCause(e)
                .log("Unable to serialize event ${event::class.simpleName}: ${e.message}")

            return null
        }
    }

    /**
     * Deserializes an event of the given type from JSON.
     *
     * @return the deserialized event, or `null` if deserialization fails
     */
    private fun deserializeEvent(
        eventClass: Class<out RedisEvent>,
        eventData: String
    ): RedisEvent? {
        val serializer = serializerCache.get(eventClass)

        if (serializer == null) {
            log.atWarning()
                .log("No serializer found for event class ${eventClass.simpleName} — cannot deserialize.")
            return null
        }

        try {
            return api.json.decodeFromString(serializer, eventData)
        } catch (e: SerializationException) {
            log.atWarning()
                .withCause(e)
                .log("Unable to deserialize event ${eventClass.simpleName}: ${e.message}")
            return null
        }
    }

    /**
     * Wire format for Redis event messages.
     */
    @Serializable
    private data class EventEnvelope(
        val eventClass: String,
        val eventData: String
    ) {
        companion object {
            fun forEvent(event: RedisEvent, data: String): EventEnvelope {
                return EventEnvelope(
                    eventClass = event.javaClass.name,
                    eventData = data
                )
            }

        }
    }

    /**
     * Internal functional interface used for fast event dispatch.
     */
    @Suppress("unused")
    @FunctionalInterface
    private fun interface RedisEventConsumer {
        fun accept(event: Any)
    }
}
