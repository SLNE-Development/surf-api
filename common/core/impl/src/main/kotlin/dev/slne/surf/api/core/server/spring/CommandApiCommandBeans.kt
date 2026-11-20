package dev.slne.surf.api.core.server.spring

import dev.jorel.commandapi.ExecutableCommand
import dev.slne.surf.api.command.CommandRegistrar
import org.springframework.beans.factory.getBeansOfType
import org.springframework.context.ApplicationContext

/**
 * Registers the commands among the beans of a plugin's contexts on a platform that runs
 * CommandAPI: beans that are a CommandAPI command or tree themselves, and [CommandRegistrar]s.
 *
 * Only the beans defined in each context itself are considered, never those of its ancestors.
 * Commands are not unregistered, because plugins are never reloaded.
 */
object CommandApiCommandBeans {

    /**
     * Registers the command beans of [contexts].
     */
    fun register(contexts: List<ApplicationContext>) {
        contexts.forEach(::registerCommands)
    }

    private fun registerCommands(context: ApplicationContext) {
        context.getBeansOfType<ExecutableCommand<*, *>>().values.forEach { it.register() }
        context.getBeansOfType<CommandRegistrar>().values.forEach(CommandRegistrar::register)
    }
}
