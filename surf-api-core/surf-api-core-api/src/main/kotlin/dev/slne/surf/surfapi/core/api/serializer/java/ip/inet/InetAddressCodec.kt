package dev.slne.surf.surfapi.core.api.serializer.java.ip.inet

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import java.net.InetAddress
import java.net.UnknownHostException

object InetAddressCodec {
    val CODEC: Codec<InetAddress> = Codec.STRING
        .comapFlatMap({ hostName ->
            try {
                DataResult.success(InetAddress.getByName(hostName))
            } catch (e: UnknownHostException) {
                DataResult.error { "Unknown host: $hostName (${e.message ?: "no details"})" }
            } catch (e: SecurityException) {
                DataResult.error { "Security exception when resolving host: $hostName (${e.message ?: "no details"})" }
            }
        }, InetAddress::getHostName)
}