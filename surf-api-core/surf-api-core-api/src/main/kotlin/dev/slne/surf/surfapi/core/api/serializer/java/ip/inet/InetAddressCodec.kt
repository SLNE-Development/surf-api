package dev.slne.surf.surfapi.core.api.serializer.java.ip.inet

import com.mojang.serialization.Codec
import java.net.InetAddress

object InetAddressCodec {
    val CODEC: Codec<InetAddress> =
        Codec.STRING.xmap({ InetAddress.getByName(it) }, { it.hostName })
}