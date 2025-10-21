package dev.slne.surf.surfapi.core.api.serializer.java.ip.ipv4

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import dev.slne.surf.surfapi.core.api.serializer.java.ip.inet.InetAddressCodec
import java.net.Inet4Address
import java.util.function.Function

object Inet4AddressCodec {
    val CODEC: Codec<Inet4Address> = InetAddressCodec.CODEC.comapFlatMap({ address ->
        if (address is Inet4Address) {
            DataResult.success(address)
        } else {
            DataResult.error { "Not an IPv4 address: $address" }
        }
    }, Function.identity())
}