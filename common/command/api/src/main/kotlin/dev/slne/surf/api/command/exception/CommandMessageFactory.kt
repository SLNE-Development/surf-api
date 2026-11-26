package dev.slne.surf.api.command.exception

import com.mojang.brigadier.Message
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import dev.slne.surf.api.spring.surfBean
import net.kyori.adventure.text.Component

/**
 * Converts an Adventure [Component] into the Brigadier [Message] that the platform's command
 * dispatcher renders as that component.
 *
 * A Brigadier message reaches the sender through the platform's own rendering, which differs per
 * platform. Each platform that creates [WrapperCommandExceptionComponent] provides one bean of this
 * type in surf-api's root context; without it, the exception cannot be created.
 */
@InternalSurfApi
fun interface CommandMessageFactory {

    /**
     * Creates the message that the platform renders as [component].
     *
     * @param component the component to show to the command sender
     * @return a message whose rendered form is [component]
     */
    fun create(component: Component): Message

    @InternalSurfApi
    companion object {

        /**
         * The [CommandMessageFactory] bean of surf-api's root context.
         */
        @InternalSurfApi
        val instance by surfBean<CommandMessageFactory>()
    }
}
