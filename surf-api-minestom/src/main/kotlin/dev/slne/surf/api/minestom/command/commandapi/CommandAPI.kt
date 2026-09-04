package dev.slne.surf.api.minestom.command.commandapi

import dev.slne.surf.api.minestom.command.commandapi.exception.CommandSyntaxException
import dev.slne.surf.api.minestom.command.commandapi.exception.CommandValidationException
import dev.slne.surf.api.minestom.command.commandapi.internal.CommandAPIPlatform
import net.kyori.adventure.text.Component
import net.minestom.server.command.CommandSender
import org.jetbrains.annotations.ApiStatus
import java.util.concurrent.atomic.AtomicReference

object CommandAPI {
    private val platform = AtomicReference<CommandAPIPlatform?>()

    @ApiStatus.Internal
    fun installPlatform(value: CommandAPIPlatform) {
        check(platform.compareAndSet(null, value)) {
            "The CommandAPI platform is already installed"
        }
    }

    @ApiStatus.Internal
    fun uninstallPlatform(value: CommandAPIPlatform) {
        platform.compareAndSet(value, null)
    }

    @ApiStatus.Internal
    fun register(definition: CommandDefinition, namespace: String?): RegisteredCommand {
        validateNamespace(namespace)
        return installedPlatform().register(definition, namespace)
    }

    fun unregister(name: String): Boolean = installedPlatform().unregister(name)

    /**
     * Dispatches [input] as [sender] and returns the executed command's result value.
     *
     * [input] carries no leading slash. Callers that must not throw check ownership first.
     *
     * @throws com.mojang.brigadier.exceptions.CommandSyntaxException when the command is unknown, an
     * argument is rejected, or input remains after a complete syntax.
     */
    fun execute(sender: CommandSender, input: String): Int =
        installedPlatform().execute(sender, input)

    fun failWithString(message: String): Nothing = failWithMessage(Component.text(message))

    fun failWithMessage(message: Component): Nothing = throw CommandSyntaxException(message)

    private fun installedPlatform(): CommandAPIPlatform = checkNotNull(platform.get()) {
        "The CommandAPI platform has not been installed; register commands after server startup"
    }

    private fun validateNamespace(namespace: String?) {
        if (namespace != null && !NAMESPACE_PATTERN.matches(namespace)) {
            throw CommandValidationException(
                "Command namespace '$namespace' must contain only lowercase letters, digits, '.', '_' or '-'",
            )
        }
    }

    private val NAMESPACE_PATTERN = Regex("[a-z0-9_.-]+")
}
