package dev.slne.surf.surfapi.bukkit.server.pdc.block

import com.google.auto.service.AutoService
import dev.slne.surf.surfapi.bukkit.api.pdc.block.BlockPdcProvider
import dev.slne.surf.surfapi.bukkit.api.pdc.block.CustomBlockPersistentDataContainer
import org.bukkit.block.Block

@AutoService(BlockPdcProvider::class)
class BlockPdcProviderImpl : BlockPdcProvider {
    override fun getPdc(block: Block): CustomBlockPersistentDataContainer {
        return CustomBlockData(block)
    }
}