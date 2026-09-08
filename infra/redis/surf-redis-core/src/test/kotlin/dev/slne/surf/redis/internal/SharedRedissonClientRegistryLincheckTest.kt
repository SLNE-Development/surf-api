package dev.slne.surf.redis.internal

import org.jetbrains.lincheck.datastructures.ModelCheckingOptions
import org.jetbrains.lincheck.datastructures.Operation
import org.jetbrains.lincheck.datastructures.StressOptions
import org.jetbrains.lincheck.datastructures.Validate
import org.jetbrains.lincheck.datastructures.forClasses
import org.junit.jupiter.api.Tag
import org.redisson.api.RedissonClient
import org.redisson.api.RedissonReactiveClient
import org.redisson.codec.BaseEventCodec
import org.redisson.config.Config
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.concurrent.withLock
import kotlin.test.Test

/**
 * Verifies that acquiring and releasing shared Redisson clients is linearizable.
 *
 * The operations mirror real usage: every owner behaves like a `RedisApi`, holding at most one lease
 * behind its own lifecycle lock, while different owners connect and disconnect concurrently. Owners
 * 1 and 2 share key A, owner 3 uses key B. Every interleaving must behave like some sequential order
 * in which owners of one key join a single client, exactly the last disconnect shuts it down, and a
 * later connect creates a fresh client. Global counters are only inspected in [validate], after each
 * invocation, because the registry shuts a client down after leaving its lock, which is observable
 * but intentionally not atomic.
 */
@Tag("lincheck")
class SharedRedissonClientRegistryLincheckTest {
    private val connector = CountingConnector()
    private val registry = SharedRedissonClientRegistry(connector)
    private val config = Config()
    private val owners = listOf(Owner(KEY_A), Owner(KEY_A), Owner(KEY_B))

    init {
        // Every acquire and release logs at INFO; tens of thousands of invocations would otherwise
        // flood the captured test output.
        Logger.getLogger(SharedRedissonClientRegistry::class.java.packageName).level = Level.OFF
    }

    @Operation
    fun connect1(): String = connect(0)

    @Operation
    fun connect2(): String = connect(1)

    @Operation
    fun connect3(): String = connect(2)

    @Operation
    fun disconnect1(): String = disconnect(0)

    @Operation
    fun disconnect2(): String = disconnect(1)

    @Operation
    fun disconnect3(): String = disconnect(2)

    @Validate
    fun validate() {
        val live = registry.activeKeys()
        val expectedShutdowns = connector.createdCount - live.size
        check(connector.shutdowns.get() == expectedShutdowns) {
            "expected $expectedShutdowns shut down clients for ${connector.createdCount} created and ${live.size} live, got ${connector.shutdowns.get()}"
        }
        for (key in listOf(KEY_A, KEY_B)) {
            val held = owners.count { it.key == key && it.lease != null }
            check((held > 0) == (key in live)) { "key $key is held by $held owner(s) but live=${key in live}" }
            if (held > 0) {
                val clients = owners.filter { it.key == key }.mapNotNull { it.lease?.client }.toSet()
                check(clients.size == 1) { "owners of $key hold different clients: $clients" }
                check(!connector.isShutDown(clients.single().redisson)) { "live client of $key is shut down" }
            }
        }
    }

    /**
     * Stress mode drives real threads through the per-key locks and is the primary check here;
     * exhaustive model checking of the lock internals is too expensive for anything beyond the small
     * scenario in [modelChecking].
     */
    @Test
    fun stressTesting() {
        StressOptions()
            .iterations(40)
            .threads(3)
            .actorsPerThread(3)
            .invocationsPerIteration(2_000)
            .check(this::class)
    }

    @Test
    fun modelChecking() {
        ModelCheckingOptions()
            .iterations(6)
            .threads(2)
            .actorsPerThread(2)
            .invocationsPerIteration(500)
            .addGuarantee(
                forClasses("java.util.concurrent.ConcurrentHashMap")
                    .allMethods()
                    .treatAsAtomic(),
            )
            .addGuarantee(
                forClasses { className ->
                    className.startsWith("com.google.common.flogger.") ||
                            className.startsWith("dev.slne.surf.api.")
                }
                    .allMethods()
                    .ignore(),
            )
            .check(this::class)
    }

    private fun connect(index: Int): String {
        val owner = owners[index]
        return owner.lock.withLock {
            if (owner.lease != null) return "already"
            val lease = registry.acquire(owner.key, "owner-$index", config)
            owner.lease = lease
            "client#${connector.serialOf(lease.client.redisson)}"
        }
    }

    private fun disconnect(index: Int): String {
        val owner = owners[index]
        return owner.lock.withLock {
            val lease = owner.lease ?: return "none"
            owner.lease = null
            val last = lease.release()
            val serial = connector.serialOf(lease.client.redisson)
            if (last) "released#$serial:last" else "released#$serial"
        }
    }

    /** One simulated `RedisApi`: at most one lease, lifecycle serialized by [lock]. */
    private class Owner(val key: RedissonConnectionKey) {
        val lock = ReentrantLock()

        @Volatile
        var lease: SharedRedissonClientRegistry.Lease? = null
    }

    /**
     * Hands out pre-built fake clients in creation order so [serialOf] is a deterministic function of
     * the sequential history. Proxies are created here, outside the analyzed operations, because
     * proxy class generation is non-deterministic under model checking.
     */
    private class CountingConnector : RedissonConnector {
        val shutdowns = AtomicInteger()
        private val next = AtomicInteger()
        private val shutDown = Array(POOL_SIZE) { AtomicInteger() }

        val createdCount: Int get() = next.get()

        private val reactive = Proxy.newProxyInstance(
            RedissonReactiveClient::class.java.classLoader,
            arrayOf(RedissonReactiveClient::class.java)
        ) { _, method, _ -> if (method.name == "hashCode") 0 else null } as RedissonReactiveClient

        private val pool: List<RedissonClient> = List(POOL_SIZE) { serial ->
            Proxy.newProxyInstance(
                RedissonClient::class.java.classLoader,
                arrayOf(RedissonClient::class.java)
            ) { self, method, args ->
                when (method.name) {
                    "shutdown" -> {
                        shutDown[serial].incrementAndGet()
                        shutdowns.incrementAndGet()
                        null
                    }

                    "reactive" -> reactive
                    "hashCode" -> serial
                    "equals" -> args!![0] === self
                    "toString" -> "FakeRedissonClient#$serial"
                    else -> null
                }
            } as RedissonClient
        }

        fun serialOf(client: RedissonClient): Int = pool.indexOfFirst { it === client }

        fun isShutDown(client: RedissonClient): Boolean = shutDown[serialOf(client)].get() > 0

        override fun connect(config: Config): RedissonClient {
            val serial = next.getAndIncrement()
            check(serial < pool.size) { "fake client pool exhausted" }
            return pool[serial]
        }

        override fun detectOsType(redisson: RedissonClient): BaseEventCodec.OSType? = null
    }

    private companion object {
        /** Upper bound on clients one scenario can create: every actor may connect to a fresh client. */
        const val POOL_SIZE = 16

        val KEY_A = RedissonConnectionKey(false, false, "redis-a", 6379, null, null)
        val KEY_B = RedissonConnectionKey(false, false, "redis-b", 6379, null, null)
    }
}
