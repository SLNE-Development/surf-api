@file:Suppress("InternalApiUsage")

package dev.slne.surf.redis.event

import com.google.common.flogger.StackSize
import dev.slne.surf.api.core.invoker.HiddenInvokerUtil
import dev.slne.surf.api.core.invoker.InvokerFactory
import dev.slne.surf.api.core.util.emptyObject2ObjectMap
import dev.slne.surf.api.core.util.logger
import dev.slne.surf.api.shared.api.util.InternalInvokerApi
import dev.slne.surf.redis.RedisApi
import dev.slne.surf.redis.RedisComponentProvider
import dev.slne.surf.redis.codec.RedisCodecException
import dev.slne.surf.redis.invoker.RedisEventInvokerTemplate
import dev.slne.surf.redis.invoker.RedisInvokerLookupProvider
import dev.slne.surf.redis.util.KotlinSerializerCache
import dev.slne.surf.redis.util.asDeferred
import it.unimi.dsi.fastutil.objects.Object2ObjectMap
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement
import org.redisson.client.codec.StringCodec
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write

@Suppress("UnstableApiUsage")
@OptIn(InternalInvokerApi::class)
class RedisEventBusImpl(private val api: RedisApi) : RedisEventBus, RedisEventCodecRegistrar {

    /**
     * Registered event handlers indexed by exact event type.
     *
     * Each entry maps a concrete [RedisEvent] subclass to an ordered list of
     * [RedisEventInvoker] instances. Invokers are hidden-class-backed wrappers around
     * the original handler `MethodHandle`, enabling JIT-constant-folding of the dispatch target.
     *
     * Dispatch is exact-type only (no inheritance lookup) for maximum performance.
     */
    private val eventHandlers =
        Object2ObjectOpenHashMap<Class<out RedisEvent>, ObjectArrayList<RedisEventInvoker>>()

    /**
     * Registry mapping serialized event type identifiers to event classes.
     */
    private val eventTypeRegistry = Object2ObjectOpenHashMap<String, Class<out RedisEvent>>()

    @Volatile
    private var handlerSnapshot: Object2ObjectMap<Class<out RedisEvent>, Array<RedisEventInvoker>> =
        emptyObject2ObjectMap()

    @Volatile
    private var typeSnapshot: Object2ObjectMap<String, Class<out RedisEvent>> =
        emptyObject2ObjectMap()

    /**
     * Guards mutations to [eventHandlers] and [eventTypeRegistry] during handler registration and
     * the snapshot rebuild that publishes them.
     */
    private val registrationLock = ReentrantReadWriteLock()

    /**
     * Cache for event serializers, resolved once per event class. Used by the inbound path.
     */
    private val serializerCache = KotlinSerializerCache<RedisEvent>(api.json.serializersModule)

    /**
     * Cache for outbound envelope serializers, resolved once per event class.
     */
    private val envelopeSerializers = EventEnvelopeSerializers(serializerCache)
    private val codecRegistry = EventCodecRegistry()
    private val missingCodecDiagnostics = ConcurrentHashMap.newKeySet<String>()

    private val topic by lazy { api.redissonReactive.getTopic(REDIS_CHANNEL, StringCodec.INSTANCE) }
    private val customRedisCodec by lazy {
        CustomEventPacketCodec.redisCodec(codecRegistry::codecForEventId)
    }
    private val customTopic by lazy {
        api.redissonReactive.getTopic(CustomEventPacketCodec.CHANNEL, customRedisCodec)
    }

    @Volatile
    private var topicListenerId: Int? = null

    @Volatile
    private var customTopicListenerId: Int? = null

    companion object {
        private val log = logger()
        private const val REDIS_CHANNEL = "surf-redis:events"

        private val EMPTY_INVOKERS = emptyArray<RedisEventInvoker>()

        private val INVOKER_FACTORY = InvokerFactory(
            /* templateClass = */ RedisEventInvokerTemplate::class.java,
            /* invokerInterface = */ RedisEventInvoker::class.java,
            /* lookup = */ RedisInvokerLookupProvider.LOOKUP
        )
    }

    override fun init(): Mono<Void> = Mono.zip(
        topic.addListener(String::class.java) { _, msg -> handleIncomingMessage(msg) },
        customTopic.addListener(CustomEventPacketCodec.DecodeResult::class.java) { _, result ->
            handleIncomingCustomMessage(result)
        }
    ).doOnNext { ids ->
        topicListenerId = ids.t1
        customTopicListenerId = ids.t2
    }.then()

    override fun close() {
        topicListenerId?.let { topic.removeListener(it).block() }
        customTopicListenerId?.let { customTopic.removeListener(it).block() }
        topicListenerId = null
        customTopicListenerId = null
        missingCodecDiagnostics.clear()
        registrationLock.write {
            eventHandlers.clear()
            eventTypeRegistry.clear()
            rebuildSnapshots()
        }
        codecRegistry.clear()
    }

    @Suppress("JavaMapForEach")
    private fun rebuildSnapshots() {
        require(registrationLock.isWriteLockedByCurrentThread) { "Must hold registrationLock write lock" }

        val handlers = Object2ObjectOpenHashMap<Class<out RedisEvent>, Array<RedisEventInvoker>>(eventHandlers.size)
        eventHandlers.forEach { type, invokers ->
            handlers[type] = invokers.toArray(EMPTY_INVOKERS)
        }

        handlerSnapshot = handlers
        typeSnapshot = Object2ObjectOpenHashMap(eventTypeRegistry)
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

        val eventClass = typeSnapshot[envelope.eventClass]

        if (eventClass == null) {
            log.atFine()
                .log("No registered event class for name: ${envelope.eventClass} - ignoring event.")
            return
        }

        val event = deserializeEvent(eventClass, envelope.eventData) ?: return
        dispatchEvent(eventClass, event)
    }

