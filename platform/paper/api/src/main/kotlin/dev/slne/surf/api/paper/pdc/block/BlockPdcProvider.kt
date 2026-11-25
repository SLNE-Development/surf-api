package dev.slne.surf.api.paper.pdc.block

import dev.slne.surf.api.shared.api.util.InternalSurfApi
import dev.slne.surf.api.spring.SurfApiBean
import dev.slne.surf.api.spring.surfBean
import org.bukkit.block.Block

@SurfApiBean
interface BlockPdcProvider {
    fun getPdc(block: Block): CustomBlockPersistentDataContainer

    @InternalSurfApi
    companion object {
        internal val instance by surfBean<BlockPdcProvider>()
    }
}

fun Block.pdc(): CustomBlockPersistentDataContainer = BlockPdcProvider.instance.getPdc(this)
