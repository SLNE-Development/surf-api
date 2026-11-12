package dev.slne.surf.redis.util

import reactor.core.publisher.Mono

@InternalRedisAPI
interface Initializable {
    fun init(): Mono<Void>
}