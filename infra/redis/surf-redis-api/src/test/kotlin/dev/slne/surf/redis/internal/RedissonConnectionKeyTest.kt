package dev.slne.surf.redis.internal

import org.redisson.misc.RedisURI
import kotlin.test.*

class RedissonConnectionKeyTest {
    @Test
    fun `same endpoint and credentials are equivalent`() {
        val first = RedissonConnectionKey.of(RedisURI("redis://user:secret@redis.internal:6379"))
        val second = RedissonConnectionKey.of(RedisURI("redis://user:secret@redis.internal:6379"))

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
    }

    @Test
    fun `redis and valkey schemes address the same connection`() {
        assertEquals(
            RedissonConnectionKey.of(RedisURI("redis://redis.internal:6379")),
            RedissonConnectionKey.of(RedisURI("valkey://redis.internal:6379")),
        )
    }

    @Test
    fun `credentials separate connections even though RedisURI ignores them`() {
        val plain = RedisURI("redis://redis.internal:6379")
        val withPassword = RedisURI("redis://secret@redis.internal:6379")
        val otherPassword = RedisURI("redis://other@redis.internal:6379")
        val withUser = RedisURI("redis://user:secret@redis.internal:6379")

        assertEquals(plain, withPassword, "precondition: RedisURI equality ignores credentials")
        assertNotEquals(RedissonConnectionKey.of(plain), RedissonConnectionKey.of(withPassword))
        assertNotEquals(RedissonConnectionKey.of(withPassword), RedissonConnectionKey.of(otherPassword))
        assertNotEquals(RedissonConnectionKey.of(withPassword), RedissonConnectionKey.of(withUser))
    }

    @Test
    fun `host port and tls separate connections`() {
        val base = RedissonConnectionKey.of(RedisURI("redis://redis.internal:6379"))

        assertNotEquals(base, RedissonConnectionKey.of(RedisURI("redis://redis.internal:6380")))
        assertNotEquals(base, RedissonConnectionKey.of(RedisURI("redis://other.internal:6379")))
        assertNotEquals(base, RedissonConnectionKey.of(RedisURI("rediss://redis.internal:6379")))
    }

    @Test
    fun `toString redacts the password`() {
        val key = RedissonConnectionKey.of(RedisURI("rediss://user:secret@redis.internal:6379"))
        val rendered = key.toString()

        assertEquals("rediss://user:***@redis.internal:6379", rendered)
        assertFalse(rendered.contains("secret"))
        assertEquals("redis://redis.internal:6379", RedissonConnectionKey.of(RedisURI("redis://redis.internal:6379")).toString())
    }
}
