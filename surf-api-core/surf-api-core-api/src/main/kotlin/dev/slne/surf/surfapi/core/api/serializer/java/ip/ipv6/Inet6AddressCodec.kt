package dev.slne.surf.surfapi.core.api.serializer.java.ip.ipv6

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import dev.slne.surf.surfapi.core.api.serializer.java.ip.inet.InetAddressCodec
import java.net.Inet6Address
import java.util.function.Function

object Inet6AddressCodec {
    val CODEC: Codec<Inet6Address> = InetAddressCodec.CODEC.comapFlatMap({ address ->
        if (address is Inet6Address) {
            DataResult.success(address)
        } else {
            DataResult.error { "Not an IPv6 address: $address" }
        }
    }, Function.identity())
}