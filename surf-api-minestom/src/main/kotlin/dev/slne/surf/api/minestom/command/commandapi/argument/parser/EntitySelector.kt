package dev.slne.surf.api.minestom.command.commandapi.argument.parser

import net.minestom.server.MinecraftServer
import net.minestom.server.command.CommandSender
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
import net.minestom.server.instance.Instance
import java.util.UUID

/**
 * A resolved entity selector, produced by [EntitySelectorParser].
 *
 * A bare player name or UUID resolves through [playerName]/[uuid] directly, bypassing
 * [predicates] entirely, matching vanilla's own selector grammar where those forms never carry
 * bracket options. Otherwise [find] gathers candidates - the sender's own entity for [self],
 * online players for a player-only selector, every entity in the relevant instances otherwise -
 * filters them with [predicates], orders them with [sorter] and caps the result at [limit].
 *
 * [predicates] and [sorter] each receive the selector's anchor position alongside the candidate,
 * resolved from [find]'s `sender` (an entity sender's own position, or the world origin for a
 * sender without one) and adjusted by any explicit `x`/`y`/`z` option. [worldLimited] restricts
 * position-based selectors (`x`, `y`, `z`, `dx`, `dy`, `dz`, `distance`) to the sender's own
 * instance when the sender is an entity; a non-entity sender has no instance to restrict to, so
 * the search falls back to every instance.
 */
internal class EntitySelector(
    val maxResults: Int,
    val includesEntities: Boolean,
    val predicates: List<(Pos, Entity) -> Boolean>,
    val sorter: ((Pos) -> Comparator<Entity>)?,
    val limit: Int,
    val self: Boolean,
    val playerName: String?,
    val uuid: UUID?,
    private val worldLimited: Boolean = false,
) {
    fun find(sender: CommandSender): List<Entity> {
        playerName?.let { name ->
            return listOfNotNull(MinecraftServer.getConnectionManager().getOnlinePlayerByUsername(name))
        }
        uuid?.let { id ->
            return listOfNotNull(findByUuid(id))
        }

        val anchor = (sender as? Entity)?.position ?: Pos.ZERO
        if (self) {
            val entity = sender as? Entity ?: return emptyList()
            return if (matches(anchor, entity)) listOf(entity) else emptyList()
        }

        val senderInstance = (sender as? Entity)?.instance
        val candidates = if (includesEntities) {
            instancesToSearch(senderInstance).asSequence().flatMap { it.entities.asSequence() }
        } else {
            MinecraftServer.getConnectionManager().onlinePlayers.asSequence()
                .filter { player -> !worldLimited || senderInstance == null || player.instance === senderInstance }
        }

        val filtered = candidates
            .filter { entity -> !entity.isRemoved }
            .filter { entity -> matches(anchor, entity) }
            .toList()
        val ordered = sorter?.let { comparatorFor -> filtered.sortedWith(comparatorFor(anchor)) } ?: filtered

        return ordered.take(minOf(limit, maxResults))
    }

    private fun matches(anchor: Pos, entity: Entity): Boolean = predicates.all { predicate -> predicate(anchor, entity) }

    private fun findByUuid(id: UUID): Entity? {
        MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(id)?.let { return it }
        return MinecraftServer.getInstanceManager().instances.asSequence()
            .flatMap { instance -> instance.entities.asSequence() }
            .find { entity -> entity.uuid == id }
    }

    private fun instancesToSearch(senderInstance: Instance?): Collection<Instance> =
        if (worldLimited && senderInstance != null) {
            listOf(senderInstance)
        } else {
            MinecraftServer.getInstanceManager().instances
        }
}
