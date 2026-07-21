package dev.slne.surf.microservice.runtime.microservice.spark

import dev.slne.surf.microservice.api.microservice.command.MicroserviceCommandContext
import me.lucko.spark.common.command.sender.CommandSender
import me.lucko.spark.lib.adventure.text.Component
import me.lucko.spark.lib.adventure.text.serializer.ansi.ANSIComponentSerializer
import java.util.*

internal class MicroserviceSparkCommandSender(
    private val context: MicroserviceCommandContext
) : CommandSender {
    override fun getName(): String {
        return "microservice-console"
    }

    override fun getUniqueId(): UUID? {
        return null
    }

    override fun sendMessage(message: Component) {
        context.sendLine(ANSIComponentSerializer.ansi().serialize(message))
    }

    override fun hasPermission(permission: String): Boolean {
        return true
    }
}