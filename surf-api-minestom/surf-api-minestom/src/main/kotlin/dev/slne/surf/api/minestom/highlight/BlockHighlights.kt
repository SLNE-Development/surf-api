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

    private val highlights = ConcurrentHashMap<HighlightKey, Highlight>()

    /**
     * Outlines the block at [position] in [color] for [viewer] alone.
     *
     * Showing a highlight where the same player already has one replaces it, so a highlight can be
     * refreshed after the block below it changed.
     *
     * Nothing is shown when the chunk of [position] is neither loaded nor loadable.
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

        val highlight = Highlight(entity)
        highlights.put(key, highlight)?.retire()

        try {
            val placement = entity.setInstance(instance, position.blockPos())

            if (placement == null) {
                highlights.remove(key, highlight)
                highlight.fail()
            } else {
                placement.whenComplete { _, _ -> highlight.placed(key, viewer) }
            }
        } catch (throwable: Throwable) {
            highlights.remove(key, highlight)
            highlight.fail()
            throw throwable
        }
    }

    /**
     * Takes back the highlight [viewer] was shown at [position], if there is one.
     *
     * @return whether a highlight was taken back
     */
    fun hide(viewer: Player, instance: Instance, position: Point): Boolean =
        highlights.remove(HighlightKey.of(viewer, instance, position))
            ?.also { it.retire() } != null

    /**
     * Takes back every highlight [viewer] was shown.
     *
     * @return how many highlights were taken back
     */
    fun hideAll(viewer: Player): Int {
        var removed = 0
        val entries = highlights.entries.iterator()

        while (entries.hasNext()) {
            val (key, highlight) = entries.next()
            if (key.viewer != viewer.uuid) continue

            entries.remove()
            highlight.retire()
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

    /**
     * A highlight entity together with the state of its asynchronous placement.
     *
     * The placement callback and [retire] exclude each other, so an entity retired while it is still
     * being placed is removed once the placement finished instead of being left in the instance.
     */
    private class Highlight(private val entity: Entity) {
        private var finished = false
        private var retired = false

        @Synchronized
        fun placed(key: HighlightKey, viewer: Player) {
            finished = true

            if (!retired && (entity.isRemoved || entity.chunk == null)) {
                highlights.remove(key, this)
                retired = true
            }

            if (retired) {
                entity.remove()
            } else {
                entity.addViewer(viewer)
            }
        }

        @Synchronized
        fun retire() {
            retired = true
            if (finished) entity.remove()
        }

        @Synchronized
        fun fail() {
            retired = true
            finished = true
            entity.remove()
        }
    }

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
