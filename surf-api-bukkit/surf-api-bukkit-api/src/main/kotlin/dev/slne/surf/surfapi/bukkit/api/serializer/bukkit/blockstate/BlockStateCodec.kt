package dev.slne.surf.surfapi.bukkit.api.serializer.bukkit.blockstate

import com.mojang.serialization.Codec
import org.bukkit.Bukkit
import org.bukkit.block.BlockState

object BlockStateCodec {
    val CODEC: Codec<BlockState> = Codec.STRING.xmap(
        { str -> Bukkit.createBlockData(str).createBlockState() },
        { blockState -> blockState.blockData.asString }
    )
}