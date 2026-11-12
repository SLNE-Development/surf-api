package dev.slne.surf.microservice.runtime.command.transport

import dev.slne.surf.api.common.util.logger
import dev.slne.surf.microservice.api.command.MicroserviceCommandOutput
import dev.slne.surf.microservice.api.command.MicroserviceCommandResult
import dev.slne.surf.microservice.api.command.MicroserviceCommandSource
import dev.slne.surf.microservice.runtime.command.MicroserviceCommandDispatcher
import kotlinx.coroutines.*
import org.jetbrains.annotations.Blocking
import java.io.*
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.channels.Channels
import java.nio.channels.ClosedChannelException
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration

/**
 * Serves `microservicectl` on a Unix domain socket at [socketPath], restricted to its owner where the file system
 * supports POSIX permissions.
 *
 * Each connection carries one request and receives the command output followed by an exit code: `0` on success,
 * `1` on a failed command and `2` on a malformed request. A shutdown requested by a command is passed to
 * [requestShutdown] only after the exit code was written.
 *
 * [start] and [stop] must not run concurrently. [stop] closes the socket, cancels running invocations and closes every
 * open connection, so a response write blocked by a client that stops reading fails instead of blocking the
 * shutdown. It then waits up to [stopTimeout] for the invocations to finish and deletes the socket file.
 */
internal class MicroserviceCommandServer(
    private val dispatcher: MicroserviceCommandDispatcher,
    private val requestShutdown: () -> Unit,
    private val socketPath: Path,
    private val stopTimeout: Duration,
) {
    private val log = logger()
    private val clients = ConcurrentHashMap.newKeySet<SocketChannel>()

    private var serverChannel: ServerSocketChannel? = null
    private var acceptJob: Job? = null

    suspend fun start(scope: CoroutineScope) {
        check(acceptJob == null) { "The microservice command server is already running." }

        val server = withContext(Dispatchers.IO) { bind() }
        serverChannel = server
        acceptJob = scope.launch(CoroutineName("MicroserviceCommandServer")) {
            server.use {
                acceptConnections(it)
            }
        }

        log.atInfo()
            .log("Microservice command socket listening at %s", socketPath)
    }

    suspend fun stop() {
        withContext(Dispatchers.IO) { serverChannel?.close() }
        serverChannel = null

        val connections = acceptJob
        acceptJob = null
        connections?.cancel()
        clients.forEach(::closeQuietly)
        connections?.let { awaitConnections(it) }

        withContext(Dispatchers.IO) { Files.deleteIfExists(socketPath) }
    }

    private suspend fun awaitConnections(connections: Job) {
        withTimeoutOrNull(stopTimeout) { connections.join() }
            ?: log.atWarning()
                .log("Socket commands did not finish within %s after cancellation.", stopTimeout)
    }

    @Blocking
    private fun bind(): ServerSocketChannel {
        socketPath.parent?.let(Files::createDirectories)
        Files.deleteIfExists(socketPath)

        val channel = ServerSocketChannel.open(StandardProtocolFamily.UNIX)
        try {
            channel.bind(UnixDomainSocketAddress.of(socketPath))
            restrictToOwner(socketPath)
            return channel
        } catch (exception: Throwable) {
            channel.close()
            Files.deleteIfExists(socketPath)
            throw exception
        }
    }

    /**
     * Serves each connection in its own child. A connection that fails does not end the loop, and its channel is
     * closed even if the child is cancelled before it starts.
     *
     * A channel is tracked before its child is launched. [stop] cancels this loop before closing the tracked channels,
     * so a channel tracked after that sweep belongs to a child that never runs and is closed on its completion.
     */
    private suspend fun acceptConnections(server: ServerSocketChannel) = supervisorScope {
        while (isActive) {
            val client = try {
                runInterruptible(Dispatchers.IO) {
                    server.accept()
                }
            } catch (_: ClosedChannelException) {
                return@supervisorScope
            }

            clients.add(client)
            val connectionJob = launch(CoroutineName("MicroserviceCommandConnection")) {
                handleClient(client)
            }
            connectionJob.invokeOnCompletion {
                clients.remove(client)
                closeQuietly(client)
            }
        }
    }

    private suspend fun handleClient(client: SocketChannel) {
        val input = DataInputStream(
            BufferedInputStream(
                Channels.newInputStream(client)
            )
        )
        val output = SocketCommandOutput(
            DataOutputStream(
                BufferedOutputStream(
                    Channels.newOutputStream(client)
                )
            )
        )

        val request = try {
            runInterruptible(Dispatchers.IO) {
                MicroserviceCommandProtocol.readRequest(input)
            }
        } catch (exception: IOException) {
            output.sendError(exception.message ?: "Invalid command request.")
            output.complete(EXIT_INVALID_REQUEST)
            return
        }

        val dispatch = dispatcher.dispatch(
            request.label,
            request.args,
            MicroserviceCommandSource.LOCAL_SOCKET,
            output
        )

        withContext(Dispatchers.IO) {
            when (val result = dispatch.result) {
                MicroserviceCommandResult.Success -> output.complete(EXIT_SUCCESS)
                is MicroserviceCommandResult.Failure -> {
                    output.sendError(result.message)
                    output.complete(EXIT_FAILURE)
                }
            }
        }

        if (dispatch.shutdownRequested) {
            requestShutdown()
        }
    }

    private fun restrictToOwner(path: Path) {
        try {
            Files.setPosixFilePermissions(
                path,
                setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE)
            )
        } catch (_: UnsupportedOperationException) {
            log.atInfo().log(
                "The file system does not support POSIX permissions; %s keeps the default ones.",
                path
            )
        }
    }

    private fun closeQuietly(client: SocketChannel) {
        try {
            client.close()
        } catch (exception: IOException) {
            log.atFine().withCause(exception).log("Failed to close a command connection")
        }
    }

    /**
     * Writes response frames. After the first I/O error, for example a disconnected client, it drops all output.
     */
    private class SocketCommandOutput(private val output: DataOutputStream) :
        MicroserviceCommandOutput {
        private val writeLock = Any()

        @Volatile
        private var failed = false

        override fun sendLine(message: String) = write {
            MicroserviceCommandProtocol.writeStandardOutput(output, message)
        }

        override fun sendError(message: String) = write {
            MicroserviceCommandProtocol.writeStandardError(output, message)
        }

        fun complete(exitCode: Int) = write {
            MicroserviceCommandProtocol.writeCompleted(output, exitCode)
        }

        private inline fun write(block: () -> Unit) {
            if (failed) return

            synchronized(writeLock) {
                if (failed) return
                try {
                    block()
                    output.flush()
                } catch (_: IOException) {
                    failed = true
                }
            }
        }
    }

    private companion object {
        const val EXIT_SUCCESS = 0
        const val EXIT_FAILURE = 1
        const val EXIT_INVALID_REQUEST = 2
    }
}
