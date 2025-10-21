package dev.slne.surf.surfapi.core.api.serializer.adventure.component

import com.google.gson.JsonParseException
import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer

object AdventureComponentCodec {
    val CODEC: Codec<Component> = Codec.STRING
        .flatXmap(::read, ::write)
        .stable()

    private fun read(string: String): DataResult<Component> = try {
        DataResult.success(GsonComponentSerializer.gson().deserialize(string))
    } catch (e: JsonParseException) {
        DataResult.error { "Failed to parse component: ${e.message}" }
    }

    private fun write(component: Component): DataResult<String> = try {
        DataResult.success(GsonComponentSerializer.gson().serialize(component))
    } catch (e: JsonParseException) {
        DataResult.error { "Failed to serialize component: ${e.message}" }
    }
}