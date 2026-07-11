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
@file:Suppress("UnstableApiUsage")

package dev.slne.surf.api.paper.server.impl.pdc.block

import com.github.shynixn.mccoroutine.folia.globalRegionDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import com.github.shynixn.mccoroutine.folia.ticksDuration
import dev.slne.surf.api.paper.server.plugin
import kotlinx.coroutines.delay
import org.bukkit.block.Block
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

object BlockPdcManager {
    private val dirtyBlocks = ConcurrentHashMap.newKeySet<DirtyBlockKey>()
    private val cleanupScheduled = AtomicBoolean()

    fun isDirty(block: Block): Boolean {
        val entry = getEntry(block)
        return dirtyBlocks.contains(entry)
    }

    fun markDirty(block: CustomBlockData) {
        val entry = getEntry(block.block)
        dirtyBlocks.add(entry)
        scheduleCleanup()
    }

    private fun scheduleCleanup() {
        if (!cleanupScheduled.compareAndSet(false, true)) return
        plugin.launch(plugin.globalRegionDispatcher) {
            delay(1.ticksDuration)
            dirtyBlocks.clear()
            cleanupScheduled.set(false)
            if (dirtyBlocks.isNotEmpty()) {
                scheduleCleanup()
            }
        }
    }

    private fun getEntry(block: Block) =
        DirtyBlockKey(block.world.uid, block.x, block.y, block.z)

    fun hasCustomData(block: Block): Boolean {
        return block.chunk.persistentDataContainer.has(CustomBlockData.getKey(block))
    }

    private data class DirtyBlockKey(
        val worldId: UUID,
        val x: Int,
        val y: Int,
        val z: Int,
    )
}
