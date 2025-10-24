package dev.slne.surf.surfapi.core.api.serializer.java.ip.inetsocket

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.slne.surf.surfapi.core.api.serializer.ranged
import java.net.InetSocketAddress

object InetSocketAddressCodec {
    val CODEC: Codec<InetSocketAddress> = RecordCodecBuilder.create { instance ->
        instance.group(
            Codec.STRING.fieldOf("address").forGetter(InetSocketAddress::getHostName),
            Codec.INT
                .ranged(0, 0xFFFF)
                .fieldOf("port")
                .forGetter(InetSocketAddress::getPort),
        ).apply(instance, ::InetSocketAddress)
    }
}