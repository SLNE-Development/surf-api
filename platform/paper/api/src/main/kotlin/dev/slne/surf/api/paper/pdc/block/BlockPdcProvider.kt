@file:OptIn(InternalSurfApi::class)

package dev.slne.surf.api.paper.pdc.block

import dev.slne.surf.api.di.surfBean
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import org.bukkit.block.Block

interface BlockPdcProvider {
    fun getPdc(block: Block): CustomBlockPersistentDataContainer

    companion object {
        val INSTANCE get() = provider
    }
}

private val provider by surfBean<BlockPdcProvider>()

fun Block.pdc() = provider.getPdc(this)
