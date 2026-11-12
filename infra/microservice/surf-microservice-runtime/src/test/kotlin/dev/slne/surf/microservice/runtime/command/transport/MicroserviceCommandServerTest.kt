package dev.slne.surf.microservice.runtime.command.transport

import dev.slne.surf.microservice.api.command.MicroserviceCommand
import dev.slne.surf.microservice.api.command.MicroserviceCommandContext
import dev.slne.surf.microservice.runtime.command.MicroserviceCommandDispatcher
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterEach
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.channels.Channels
import java.nio.channels.SocketChannel
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * Speaks the wire format of `microservicectl` (`surf-microservice-gradle-plugin/src/main/resources/microservicectl`).
 */
class MicroserviceCommandServerTest {
    private val socketDirectory: Path = Files.createTempDirectory("sms")
    private val socketPath: Path = socketDirectory.resolve("c.sock")
    private val uncaught = CopyOnWriteArrayList<Throwable>()
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, exception -> uncaught += exception },
    )
    private val shutdownRequest = CompletableDeferred<Unit>()
    private val floodLinesSent = AtomicInteger()
    private val floodFinished = AtomicBoolean()
    private val lingerStarted = CompletableDeferred<Unit>()
    private val lingerCleanedUp = AtomicBoolean()
    private val stuckStarted = CompletableDeferred<Unit>()
    private val stuckRelease = CompletableDeferred<Unit>()

    private val commands = listOf(
        command("echo") { args -> args.forEach(::sendLine) },
        command("fails") { args ->
            sendError("detail")
            fail("failed with ${args.size} arguments")
        },
        command("exit") {
            sendLine("bye")
            requestShutdown()
        },
        command("crash") { throw InternalError("crashed") },
        command("linger") {
            lingerStarted.complete(Unit)
            try {
                awaitCancellation()
            } finally {
                withContext(NonCancellable) {
                    delay(100)
                    lingerCleanedUp.set(true)
                }
            }
        },
        command("stuck") {
            stuckStarted.complete(Unit)
            withContext(NonCancellable) { stuckRelease.await() }
        },
        command("flood") {
            try {
                repeat(FLOOD_LINE_COUNT) {
                    sendLine(FLOOD_LINE)
                    floodLinesSent.incrementAndGet()
                }
            } finally {
                floodFinished.set(true)
            }
        },
    )
    private val server = MicroserviceCommandServer(
        dispatcher = MicroserviceCommandDispatcher { label -> commands.firstOrNull { it.matches(label) } },
        requestShutdown = { shutdownRequest.complete(Unit) },
        socketPath = socketPath,
        stopTimeout = STOP_TIMEOUT,
    )

    @AfterEach
    fun tearDown() {
        stuckRelease.complete(Unit)
        runBlocking { server.stop() }
        scope.cancel()
        socketDirectory.deleteIfExists()
    }

    @Test
    fun `streams the output and exits with 0`() = runBlocking {
        server.start(scope)

        val response = send(listOf("echo", "a b", "ü"))

        assertEquals(Response(lines = listOf("OUT a b", "OUT ü"), exitCode = 0), response)
    }

    @Test
    fun `reports a failed command on standard error and exits with 1`() = runBlocking {
        server.start(scope)

        val response = send(listOf("fails", "x"))

        assertEquals(Response(listOf("ERR detail", "ERR failed with 1 arguments"), exitCode = 1), response)
    }

    @Test
    fun `reports an unknown command and exits with 1`() = runBlocking {
        server.start(scope)

        assertEquals(1, send(listOf("missing")).exitCode)
    }

    @Test
    fun `rejects a request with a wrong header and exits with 2`() = runBlocking {
        server.start(scope)

        val response = exchange { output -> output.writeInt(0x12345678) }

        assertEquals(2, response.exitCode)
    }

    @Test
    fun `requests the shutdown after the response`() = runBlocking {
        server.start(scope)

        val response = send(listOf("exit"))

        assertEquals(Response(listOf("OUT bye"), exitCode = 0), response)
        withTimeout(5.seconds) { shutdownRequest.await() }
    }

    @Test
    fun `keeps serving after a command throws a virtual machine error`() = runBlocking {
        server.start(scope)

        assertFailsWith<EOFException> { send(listOf("crash")) }

        assertEquals(0, send(listOf("echo", "still there")).exitCode)
        assertEquals(listOf("crashed"), uncaught.map { it.message })
    }

    @Test
    fun `stops without waiting for a client that sends nothing`() = runBlocking {
        server.start(scope)

        connect().use {
            delay(200)

            withTimeout(5.seconds) { server.stop() }
        }
    }

    @Test
    fun `stops without waiting for a client that does not read a large response`() = runBlocking {
        server.start(scope)

        connect().use { client ->
            writeRequest(DataOutputStream(Channels.newOutputStream(client)), listOf("flood"))
            awaitStalledFlood()

            withTimeout(5.seconds) { server.stop() }

            // Without closing the connection, stop() only returns after STOP_TIMEOUT with the write still blocked.
            assertTrue(floodFinished.get(), "stop returned while the response write was still blocked")
        }
    }

    @Test
    fun `waits for a cancelled command to finish on stop`() = runBlocking {
        server.start(scope)

        connect().use { client ->
            writeRequest(DataOutputStream(Channels.newOutputStream(client)), listOf("linger"))
            withTimeout(EXCHANGE_TIMEOUT) { lingerStarted.await() }

            server.stop()

            assertTrue(lingerCleanedUp.get(), "stop returned before the command finished")
        }
    }

    @Test
    fun `stops after the timeout when a command ignores the cancellation`() = runBlocking {
        server.start(scope)

        connect().use { client ->
            writeRequest(DataOutputStream(Channels.newOutputStream(client)), listOf("stuck"))
            withTimeout(EXCHANGE_TIMEOUT) { stuckStarted.await() }

            withTimeout(5.seconds) { server.stop() }
        }
    }

    @Test
    fun `deletes the socket file on stop`() = runBlocking {
        server.start(scope)
        server.stop()

        assertFalse(socketPath.exists())
    }

    private suspend fun send(command: List<String>) = exchange { output -> writeRequest(output, command) }

    private fun writeRequest(output: DataOutputStream, command: List<String>) {
        output.writeInt(MAGIC)
        output.writeInt(VERSION)
        output.writeInt(command.size)
        command.forEach { argument ->
            val bytes = argument.toByteArray(Charsets.UTF_8)
            output.writeInt(bytes.size)
            output.write(bytes)
        }
        output.flush()
    }

    private fun connect(): SocketChannel = SocketChannel.open(StandardProtocolFamily.UNIX).apply {
        connect(UnixDomainSocketAddress.of(socketPath))
    }

    /**
     * Waits until the flood command blocks on a full socket buffer.
     */
    private suspend fun awaitStalledFlood() = withTimeout(EXCHANGE_TIMEOUT) {
        var previous = -1
        while (true) {
            val current = floodLinesSent.get()
            if (current == previous && current > 0) break
            previous = current
            delay(300)
        }
        check(floodLinesSent.get() < FLOOD_LINE_COUNT) { "The flood command finished without blocking." }
    }

    private suspend fun exchange(writeRequest: (DataOutputStream) -> Unit): Response = withTimeout(EXCHANGE_TIMEOUT) {
        runInterruptible(Dispatchers.IO) {
            connect().use { channel ->
                val output = DataOutputStream(Channels.newOutputStream(channel))
                writeRequest(output)
                output.flush()
                readResponse(DataInputStream(BufferedInputStream(Channels.newInputStream(channel))))
            }
        }
    }

    private fun readResponse(input: DataInputStream): Response {
        val lines = mutableListOf<String>()
        while (true) {
            when (val type = input.readByte().toInt()) {
                STANDARD_OUTPUT -> lines += "OUT ${readString(input)}"
                STANDARD_ERROR -> lines += "ERR ${readString(input)}"
                COMPLETED -> return Response(lines, input.readInt())
                else -> error("Unknown response type $type")
            }
        }
    }

    private fun readString(input: DataInputStream): String {
        val bytes = ByteArray(input.readInt())
        input.readFully(bytes)
        return String(bytes, Charsets.UTF_8)
    }

    private fun command(name: String, block: suspend MicroserviceCommandContext.(List<String>) -> Unit) =
        object : MicroserviceCommand(name) {
            override suspend fun MicroserviceCommandContext.execute(args: List<String>) = block(args)
        }

    private data class Response(val lines: List<String>, val exitCode: Int)

    private companion object {
        const val MAGIC = 0x53555246
        const val VERSION = 1
        const val STANDARD_OUTPUT = 1
        const val STANDARD_ERROR = 2
        const val COMPLETED = 3
        const val FLOOD_LINE_COUNT = 10_000
        val FLOOD_LINE = "x".repeat(64 * 1024)
        val EXCHANGE_TIMEOUT = 10.seconds
        val STOP_TIMEOUT = 1.seconds
    }
}
