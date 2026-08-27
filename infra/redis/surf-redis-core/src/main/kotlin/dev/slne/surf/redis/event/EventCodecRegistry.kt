package dev.slne.surf.redis.event

import dev.slne.surf.api.core.util.emptyObject2ObjectMap
import dev.slne.surf.api.core.util.freeze
import dev.slne.surf.redis.codec.RedisCodec
import it.unimi.dsi.fastutil.objects.Object2ObjectMap
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import java.nio.charset.StandardCharsets
import kotlin.reflect.full.companionObject

internal class EventCodecRegistry {
    typealias ByClass = Object2ObjectMap<Class<out RedisEvent>, EventCodecRegistration>
    typealias ById = Object2ObjectMap<String, EventCodecRegistration>

    private val registrationsByClass: ByClass = Object2ObjectOpenHashMap()
    private val registrationsById: ById = Object2ObjectOpenHashMap()

    @Volatile
    private var frozenByClass: ByClass = emptyObject2ObjectMap()

    @Volatile
    private var frozenById: ById = emptyObject2ObjectMap()

    @Volatile
    private var frozen = false

    private val discoveredCodecs = object : ClassValue<RedisEventCodec<out RedisEvent>?>() {
        override fun computeValue(type: Class<*>): RedisEventCodec<out RedisEvent>? {
            if (!RedisEvent::class.java.isAssignableFrom(type)) return null
            return discoverCompanionCodec(type)
        }
    }

    private val onDemandRegistrations = object : ClassValue<EventCodecRegistration?>() {
        override fun computeValue(type: Class<*>): EventCodecRegistration? {
            val codec = discoveredCodecs.get(type) ?: return null

            @Suppress("UNCHECKED_CAST")
            return registrationUnchecked(type as Class<out RedisEvent>, codec, explicit = false)
        }
    }

    @Synchronized
    fun <E : RedisEvent> registerExplicit(eventType: Class<E>, codec: RedisEventCodec<E>) {
        check(!frozen) { "Cannot register an event codec after RedisApi has been frozen" }

        val registration = registration(eventType, codec, explicit = true)
        val previous = registrationsByClass[eventType]
        if (previous?.explicit == true) {
            throw IllegalArgumentException("An explicit codec is already registered for event '${eventType.name}'")
        }

        val collision = registrationsById[registration.eventId]
        if (collision != null && collision.eventType != eventType) {
            throw eventIdCollision(registration, collision)
        }

        if (previous != null) registrationsById.remove(previous.eventId, previous)
        registrationsByClass[eventType] = registration
        registrationsById[registration.eventId] = registration
    }

    @Synchronized
    fun registerDiscovered(eventType: Class<out RedisEvent>) {
        check(!frozen) { "Cannot register an event type after RedisApi has been frozen" }

        if (registrationsByClass.containsKey(eventType)) return

        val codec = discoveredCodecs.get(eventType) ?: return
        val registration = registrationUnchecked(eventType, codec, explicit = false)

        val collision = registrationsById[registration.eventId]
        if (collision != null && collision.eventType != eventType) {
            throw eventIdCollision(registration, collision)
        }

        registrationsByClass[eventType] = registration
        registrationsById[registration.eventId] = registration
    }

    @Synchronized
    fun freeze() {
        check(!frozen) { "Event codec registry is already frozen" }
        frozenByClass = Object2ObjectOpenHashMap(registrationsByClass).freeze()
        frozenById = Object2ObjectOpenHashMap(registrationsById).freeze()
        frozen = true
    }

