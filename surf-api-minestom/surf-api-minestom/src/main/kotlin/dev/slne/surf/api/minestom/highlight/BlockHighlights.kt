package dev.slne.surf.api.minestom.highlight

import net.kyori.adventure.text.format.TextColor
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.metadata.display.BlockDisplayMeta
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Outlines single blocks for single players.
 *
 * A highlight lives until it is taken back, so whoever shows one is responsible for hiding it
 * again - [hideAll] takes back everything a single player was shown, which is what a player leaving
 * usually calls for.
 */
object BlockHighlights {

    private const val OVERSIZE = 0.02

    private val highlights = ConcurrentHashMap<HighlightKey, Entity>()

    /**
     * Outlines the block at [position] in [color] for [viewer] alone.
     *
     * Showing a highlight where the same player already has one replaces it, so a highlight can be
     * refreshed after the block below it changed.
     *
     * @param viewer the only player who sees the highlight
     * @param instance the instance the block sits in
     * @param position any point inside the block to outline
     * @param color the colour the outline glows in, or `null` for the client's default
     * @param block the block the highlight is drawn as; by default the block that stands at
     * [position], which needs its chunk to be loaded to be readable
     */
    fun show(
        viewer: Player,
        instance: Instance,
        position: Point,
        color: TextColor? = null,
        block: Block = instance.blockAtOrAir(position)
    ) {
        val key = HighlightKey.of(viewer, instance, position)

        val entity = Entity(EntityType.BLOCK_DISPLAY).apply {
            isAutoViewable = false
            setNoGravity(true)
            isGlowing = true

            editEntityMeta(BlockDisplayMeta::class.java) { meta ->
                meta.setBlockState(block)
                meta.scale = Vec(1 + OVERSIZE, 1 + OVERSIZE, 1 + OVERSIZE)
                meta.translation = Vec(-OVERSIZE / 2, -OVERSIZE / 2, -OVERSIZE / 2)
                color?.let { meta.glowColorOverride = it.value() }
            }
        }

        highlights.put(key, entity)?.remove()
        entity.setInstance(instance, position.blockPos())
            .thenRun { entity.addViewer(viewer) }
    }

    /**
     * Takes back the highlight [viewer] was shown at [position], if there is one.
     *
     * @return whether a highlight was taken back
     */
    fun hide(viewer: Player, instance: Instance, position: Point): Boolean =
        highlights.remove(HighlightKey.of(viewer, instance, position))
            ?.also { it.remove() } != null

    /**
     * Takes back every highlight [viewer] was shown.
     *
     * @return how many highlights were taken back
     */
    fun hideAll(viewer: Player): Int {
        var removed = 0
        val entries = highlights.entries.iterator()

        while (entries.hasNext()) {
            val (key, entity) = entries.next()
            if (key.viewer != viewer.uuid) continue

            entries.remove()
            entity.remove()
            removed++
        }

        return removed
    }

    /**
     * Returns how many highlights [viewer] is currently being shown.
     */
    fun count(viewer: Player): Int = highlights.keys.count { it.viewer == viewer.uuid }

    private fun Instance.blockAtOrAir(position: Point): Block =
        if (isChunkLoaded(position)) getBlock(position) else Block.AIR

    private fun Point.blockPos() =
        Pos(blockX().toDouble(), blockY().toDouble(), blockZ().toDouble())

    private data class HighlightKey(
        val viewer: UUID,
        val instance: UUID,
        val x: Int,
        val y: Int,
        val z: Int
    ) {
        companion object {
            fun of(viewer: Player, instance: Instance, position: Point) = HighlightKey(
                viewer.uuid,
                instance.uuid,
                position.blockX(),
                position.blockY(),
                position.blockZ()
            )
        }
    }
}
