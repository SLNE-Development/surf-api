package dev.slne.surf.api.paper.scenario.test

import dev.slne.surf.api.paper.scenario.probe.ProbeProtocol
import dev.slne.surf.api.paper.server.harness.BrokerContainers
import dev.slne.surf.api.paper.server.harness.PaperServer
import dev.slne.surf.api.paper.server.harness.PaperServerInputs
import dev.slne.surf.api.paper.server.harness.ServerDirectory

/**
 * A running server with surf-api and the probe plugin, its brokers, and the bots connected to it.
 *
 * One instance serves a whole test class: booting a server takes most of a minute. Every
 * [ScenarioServer] starts on a fresh world, so nothing a previous run left in the world or the
 * playerdata leaks into a scenario. [stop], [kill] and [start] keep the world, as a real restart
 * or crash would.
 */
class ScenarioServer private constructor(
    val server: PaperServer,
    private val brokers: BrokerContainers,
) : AutoCloseable {

    val probe = Probe(server)

    private val bots = mutableListOf<Bot>()

    /**
     * The world's name, as the probe's position-based requests take it.
     */
    val worldName: String = WORLD

    /**
     * Joins a bot as [name] and waits until the server saw it join.
     */
    fun connect(name: String): Bot {
        val from = server.mark()
        val bot = Bot.connect(name, server.directory.port)

        bots += bot
        probe.awaitEvent(ProbeProtocol.JOIN, name, from)

        return bot
    }

    /**
     * Starts the server again on the same world after [stop] or [kill].
     */
    fun start() {
        server.start()
        server.awaitStarted()
    }

    /**
     * Stops the server normally, as the `stop` command does.
     */
    fun stop() {
        server.stop()
    }

    /**
     * Kills the server process without letting the server save, as a crash does.
     */
    fun kill() {
        server.kill()
    }

    override fun close() {
        for (bot in bots) {
            bot.close()
        }

        server.stop()
        brokers.close()
    }

    companion object {

        private const val WORLD = "world"

        /**
         * A flat, peaceful world without structures, so no mob, terrain or loot interferes with
         * the items a scenario counts, and room for several bots.
         */
        private val PROPERTIES = mapOf(
            "level-name" to WORLD,
            "level-type" to "minecraft:flat",
            "generate-structures" to "false",
            "difficulty" to "peaceful",
            "max-players" to "20",
            "motd" to "surf-api scenario test",
        )

        /**
         * What makes a log line worth quoting in a failure.
         */
        private val INTERESTING = listOf(
            "surf-paper-api",
            "SurfScenarioProbe",
            ProbeProtocol.MARKER,
            "PluginContextException",
            "UnsatisfiedDependencyException",
            "lost connection",
        )

        /**
         * Assembles the server from the inputs the build handed over, starts its brokers and the
         * server, and waits until it finished starting.
         */
        fun start(): ScenarioServer {
            val inputs = PaperServerInputs.fromSystemProperties()
            val directory = ServerDirectory.assemble(
                inputs.workDir.resolve("server"),
                inputs.serverJar(),
                inputs.thirdPartyPlugins() + inputs.pluginJars(),
                properties = PROPERTIES,
                freshWorld = true,
            )
            val brokers = BrokerContainers.start()
            val server = PaperServer(
                directory = directory,
                javaLauncher = inputs.javaLauncher,
                jvmArgs = inputs.jvmArgs,
                environment = brokers.environment,
                interesting = INTERESTING,
            )
            val scenario = ScenarioServer(server, brokers)

            try {
                scenario.start()
            } catch (failure: Throwable) {
                scenario.close()
                throw failure
            }

            return scenario
        }
    }
}
