package dev.slne.surf.redis.util

import dev.slne.surf.redis.RedisInstance
import org.redisson.api.RStreamReactive
import org.redisson.api.stream.StreamMessageId
import org.redisson.api.stream.StreamRangeArgs
import org.redisson.api.stream.StreamReadArgs
import reactor.core.Disposable
import reactor.core.publisher.Mono
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

fun <K : Any, V : Any> RStreamReactive<K, V>.fetchLatestStreamId(): Mono<StreamMessageId> {
    val args = StreamRangeArgs.startId(StreamMessageId.MAX)
        .endId(StreamMessageId.MIN)
        .count(1)

    return rangeReversed(args)
        .map { map -> map.keys.firstOrNull() ?: StreamMessageId(0, 0) }
        .defaultIfEmpty(StreamMessageId(0, 0))
        .onErrorReturn(StreamMessageId(0, 0))
}

fun <K : Any, V : Any> RStreamReactive<K, V>.pollContinuously(
    cursorId: AtomicReference<StreamMessageId>,
    pollInterval: Duration = 250.milliseconds,
    count: Int = 200,
    handler: Result<Map<StreamMessageId, Map<K, V>>>.() -> Unit,
): Disposable = Mono.defer<Void> {
    val args = StreamReadArgs.greaterThan(cursorId.get())
        .count(count)

    read(args)
        .filter { it.isNotEmpty() }
        .flatMap { batch ->
            handler(Result.success(batch))
            Mono.empty()
        }
}
    .delayElement(pollInterval.toJavaDuration(), RedisInstance.get().streamPollScheduler)
    .onErrorResume { e ->
        handler(Result.failure(e))
        Mono.empty()
    }
    .repeat()
    .subscribe()
