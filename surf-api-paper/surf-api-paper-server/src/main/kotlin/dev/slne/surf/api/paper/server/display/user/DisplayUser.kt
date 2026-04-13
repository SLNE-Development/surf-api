package dev.slne.surf.api.paper.server.display.user

import com.github.retrooper.packetevents.wrapper.PacketWrapper
import dev.slne.surf.api.paper.server.display.DisplaySession
import dev.slne.surf.api.paper.extensions.server
import dev.slne.surf.api.core.extensions.sendPacket
import java.util.*

class DisplayUser(
    val uuid: UUID
) {
    val player get() = server.getPlayer(uuid)

    val entityId: Int get() = player?.entityId ?: -1

    var session: DisplaySession? = null

    val inSession: Boolean get() = session?.isActive == true

    fun sendPacket(packet: PacketWrapper<*>) {
        player?.let { packet.sendPacket(it) }
    }

    companion object {
        private val users = mutableMapOf<UUID, DisplayUser>()

        fun of(uuid: UUID): DisplayUser = users.getOrPut(uuid) { DisplayUser(uuid) }

        fun remove(uuid: UUID) {
            users.remove(uuid)
        }

        fun get(uuid: UUID): DisplayUser? = users[uuid]
    }
}
