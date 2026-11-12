package dev.slne.surf.api.paper.server.impl.pdc.block

import dev.slne.surf.api.di.Service
import dev.slne.surf.api.paper.pdc.block.BlockPdcProvider
import dev.slne.surf.api.paper.pdc.block.CustomBlockPersistentDataContainer
import dev.slne.surf.api.paper.region.TickThreadGuard
import org.bukkit.block.Block

@Service
class BlockPdcProviderImpl : BlockPdcProvider {
    override fun getPdc(block: Block): CustomBlockPersistentDataContainer {
        TickThreadGuard.INSTANCE.ensureTickThread(block.world, block.location, "Cannot get PDC of block off tick thread!")
        return CustomBlockData(block)
    }
}
