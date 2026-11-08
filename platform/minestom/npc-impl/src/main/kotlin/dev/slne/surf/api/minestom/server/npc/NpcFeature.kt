package dev.slne.surf.api.minestom.server.npc

import codes.bed.minestom.npc.StomNPCs
import dev.slne.surf.api.minestom.server.SurfMinestomFeature
import dev.slne.surf.api.minestom.server.SurfMinestomServer
import dev.slne.surf.api.minestom.server.SurfMinestomServerBuilder
import net.minestom.server.event.Event
import net.minestom.server.event.EventFilter
import net.minestom.server.event.EventNode

/**
 * Initializes the StomNPCs library so that [mannequinNpc]s can be spawned and interacted with.
 *
 * StomNPCs binds its manager to the event node of its first initialization for the lifetime of the
 * process. The feature therefore owns a single process-wide node, attaches it to the event node of
 * the running server on [load] and detaches it on [disable], so that a server started again in the
 * same process still receives NPC events.
 */
class NpcFeature internal constructor() : SurfMinestomFeature {

    override val id: String = ID

    override fun load(api: SurfMinestomServer) {
        attach(api.eventNode)
    }

    override fun disable(api: SurfMinestomServer) {
        detach(api.eventNode)
    }

    companion object {
        const val ID = "npc"

        private val node = EventNode.type("stom-npcs", EventFilter.INSTANCE)
        private var parent: EventNode<Event>? = null

        /**
         * Attaches the process-wide NPC node to [parent], replacing its previous parent, and
         * initializes StomNPCs on it once.
         */
        @Synchronized
        internal fun attach(parent: EventNode<Event>) {
            this.parent?.removeChild(node)
            parent.addChild(node)
            this.parent = parent

            StomNPCs.initialize(node)
        }

        /**
         * Detaches the process-wide NPC node when it is attached to [parent].
         */
        @Synchronized
        internal fun detach(parent: EventNode<Event>) {
            if (this.parent !== parent) return

            parent.removeChild(node)
            this.parent = null
        }
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
