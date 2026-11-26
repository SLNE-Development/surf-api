package dev.slne.surf.api.paper.sharedbean.consumer

import dev.slne.surf.api.command.SurfCommandUtil
import io.papermc.paper.command.brigadier.MessageComponentSerializer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.springframework.stereotype.Service

/**
 * Reports whether a command exception message reaches the server's command renderer as the
 * component it was created from.
 */
@Service
class ConsumerCommandMessageProbe {

    /**
     * `ok` when the message of an exception from [SurfCommandUtil] renders, through the Brigadier
     * message conversion of the Paper api, as the original component. Otherwise names the
     * difference or the failure, so a broken conversion cannot keep the plugin from enabling.
     */
    val commandMessage: String
        get() {
            val original = Component.text("Kein Zugriff", NamedTextColor.RED)

            return try {
                val message = SurfCommandUtil.createException(original).exception.rawMessage
                val rendered = MessageComponentSerializer.message().deserialize(message)

                if (rendered == original) {
                    "ok"
                } else {
                    "mismatch(rendered=$rendered, string=${message.string})"
                }
            } catch (e: Exception) {
                "failed(${e.javaClass.simpleName}: ${e.message})"
            }
        }
}
