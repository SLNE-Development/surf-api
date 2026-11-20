package dev.slne.surf.api.paper.pdc.block

import dev.slne.surf.api.spring.SurfApiBean
import dev.slne.surf.api.spring.surfBean
import org.bukkit.block.Block

@SurfApiBean
interface BlockPdcProvider {
    fun getPdc(block: Block): CustomBlockPersistentDataContainer
}

private val provider by surfBean<BlockPdcProvider>()

fun Block.pdc() = provider.getPdc(this)
