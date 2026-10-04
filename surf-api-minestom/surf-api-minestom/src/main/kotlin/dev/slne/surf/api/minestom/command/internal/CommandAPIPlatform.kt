package dev.slne.surf.api.minestom.command.internal

import dev.slne.surf.api.minestom.command.CommandDefinition
import dev.slne.surf.api.minestom.command.RegisteredCommand
import net.minestom.server.command.CommandSender
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
interface CommandAPIPlatform {
    fun register(definition: CommandDefinition, namespace: String?): RegisteredCommand

    fun unregister(name: String): Boolean

    fun execute(sender: CommandSender, input: String): Int
}
