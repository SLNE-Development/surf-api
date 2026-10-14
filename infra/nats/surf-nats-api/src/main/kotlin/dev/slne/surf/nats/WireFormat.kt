package dev.slne.surf.nats

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap

/**
 * Serialization formats available for outgoing messages, selected by [NatsFormat].
 */
public enum class WireFormat(@InternalNatsApi public val contentType: String) {
    /**
     * Compact binary format used by default.
     */
    CBOR("application/cbor"),

    /**
     * Text format for interoperability and debugging.
     */
    JSON("application/json");

    /**
     * Provides format lookup for library modules.
     */
    public companion object {
        private val byType = Object2ObjectOpenHashMap<String, WireFormat>()

        init {
            for (format in entries) {
                byType[format.contentType] = format
            }
            byType.trim()
        }

        @InternalNatsApi
        public fun fromContentType(contentType: String): WireFormat? {
            return byType[contentType] ?: byType[contentType.substringBefore(';').trim().lowercase()]
        }
    }
}