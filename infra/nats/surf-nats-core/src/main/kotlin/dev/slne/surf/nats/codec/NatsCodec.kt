package dev.slne.surf.nats.codec

import dev.slne.surf.api.core.serializer.SurfSerializerModule
import dev.slne.surf.api.core.serializer.java.uuid.JavaUUIDSerializer
import dev.slne.surf.api.core.serializer.java.uuid.JavaUUIDStringSerializer
import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.WireFormat
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.overwriteWith

@OptIn(ExperimentalSerializationApi::class)
@InternalNatsApi
internal class NatsCodec(serializersModule: SerializersModule) {

    private val cborSerializersModule = SurfSerializerModule.all
        .overwriteWith(SerializersModule {
            contextual(JavaUUIDSerializer) // Ensure binary uuid is used
        })
        .overwriteWith(serializersModule)

    private val jsonSerializersModule = SurfSerializerModule.all
        .overwriteWith(SerializersModule {
            contextual(JavaUUIDStringSerializer) // Ensure string uuid is used
        })
        .overwriteWith(serializersModule)

    private val cbor = Cbor {
        ignoreUnknownKeys = true
        encodeDefaults = true
        this.serializersModule = cborSerializersModule
        alwaysUseByteString = true
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        namingStrategy = JsonNamingStrategy.SnakeCase
        this.serializersModule = jsonSerializersModule
    }

    fun <T> encode(serializer: SerializationStrategy<T>, value: T, format: WireFormat): ByteArray {
        return when (format) {
            WireFormat.CBOR -> cbor.encodeToByteArray(serializer, value)
            WireFormat.JSON -> json.encodeToString(serializer, value).encodeToByteArray()
        }
    }

    fun <T> encode(serializers: WireSerializers<T>, value: T, format: WireFormat): ByteArray {
        return encode(serializers[format], value, format)
    }

    fun <T> decode(
        deserializer: DeserializationStrategy<T>,
        bytes: ByteArray,
        contentType: String?
    ): T {
        return decode(deserializer, bytes, formatOf(contentType))
    }

    fun <T> decode(serializers: WireSerializers<T>, bytes: ByteArray, contentType: String?): T {
        val format = formatOf(contentType)
        return decode(serializers[format], bytes, format)
    }

    private fun <T> decode(deserializer: DeserializationStrategy<T>, bytes: ByteArray, format: WireFormat): T {
        return when (format) {
            WireFormat.CBOR -> cbor.decodeFromByteArray(deserializer, bytes)
            WireFormat.JSON -> json.decodeFromString(deserializer, bytes.decodeToString())
        }
    }

    private fun formatOf(contentType: String?): WireFormat {
        requireNotNull(contentType) { "Missing Content-Type header, expected one of: $SUPPORTED_CONTENT_TYPES" }
        return requireNotNull(WireFormat.fromContentType(contentType)) {
            "Unsupported content type '$contentType', expected one of: $SUPPORTED_CONTENT_TYPES"
        }
    }

    fun serializersModule(format: WireFormat): SerializersModule {
        return when (format) {
            WireFormat.CBOR -> cborSerializersModule
            WireFormat.JSON -> jsonSerializersModule
        }
    }

    fun jsonName(serialName: String): String {
        val strategy = json.configuration.namingStrategy ?: return serialName
        return strategy.serialNameForJson(NAME_DESCRIPTOR, 0, serialName)
    }

    private companion object {
        private val SUPPORTED_CONTENT_TYPES = WireFormat.entries.joinToString { it.contentType }
        private val NAME_DESCRIPTOR = buildClassSerialDescriptor("dev.slne.surf.nats.Name")
    }
}