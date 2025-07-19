package dev.slne.surf.surfapi.bukkit.server.impl.visualizer.visualizer

import dev.slne.surf.surfapi.core.api.util.mutableObject2ObjectMapOf
import dev.slne.surf.surfapi.core.api.util.mutableObjectSetOf
import it.unimi.dsi.fastutil.objects.ObjectSet
import org.bukkit.Chunk
import org.bukkit.entity.Player
import java.util.*

object VisualizerManager {
    private val activeVisualizers =
        mutableObject2ObjectMapOf<UUID, ObjectSet<AbstractSurfVisualizerImpl>>()

    fun processChunkReceiveUpdateForPlayer(player: Player, chunk: Chunk) {
        val activeVisualizerSet = activeVisualizers.get(player.uniqueId) ?: return
        activeVisualizerSet.forEach { it.onPlayerReceiveChunk(player, chunk) }
    }

    fun processChunkUnloadForPlayer(player: Player, chunk: Chunk) {
        val activeVisualizerSet = activeVisualizers[player.uniqueId] ?: return
        activeVisualizerSet.forEach { it.onPlayerUnloadChunk(player, chunk) }
    }

    fun isActiveVisualizer(uid: UUID): Boolean {
        return activeVisualizers.get(uid)?.isNotEmpty() ?: false
    }

    fun addVisualizer(uniqueId: UUID, visualizerImpl: AbstractSurfVisualizerImpl) {
        activeVisualizers.computeIfAbsent(uniqueId) { mutableObjectSetOf() }.add(visualizerImpl)
    }

    fun removeVisualizer(uniqueId: UUID, visualizerImpl: AbstractSurfVisualizerImpl) {
        activeVisualizers[uniqueId]?.remove(visualizerImpl)
        if (activeVisualizers[uniqueId]?.isEmpty() == true) {
            activeVisualizers.remove(uniqueId)
        }
    }

    fun removeVisualizer(visualizerImpl: AbstractSurfVisualizerImpl) {
        activeVisualizers.values.forEach { it.remove(visualizerImpl) }
        activeVisualizers.entries.removeIf { it.value.isEmpty() }
    }

    fun processPlayerQuit(player: Player) {
        val visualizers = activeVisualizers.remove(player.uniqueId)
        visualizers?.forEach { it.removeViewer(player) }
    }
}