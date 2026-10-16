package dev.slne.surf.api.minestom.server.impl.command

import dev.slne.surf.api.minestom.command.CommandAPI
import dev.slne.surf.api.minestom.extension.CommandManager
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.utils.callback.CommandCallback

/**
 * Installs the CommandAPI on Minestom.
 *
 * Minestom's command manager hands CommandAPI commands to the Brigadier dispatcher and merges
 * their tree into the declared commands through `CommandManagerMixin`, which requires the server
 * to run with the surf Mixin agent.
 */
internal class MinestomCommandAPIService {
    private val ownership = MinestomCommandOwnership()
    private var platform: MinestomCommandAPIPlatform? = null
    private var previousUnknownCommandCallback: CommandCallback? = null

    fun start(node: EventNode<Event>) {
        check(platform == null) { "Minestom CommandAPI is already installed" }

        CommandAPITranslations.register()

        val installed = MinestomCommandAPIPlatform(CommandManager, ownership)
        CommandAPI.installPlatform(installed)
        platform = installed

        MinestomSuggestionListener(ownership).register(node)

        previousUnknownCommandCallback = CommandManager.unknownCommandCallback
        CommandManager.unknownCommandCallback = CommandCallback(CommandAPIHook::reportUnknown)
    }

    fun stop() {
        val installed = platform ?: return
        platform = null

        CommandManager.unknownCommandCallback = previousUnknownCommandCallback
        previousUnknownCommandCallback = null

        try {
            installed.close()
        } finally {
            CommandAPI.uninstallPlatform(installed)
        }
    }
}
