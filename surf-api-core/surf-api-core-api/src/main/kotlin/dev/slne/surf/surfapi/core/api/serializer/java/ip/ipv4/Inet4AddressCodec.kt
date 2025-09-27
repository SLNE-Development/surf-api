package dev.slne.surf.surfapi.core.api.serializer.java.ip.ipv4

import com.mojang.serialization.Codec
import java.net.Inet4Address

object Inet4AddressCodec {
    val CODEC: Codec<Inet4Address> =
        Codec.STRING.xmap({ Inet4Address.getByName(it) as Inet4Address }, { it.hostName })
}