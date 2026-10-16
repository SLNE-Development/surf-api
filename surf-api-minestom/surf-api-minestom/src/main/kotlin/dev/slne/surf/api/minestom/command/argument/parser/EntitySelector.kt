package dev.slne.surf.api.minestom.command.argument.parser

import net.minestom.server.MinecraftServer
import net.minestom.server.command.CommandSender
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
import net.minestom.server.instance.Instance
import java.util.*

/**
 * Entity selector produced by [EntitySelectorParser].
 *
 * [find] resolves a selector given as a bare [playerName] or [uuid] directly; [predicates] do not
 * apply to those forms. Any other selector takes its candidates from the sender for [self], from
 * the online players, or, when [includesEntities] is set, from the entities of the searched
 * instances. Candidates are filtered by [predicates], ordered by [sorter] and truncated to the
 * smaller of [limit] and [maxResults].
 *
 * [predicates] and [sorter] receive the selector origin: the sender's position, or the world origin
 * when the sender is not an [Entity], with each axis replaced by the `x`, `y` or `z` option
 * through [position].
 *
 * When [worldLimited] is set and the sender is an entity in an instance, only that instance is
 * searched; otherwise all instances are.
 */
internal class EntitySelector(
    val maxResults: Int,
    val includesEntities: Boolean,
    val predicates: List<(Pos, Entity) -> Boolean>,
    val sorter: ((Pos) -> Comparator<Entity>)?,
    val limit: Int,
    val position: (Pos) -> Pos,
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

        val origin = position((sender as? Entity)?.position ?: Pos.ZERO)
        if (self) {
            val entity = sender as? Entity ?: return emptyList()
            return if (matches(origin, entity)) listOf(entity) else emptyList()
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
            .filter { entity -> matches(origin, entity) }
        val resultLimit = minOf(limit, maxResults)
        val comparator = sorter?.invoke(origin) ?: return filtered.take(resultLimit).toList()

        if (resultLimit == 1) return listOfNotNull(filtered.minWithOrNull(comparator))
        return filtered.toList().sortedWith(comparator).take(resultLimit)
    }

    private fun matches(origin: Pos, entity: Entity): Boolean = predicates.all { predicate -> predicate(origin, entity) }

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
