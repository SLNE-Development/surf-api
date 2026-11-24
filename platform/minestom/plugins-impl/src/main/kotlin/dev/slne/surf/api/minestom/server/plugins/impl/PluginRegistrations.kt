package dev.slne.surf.api.minestom.server.plugins.impl

import dev.slne.surf.api.command.CommandRegistrar
import dev.slne.surf.api.core.server.spring.SurfEventHandlerBeans
import dev.slne.surf.api.event.SurfEventBus
import dev.slne.surf.api.minestom.command.CommandAPICommand
import dev.slne.surf.api.minestom.command.CommandTree
import dev.slne.surf.api.spring.util.FailureCollector
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import org.springframework.beans.factory.getBeansOfType
import org.springframework.context.ApplicationContext
import org.springframework.context.ConfigurableApplicationContext

/**
 * Registers what the beans of one plugin's contexts declare, in the plugin's enable phase, and
 * removes it again when the plugin is disabled.
 *
 * Registers beans of type [EventNode] as children of the plugin's event node, beans of type
 * [CommandAPICommand] and [CommandTree] and [CommandRegistrar] beans as commands, and beans with
 * `@SurfEventHandler` methods as listeners of the [SurfEventBus]. Only beans defined in a context
 * itself count, never those of its ancestors.
 *
 * Commands are attributed to the plugin that is current on the calling thread, so [register] has to
 * run while the plugin's lifecycle runs. What [register] did stays recorded when it fails halfway,
 * so that [unregister] still removes it. Commands are removed by the plugin manager together with
 * the plugin's other commands. Not thread-safe; the plugin manager calls it from one thread at a
 * time.
 *
 * Minestom's commands are surf's own, not CommandAPI's, so they are registered here rather than
 * through `CommandApiCommandBeans` of the core module.
 */
internal class PluginRegistrations(private val pluginNode: EventNode<Event>) {

    private val attachedNodes = mutableListOf<EventNode<*>>()
    private val surfEventListeners = mutableListOf<Any>()

    /**
     * Registers the beans of [contexts].
     */
    fun register(contexts: List<ConfigurableApplicationContext>) {
        contexts.forEach(::attachEventNodes)
        contexts.forEach(::registerCommands)
        SurfEventHandlerBeans.register(contexts, surfEventListeners)
    }

    /**
     * Removes the event nodes and event bus listeners [register] added.
     */
    fun unregister() {
        val listeners = surfEventListeners.toList()
        surfEventListeners.clear()
        val nodes = attachedNodes.toList()
        attachedNodes.clear()

        val failures = FailureCollector()
        fun step(action: () -> Unit) {
            try {
                action()
            } catch (stepFailure: Throwable) {
                failures.add(stepFailure)
            }
        }

        step { SurfEventHandlerBeans.unregister(listeners) }

        for (node in nodes) {
            step { pluginNode.removeChild(node) }
        }

        failures.throwIfAny()
    }

    private fun attachEventNodes(context: ApplicationContext) {
        for (node in context.getBeansOfType<EventNode<*>>().values) {
            pluginNode.addChild(node)
            attachedNodes += node
        }
    }

    private fun registerCommands(context: ApplicationContext) {
        for (command in context.getBeansOfType<CommandAPICommand>().values) {
            command.register()
        }

        for (tree in context.getBeansOfType<CommandTree>().values) {
            tree.register()
        }

        context.getBeansOfType<CommandRegistrar>().values.forEach(CommandRegistrar::register)
    }
}
