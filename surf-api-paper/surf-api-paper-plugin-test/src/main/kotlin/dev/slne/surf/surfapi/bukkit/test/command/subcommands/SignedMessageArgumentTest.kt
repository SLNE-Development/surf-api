package dev.slne.surf.surfapi.bukkit.test.command.subcommands

import dev.jorel.commandapi.CommandAPICommand
import dev.slne.surf.api.core.command.args.awaiting
import dev.slne.surf.api.paper.command.args.asyncSignedMessageArgument
import dev.slne.surf.api.paper.command.executors.playerExecutorSuspend
import dev.slne.surf.api.paper.extensions.server
import net.kyori.adventure.chat.ChatType
import net.kyori.adventure.chat.SignedMessage

class SignedMessageArgumentTest(name: String) : CommandAPICommand(name) {
    init {
        asyncSignedMessageArgument("message")
        playerExecutorSuspend { sender, args ->
            val message = args.awaiting<SignedMessage>("message")
            server.sendMessage(message, ChatType.CHAT.bind(sender.displayName()))
        }
    }
}