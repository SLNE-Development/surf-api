/*
 * This file is part of surf-api.
 *
 * surf-api is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * surf-api is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Portions of this file are derived from “CustomBlockData”
 * by mfnalex – https://github.com/mfnalex/CustomBlockData –
 * licensed under the GNU General Public License v3.0 only.
 *
 * Copyright (c) 2025 twisti-dev and contributors
 */
package dev.slne.surf.surfapi.bukkit.server.impl.pdc.block

import com.github.shynixn.mccoroutine.folia.globalRegionDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import com.github.shynixn.mccoroutine.folia.ticks
import dev.slne.surf.surfapi.bukkit.server.plugin
import dev.slne.surf.surfapi.core.api.util.mutableObjectSetOf
import io.papermc.paper.math.BlockPosition
import kotlinx.coroutines.delay
import org.bukkit.block.Block
import java.util.*


object BlockPdcManager {
    private val dirtyBlocks = mutableObjectSetOf<Pair<UUID, BlockPosition>>()

    fun isDirty(block: Block): Boolean {
        val entry = getEntry(block)
        return dirtyBlocks.contains(entry)
    }

    fun markDirty(block: CustomBlockData) {
        val entry = getEntry(block.block)
        dirtyBlocks.add(entry)

        plugin.launch(plugin.globalRegionDispatcher) {
            delay(1.ticks)
            dirtyBlocks.remove(entry)
        }
    }

    private fun getEntry(block: Block): Pair<UUID, BlockPosition> {
        val uuid = block.world.uid
        val position = block.location.toBlock()
        return uuid to position
    }

    fun hasCustomData(block: Block): Boolean {
        return block.chunk.persistentDataContainer.has(CustomBlockData.getKey(block))
    }
}