    override fun publish(event: RedisEvent): Deferred<Long> {
        check(api.isConnected()) { "Cannot publish a Redis event before RedisApi is connected" }
        RedisComponentProvider.injectOriginId(event)

        val customCodec = codecRegistry.codecForPublishing(event.javaClass)
        if (customCodec != null) {
            return customTopic
                .publish(CustomEventPacketCodec.outbound(event, customCodec))
                .onErrorResume(RedisCodecException::class.java) { failure ->
                    log.atWarning()
                        .withCause(failure)
                        .log(
                            "Unable to encode custom Redis event '%s' with codec '%s'",
                            customCodec.eventType.name,
                            customCodec.codec.codecId
                        )
                    Mono.just(0L)
                }
                .asDeferred()
        }

        val message = serializeEnvelope(event) ?: return CompletableDeferred(0L)

        return topic.publish(message).asDeferred()
    }

    override fun registerListener(listener: Any) {
        require(!api.isFrozen()) { "Cannot register listener after RedisApi has been frozen." }

        val methods = listener.javaClass.declaredMethods

        for (method in methods) {
            if (method.isAnnotationPresent(OnRedisEvent::class.java)) {
                val validParamCount = when {
                    HiddenInvokerUtil.isSuspendFunction(method) -> 2
                    else -> 1
                }

                if (method.parameterCount != validParamCount) {
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

                if (!INVOKER_FACTORY.canAccess(listener, method)) {
                    log.atSevere()
                        .withStackTrace(StackSize.MEDIUM)
                        .log(
                            "Method ${method.name} in ${listener.javaClass.name} is not accessible via privateLookupIn " +
                                    "— ensure the package '${listener.javaClass.packageName}' is opened to the surf-redis module. " +
                                    "Cannot register as event handler."
                        )
                    continue
                }

                @Suppress("UNCHECKED_CAST")
                firstParamType as Class<out RedisEvent>

                codecRegistry.registerDiscovered(firstParamType)

                val invoker = INVOKER_FACTORY.create(listener, method, firstParamType)

                registrationLock.write {
                    eventTypeRegistry[firstParamType.name] = firstParamType
                    eventHandlers.computeIfAbsent(firstParamType) { ObjectArrayList() }
                        .add(invoker)
                    rebuildSnapshots()
                }
            }
        }
    }

    override fun <E : RedisEvent> registerEventCodec(
        eventType: Class<E>,
        codec: RedisEventCodec<E>
    ) = codecRegistry.registerExplicit(eventType, codec)

    override fun registerEventType(eventType: Class<out RedisEvent>) {
        codecRegistry.registerDiscovered(eventType)
    }

    override fun freezeEventCodecs() {
        codecRegistry.freeze()
    }

    private fun handleIncomingCustomMessage(result: CustomEventPacketCodec.DecodeResult) {
        try {
            when (result) {
                is CustomEventPacketCodec.DecodeResult.MissingCodec -> {
                    if (!missingCodecDiagnostics.add("missing:${result.eventId}:${result.codecVersion}")) return
                    log.atWarning()
                        .withStackTrace(StackSize.SMALL)
                        .log(
                            "No codec is registered for custom Redis event ID '%s' version %s; ignoring this event type",
                            result.eventId,
                            result.codecVersion
                        )
                }

                is CustomEventPacketCodec.DecodeResult.VersionMismatch -> {
                    val registration = result.registration
                    if (!missingCodecDiagnostics.add(
                            "version:${registration.eventId}:${result.receivedVersion}"
                        )
                    ) return
                    log.atWarning()
                        .withStackTrace(StackSize.SMALL)
                        .log(
                            "Codec version mismatch for custom Redis event '%s': registered=%s, received=%s",
                            registration.eventId,
                            registration.version,
                            result.receivedVersion
                        )
                }

                is CustomEventPacketCodec.DecodeResult.Event -> {
                    RedisComponentProvider.injectEventMetadata(
                        result.event,
                        result.timestamp,
                        result.originId
                    )
                    dispatchEvent(result.registration.eventType, result.event)
                }

                is CustomEventPacketCodec.DecodeResult.Failure -> {
                    log.atWarning()
                        .withCause(result.exception)
                        .log(
                            "Unable to decode custom Redis event packet: %s",
                            result.exception.message
                        )
                }
            }
        } catch (e: Exception) {
            log.atWarning()
                .withCause(e)
                .log("Unable to decode custom Redis event packet: %s", e.message)
        }
    }

    private fun dispatchEvent(eventClass: Class<out RedisEvent>, event: RedisEvent) {
        val handlers = handlerSnapshot[eventClass] ?: return

        for (invoker in handlers) {
            api.redisListenerScope.launch {
                try {
                    invoker.invoke(event)
                } catch (e: Throwable) {
                    if (e is CancellationException) throw e
                    log.atSevere()
                        .withCause(e)
                        .log("Error handling event ${event.javaClass.simpleName}: ${e.message}")
                }
            }
        }
    }

    /**
     * Serializes the given event, wrapped in its wire envelope, to a JSON string.
     *
     * @return the serialized envelope, or `null` if no serializer is available
     */
    private fun serializeEnvelope(event: RedisEvent): String? {
        val serializer = envelopeSerializers.get(event.javaClass)

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
        eventData: JsonElement
    ): RedisEvent? {
        val serializer = serializerCache.get(eventClass)

        if (serializer == null) {
            log.atWarning()
                .log("No serializer found for event class ${eventClass.simpleName} — cannot deserialize.")
            return null
        }

        try {
            return api.json.decodeFromJsonElement(serializer, eventData)
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
        val eventData: JsonElement
    )
}
