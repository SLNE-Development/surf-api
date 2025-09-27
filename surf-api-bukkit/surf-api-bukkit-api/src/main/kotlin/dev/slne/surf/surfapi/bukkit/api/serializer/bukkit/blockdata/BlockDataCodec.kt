package dev.slne.surf.surfapi.bukkit.api.serializer.bukkit.blockdata

import com.mojang.serialization.Codec
import org.bukkit.Bukkit
import org.bukkit.block.data.BlockData

object BlockDataCodec {
    val CODEC: Codec<BlockData> = Codec.STRING.xmap(
        { str -> Bukkit.createBlockData(str) },
        { blockData -> blockData.asString }
    )
}