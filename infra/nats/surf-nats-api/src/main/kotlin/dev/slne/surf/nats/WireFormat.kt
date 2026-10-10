package dev.slne.surf.nats

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap

public enum class WireFormat(@InternalNatsApi public val contentType: String) {
    CBOR("application/cbor"),
    JSON("application/json");

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
            return byType[contentType.substringBefore(';').trim().lowercase()]
        }
    }
}