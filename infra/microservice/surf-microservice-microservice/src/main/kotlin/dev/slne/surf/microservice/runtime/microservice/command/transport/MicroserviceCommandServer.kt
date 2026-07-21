package dev.slne.surf.microservice.runtime.microservice.command.transport

import dev.slne.surf.api.core.util.logger
import dev.slne.surf.microservice.api.microservice.command.*
import kotlinx.coroutines.*
import kotlinx.io.IOException
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.channels.Channels
import java.nio.channels.ClosedChannelException
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

internal class MicroserviceCommandServer(
    private val scope: CoroutineScope,
    private val commandManager: MicroserviceCommandManager,
    private val requestShutdown: () -> Unit,
    private val socketPath: Path = microserviceCommandSocketPath
) {
    private val log = logger()

    private val serverChannel = AtomicReference<ServerSocketChannel?>()
    private var acceptJob: Job? = null

    suspend fun start() {
        check(acceptJob == null) {
            "The microservice command server is already running."
        }

        val server = withContext(Dispatchers.IO) {
            socketPath.parent?.let(Files::createDirectories)
            Files.deleteIfExists(socketPath)

            val channel = ServerSocketChannel.open(StandardProtocolFamily.UNIX)
            try {
                channel.bind(UnixDomainSocketAddress.of(socketPath))

                setSocketPermissions(socketPath)

                channel
            } catch (exception: Throwable) {
                channel.close()
                Files.deleteIfExists(socketPath)
                throw exception
            }
        }

        serverChannel.set(server)

        acceptJob = scope.launch(Dispatchers.IO + CoroutineName("MicroserviceCommandServer")) {
            try {
                while (isActive) {
                    val client = try {
                        server.accept()
                    } catch (_: ClosedChannelException) {
                        break
                    }

                    launch(CoroutineName("MicroserviceCommandConnection")) {
                        handleClient(client)
                    }
                }
            } finally {
                serverChannel.compareAndSet(server, null)

                runCatching {
                    server.close()
                }

                runCatching {
                    Files.deleteIfExists(socketPath)
                }
            }
        }

        log.atInfo()
            .log("Microservice command socket listening at $socketPath")
    }

    suspend fun stop() {
        serverChannel.getAndSet(null)?.let { channel ->
            runCatching {
                channel.close()
            }
        }

        acceptJob?.cancelAndJoin()
        acceptJob = null

        withContext(Dispatchers.IO) {
            Files.deleteIfExists(socketPath)
        }
    }

    private suspend fun handleClient(channel: SocketChannel) {
        channel.use { client ->
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
                MicroserviceCommandProtocol.readRequest(input)
            } catch (exception: IOException) {
                output.sendError(
                    exception.message ?: "Invalid command request."
                )
                output.complete(2)
                return
            }

            val shutdownRequested = AtomicBoolean(false)

            val context = MicroserviceCommandContext(
                coroutineContext = currentCoroutineContext(),
                source = MicroserviceCommandSource.LOCAL_SOCKET,
                output = output,
                shutdownRequest = {
                    shutdownRequested.set(true)
                }
            )

            val result = commandManager.executeCommand(
                label = request.label,
                args = request.args,
                context = context
            )

            when (result) {
                MicroserviceCommandResult.Success -> {
                    output.complete(0)
                }

                is MicroserviceCommandResult.Failure -> {
                    output.sendError(result.message)
                    output.complete(1)
                }
            }

            if (shutdownRequested.get()) {
                requestShutdown()
            }
        }
    }

    private fun setSocketPermissions(path: Path) {
        try {
            Files.setPosixFilePermissions(
                path,
                setOf(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE
                )
            )
        } catch (exception: UnsupportedOperationException) {
            log.atWarning()
                .withCause(exception)
                .log("The file system does not support POSIX socket permissions.")
        }
    }

    private class SocketCommandOutput(
        private val output: DataOutputStream
    ) : MicroserviceCommandOutput {
        private val writeLock = Any()

        @Volatile
        private var failed = false

        override fun sendLine(message: String) {
            write {
                MicroserviceCommandProtocol.writeStandardOutput(
                    output,
                    message
                )
            }
        }

        override fun sendError(message: String) {
            write {
                MicroserviceCommandProtocol.writeStandardError(
                    output,
                    message
                )
            }
        }

        fun complete(exitCode: Int) {
            write {
                MicroserviceCommandProtocol.writeCompleted(
                    output,
                    exitCode
                )
            }
        }

        private inline fun write(block: () -> Unit) {
            if (failed) {
                return
            }

            synchronized(writeLock) {
                if (failed) {
                    return
                }

                try {
                    block()
                    output.flush()
                } catch (_: IOException) {
                    failed = true
                }
            }
        }
    }
}