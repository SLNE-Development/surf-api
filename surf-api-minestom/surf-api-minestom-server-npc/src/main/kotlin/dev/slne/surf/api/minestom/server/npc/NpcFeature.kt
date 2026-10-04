package dev.slne.surf.api.minestom.server.npc

import codes.bed.minestom.npc.StomNPCs
import dev.slne.surf.api.minestom.server.SurfMinestomServer
import dev.slne.surf.api.minestom.server.SurfMinestomServerBuilder
import dev.slne.surf.api.minestom.server.SurfMinestomFeature
import net.minestom.server.event.EventFilter
import net.minestom.server.event.EventNode

/**
 * Runs the NPC library, so that [mannequinNpc]s can be spawned and interacted with.
 */
class NpcFeature internal constructor() : SurfMinestomFeature {

    override val id: String = ID

    override fun load(api: SurfMinestomServer) {
        val node = EventNode.type("stom-npcs", EventFilter.INSTANCE)
        api.eventNode.addChild(node)

        StomNPCs.initialize(node)
    }

    companion object {
        const val ID = "npc"
    }
}

/**
 * Runs the NPC library, so that [mannequinNpc]s can be spawned and interacted with.
 */
fun SurfMinestomServerBuilder.withNpcLib() {
    install(NpcFeature())
}
