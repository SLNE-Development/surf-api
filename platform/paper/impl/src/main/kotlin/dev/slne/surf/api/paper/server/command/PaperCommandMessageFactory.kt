package dev.slne.surf.api.paper.server.command

import com.mojang.brigadier.Message
import dev.jorel.commandapi.BukkitTooltip
import dev.slne.surf.api.command.exception.CommandMessageFactory
import net.kyori.adventure.text.Component
import org.springframework.stereotype.Service

/**
 * Creates the Brigadier messages that Paper renders as Adventure components.
 */
@Service
internal class PaperCommandMessageFactory : CommandMessageFactory {

    override fun create(component: Component): Message {
        return BukkitTooltip.messageFromAdventureComponent(component)
    }
}
