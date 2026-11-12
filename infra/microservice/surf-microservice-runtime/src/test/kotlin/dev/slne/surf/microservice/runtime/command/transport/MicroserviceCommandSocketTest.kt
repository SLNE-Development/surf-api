package dev.slne.surf.microservice.runtime.command.transport

import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.deleteIfExists
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class MicroserviceCommandSocketTest {
    private val temporaryDirectory: Path = Files.createTempDirectory("sms")

    @AfterTest
    fun tearDown() {
        temporaryDirectory.deleteIfExists()
    }

    @Test
    fun `a short configured path is used unchanged`() {
        val configured = temporaryDirectory.resolve("c.sock")

        assertEquals(configured.toAbsolutePath(), MicroserviceCommandSocket.resolvePath(configured, temporaryDirectory))
    }

    @Test
    fun `a path longer than the socket limit falls back to the temporary directory`() {
        val deep = deepPath("microservice.sock")

        val resolved = MicroserviceCommandSocket.resolvePath(deep, temporaryDirectory)

        assertEquals(temporaryDirectory.toAbsolutePath(), resolved.parent)
        assertTrue(resolved.byteLength() <= MicroserviceCommandSocket.MAX_PATH_BYTES, "$resolved")
        assertEquals(resolved, MicroserviceCommandSocket.resolvePath(deep, temporaryDirectory), "fallback must be stable")
    }

    @Test
    fun `different long paths fall back to different sockets`() {
        val first = MicroserviceCommandSocket.resolvePath(deepPath("first.sock"), temporaryDirectory)
        val second = MicroserviceCommandSocket.resolvePath(deepPath("second.sock"), temporaryDirectory)

        assertNotEquals(first, second)
    }

    @Test
    fun `the limit counts UTF-8 bytes, not characters`() {
        val base = temporaryDirectory.toAbsolutePath().toString().length + 1
        val name = "ü".repeat((MicroserviceCommandSocket.MAX_PATH_BYTES - base) / 2 + 1)
        val configured = temporaryDirectory.resolve(name)
        assertTrue(configured.toAbsolutePath().toString().length <= MicroserviceCommandSocket.MAX_PATH_BYTES)

        val resolved = MicroserviceCommandSocket.resolvePath(configured, temporaryDirectory)

        assertNotEquals(configured.toAbsolutePath(), resolved)
    }

    @Test
    fun `fails when the fallback is too long as well`() {
        assertFailsWith<IllegalStateException> {
            MicroserviceCommandSocket.resolvePath(deepPath("c.sock"), deepPath("tmp"))
        }
    }

    @Test
    fun `the fallback of a deep path can be bound and connected`() {
        val resolved = MicroserviceCommandSocket.resolvePath(deepPath("microservice.sock"), temporaryDirectory)

        try {
            ServerSocketChannel.open(StandardProtocolFamily.UNIX).use { server ->
                server.bind(UnixDomainSocketAddress.of(resolved))
                SocketChannel.open(UnixDomainSocketAddress.of(resolved)).use { client ->
                    assertTrue(client.isConnected)
                }
            }
        } finally {
            resolved.deleteIfExists()
        }
    }

    private fun deepPath(name: String): Path =
        temporaryDirectory.resolve("a-deep-working-directory".repeat(6)).resolve("run").resolve(name)

    private fun Path.byteLength(): Int = toString().toByteArray(Charsets.UTF_8).size
}
