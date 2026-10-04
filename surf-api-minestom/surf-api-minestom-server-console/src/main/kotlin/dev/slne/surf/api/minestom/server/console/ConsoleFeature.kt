package dev.slne.surf.api.minestom.server.console

import dev.slne.surf.api.minestom.server.SurfMinestomServer
import dev.slne.surf.api.minestom.server.SurfMinestomServerBuilder
import dev.slne.surf.api.minestom.server.SurfMinestomServerDsl
import dev.slne.surf.api.minestom.server.SurfMinestomFeature
import net.minecrell.terminalconsole.SimpleTerminalConsole
import net.minestom.server.MinecraftServer
import kotlin.system.exitProcess

/**
 * Configures [withConsole].
 */
@SurfMinestomServerDsl
class ConsoleFeatureBuilder internal constructor() {

    /** The name of the thread reading the console. */
    var threadName: String = "surf-minestom-console"

    /**
     * Runs when the console asks the server to stop, e.g. on Ctrl+C or end of input.
     *
     * Stops the server and exits the process by default.
     */
    var onShutdown: () -> Unit = {
        MinecraftServer.stopCleanly()
        exitProcess(0)
    }
}

/**
 * Reads commands from the terminal and runs them as the console, with line editing and history.
 *
 * Commands reach Minestom's command manager, so CommandAPI commands work as well. For colored
 * output that does not interleave with the input line, log through Log4j's `TerminalConsole`
 * appender, e.g. with a `log4j2.xml` such as:
 *
 * ```
 * <Appenders>
 *     <TerminalConsole name="Console">
 *         <PatternLayout pattern="%highlightError{[%d{HH:mm:ss} %level]: %msg%n%xEx}"/>
 *     </TerminalConsole>
 * </Appenders>
 * ```
 */
class ConsoleFeature internal constructor(
    private val settings: ConsoleFeatureBuilder,
) : SurfMinestomFeature {

    override val id: String = ID

    private var thread: Thread? = null

    override fun enable(api: SurfMinestomServer) {
        thread = Thread(SurfTerminalConsole(settings.onShutdown)::start, settings.threadName).apply {
            isDaemon = true
            start()
        }
    }

    override fun disable(api: SurfMinestomServer) {
        // The thread is a daemon and stops reading once the server no longer runs. It is not
        // interrupted, since the shutdown itself may have been started from the console.
        thread = null
    }

    private class SurfTerminalConsole(
        private val shutdownHandler: () -> Unit,
    ) : SimpleTerminalConsole() {
        override fun isRunning(): Boolean = MinecraftServer.isStarted()

        override fun runCommand(command: String) {
            val commandManager = MinecraftServer.getCommandManager()
            commandManager.execute(commandManager.consoleSender, command)
        }

        override fun shutdown() = shutdownHandler()
    }

    companion object {
        const val ID = "console"
    }
}

/**
 * Reads commands from the terminal and runs them as the console.
 *
 * @see ConsoleFeature
 */
fun SurfMinestomServerBuilder.withConsole(block: ConsoleFeatureBuilder.() -> Unit = {}) {
    install(ConsoleFeature(ConsoleFeatureBuilder().apply(block)))
}
