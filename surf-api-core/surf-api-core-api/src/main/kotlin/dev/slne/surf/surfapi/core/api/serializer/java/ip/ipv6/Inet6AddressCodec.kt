package dev.slne.surf.surfapi.core.api.serializer.java.ip.ipv6

import com.mojang.serialization.Codec
import java.net.Inet6Address

object Inet6AddressCodec {
    val CODEC: Codec<Inet6Address> =
        Codec.STRING.xmap({ Inet6Address.getByName(it) as Inet6Address }, { it.hostName })
}