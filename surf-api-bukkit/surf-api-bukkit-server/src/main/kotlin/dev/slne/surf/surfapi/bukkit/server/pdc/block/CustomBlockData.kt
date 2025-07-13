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
package dev.slne.surf.surfapi.bukkit.server.pdc.block

import dev.slne.surf.surfapi.bukkit.api.pdc.block.CustomBlockPersistentDataContainer
import dev.slne.surf.surfapi.bukkit.api.pdc.block.pdc
import dev.slne.surf.surfapi.bukkit.api.util.key
import org.bukkit.Chunk
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType

class CustomBlockData(val block: Block) : CustomBlockPersistentDataContainer {
    override val chunk: Chunk = block.chunk
    private val key = getKey(block)
    private val pdc = getPersistentDataContainer()

    private fun getPersistentDataContainer(): PersistentDataContainer {
        val chunkPdc = chunk.persistentDataContainer
        val blockPdc = chunkPdc.get(key, PersistentDataType.TAG_CONTAINER)
        if (blockPdc != null) return blockPdc
        return chunkPdc.adapterContext.newPersistentDataContainer()
    }

    override fun clear() {
        pdc.keys.forEach(pdc::remove)
        save()
    }

    private fun save() {
        BlockPdcManager.markDirty(this)
        if (pdc.isEmpty) {
            chunk.persistentDataContainer.remove(key)
        } else {
            chunk.persistentDataContainer.set(key, PersistentDataType.TAG_CONTAINER, pdc)
        }
    }

    override fun copyTo(block: Block) {
        copyTo(block.pdc(), true)
    }

    override fun <P : Any, C : Any> set(
        key: NamespacedKey,
        type: PersistentDataType<P, C>,
        value: C,
    ) {
        pdc.set(key, type, value)
        save()
    }

    override fun remove(key: NamespacedKey) {
        pdc.remove(key)
        save()
    }

    override fun readFromBytes(bytes: ByteArray, clear: Boolean) {
        pdc.readFromBytes(bytes, clear)
    }

    override fun <P : Any, C : Any> has(
        key: NamespacedKey,
        type: PersistentDataType<P, C>,
    ): Boolean {
        return pdc.has(key, type)
    }

    override fun has(key: NamespacedKey): Boolean {
        return pdc.has(key)
    }

    override fun <P : Any, C : Any> get(
        key: NamespacedKey,
        type: PersistentDataType<P, C>,
    ): C? {
        return pdc.get(key, type)
    }

    override fun <P : Any, C : Any> getOrDefault(
        key: NamespacedKey,
        type: PersistentDataType<P, C>,
        defaultValue: C,
    ): C {
        return pdc.getOrDefault(key, type, defaultValue)
    }

    override fun getKeys(): Set<NamespacedKey> {
        return pdc.keys
    }

    override fun isEmpty(): Boolean {
        return pdc.isEmpty
    }

    override fun copyTo(
        other: PersistentDataContainer,
        replace: Boolean,
    ) {
        pdc.copyTo(other, replace)
    }

    override fun getAdapterContext(): PersistentDataAdapterContext {
        return pdc.adapterContext
    }

    override fun serializeToBytes(): ByteArray {
        return pdc.serializeToBytes()
    }

    companion object {
        fun getKey(block: Block): NamespacedKey {
            return key("x${block.x and 15}y${block.y}z${block.z and 15}")
        }
    }
}