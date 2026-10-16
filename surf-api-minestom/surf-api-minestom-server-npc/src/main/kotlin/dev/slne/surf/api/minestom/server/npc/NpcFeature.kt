package dev.slne.surf.api.minestom.server.npc

import codes.bed.minestom.npc.StomNPCs
import dev.slne.surf.api.minestom.server.SurfMinestomFeature
import dev.slne.surf.api.minestom.server.SurfMinestomServer
import dev.slne.surf.api.minestom.server.SurfMinestomServerBuilder
import net.minestom.server.event.EventFilter
import net.minestom.server.event.EventNode

/**
 * Initializes the StomNPCs library on a child of the server's event node, so that [mannequinNpc]s
 * can be spawned and interacted with.
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
 * Installs the NPC library.
 *
 * @see NpcFeature
 */
fun SurfMinestomServerBuilder.withNpcLib() {
    install(NpcFeature())
}
