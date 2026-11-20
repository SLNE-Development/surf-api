package dev.slne.surf.api.minestom.server.plugins

import dev.slne.minestom.fixtures.FixtureEvent
import dev.slne.minestom.fixtures.FixtureLog
import dev.slne.minestom.fixtures.alpha.AlphaGreetingService
import dev.slne.minestom.fixtures.alpha.AlphaPlugin
import dev.slne.minestom.fixtures.beta.BETA_COMMAND
import dev.slne.minestom.fixtures.beta.BetaPlugin
import dev.slne.minestom.fixtures.beta.BetaService
import dev.slne.surf.api.minestom.plugin.PluginDependency
import dev.slne.surf.api.minestom.plugin.PluginMeta
import dev.slne.surf.api.minestom.server.plugins.impl.PluginCandidate
import dev.slne.surf.api.minestom.server.plugins.impl.PluginManagerImpl
import dev.slne.surf.api.minestom.server.surfMinestomServer
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import dev.slne.surf.api.spring.SurfContexts
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.context.ConfigurableApplicationContext
import java.nio.file.Path

/**
 * Runs the `alpha` and `beta` fixture plugins, where `beta` depends on `alpha`, on a real surf
 * server and checks what the plugin manager does with their Spring contexts.
 */
@EnvTest
@OptIn(InternalSurfApi::class)
class PluginContextsTest {

    private fun candidate(id: String, main: Class<*>, vararg dependencies: String) = PluginCandidate(
        PluginMeta(id, main.name, dependencies = dependencies.map { PluginDependency(it) }),
        jar = null,
    )

    /**
     * Runs [block] with a manager that loaded `beta` and `alpha`, in that order, below the root
     * context of a started surf server. The manager is not enabled.
     */
    private fun withLoadedPlugins(directory: Path, block: (PluginManagerImpl, ConfigurableApplicationContext) -> Unit) {
        FixtureLog.events.clear()
        BetaPlugin.greetingFromAlpha = null
        BetaPlugin.alphaInternalsVisible = null
        BetaPlugin.surfEventsHandled = 0

        val server = surfMinestomServer {}
        try {
            val root = SurfContexts.root as ConfigurableApplicationContext
            val manager = PluginManagerImpl(directory, server.eventNode, javaClass.classLoader, root)
            try {
                val failures = manager.load(
                    listOf(
                        candidate("beta", BetaPlugin::class.java, "alpha"),
                        candidate("alpha", AlphaPlugin::class.java),
                    )
                )
                assertEquals(emptyMap<String, String>(), failures)
                block(manager, root)
            } finally {
                manager.disableAll()
            }
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `the phases run the load hooks of a plugin's beans provider first`(@TempDir directory: Path, env: Env) {
        withLoadedPlugins(directory) { manager, _ ->
            assertEquals(listOf("alpha:load", "beta:load"), FixtureLog.events)

            assertEquals(emptyMap<String, String>(), manager.enable())
            assertEquals(
                listOf("alpha:load", "beta:load", "alpha:enable", "beta:enable"),
                FixtureLog.events,
            )
        }

        assertEquals(
            listOf(
                "alpha:load", "beta:load", "alpha:enable", "beta:enable",
                "beta:disable", "beta:destroy", "alpha:disable", "alpha:destroy",
            ),
            FixtureLog.events,
            "the consumer is disabled and closed before its provider",
        )
    }

    @Test
    fun `an uncaught failure in a plugin's coroutine is logged with the plugin's id`(@TempDir directory: Path, env: Env) {
        withLoadedPlugins(directory) { manager, _ ->
            val alpha = checkNotNull(manager.plugin("alpha"))
            val failure = IllegalStateException("failure in a coroutine of alpha")

            val events = CapturedLog.during {
                runBlocking { alpha.scope.launch { throw failure }.join() }
            }

            val messages = events.filter { it.thrown === failure }.map { it.message.formattedMessage }
            assertEquals(1, messages.size, "the failure is logged once")
            assertTrue("'alpha'" in messages.single(), messages.single())
        }
    }

    @Test
    fun `a plugin injects the plugin api of its dependency and nothing else of it`(@TempDir directory: Path, env: Env) {
        withLoadedPlugins(directory) { _, root ->
            assertEquals("hello from alpha", BetaPlugin.greetingFromAlpha)
            assertEquals(false, BetaPlugin.alphaInternalsVisible, "alpha's private beans stay private")
            assertEquals(0, root.getBeanNamesForType(AlphaGreetingService::class.java).size, "the root holds no plugin bean")
            assertEquals(0, root.getBeanNamesForType(BetaService::class.java).size, "the root holds no plugin bean")
        }
    }

    @Test
    fun `the enable phase attaches event nodes, commands and event bus listeners and the disable phase removes them`(
        @TempDir directory: Path,
        env: Env,
    ) {
        withLoadedPlugins(directory) { manager, _ ->
            val beta = checkNotNull(manager.plugin("beta"))
            val commands = env.process().command()
            assertFalse(commands.commandExists(BETA_COMMAND), "no command is registered while loading")
            assertEquals(emptyList<String>(), beta.eventNode.children.map { it.name }, "nothing is attached while loading")
            FixtureEvent().call()
            assertEquals(0, BetaPlugin.surfEventsHandled, "no listener is registered while loading")

            manager.enable()
            assertEquals(listOf("beta-listeners"), beta.eventNode.children.map { it.name })
            assertTrue(commands.commandExists(BETA_COMMAND))
            FixtureEvent().call()
            assertEquals(1, BetaPlugin.surfEventsHandled)

            manager.disableAll()
            assertEquals(emptyList<String>(), beta.eventNode.children.map { it.name }, "the event node is removed")
            assertNull(beta.eventNode.parent, "the plugin's own node is removed")
            assertFalse(commands.commandExists(BETA_COMMAND), "the command is unregistered while the server still runs")
            FixtureEvent().call()
            assertEquals(1, BetaPlugin.surfEventsHandled, "the event bus listener is unregistered")
            assertEquals(emptyList<Any>(), manager.plugins.toList(), "the plugins are removed")
        }
    }
}
