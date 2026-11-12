package dev.slne.surf.redis.event

import dev.slne.surf.redis.util.KotlinSerializerCache
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.modules.SerializersModule

/**
 * Provides cached serializers for encoding Redis events directly into their JSON envelope.
 *
 * Each serializer is created once per event class and writes the envelope and event payload in a
 * single serialization pass, avoiding the intermediate [kotlinx.serialization.json.JsonElement]
 * tree that would otherwise be created for the event payload.
 *
 * The serializers are encode-only because incoming envelopes are decoded separately by the event
 * bus.
 */
class EventEnvelopeSerializers(
    private val eventSerializers: KotlinSerializerCache<RedisEvent>
) {
    constructor(module: SerializersModule) : this(KotlinSerializerCache<RedisEvent>(module))

    private val envelopeSerializers = object : ClassValue<KSerializer<RedisEvent>?>() {
        override fun computeValue(type: Class<*>): KSerializer<RedisEvent>? {
            val eventSerializer = eventSerializers.get(type) ?: return null
            return EventEnvelopeSerializer(type.name, eventSerializer)
        }
    }

    /**
     * Returns the cached envelope serializer for [eventType].
     *
     * @return the serializer, or `null` if no Kotlin serializer is available for the event type
     */
    fun get(eventType: Class<out RedisEvent>): KSerializer<RedisEvent>? {
        return envelopeSerializers.get(eventType)
    }
}

/**
 * Encodes a single event type as an event envelope without materializing its payload first.
 *
 * The descriptor intentionally models the envelope as a class so configured serialization
 * behavior such as JSON naming strategies is applied to its fields normally.
 *
 * Deserialization is not supported.
 */
private class EventEnvelopeSerializer(
    private val eventClass: String,
    private val eventSerializer: KSerializer<RedisEvent>
) : KSerializer<RedisEvent> {
    override val descriptor = buildClassSerialDescriptor("dev.slne.surf.redis.event.EventEnvelope") {
        element<String>("eventClass")
        element("eventData", eventSerializer.descriptor)
    }

    override fun serialize(encoder: Encoder, value: RedisEvent) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, eventClass)
            encodeSerializableElement(descriptor, 1, eventSerializer, value)
        }
    }

    override fun deserialize(decoder: Decoder): RedisEvent =
        throw UnsupportedOperationException(
            "EventEnvelopeSerializer is encode-only; inbound envelopes are decoded by the event bus"
        )
}
