@file:OptIn(ExperimentalSerializationApi::class)

package dev.slne.surf.surfapi.core.api.serializer.java.ip.inet

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import java.net.InetAddress

typealias SerializableInetAddress = @Serializable(with = InetAddressSerializer::class) InetAddress

@Serializer(forClass = InetAddress::class)
object InetAddressSerializer : KSerializer<InetAddress>