package dev.slne.surf.api.minestom.server.npc.impl

import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity

/**
 * Check used by the NPC mixins to skip the per-tick `syncWithNpc` teleport of an auxiliary
 * StomNPCs entity (text display, interaction hitbox) that is already in place.
 *
 * Used by [dev.slne.surf.api.minestom.server.npc.mixin.TextDisplayControllerMixin] and
 * [dev.slne.surf.api.minestom.server.npc.mixin.InteractionControllerMixin].
 */
object NpcDisplaySync {

    /**
     * Returns `true` when [auxiliary] is at `npc.position.add(offset)` with the NPC's normalized
     * yaw and pitch. Compares the components directly and does not allocate the target [Pos].
     *
     * Returns `false` when [auxiliary] is `null` or any component differs, including NaN. The
     * caller then runs the original teleport.
     */
    @JvmStatic
    fun isSynchronized(auxiliary: Entity?, npc: Entity, offset: Vec): Boolean {
        if (auxiliary == null) return false

        val npcPosition = npc.position
        val auxiliaryPosition = auxiliary.position

        return auxiliaryPosition.x() == npcPosition.x() + offset.x() &&
                auxiliaryPosition.y() == npcPosition.y() + offset.y() &&
                auxiliaryPosition.z() == npcPosition.z() + offset.z() &&
                auxiliaryPosition.yaw() == Pos.fixYaw(npcPosition.yaw()) &&
                auxiliaryPosition.pitch() == Pos.fixPitch(npcPosition.pitch())
    }
}
