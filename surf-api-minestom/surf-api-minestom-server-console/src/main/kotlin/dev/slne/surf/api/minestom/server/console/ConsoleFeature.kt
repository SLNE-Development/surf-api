package dev.slne.surf.api.minestom.server.console

import dev.slne.surf.api.minestom.extension.SchedulerManager
import dev.slne.surf.api.minestom.server.*
import net.minecrell.terminalconsole.SimpleTerminalConsole
import net.minestom.server.MinecraftServer

/**
 * Configures [withConsole].
 */
@SurfMinestomServerDsl
class ConsoleFeatureBuilder internal constructor() {

    /**
     * The name of the thread reading the console.
     */
    var threadName: String = "surf-minestom-console"

    /**
     * Runs on the console thread when the console asks the server to stop, e.g. on Ctrl+C or end
     * of input.
     *
     * Calls [exitFlushingLogs] by default: stops the server with [SurfMinestomServer.stop], which
     * also runs its shutdown actions, flushes the logs and exits the process. The console thread is
     * no tick thread, so it may block; this also works when the api is already stopped.
     */
    var onShutdown: () -> Unit = { exitFlushingLogs(0) }
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

    override fun enable(api: SurfMinestomServer) {
        val console = Thread(SurfTerminalConsole(settings.onShutdown)::start, settings.threadName).apply {
            isDaemon = true
        }

        SchedulerManager.scheduleNextTick(console::start)
    }

    override fun disable(api: SurfMinestomServer) {
        // The console thread is not interrupted: the shutdown may have been started from it. The
        // daemon thread stops once the server is no longer started.
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
