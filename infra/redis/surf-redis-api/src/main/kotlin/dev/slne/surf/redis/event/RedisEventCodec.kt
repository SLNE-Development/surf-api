package dev.slne.surf.redis.event

import dev.slne.surf.redis.codec.RedisCodec

/**
 * Binary or textual codec for a concrete [RedisEvent] type.
 *
 * The default [eventId] is the event class name when this codec is implemented by the event's
 * companion object. Explicitly registered codecs should normally override it. Event IDs must be
 * deterministic across JVM processes and must never be assigned from registration order.
 *
 * A codec declared directly by a Kotlin companion object is discovered automatically when the
 * event type is registered by a listener:
 *
 * ```
 * class PlayerJoined(val playerName: String) : RedisEvent() {
 *     companion object : RedisEventCodec<PlayerJoined> {
 *         override fun encode(buffer: ByteBuf, value: PlayerJoined) {
 *             buffer.writeString(value.playerName)
 *         }
 *
 *         override fun decode(buffer: ByteBuf) = PlayerJoined(buffer.readString())
 *     }
 * }
 * ```
 *
 * Custom-coded events use a separate binary Redis channel and never appear on the JSON
 * event channel. Implementations must follow the thread-safety and buffer-ownership rules in
 * [RedisCodec]. Event timestamp and origin metadata are carried by the event envelope and do not
 * need to be encoded by the codec.
 */
interface RedisEventCodec<T : RedisEvent> : RedisCodec<T> {
    /**
     * Stable event identifier used to route binary event packets.
     */
    val eventId: String
        get() = javaClass.enclosingClass?.name
            ?: error(
                "RedisEventCodec.eventId must be overridden when the codec is not an event companion object"
            )
}