    /**
     * Resolves the codec registration for publishing events of the specified type.
     *
     * This method retrieves a previously registered or discovered codec for the given event type.
     * If no codec has been registered or discovered for the provided type, it returns `null`.
     * If a codec is found, but its associated event ID conflicts with one already registered for a
     * different event type, an exception is thrown to signal the collision.
     *
     * @param eventType The class type of the event for which the codec registration is requested.
     *                  Must extend [RedisEvent].
     * @return The [EventCodecRegistration] for the specified event type if a codec is found;
     *         otherwise, `null`.
     * @throws IllegalArgumentException If an event ID collision is detected during the lookup process.
     */
    fun codecForPublishing(eventType: Class<out RedisEvent>): EventCodecRegistration? {
        val registered = if (frozen) {
            frozenByClass[eventType]
        } else synchronized(this) {
            registrationsByClass[eventType]
        }

        if (registered != null) return registered

        val registration = onDemandRegistrations.get(eventType) ?: return null
        val collision = if (frozen) {
            frozenById[registration.eventId]
        } else synchronized(this) {
            registrationsById[registration.eventId]
        }

        if (collision != null && collision.eventType != eventType) {
            throw eventIdCollision(registration, collision)
        }
        return registration
    }

    /**
     * Retrieves the codec registration associated with the specified event ID.
     *
     * This method attempts to retrieve the appropriate [EventCodecRegistration] based on the given
     * event ID. If the registry is in a frozen state, it checks the frozen mappings; otherwise, it
     * performs a thread-safe lookup in the active registry.
     *
     * @param eventId The unique identifier of the event for which the codec is requested.
     * @return The [EventCodecRegistration] corresponding to the specified event ID if it exists;
     *         otherwise, `null`.
     */
    fun codecForEventId(eventId: String): EventCodecRegistration? {
        return if (frozen) {
            frozenById[eventId]
        } else synchronized(this) {
            registrationsById[eventId]
        }
    }

    @Synchronized
    fun clear() {
        for (eventType in registrationsByClass.keys) {
            discoveredCodecs.remove(eventType)
            onDemandRegistrations.remove(eventType)
        }
        registrationsByClass.clear()
        registrationsById.clear()
        frozenByClass = emptyObject2ObjectMap()
        frozenById = emptyObject2ObjectMap()
    }

    private fun <E : RedisEvent> registration(
        eventType: Class<E>,
        codec: RedisEventCodec<E>,
        explicit: Boolean
    ): EventCodecRegistration = registrationUnchecked(eventType, codec, explicit)

    private fun registrationUnchecked(
        eventType: Class<out RedisEvent>,
        codec: RedisEventCodec<out RedisEvent>,
        explicit: Boolean
    ): EventCodecRegistration {
        val eventId = codec.eventId
        require(eventId.isNotBlank()) { "Event codec ID for '${eventType.name}' must not be blank" }
        val encodedLength = eventId.toByteArray(StandardCharsets.UTF_8).size
        require(encodedLength <= MAX_EVENT_ID_BYTES) {
            "Event codec ID for '${eventType.name}' is $encodedLength bytes; maximum is $MAX_EVENT_ID_BYTES"
        }
        validateCodecIdentity(codec, eventType)

        @Suppress("UNCHECKED_CAST")
        return EventCodecRegistration(
            eventType,
            codec as RedisEventCodec<RedisEvent>,
            eventId,
            codec.version,
            explicit
        )
    }

    private fun validateCodecIdentity(codec: RedisCodec<*>, eventType: Class<out RedisEvent>) {
        require(codec.codecId.isNotBlank()) { "Codec ID for event '${eventType.name}' must not be blank" }
        require(codec.version > 0) { "Codec version for event '${eventType.name}' must be positive" }
    }

    private fun discoverCompanionCodec(type: Class<*>): RedisEventCodec<out RedisEvent>? {
        val companionClass = type.kotlin.companionObject ?: return null

        val companion = try {
            companionClass.objectInstance
        } catch (exception: Exception) {
            throw IllegalStateException(
                "Could not access companion object '${companionClass.qualifiedName}' " +
                        "of class '${type.name}'",
                exception
            )
        } ?: throw IllegalStateException(
            "Companion object '${companionClass.qualifiedName}' of class '${type.name}' " +
                    "has no accessible instance"
        )

        if (companion is RedisEventCodec<*>) return companion
        check(companion !is RedisCodec<*>) { "Companion object for event '${type.name}' implements RedisCodec but not RedisEventCodec" }

        val candidates = ObjectArrayList<RedisEventCodec<out RedisEvent>>(2)
        for (method in companion.javaClass.methods) {
            if (method.parameterCount != 0 || method.name !in CODEC_PROVIDER_METHODS ||
                !RedisEventCodec::class.java.isAssignableFrom(method.returnType)
            ) continue

            val value = method.invoke(companion) ?: continue
            @Suppress("UNCHECKED_CAST")
            candidates += value as RedisEventCodec<out RedisEvent>
        }

        val distinct = candidates.distinctBy { System.identityHashCode(it) }
        require(distinct.size <= 1) {
            "Companion object for event '${type.name}' provides multiple RedisEventCodec instances"
        }
        return distinct.firstOrNull()
    }

