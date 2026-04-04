package dev.slne.surf.api.paper.pdc.block

import org.bukkit.Chunk
import org.bukkit.block.Block
import org.bukkit.persistence.PersistentDataContainer

interface CustomBlockPersistentDataContainer : PersistentDataContainer {
    val chunk: Chunk
    fun copyTo(block: Block)
    fun clear()
}