package dev.slne.surf.api.paper.server.nms.v1_21_11.scoreboard

import dev.slne.surf.api.paper.nms.bridges.packets.PacketOperation
import dev.slne.surf.api.paper.nms.common.scoreboard.PlayerNmsScoreboard
import dev.slne.surf.api.paper.server.nms.v1_21_11.bridges.packets.V1_21_11PacketOperationImpl
import dev.slne.surf.api.paper.server.nms.v1_21_11.extensions.toNms
import dev.slne.surf.api.paper.sidebar.SidebarLine
import net.kyori.adventure.text.Component
import net.minecraft.network.chat.numbers.BlankFormat
import net.minecraft.network.chat.numbers.FixedFormat
import net.minecraft.network.protocol.game.ClientboundResetScorePacket
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket
import net.minecraft.network.protocol.game.ClientboundSetScorePacket
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.Objective
import net.minecraft.world.scores.Scoreboard
import net.minecraft.world.scores.criteria.ObjectiveCriteria
import org.bukkit.entity.Player
import java.util.*

@Suppress("ClassName")
class V1_21_11PlayerNmsScoreboardImpl(player: Player, title: Component) : PlayerNmsScoreboard(player, title) {

    override fun createObjective(title: Component): PacketOperation {
        val objective = objective(title)

        return V1_21_11PacketOperationImpl.multi { _, packets ->
            packets.add(
                ClientboundSetObjectivePacket(
                    objective,
                    ClientboundSetObjectivePacket.METHOD_ADD
                )
            )
            packets.add(ClientboundSetDisplayObjectivePacket(DisplaySlot.SIDEBAR, objective))
        }
    }

    override fun updateObjective(title: Component): PacketOperation {
        return V1_21_11PacketOperationImpl.simple {
            ClientboundSetObjectivePacket(
                objective(title),
                ClientboundSetObjectivePacket.METHOD_CHANGE
            )
        }
    }

    override fun removeObjective(): PacketOperation = V1_21_11PacketOperationImpl.simple {
        ClientboundSetObjectivePacket(
            objective(Component.empty()),
            ClientboundSetObjectivePacket.METHOD_REMOVE
        )
    }

    override fun setScore(owner: String, score: Int, line: SidebarLine): PacketOperation {
        return V1_21_11PacketOperationImpl.simple {
            val format = line.score?.let { FixedFormat(it.toNms()) } ?: BlankFormat.INSTANCE
            ClientboundSetScorePacket(
                owner,
                id,
                score,
                Optional.of(line.text.toNms()),
                Optional.of(format)
            )
        }
    }

    override fun resetScore(owner: String): PacketOperation = V1_21_11PacketOperationImpl.simple {
        ClientboundResetScorePacket(owner, id)
    }

    private fun objective(title: Component) = Objective(
        scoreboard,
        id,
        ObjectiveCriteria.DUMMY,
        title.toNms(),
        ObjectiveCriteria.RenderType.INTEGER,
        false,
        null
    )

    companion object {
        private val scoreboard = Scoreboard()
    }
}
