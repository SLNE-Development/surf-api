package dev.slne.surf.redis.util

import dev.slne.surf.redis.RedisInstance
import dev.slne.surf.surfapi.core.api.util.logger
import org.redisson.api.RExpirableReactive
import reactor.core.Disposable
import reactor.core.publisher.Mono
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.toJavaDuration

object RedisExpirableUtils {
    private val log = logger()

    fun refreshContinuously(
        ttl: Duration,
        vararg objects: RExpirableReactive
    ): Disposable {
        val delay = (ttl.inWholeSeconds / 2).coerceIn(1, 15)
        val objectNames = objects.joinToString(", ") { it.name }

        return Mono.`when`(
            objects.map { it.expire(ttl.toJavaDuration()) }
        ).delayElement(java.time.Duration.ofSeconds(delay), RedisInstance.get().ttlRefreshScheduler)
            .onErrorResume { e ->
                log.atWarning()
                    .withCause(e)
                    .log("Failed to refresh TTL for Redis expirable objects: $objectNames")
                Mono.empty<Void>()
            }
            .repeat()
            .subscribe()
    }
}