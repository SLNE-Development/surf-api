package dev.slne.surf.redis.internal

import dev.slne.surf.api.core.util.logger
import dev.slne.surf.redis.util.InternalRedisAPI
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
import org.jetbrains.annotations.Blocking
import org.redisson.config.Config
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@InternalRedisAPI
class SharedRedissonClientRegistry(
    private val connector: RedissonConnector = RedissonConnector.DEFAULT,
) {
    private val entries = ConcurrentHashMap<RedissonConnectionKey, Entry>()

    @InternalRedisAPI
    inner class Lease internal constructor(
        private val entry: Entry,
        val client: SharedRedissonClient,
        val owner: String,
    ) {
        private val released = AtomicBoolean(false)

        val isReleased: Boolean get() = released.get()

        @Blocking
        fun release(): Boolean {
            return released.compareAndSet(false, true) && releaseLease(this, entry)
        }

        override fun toString(): String = "Lease(owner=$owner, client=$client, released=$isReleased)"
    }

    @Blocking
    fun acquire(key: RedissonConnectionKey, owner: String, config: Config): Lease {
        while (true) {
            val entry = entries.computeIfAbsent(key, ::Entry)
            tryAcquire(entry, owner, config)?.let { return it }
        }
    }

    fun activeKeys(): Set<RedissonConnectionKey> = entries.keys.toSet()

    fun ownersOf(key: RedissonConnectionKey): List<String> {
        val entry = entries[key] ?: return emptyList()
        return entry.lock.withLock { entry.leases.map { it.owner } }
    }

    @Blocking
    fun shutdownAll(): Map<RedissonConnectionKey, List<String>> {
        val leaked = Object2ObjectLinkedOpenHashMap<RedissonConnectionKey, List<String>>()
        val clients = ObjectArrayList<SharedRedissonClient>()

        for (entry in entries.values.toList()) {
            entry.lock.withLock {
                if (entry.closed) return@withLock
                val client = entry.client
                if (client != null) {
                    leaked[entry.key] = entry.leases.map { it.owner }
                    clients += client
                }
                closeLocked(entry)
            }
        }

        var failure: Throwable? = null
        for (client in clients) {
            try {
                shutdown(client, "platform shutdown")
            } catch (t: Throwable) {
                if (failure == null) failure = t else failure.addSuppressed(t)
            }
        }
        failure?.let { throw it }

        return leaked
    }

    private fun tryAcquire(entry: Entry, owner: String, config: Config): Lease? = entry.lock.withLock {
        if (entry.closed) return null

        val client = entry.client ?: createLocked(entry, config)
        val lease = Lease(entry, client, owner)
        entry.leases += lease

        log.atInfo()
            .log(
                "%s attached to shared Redisson client %s (%d owner(s))",
                owner,
                entry.key,
                entry.leases.size
            )
        lease
    }

    private fun createLocked(entry: Entry, config: Config): SharedRedissonClient {
        check(entry.leases.isEmpty()) { "Shared Redisson client entry has leases but no client" }

        val redisson = try {
            connector.connect(config)
        } catch (failure: Throwable) {
            closeLocked(entry)
            throw failure
        }

        val client = try {
            SharedRedissonClient(
                key = entry.key,
                redisson = redisson,
                redissonReactive = redisson.reactive(),
                redisOsType = connector.detectOsType(redisson),
            )
        } catch (failure: Throwable) {
            closeLocked(entry)
            try {
                redisson.shutdown()
            } catch (shutdownFailure: Throwable) {
                failure.addSuppressed(shutdownFailure)
            }
            throw failure
        }

        entry.client = client
        log.atInfo().log("Created shared Redisson client %s", entry.key)
        return client
    }

    private fun releaseLease(lease: Lease, entry: Entry): Boolean {
        val orphaned = entry.lock.withLock {
            if (!entry.leases.remove(lease)) return false
            if (entry.leases.isNotEmpty()) {
                log.atInfo()
                    .log(
                        "%s detached from shared Redisson client %s (%d owner(s) left)",
                        lease.owner,
                        entry.key,
                        entry.leases.size
                    )
                return false
            }
            closeLocked(entry)
            entry.client
        } ?: return false

        shutdown(orphaned, "last owner ${lease.owner} released it")
        return true
    }

    private fun closeLocked(entry: Entry) {
        entry.closed = true
        entry.leases.clear()
        entries.remove(entry.key, entry)
    }

    @Blocking
    private fun shutdown(client: SharedRedissonClient, reason: String) {
        log.atInfo().log("Shutting down shared Redisson client %s (%s)", client.key, reason)
        client.redisson.shutdown()
    }

    @InternalRedisAPI
    internal class Entry(val key: RedissonConnectionKey) {
        val lock = ReentrantLock()
        var client: SharedRedissonClient? = null
        val leases = ArrayList<Lease>()
        var closed = false
    }

    @InternalRedisAPI
    companion object {
        private val log = logger()

        val instance = SharedRedissonClientRegistry()
    }
}
