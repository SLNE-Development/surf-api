package dev.slne.surf.microservice.runtime.spark

import dev.slne.surf.microservice.api.command.MicroserviceCommandOutput
import me.lucko.spark.common.command.sender.CommandSender
import me.lucko.spark.lib.adventure.text.Component
import me.lucko.spark.lib.adventure.text.serializer.ansi.ANSIComponentSerializer
import java.util.UUID

/**
 * Forwards spark messages, rendered with ANSI colors, to a command output. Holds every permission.
 */
internal class MicroserviceSparkCommandSender(
    private val output: MicroserviceCommandOutput,
) : CommandSender {
    override fun getName(): String = "microservice-console"

    override fun getUniqueId(): UUID? = null

    override fun sendMessage(message: Component) {
        output.sendLine(ANSIComponentSerializer.ansi().serialize(message))
    }

    override fun hasPermission(permission: String): Boolean = true
}
