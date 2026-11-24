package dev.slne.surf.api.paper.sharedbean.consumer

import dev.slne.surf.api.common.util.logger
import dev.slne.surf.api.spring.SurfLifecycle
import dev.slne.surf.redis.RedisApi
import dev.slne.surf.redis.libs.redisson.api.RedissonClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import java.util.*

/**
 * Proves that the Redis integration bundled into surf-api serves a plugin on a real server.
 *
 * The probe injects [RedisApi] and creates a sync structure while the context refreshes. surf-api
 * connects right after the refresh, so both checks of [onEnable] run against a live connection:
 *
 * - [syncValue]: a value written through the structure reads back locally and from Redis.
 * - [redisson]: the injected `RedissonClient` proxy forwards a command to the connected client.
 *
 * Both hold `ok` on success, and otherwise the failure.
 */
@Service
class ConsumerRedisProbe(
    redis: RedisApi,
    private val redissonClient: RedissonClient,
) : SurfLifecycle {

    private val log = logger()

    private val value = redis.createSyncValue<String>(VALUE_ID, "unset")

    @Volatile
    var syncValue = UNCHECKED

    @Volatile
    var redisson = UNCHECKED

    override suspend fun onEnable() {
        syncValue = check("sync value") { writeAndReadBackSyncValue() }
        redisson = check("redisson proxy") { incrementThroughProxy() }
    }

    private suspend fun writeAndReadBackSyncValue(): Boolean {
        val written = "probe-${UUID.randomUUID()}"
        value.setAndAwait(written)

        return value.get() == written && value.getRemote() == written
    }

    private suspend fun incrementThroughProxy(): Boolean {
        return withContext(Dispatchers.IO) {
            redissonClient.getAtomicLong(COUNTER_ID).incrementAndGet() > 0
        }
    }

    private suspend fun check(name: String, probe: suspend () -> Boolean): String {
        return try {
            if (probe()) "ok" else "mismatch"
        } catch (failure: Exception) {
            log.atWarning()
                .withCause(failure)
                .log("The redis probe '%s' failed", name)
            "failed:${failure.javaClass.simpleName}"
        }
    }

    private companion object {
        const val VALUE_ID = "surf-shared-bean-probe-value"
        const val COUNTER_ID = "surf-shared-bean-probe-counter"
        const val UNCHECKED = "unchecked"
    }
}
