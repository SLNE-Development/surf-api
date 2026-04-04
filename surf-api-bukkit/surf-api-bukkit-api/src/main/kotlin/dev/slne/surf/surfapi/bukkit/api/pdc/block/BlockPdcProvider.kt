package dev.slne.surf.surfapi.bukkit.api.pdc.block

import dev.slne.surf.surfapi.core.api.util.requiredService
import org.bukkit.block.Block

interface BlockPdcProvider {
    fun getPdc(block: Block): CustomBlockPersistentDataContainer

    companion object : BlockPdcProvider by provider {
        val INSTANCE get() = provider
    }
}

private val provider = requiredService<BlockPdcProvider>()

fun Block.pdc() = provider.getPdc(this)