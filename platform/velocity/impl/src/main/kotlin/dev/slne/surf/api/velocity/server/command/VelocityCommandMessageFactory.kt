package dev.slne.surf.api.velocity.server.command

import com.mojang.brigadier.Message
import com.velocitypowered.api.command.VelocityBrigadierMessage
import dev.slne.surf.api.command.exception.CommandMessageFactory
import net.kyori.adventure.text.Component
import org.springframework.stereotype.Service

/**
 * Creates the Brigadier messages that Velocity renders as Adventure components.
 */
@Service
internal class VelocityCommandMessageFactory : CommandMessageFactory {

    override fun create(component: Component): Message {
        return VelocityBrigadierMessage.tooltip(component)
    }
}
