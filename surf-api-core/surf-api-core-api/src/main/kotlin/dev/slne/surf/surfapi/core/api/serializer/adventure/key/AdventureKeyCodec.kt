package dev.slne.surf.surfapi.core.api.serializer.adventure.key

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import net.kyori.adventure.key.InvalidKeyException
import net.kyori.adventure.key.Key

object AdventureKeyCodec {
    val CODEC: Codec<Key> = Codec.STRING.comapFlatMap({ string ->
        try {
            DataResult.success(Key.key(string))
        } catch (e: InvalidKeyException) {
            DataResult.error { e.message ?: "Invalid key: $string" }
        }
    }, Key::asString)
}