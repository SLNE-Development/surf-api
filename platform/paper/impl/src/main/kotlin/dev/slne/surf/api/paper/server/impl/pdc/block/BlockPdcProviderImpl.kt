package dev.slne.surf.api.paper.server.impl.pdc.block

import com.google.auto.service.AutoService
import dev.slne.surf.api.paper.pdc.block.BlockPdcProvider
import dev.slne.surf.api.paper.pdc.block.CustomBlockPersistentDataContainer
import dev.slne.surf.api.paper.region.TickThreadGuard
import org.bukkit.block.Block

@AutoService(BlockPdcProvider::class)
class BlockPdcProviderImpl : BlockPdcProvider {
    override fun getPdc(block: Block): CustomBlockPersistentDataContainer {
        TickThreadGuard.ensureTickThread(block.world, block.location, "Cannot get PDC of block off tick thread!")
        return CustomBlockData(block)
    }
}