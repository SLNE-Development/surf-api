package dev.slne.surf.redis.util

import org.redisson.api.RStreamReactive
import org.redisson.api.stream.StreamMessageId
import org.redisson.api.stream.StreamRangeArgs
import reactor.core.publisher.Mono

fun <K : Any, V : Any> RStreamReactive<K, V>.fetchLatestStreamId(): Mono<StreamMessageId> {
    val args = StreamRangeArgs.startId(StreamMessageId.MAX)
        .endId(StreamMessageId.MIN)
        .count(1)

    return rangeReversed(args)
        .map { map -> map.keys.firstOrNull() ?: StreamMessageId(0, 0) }
        .defaultIfEmpty(StreamMessageId(0, 0))
        .onErrorReturn(StreamMessageId(0, 0))
}