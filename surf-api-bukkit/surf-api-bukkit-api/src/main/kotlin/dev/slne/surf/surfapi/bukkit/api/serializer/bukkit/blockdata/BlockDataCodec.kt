package dev.slne.surf.surfapi.bukkit.api.serializer.bukkit.blockdata

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import dev.slne.surf.surfapi.bukkit.api.extensions.server
import org.bukkit.block.data.BlockData

object BlockDataCodec {
    val CODEC: Codec<BlockData> = Codec.STRING.comapFlatMap({ data ->
        try {
            DataResult.success(server.createBlockData(data))
        } catch (e: IllegalArgumentException) {
            DataResult.error { "Invalid block data: $data (${e.message})" }
        }
    }, BlockData::getAsString)
}