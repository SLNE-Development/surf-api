package dev.slne.surf.redis.codec

import dev.slne.surf.api.core.serializer.SurfSerializerModule
import dev.slne.surf.api.core.serializer.java.uuid.JavaUUIDStringSerializer
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.overwriteWith
import kotlinx.serialization.serializer
import java.nio.charset.StandardCharsets

/**
 * Encodes and decodes values as UTF-8 JSON using Kotlin serialization.
 *
 * The codec uses snake case property names, includes default values during encoding, and ignores
 * unknown properties during decoding. Additional serializers can be supplied through one of the
 * [of] factory methods.
 *
 * Each encoded value occupies the remaining Redis payload without an additional length prefix.
 *
 * @param T the non-null value type handled by this codec
 * @param serializersModule the serializers module used by the JSON format
 * @param serializer the serializer used to encode and decode values of type [T]
 */
class JsonKotlinCodec<T : Any> private constructor(
    serializersModule: SerializersModule,
    private val serializer: KSerializer<T>,
) : AbstractCodec<T>() {

    /**
     * The configured JSON format used to encode and decode values.
     *
     * Unknown properties are ignored during decoding, default values are encoded, and property
     * names are converted to snake case.
     */
    @OptIn(ExperimentalSerializationApi::class)
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        namingStrategy = JsonNamingStrategy.SnakeCase
        this.serializersModule = serializersModule
    }

    override fun write(buf: ByteBuf, value: T) {
        val encoded = json.encodeToString(serializer, value)
        val encodedLength = ByteBufUtil.utf8Bytes(encoded)

        ByteBufUtil.reserveAndWriteUtf8(
            buf,
            encoded,
            encodedLength
        )
    }

    override fun read(buf: ByteBuf): T {
        val encoded = buf.readCharSequence(
            buf.readableBytes(),
            StandardCharsets.UTF_8
        ).toString()

        return json.decodeFromString(serializer, encoded)
    }


    companion object {

        /**
         * The default serializers module used by JSON codecs.
         *
         * It contains all serializers provided by [SurfSerializerModule] and overrides the default
         * UUID representation with [JavaUUIDStringSerializer].
         */
        @PublishedApi
        internal val DEFAULT_SERIALIZERS: SerializersModule =
            SurfSerializerModule.all.overwriteWith(
                SerializersModule {
                    contextual(JavaUUIDStringSerializer)
                }
            )

        /**
         * Creates a JSON codec for [T] using the serializer resolved from the combined serializers
         * module.
         *
         * Entries in [additionalSerializers] override matching entries from [DEFAULT_SERIALIZERS].
         *
         * @param T the non-null value type handled by the codec
         * @param additionalSerializers serializers to add to or override in the default module
         * @return a JSON codec for values of type [T]
         * @throws kotlinx.serialization.SerializationException if no serializer for [T] can be resolved
         */
        inline fun <reified T : Any> of(
            additionalSerializers: SerializersModule = EmptySerializersModule(),
        ): JsonKotlinCodec<T> {
            val serializersModule = DEFAULT_SERIALIZERS.overwriteWith(additionalSerializers)

            val serializer = serializersModule.serializer<T>()
            return of(serializer, serializersModule)
        }

        /**
         * Creates a JSON codec using an explicitly provided [serializer].
         *
         * Entries in [additionalSerializers] override matching entries from [DEFAULT_SERIALIZERS].
         *
         * @param T the non-null value type handled by the codec
         * @param serializer the serializer used to encode and decode values
         * @param additionalSerializers serializers to add to or override in the default module
         * @return a JSON codec using the supplied serializer
         */
        fun <T : Any> of(
            serializer: KSerializer<T>,
            additionalSerializers: SerializersModule = EmptySerializersModule(),
        ): JsonKotlinCodec<T> {
            val serializersModule = DEFAULT_SERIALIZERS.overwriteWith(additionalSerializers)
            return JsonKotlinCodec(serializersModule, serializer)
        }
    }
}