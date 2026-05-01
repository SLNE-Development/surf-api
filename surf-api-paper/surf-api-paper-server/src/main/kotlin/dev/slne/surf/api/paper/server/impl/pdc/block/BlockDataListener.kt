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
package dev.slne.surf.api.paper.server.impl.pdc.block

import dev.slne.surf.api.paper.pdc.block.CustomBlockPersistentDataContainer
import dev.slne.surf.api.paper.pdc.block.pdc
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
import org.bukkit.block.Block
import org.bukkit.block.PistonMoveReaction
import org.bukkit.block.data.type.Fire
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.*
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.world.StructureGrowEvent

object BlockDataListener : Listener {
    private fun remove(event: BlockEvent) {
        removeFromBlock(event.block)
    }

    private fun removeFromBlock(block: Block) {
        if (BlockPdcManager.hasCustomData(block)) {
            block.pdc().clear()
        }
    }

    private fun removeFromBlockList(blocks: List<Block>) {
        for (block in blocks) {
            removeFromBlock(block)
        }
    }

    @EventHandler(
        priority = EventPriority.MONITOR,
        ignoreCancelled = true
    )
    fun onBlockBreak(event: BlockBreakEvent) {
        if (event.isCancelled) return
        remove(event)
    }

    @EventHandler(
        priority = EventPriority.MONITOR,
        ignoreCancelled = true
    )
    fun onBlockPlace(event: BlockPlaceEvent) {
        if (event.isCancelled) return
        if (!BlockPdcManager.isDirty(event.block)) {
            remove(event)
        }
    }

    @EventHandler(
        priority = EventPriority.MONITOR,
        ignoreCancelled = true
    )
    fun onEntityChangeBlock(event: EntityChangeBlockEvent) {
        if (event.isCancelled) return
        if (event.to != event.block.type) {
            removeFromBlock(event.block)
        }
    }

    @EventHandler(
        priority = EventPriority.MONITOR,
        ignoreCancelled = true
    )
    fun onBlockExplode(event: BlockExplodeEvent) {
        if (event.isCancelled) return
        removeFromBlockList(event.blockList())
    }

    @EventHandler(
        priority = EventPriority.MONITOR,
        ignoreCancelled = true
    )
    fun onEntityExplode(event: EntityExplodeEvent) {
        if (event.isCancelled) return
        removeFromBlockList(event.blockList())
    }

    @EventHandler(
        priority = EventPriority.MONITOR,
        ignoreCancelled = true
    )
    fun onBlockBurn(event: BlockBurnEvent) {
        if (event.isCancelled) return
        remove(event)
    }

    @EventHandler(
        priority = EventPriority.MONITOR,
        ignoreCancelled = true
    )
    fun onBlockPistonExtend(event: BlockPistonExtendEvent) {
        if (event.isCancelled) return
        handlePiston(event.blocks, event)
    }

    @EventHandler(
        priority = EventPriority.MONITOR,
        ignoreCancelled = true
    )
    fun onBlockPistonRetract(event: BlockPistonRetractEvent) {
        if (event.isCancelled) return
        handlePiston(event.blocks, event)
    }

    @EventHandler(
        priority = EventPriority.MONITOR,
        ignoreCancelled = true
    )
    fun onBlockFade(event: BlockFadeEvent) {
        if (event.isCancelled) return
        if (event.block.blockData is Fire) return
        if (event.newState.type != event.block.type) {
            remove(event)
        }
    }

    @EventHandler(
        priority = EventPriority.MONITOR,
        ignoreCancelled = true
    )
    fun onStructureGrow(event: StructureGrowEvent) {
        if (event.isCancelled) return
        removeFromBlockList(event.blocks.map { it.block })
    }

    @EventHandler(
        priority = EventPriority.MONITOR,
        ignoreCancelled = true
    )
    fun onBlockFertilize(event: BlockFertilizeEvent) {
        if (event.isCancelled) return
        removeFromBlockList(event.blocks.map { it.block })
    }

    private fun handlePiston(blocks: List<Block>, event: BlockPistonEvent) {
        val map = Object2ObjectLinkedOpenHashMap<Block, CustomBlockPersistentDataContainer>()
        val direction = event.direction
        for (block in blocks) {
            if (!BlockPdcManager.hasCustomData(block)) continue
            val pdc = block.pdc()
            if (pdc.isEmpty) continue
            val reaction = block.pistonMoveReaction
            if (reaction == PistonMoveReaction.BREAK) {
                removeFromBlock(block)
                continue
            }
            val destination = block.getRelative(direction)
            map[destination] = pdc
        }
        for ((block, pdc) in map.reversed()) {
            pdc.copyTo(block)
            pdc.clear()
        }
    }
}