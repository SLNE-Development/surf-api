@file:OptIn(ExperimentalSerializationApi::class)

package dev.slne.surf.surfapi.core.api.serializer.java.ip.ipv4

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import java.net.Inet4Address

typealias SerializableInet4Address = @Serializable(with = Inet4AddressSerializer::class) Inet4Address

@Serializer(forClass = Inet4Address::class)
object Inet4AddressSerializer : KSerializer<Inet4Address>