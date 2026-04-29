package dev.slne.surf.api.core.serializer.java.crypt.publickey

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.*
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec

typealias SerializablePublicKey = @Serializable(with = PublicKeySerializer::class) PublicKey

object PublicKeySerializer : KSerializer<PublicKey> {
    // KeyFactory is not thread-safe, so we cache them per thread
    private val keyFactoryCache: ThreadLocal<Object2ObjectOpenHashMap<String, KeyFactory>> =
        ThreadLocal.withInitial { Object2ObjectOpenHashMap(4) }

    override val descriptor = buildClassSerialDescriptor("surf.api.java.crypt.PublicKey") {
        element<String>("format")
        element<ByteArray>("bytes")
    }

    override fun serialize(encoder: Encoder, value: PublicKey) {
        val format = value.format
        require(format == "X.509") {
            "PublicKey must be in X.509 format, but was '$format' (algorithm=${value.algorithm})"
        }

        val encoded = value.encoded
            ?: throw IllegalArgumentException("PublicKey of algorithm '${value.algorithm}' does not support encoding")

        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, format)
            encodeSerializableElement(descriptor, 1, ByteArraySerializer(), encoded)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): PublicKey = decoder.decodeStructure(descriptor) {
        var format: String? = null
        var encoded: ByteArray? = null

        if (decodeSequentially()) {
            format = decodeStringElement(descriptor, 0)
            encoded = decodeSerializableElement(descriptor, 1, ByteArraySerializer())
        } else while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                0 -> format = decodeStringElement(descriptor, 0)
                1 -> encoded = decodeSerializableElement(descriptor, 1, ByteArraySerializer())
                CompositeDecoder.DECODE_DONE -> break
                else -> error("Unexpected index: $index")
            }
        }

        requireNotNull(format) { "Missing 'format'" }
        requireNotNull(encoded) { "Missing 'encoded'" }

        val keyFactory = keyFactoryFor(format)
        val keySpec = X509EncodedKeySpec(encoded)
        keyFactory.generatePublic(keySpec)
    }

    private fun keyFactoryFor(algorithm: String): KeyFactory {
        val cache = keyFactoryCache.get()
        return cache.computeIfAbsent(algorithm) { KeyFactory.getInstance(algorithm) }
    }
}