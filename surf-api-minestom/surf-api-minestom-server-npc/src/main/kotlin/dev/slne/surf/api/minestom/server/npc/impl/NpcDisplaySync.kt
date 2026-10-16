package dev.slne.surf.api.minestom.server.npc.impl

import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity

/**
 * Decides whether a StomNPCs auxiliary entity (TEXT_DISPLAY, INTERACTION) still sits on the
 * transform its controller would teleport it to, so the per-tick `syncWithNpc` teleport can be
 * skipped. Used by [dev.slne.surf.api.minestom.server.npc.mixin.TextDisplayControllerMixin] and
 * [dev.slne.surf.api.minestom.server.npc.mixin.InteractionControllerMixin].
 */
object NpcDisplaySync {

    /**
     * Returns true when [auxiliary] is already at `npc.position.add(offset)`, comparing the exact
     * coordinates a teleport would store instead of building the target [Pos].
     *
     * Returns false for a missing [auxiliary] and for any transform the controller could not have
     * produced (clamped or NaN coordinates), so the caller falls back to the original teleport.
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
