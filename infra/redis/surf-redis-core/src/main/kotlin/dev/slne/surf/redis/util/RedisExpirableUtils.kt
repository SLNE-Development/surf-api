package dev.slne.surf.redis.util

import dev.slne.surf.redis.RedisInstance
import dev.slne.surf.surfapi.core.api.util.logger
import org.redisson.api.RExpirableReactive
import reactor.core.Disposable
import reactor.core.Disposables
import reactor.core.publisher.Mono
import kotlin.time.Duration
import kotlin.time.toJavaDuration

object RedisExpirableUtils {
    private val log = logger()

    fun refreshContinuously(
        ttl: Duration,
        vararg objects: RExpirableReactive
    ): Disposable {
        if (ttl == Duration.ZERO || ttl.isNegative()) return Disposables.disposed()
        val delay = Math.clamp(ttl.inWholeSeconds - 2, 1L, 15L)
        val objectNames = objects.joinToString(", ") { it.name }

        return Mono.defer {
            Mono.`when`(objects.map { it.expire(ttl.toJavaDuration()) }).then()
        }
            .onErrorResume { e ->
                log.atWarning()
                    .withCause(e)
                    .log("Failed to refresh TTL for Redis expirable objects: $objectNames")
                Mono.empty()
            }
            .then(Mono.delay(java.time.Duration.ofSeconds(delay), RedisInstance.get().ttlRefreshScheduler))
            .repeat()
            .subscribe()
    }
}