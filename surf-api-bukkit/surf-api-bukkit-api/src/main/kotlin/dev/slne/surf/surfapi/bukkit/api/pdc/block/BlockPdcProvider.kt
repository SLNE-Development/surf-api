package dev.slne.surf.surfapi.bukkit.api.pdc.block

import dev.slne.surf.surfapi.core.api.util.requiredService
import org.bukkit.block.Block

interface BlockPdcProvider {
    fun getPdc(block: Block): CustomBlockPersistentDataContainer

    companion object {
        val instance = requiredService<BlockPdcProvider>()
    }
}

val blockPdcProvider: BlockPdcProvider get() = BlockPdcProvider.instance

fun Block.pdc() = blockPdcProvider.getPdc(this)