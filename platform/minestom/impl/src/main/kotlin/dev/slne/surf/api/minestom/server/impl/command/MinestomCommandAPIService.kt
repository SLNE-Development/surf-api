package dev.slne.surf.api.minestom.server.impl.command

import com.mojang.brigadier.exceptions.CommandSyntaxException
import dev.slne.surf.api.minestom.command.CommandAPI
import dev.slne.surf.api.minestom.extension.CommandManager
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.utils.callback.CommandCallback

/**
 * Installs and removes the CommandAPI on Minestom.
 *
 * Dispatch and declaration merging depend on `CommandManagerMixin`, so the server must run with the
 * surf Mixin agent. [start] and [stop] are not thread-safe and belong to the server lifecycle.
 */
internal class MinestomCommandAPIService {
    private val ownership = MinestomCommandOwnership()
    private var platform: MinestomCommandAPIPlatform? = null
    private var previousUnknownCommandCallback: CommandCallback? = null

    /**
     * Installs the platform, registers tab completion on [node] and replaces the manager's unknown
     * command callback.
     *
     * @throws IllegalStateException if the service is already started
     */
    fun start(node: EventNode<Event>) {
        check(platform == null) { "Minestom CommandAPI is already installed" }

        CommandAPITranslations.register()

        // Process-wide, as in vanilla: failed parses while typing do not need a stack trace
        CommandSyntaxException.ENABLE_COMMAND_STACK_TRACES = false

        val previousActive = MinestomCommandAPIPlatform.active()
        val installed = MinestomCommandAPIPlatform(CommandManager, ownership)
        try {
            CommandAPI.installPlatform(installed)
        } catch (failure: Throwable) {
            try {
                MinestomCommandAPIPlatform.discard(installed, previousActive)
            } catch (discardFailure: Throwable) {
                failure.addSuppressed(discardFailure)
            }
            throw failure
        }
        platform = installed

        previousUnknownCommandCallback = CommandManager.unknownCommandCallback
        MinestomSuggestionListener(ownership).register(node)

        CommandManager.unknownCommandCallback = CommandCallback(CommandAPIHook::reportUnknown)
    }

    /**
     * Closes the platform and restores the unknown command callback that [start] replaced.
     *
     * Does nothing when the service is not started.
     */
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
