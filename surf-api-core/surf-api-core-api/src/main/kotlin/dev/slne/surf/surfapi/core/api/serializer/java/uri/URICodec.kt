package dev.slne.surf.surfapi.core.api.serializer.java.uri

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import java.net.URI
import java.net.URISyntaxException

object URICodec {
    val CODEC: Codec<URI> = Codec.STRING.comapFlatMap({ string ->
        try {
            DataResult.success(URI(string))
        } catch (e: URISyntaxException) {
            DataResult.error { "Uri syntax error: ${e.message}" }
        }
    }, URI::toString).stable()
}