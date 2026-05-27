package dev.slne.surf.api.paper.server.nms.v26_1.packet.listener

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.tree.ArgumentCommandNode
import com.mojang.brigadier.tree.CommandNode
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.common.CommandSendPacketBlockerListener
import dev.slne.surf.api.paper.packet.listener.listener.annotation.ClientboundListener
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.protocol.game.ClientboundCommandsPacket
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer
import java.util.*


@Suppress("ClassName")
@OptIn(NmsUseWithCaution::class)
class V26_1CommandSendPacketBlockerListenerImpl(blockedPlayer: Set<UUID>) :
    CommandSendPacketBlockerListener(blockedPlayer) {

    private val loadingCommandsDispatcher = CommandDispatcher<CommandSourceStack>()
    private val commandNodeInspector = object : ClientboundCommandsPacket.NodeInspector<CommandSourceStack> {
        override fun suggestionId(p0: ArgumentCommandNode<CommandSourceStack, *>): Identifier? {
            return null
        }

        override fun isExecutable(p0: CommandNode<CommandSourceStack>): Boolean {
            return false
        }

        override fun isRestricted(p0: CommandNode<CommandSourceStack>): Boolean {
            return false
        }
    }

    init {
        loadingCommandsDispatcher.register(LiteralArgumentBuilder.literal("commands-are-loading"))
    }

    @ClientboundListener
    fun onClientboundCommandsPacket(
        packet: ClientboundCommandsPacket,
        player: ServerPlayer
    ): ClientboundCommandsPacket? {
        if (blockedPlayer.contains(player.uuid)) {
            return if (receivedCommandPacket.add(player.uuid)) {
                ClientboundCommandsPacket(loadingCommandsDispatcher.root, commandNodeInspector)
            } else {
                null
            }
        }

        return packet
    }
}