    private fun eventIdCollision(
        attempted: EventCodecRegistration,
        existing: EventCodecRegistration
    ) = IllegalArgumentException(
        "Event codec ID '${attempted.eventId}' for '${attempted.eventType.name}' is already used by '${existing.eventType.name}'"
    )

    companion object {
        const val MAX_EVENT_ID_BYTES = 512
        private val CODEC_PROVIDER_METHODS = setOf("getCodec", "getRedisEventCodec")
    }
}

/**
 * Represents the registration details of a codec for a specific [RedisEvent] type.
 *
 * This class associates a concrete [RedisEvent] type with its corresponding [RedisEventCodec]
 * implementation, uniquely identified by an event ID and version. It also tracks whether the
 * registration is explicitly defined and maintains an estimated packet size for efficient
 * transport of serialized events.
 *
 * @property eventType The class type of the [RedisEvent] associated with this registration.
 * @property codec The codec responsible for encoding and decoding the event.
 * @property eventId A stable identifier for the event, used for routing purposes.
 * @property version The version of the event type or codec implementation.
 * @property explicit Indicates if this registration was explicitly declared by the user.
 */
internal data class EventCodecRegistration(
    val eventType: Class<out RedisEvent>,
    val codec: RedisEventCodec<RedisEvent>,
    val eventId: String,
    val version: Int,
    val explicit: Boolean
) {
    @Volatile
    private var measuredPacketSizeEstimate: Int = 0

    val packetSizeEstimate: Int
        get() = measuredPacketSizeEstimate.takeIf { it > 0 }
            ?: CustomEventPacketCodec.DEFAULT_INITIAL_CAPACITY


    /**
     * Adjusts the estimated packet size based on the provided actual packet size.
     *
     * The method uses an exponentially weighted moving average (EWMA) algorithm to update the
     * `measuredPacketSizeEstimate` field, which is used to optimize the transport of serialized events.
     * The adjustment is capped to stay within a predefined minimum and maximum packet size range.
     *
     * @param actualSize The actual size of the packet to be recorded, expressed as an integer.
     */
    fun recordPacketSize(actualSize: Int) {
        val previous = measuredPacketSizeEstimate
        if (previous == 0) {
            measuredPacketSizeEstimate =
                actualSize.coerceIn(1, CustomEventPacketCodec.MAX_PACKET_SIZE)
            return
        }
        val delta = actualSize - previous
        val adjustment = when {
            delta > 0 -> maxOf(1, delta / PACKET_SIZE_EWMA_WEIGHT)
            delta < 0 -> minOf(-1, delta / PACKET_SIZE_EWMA_WEIGHT)
            else -> 0
        }
        measuredPacketSizeEstimate = (previous + adjustment)
            .coerceIn(1, CustomEventPacketCodec.MAX_PACKET_SIZE)
    }

    private companion object {
        /**
         * Weight factor used in the calculation of the exponentially weighted moving average (EWMA)
         * for packet size estimation. This constant determines the influence of the most recent
         * packet size measurement on the overall estimated packet size.
         *
         * A higher value of this weight prioritizes recent measurements more heavily, making
         * the estimate more sensitive to recent changes. Conversely, a lower value results in
         * a smoother estimate that is less impacted by recent fluctuations.
         */
        const val PACKET_SIZE_EWMA_WEIGHT = 4
    }
}
