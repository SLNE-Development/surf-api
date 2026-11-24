package dev.slne.minestom.fixtures.test

import dev.slne.minestom.fixtures.FixtureEvent
import dev.slne.surf.api.command.CommandRegistrar
import dev.slne.surf.api.event.SurfEventHandler
import dev.slne.surf.api.minestom.command.CommandAPICommand
import dev.slne.surf.api.minestom.command.dsl.anyExecutor
import dev.slne.surf.api.minestom.plugin.SurfMinestomPlugin
import dev.slne.surf.api.spring.SurfLifecycle
import net.minestom.server.MinecraftServer
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerSpawnEvent
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

const val REGISTRAR_COMMAND = "test-plugin-command"
const val BEAN_COMMAND = "test-plugin-bean-command"

/**
 * A plugin on the test class path, found through its service entry.
 *
 * Its context holds a lifecycle bean, an event node, a command bean, a command registrar and a
 * surf event listener.
 */
class TestPlugin : SurfMinestomPlugin() {
    init {
        instance = this
    }

    /**
     * What the lifecycle bean saw while it enabled, i.e. after the enable phase registered the
     * plugin's listeners and commands.
     */
    data class SeenAtEnable(
        val eventNodeAttached: Boolean,
        val registrarCommand: Boolean,
        val beanCommand: Boolean,
    )

    companion object {
        val calls = mutableListOf<String>()
        var instance: TestPlugin? = null
        var seenAtEnable: SeenAtEnable? = null
        var surfEventsHandled = 0
    }
}

@Component
class TestPluginLifecycle(
    private val plugin: TestPlugin,
    private val node: EventNode<Event>,
) : SurfLifecycle {
    override suspend fun onLoad() {
        TestPlugin.calls += "load"
    }

    override suspend fun onEnable() {
        TestPlugin.calls += "enable"
        val commands = MinecraftServer.process().command()
        TestPlugin.seenAtEnable = TestPlugin.SeenAtEnable(
            eventNodeAttached = node.parent === plugin.eventNode,
            registrarCommand = commands.commandExists(REGISTRAR_COMMAND),
            beanCommand = commands.commandExists(BEAN_COMMAND),
        )
    }

    override suspend fun onDisable() {
        TestPlugin.calls += "disable"
    }
}

@Component
class TestPluginCommands : CommandRegistrar {
    override fun register() {
        CommandAPICommand(REGISTRAR_COMMAND).anyExecutor { _, _ -> }.register()
    }
}

@Component
class TestPluginSurfListener {
    @SurfEventHandler
    fun onFixtureEvent(event: FixtureEvent) {
        TestPlugin.surfEventsHandled++
    }
}

@Configuration(proxyBeanMethods = false)
class TestPluginBeans {

    @Bean
    fun listenerNode(): EventNode<Event> {
        return EventNode.all("test-plugin-listeners").addListener(PlayerSpawnEvent::class.java) { }
    }

    @Bean
    fun beanCommand(): CommandAPICommand = CommandAPICommand(BEAN_COMMAND).anyExecutor { _, _ -> }
}
