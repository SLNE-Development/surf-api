package dev.slne.surf.surfapi.core.api.serializer.java.url

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import dev.slne.surf.surfapi.core.api.serializer.java.uri.URICodec
import java.net.MalformedURLException
import java.net.URISyntaxException
import java.net.URL

object URLCodec {
    val CODEC: Codec<URL> = URICodec.CODEC.flatXmap({ uri ->
        try {
            DataResult.success(uri.toURL())
        } catch (e: MalformedURLException) {
            DataResult.error { "Malformed URL: ${e.message}" }
        } catch (e: IllegalArgumentException) {
            DataResult.error { "URL is not absolute: ${e.message}" }
        }
    }, { url ->
        try {
            DataResult.success(url.toURI())
        } catch (e: URISyntaxException) {
            DataResult.error { "URI syntax error: ${e.message}" }
        }
    })
}