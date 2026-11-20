package dev.slne.surf.api.redis

import org.junit.jupiter.api.Test
import org.redisson.api.RedissonClient
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RedisClientProxyTest {

    private val target = RecordingClient()
    private var connected = false
    private val client = RedisClientProxy.create(
        RedissonClient::class.java,
        "demo",
        isConnected = { connected },
        target = { target.client },
    )

    @Test
    fun `a call before the connection opens fails and names the plugin`() {
        val failure = assertFailsWith<IllegalStateException> { client.isShutdown }

        assertEquals("Redis is not connected yet for plugin demo", failure.message)
        assertTrue(target.calls.isEmpty())
    }

    @Test
    fun `a call while connected reaches the target with its arguments and result`() {
        connected = true

        assertTrue(client.isShutdown)
        client.getBucket<String>("key")

        assertEquals(listOf("isShutdown" to emptyList<Any?>(), "getBucket" to listOf<Any?>("key")), target.calls)
    }

    @Test
    fun `a call after the connection closed fails again`() {
        connected = true
        client.isShutdown
        connected = false

        assertFailsWith<IllegalStateException> { client.isShutdown }
    }

    @Test
    fun `an exception of the target is rethrown unwrapped`() {
        connected = true
        target.failure = IllegalArgumentException("boom")

        val failure = assertFailsWith<IllegalArgumentException> { client.isShutdown }

        assertEquals("boom", failure.message)
    }

    @Test
    fun `shutdown is refused, connected or not`() {
        assertFailsWith<UnsupportedOperationException> { client.shutdown() }

        connected = true
        assertFailsWith<UnsupportedOperationException> { client.shutdown(0, 1, TimeUnit.SECONDS) }
        assertTrue(target.calls.isEmpty())
    }

    @Test
    fun `equals, hashCode and toString work while disconnected and never reach the target`() {
        val other = RedisClientProxy.create(RedissonClient::class.java, "demo", { false }, { target.client })

        assertEquals(client, client)
        assertNotEquals(client, other)
        assertEquals(System.identityHashCode(client), client.hashCode())
        assertEquals("RedissonClient proxy of plugin demo", client.toString())
        assertTrue(target.calls.isEmpty())
    }

    private class RecordingClient {
        val calls = mutableListOf<Pair<String, List<Any?>>>()
        var failure: Throwable? = null

        val client: RedissonClient = Proxy.newProxyInstance(
            RedissonClient::class.java.classLoader,
            arrayOf(RedissonClient::class.java),
            InvocationHandler { _, method, arguments ->
                calls += method.name to (arguments?.toList() ?: emptyList())
                failure?.let { throw it }
                if (method.returnType == Boolean::class.javaPrimitiveType) true else null
            },
        ) as RedissonClient
    }
}
