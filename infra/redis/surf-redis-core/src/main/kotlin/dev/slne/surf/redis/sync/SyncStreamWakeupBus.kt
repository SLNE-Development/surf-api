package dev.slne.surf.redis.sync

import com.github.benmanes.caffeine.cache.Caffeine
import dev.slne.surf.api.core.util.logger
import dev.slne.surf.redis.RedisApi
import org.redisson.client.codec.StringCodec
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

internal class SyncStreamWakeupBus private constructor(
    private val api: RedisApi,
) {
    companion object {
        private val log = logger()

        private const val CHANNEL = "surf-redis:sync:wakeup"
        private const val MESSAGE_DELIMITER = '\u0000'

        private val byApi = Caffeine.newBuilder()
            .weakKeys()
            .weakValues()
            .build<RedisApi, SyncStreamWakeupBus>()

        fun getInstance(api: RedisApi): SyncStreamWakeupBus =
            byApi.get(api) { SyncStreamWakeupBus(it) }
    }

    private val topic by lazy {
        api.redissonReactive.getTopic(CHANNEL, StringCodec.INSTANCE)
    }

    private val subscribers = ConcurrentHashMap<String, CopyOnWriteArrayList<Sinks.Many<Unit>>>()

    private val initialization: Mono<Void> by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        topic.addListener(String::class.java) { _, message ->
            handleWakeup(message)
        }
            .then()
            .cache()
    }

    fun init(): Mono<Void> = initialization

    fun register(streamKey: String): Sinks.Many<Unit> {
        val sink = Sinks.many()
            .multicast()
            .directBestEffort<Unit>()

        subscribers.computeIfAbsent(streamKey) {
            CopyOnWriteArrayList()
        }.add(sink)

        return sink
    }

    fun unregister(
        streamKey: String,
        sink: Sinks.Many<Unit>,
    ) {
        val streamSubscribers = subscribers[streamKey] ?: return

        streamSubscribers.remove(sink)
        sink.tryEmitComplete()

        if (streamSubscribers.isEmpty()) {
            subscribers.remove(streamKey, streamSubscribers)
        }
    }

    fun publish(streamKey: String) {
        val message = buildString(
            streamKey.length + api.clientId.length + 1
        ) {
            append(streamKey)
            append(MESSAGE_DELIMITER)
            append(api.clientId)
        }

        topic.publish(message)
            .subscribe(
                {},
                { throwable ->
                    log.atFine()
                        .withCause(throwable)
                        .log("Failed to publish sync stream wakeup for '$streamKey'")
                },
            )
    }

    private fun handleWakeup(message: String) {
        val delimiter = message.lastIndexOf(MESSAGE_DELIMITER)
        if (delimiter <= 0 || delimiter == message.lastIndex) {
            return
        }

        val origin = message.substring(delimiter + 1)

        // The originating RedisApi already knows about the write through the Lua result
        if (origin == api.clientId) {
            return
        }

        val streamKey = message.substring(0, delimiter)
        val streamSubscribers = subscribers[streamKey] ?: return

        for (sink in streamSubscribers) {
            sink.tryEmitNext(Unit)
        }
    }
}