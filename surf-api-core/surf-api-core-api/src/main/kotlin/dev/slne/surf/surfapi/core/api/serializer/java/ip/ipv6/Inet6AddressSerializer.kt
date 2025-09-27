@file:OptIn(ExperimentalSerializationApi::class)

package dev.slne.surf.surfapi.core.api.serializer.java.ip.ipv6

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import java.net.Inet6Address

typealias SerializableInet6Address = @Serializable(with = Inet6AddressSerializer::class) Inet6Address

@Serializer(forClass = Inet6Address::class)
object Inet6AddressSerializer : KSerializer<Inet6Address>