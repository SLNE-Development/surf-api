package dev.slne.surf.api.minestom.server.impl.performance

import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType

/**
 * Decides which entities `EntityTickMixin` lets tick.
 *
 * [configure] replaces the disabled types atomically; [shouldTick] is lock-free and safe to call
 * from any tick thread.
 */
object EntityTickFilter {

    @Volatile
    private var disabledTypeIds = BooleanArray(0)

    /**
     * Whether [entity] ticks, that is, its type is not disabled.
     */
    @JvmStatic
    fun shouldTick(entity: Entity): Boolean {
        val disabled = disabledTypeIds
        val id = entity.entityType.id()
        return id < 0 || id >= disabled.size || !disabled[id]
    }

    /**
     * Disables ticking for exactly [types].
     */
    fun configure(types: Collection<EntityType>) {
        val disabled = BooleanArray((types.maxOfOrNull(EntityType::id) ?: -1) + 1)
        types.forEach { type -> disabled[type.id()] = true }
        disabledTypeIds = disabled
    }
}
