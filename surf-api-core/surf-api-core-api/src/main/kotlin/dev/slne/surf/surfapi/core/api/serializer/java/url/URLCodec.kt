package dev.slne.surf.surfapi.core.api.serializer.java.url

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import java.net.URI
import java.net.URISyntaxException
import java.net.URL

object URLCodec {
    val CODEC: Codec<URL> = Codec.STRING.comapFlatMap({ string ->
        try {
            DataResult.success(URI(string).toURL())
        } catch (e: URISyntaxException) {
            DataResult.error { "Url syntax error: ${e.message}" }
        }
    }, { url ->
        url.toURI().toString()
    }).stable()
}