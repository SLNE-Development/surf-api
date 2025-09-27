@file:OptIn(ExperimentalSerializationApi::class)

package dev.slne.surf.surfapi.core.api.serializer.java.ip.inetsocket

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import java.net.InetSocketAddress

typealias SerializableInetSocketAddress = @Serializable(with = InetSocketAddressSerializer::class) InetSocketAddress

object InetSocketAddressSerializer : KSerializer<InetSocketAddress> {
    override val descriptor = buildClassSerialDescriptor("surfapi.java.ip.InetSocketAddress") {
        element<String>("hostname")
        element<Int>("port")
    }

    override fun serialize(
        encoder: Encoder,
        value: InetSocketAddress,
    ) = encoder.encodeStructure(descriptor) {
        encodeStringElement(descriptor, 0, value.hostName)
        encodeIntElement(descriptor, 1, value.port)
    }

    override fun deserialize(decoder: Decoder) = decoder.decodeStructure(descriptor) {
        var hostname: String? = null
        var port: Int? = null

        if (decodeSequentially()) {
            hostname = decodeStringElement(descriptor, 0)
            port = decodeIntElement(descriptor, 1)
        } else while (true) {
            when (decodeElementIndex(descriptor)) {
                0 -> hostname = decodeStringElement(descriptor, 0)
                1 -> port = decodeIntElement(descriptor, 1)
                else -> break
            }
        }

        require(hostname != null) { "Hostname must not be null" }
        require(port != null) { "Port must not be null" }

        InetSocketAddress(hostname, port)
    }
}