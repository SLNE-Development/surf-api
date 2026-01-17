package dev.slne.surf.redis.sync

import dev.slne.surf.redis.RedisApi
import dev.slne.surf.redis.util.LuaScriptExecutor
import dev.slne.surf.redis.util.LuaScriptRegistry
import dev.slne.surf.surfapi.core.api.util.logger
import org.jetbrains.annotations.MustBeInvokedByOverriders
import org.redisson.api.RAtomicLongReactive
import org.redisson.api.RScript
import org.redisson.api.RStreamReactive
import org.redisson.api.stream.StreamMessageId
import org.redisson.api.stream.StreamRangeArgs
import org.redisson.api.stream.StreamReadArgs
import org.redisson.client.codec.StringCodec
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

abstract class AbstractStreamSyncStructure<L, R : AbstractSyncStructure.VersionedSnapshot>(
    api: RedisApi,
    id: String,
    ttl: Duration,
    scriptRegistry: LuaScriptRegistry
) : AbstractSyncStructure<L, R>(api, id, ttl) {
    companion object {
        private val log = logger()

        const val STREAM_FIELD_TYPE = "T"
        const val STREAM_FIELD_MSG = "M"

        private val streamScheduler = Schedulers.newBoundedElastic(
            8,
            Schedulers.DEFAULT_BOUNDED_ELASTIC_QUEUESIZE,
            "surf-redis-sync-stream-poll",
            60,
            true
        )
    }

    protected val instanceId: String = api.clientId
    protected open val msgDelimiter: String = "\u0000"
    protected open val streamMaxLength: Int = 10_000

    private val lastVersion = AtomicLong(0L)
    private val bootstrapped = AtomicBoolean(false)

    protected open val streamReadCount: Int = 200
    protected open val streamPollInterval: Duration = 250.milliseconds
    protected val stream: RStreamReactive<String, String> by lazy {
        api.redissonReactive.getStream(streamKey, StringCodec.INSTANCE)
    }

    private val cursorId = AtomicReference<StreamMessageId>(StreamMessageId(0, 0))
    private val resyncInFlight = AtomicBoolean(false)

    protected abstract val structureNamespace: String
    protected val namespace: String = "$structureNamespace:$id:"
    protected val dataKey = "${namespace}snapshot"
    protected val versionKey = "${namespace}version"
    protected val streamKey: String = "$dataKey:stream"

    protected val versionCounter: RAtomicLongReactive by lazy { api.redissonReactive.getAtomicLong(versionKey) }
    protected val scriptExecutor = LuaScriptExecutor.getInstance(api, scriptRegistry)

    @MustBeInvokedByOverriders
    override fun init(): Mono<Void> {
        return fetchLastStreamId()
            .doOnNext { cursorId.set(it) }
            .then(super.init())
            .doOnSuccess { trackDisposable(startPolling()) }
            .then()
    }

    private fun processStreamEvent(type: String, msg: String) {
        val splitted = msg.split(msgDelimiter)
        if (splitted.size < 3) return
        val version = splitted[0].toLongOrNull() ?: return
        val origin = splitted[1]
        val payload = splitted.subList(2, splitted.size)

        if (!applyVersion(version)) return
        if (origin == instanceId) return

        onStreamEvent(type, StreamEventData(version, origin, payload))
    }

    protected abstract fun onStreamEvent(type: String, data: StreamEventData)

    protected fun writeToRemote(
        script: String,
        eventType: String,
        vararg values: String
    ) {
        scriptExecutor.execute<Long>(
            script,
            RScript.Mode.READ_WRITE,
            RScript.ReturnType.LONG,
            listOf(dataKey, streamKey, versionKey),
            instanceId,
            msgDelimiter,
            streamMaxLength,
            STREAM_FIELD_TYPE,
            STREAM_FIELD_MSG,
            eventType,
            *values
        ).subscribe(
            { newVersion ->
                when (newVersion) {
                    -1L -> requestResync()
                    0L -> Unit
                    else -> applyVersion(newVersion)
                }
            },
            { e -> log.atWarning().withCause(e).log("Error executing Lua script for '$id' ($streamKey)") }
        )
    }

    protected fun applyVersion(ver: Long): Boolean {
        if (!bootstrapped.get()) {
            requestResync()
            return false
        }

        val current = lastVersion.get()
        return when {
            ver <= current -> false
            ver == current + 1 -> {
                lastVersion.set(ver)
                true
            }

            else -> {
                requestResync()
                false
            }
        }
    }

    protected fun expireStreamKey(): Mono<Boolean> {
        return stream.expire(ttl.toJavaDuration())
    }

    final override fun refreshTtl(): Mono<*> {
        return Mono.`when`(expireStreamKey(), versionCounter.expire(ttl.toJavaDuration()), refreshTtl0())
    }

    protected open fun refreshTtl0(): Mono<*> = Mono.empty<Void>()

    private fun startPolling(): Disposable {
        return Flux.interval(java.time.Duration.ZERO, streamPollInterval.toJavaDuration(), streamScheduler)
            .concatMap { pollOnce() }
            .onErrorResume { e ->
                log.atWarning().withCause(e).log("Stream poll failed for '$id' ($streamKey)")
                requestResync()
                Mono.empty()
            }
            .repeat()
            .subscribe()
    }

    private fun pollOnce(): Mono<Void> {
        val from = cursorId.get()
        val args = StreamReadArgs.greaterThan(from)
            .count(streamReadCount)

        return stream.read(args)
            .filter { it.isNotEmpty() }
            .flatMap { batch ->
                val lastId = batch.keys.maxWithOrNull(compareBy({ it.id0 }, { it.id1 })) ?: from
                cursorId.set(lastId)

                for ((_, fields) in batch) {
                    val type = fields[STREAM_FIELD_TYPE] ?: continue
                    val msg = fields[STREAM_FIELD_MSG] ?: continue
                    try {
                        processStreamEvent(type, msg)
                    } catch (t: Throwable) {
                        log.atWarning().withCause(t).log("Error handling stream event for '$id' ($streamKey)")
                        requestResync()
                    }
                }

                Mono.empty()
            }
    }

    private fun fetchLastStreamId(): Mono<StreamMessageId> {
        val args = StreamRangeArgs.startId(StreamMessageId.MAX)
            .endId(StreamMessageId.MIN)
            .count(1)

        return stream.rangeReversed(args)
            .map { map -> map.keys.firstOrNull() ?: StreamMessageId(0, 0) }
            .defaultIfEmpty(StreamMessageId(0, 0))
            .onErrorReturn(StreamMessageId(0, 0))
    }

    protected fun requestResync() {
        if (!resyncInFlight.compareAndSet(false, true)) return

        loadFromRemote()
            .doFinally { resyncInFlight.set(false) }
            .subscribe()
    }

    @MustBeInvokedByOverriders
    override fun overrideFromRemote(raw: R) {
        lastVersion.set(raw.version)
        bootstrapped.set(true)
    }

    protected data class StreamEventData(val version: Long, val origin: String, val payload: List<String>)
}