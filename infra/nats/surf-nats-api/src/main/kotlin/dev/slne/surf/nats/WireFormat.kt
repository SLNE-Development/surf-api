package dev.slne.surf.nats

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap

enum class WireFormat(@InternalNatsApi val contentType: String) {
    CBOR("application/cbor"),
    JSON("application/json");

    companion object {
        private val byType = Object2ObjectOpenHashMap<String, WireFormat>()

        init {
            for (format in entries) {
                byType[format.contentType] = format
            }
            byType.trim()
        }

        @InternalNatsApi
        fun fromContentType(contentType: String): WireFormat? {
            return byType[contentType.substringBefore(';').trim().lowercase()]
        }
    }
}