package dev.slne.surf.redis.sync

import dev.slne.surf.api.core.util.logger
import dev.slne.surf.redis.RedisApi
import dev.slne.surf.redis.util.*
import org.jetbrains.annotations.MustBeInvokedByOverriders
import org.redisson.api.RAtomicLongReactive
import org.redisson.api.RBucketReactive
import org.redisson.api.RScript
import org.redisson.api.RStreamReactive
import org.redisson.api.stream.StreamMessageId
import org.redisson.client.codec.StringCodec
import reactor.core.Disposable
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration
import kotlin.time.toJavaDuration

abstract class AbstractStreamSyncStructure<L, R : AbstractSyncStructure.VersionedSnapshot>(
    api: RedisApi,
    id: String,
    ttl: Duration,
    scriptRegistry: LuaScriptRegistry,
    structureNamespace: String,
    private val codecDescriptor: String? = null
) : AbstractSyncStructure<L, R>(api, id, ttl) {
    companion object {
        private val log = logger()

        const val STREAM_FIELD_TYPE = "T"
        const val STREAM_FIELD_MSG = "M"

        private const val MESSAGE_DELIMITER = '\u0000'
        private const val MAX_STREAM_LENGTH = 10_000
    }

    protected val instanceId: String = api.clientId

    private val versions = StreamVersionTracker()

    protected val stream: RStreamReactive<String, String> by lazy {
        api.redissonReactive.getStream(streamKey, StringCodec.INSTANCE)
    }

    private val cursorId = AtomicReference(StreamMessageId(0, 0))
    private val resyncInFlight = AtomicBoolean(false)

    protected val namespace: String = "$structureNamespace${this.id}:"
    protected val dataKey = "${namespace}snapshot"
    protected val versionKey = "${namespace}version"
    protected val streamKey: String = "${namespace}stream"
    private val codecKey: String = "${namespace}codec"

    private val codecBucket: RBucketReactive<String> by lazy {
        api.redissonReactive.getBucket(codecKey, StringCodec.INSTANCE)
    }

    protected val versionCounter: RAtomicLongReactive by lazy {
        api.redissonReactive.getAtomicLong(
            versionKey
        )
    }
    protected val scriptExecutor = LuaScriptExecutor.getInstance(api, scriptRegistry)

    // Pre-computed per-instance constants reused on every Lua script invocation. Reduces
    // per-op allocations of identical Strings / List<Any> wrappers on the hot path.
    private val msgDelimiterStr: String = MESSAGE_DELIMITER.toString()
    private val streamMaxLengthStr: String = MAX_STREAM_LENGTH.toString()
    private val scriptKeys: List<Any> = listOf(dataKey, streamKey, versionKey)

    private val wakeupBus = SyncStreamWakeupBus.getInstance(api)
    private val wakeupSink = wakeupBus.register(streamKey)

    @MustBeInvokedByOverriders
    override fun init(): Mono<Void> {
        return wakeupBus.init()
            .then(validateCodecConfiguration())
            .then(stream.fetchLatestStreamId())
            .doOnNext { cursorId.set(it) }
            .then(super.init())
            .doOnSuccess {
                trackDisposable(startPolling())
                trackDisposable(
                    RedisExpirableUtils.refreshContinuously(
                        ttl,
                        stream,
                        versionCounter,
                        *codecDescriptor?.let { arrayOf(codecBucket) }.orEmpty()
                    )
                )
            }
            .then()
    }

    private fun processStreamEvent(type: String, msg: String) {
        // Parse "version<DELIM>origin<DELIM>payload" without allocating an ArrayList per message.
        val firstDelim = msg.indexOf(MESSAGE_DELIMITER)
        if (firstDelim < 0) {
            log.atWarning()
                .log(
                    "Malformed stream message for type %s: expected at least 2 parts but got 1: %s",
                    type,
                    msg
                )
            return
        }
        val secondDelim = msg.indexOf(MESSAGE_DELIMITER, firstDelim + 1)
        if (secondDelim < 0) {
            throw IllegalArgumentException(
                "Malformed stream message for type '$type': missing payload delimiter in message: $msg"
            )
        }

        val origin = msg.substring(firstDelim + 1, secondDelim)
        val version = parseVersion(msg, firstDelim)

        if (version == null) {
            log.atWarning()
                .log(
                    "Malformed stream message for type %s: invalid version in message: %s",
                    type,
                    msg
                )
            return
        }

        if (origin.isBlank()) {
            log.atWarning()
                .log(
                    "Malformed stream message for type %s: empty origin part in message: %s",
                    type,
                    msg
                )
            return
        }

        val payload = msg.substring(secondDelim + 1)

        if (!applyVersion(version)) return
        if (origin == instanceId) return

        onStreamEvent(type, StreamEventData(version, origin, payload))
    }

    private fun parseVersion(message: String, end: Int): Long? {
        if (end == 0) return null
        var result = 0L
        for (i in 0 until end) {
            val digit = message[i].code - '0'.code
            if (digit !in 0..9 || result > (Long.MAX_VALUE - digit) / 10L) return null
            result = result * 10L + digit
        }
        return result
    }

    private fun validateCodecConfiguration(): Mono<Void> {
        val expected = codecDescriptor
        return codecBucket.get()
            .flatMap { actual ->
                if (expected == null) {
                    Mono.error<Void>(
                        IllegalStateException(
                            "Cannot use JSON encoding for synchronized structure '$id': " +
                                    "existing Redis data uses codec '$actual'"
                        )
                    ).thenReturn(true)
                } else {
                    validateCodecDescriptor(expected, actual).thenReturn(true)
                }
            }
            .switchIfEmpty(
                if (expected == null) {
                    Mono.just(true)
                } else {
                    api.redissonReactive.keys.countExists(dataKey)
                        .flatMap { existingDataKeys ->
                            if (existingDataKeys > 0L) {
                                Mono.error(
                                    IllegalStateException(
                                        "Cannot enable custom codec '$expected' for synchronized structure '$id': " +
                                                "existing Redis data has no codec metadata"
                                    )
                                )
                            } else {
                                registerCodecDescriptor(codecBucket, expected)
                            }
                        }
                        .thenReturn(true)
                }
            )
            .then()
    }

    private fun registerCodecDescriptor(
        bucket: RBucketReactive<String>,
        descriptor: String
    ): Mono<Void> {
        val register = if (ttl > Duration.ZERO) {
            bucket.setIfAbsent(descriptor, ttl.toJavaDuration())
        } else {
            bucket.setIfAbsent(descriptor)
        }
        return register.then(bucket.get())
            .switchIfEmpty(
                Mono.error(
                    IllegalStateException("Codec metadata disappeared while initializing '$id' ($codecKey)")
                )
            )
            .flatMap { actual -> validateCodecDescriptor(descriptor, actual) }
    }

    private fun validateCodecDescriptor(expected: String, actual: String): Mono<Void> {
        return if (actual == expected) {
            Mono.empty()
        } else {
            Mono.error(
                IllegalStateException(
                    "Codec mismatch for synchronized structure '$id': expected '$expected', found '$actual'"
                )
            )
        }
    }

    protected abstract fun onStreamEvent(type: String, data: StreamEventData)

    protected fun writeToRemote(
        script: String,
        eventType: String,
        vararg values: String
    ) {
        executeWrite(script, eventType, *values)
            .subscribe({}, {})
    }

    protected fun writeToRemoteAwait(
        script: String,
        eventType: String,
        vararg values: String,
    ): Mono<Long> {
        return executeWrite(script, eventType, *values)
    }

    private fun executeWrite(
        script: String,
        eventType: String,
        vararg values: String,
    ): Mono<Long> {
        return scriptExecutor.execute<Long>(
            script,
            RScript.Mode.READ_WRITE,
            RScript.ReturnType.LONG,
            scriptKeys,
            instanceId,
            msgDelimiterStr,
            streamMaxLengthStr,
            STREAM_FIELD_TYPE,
            STREAM_FIELD_MSG,
            eventType,
            *values,
        ).doOnNext { newVersion ->
            when {
                newVersion < 0L -> {
                    requestResync()
                }

                newVersion == 0L -> Unit

                else -> {
                    applyVersion(newVersion)
                    wakeupBus.publish(streamKey)
                }
            }
        }.doOnError { throwable ->
            log.atWarning()
                .withCause(throwable)
                .log("Error executing Lua script '$script' for '$id' ($streamKey)")

            requestResync()
        }
    }

    protected fun writeBatchToRemote(
        script: String,
        eventType: String,
        vararg values: String,
    ) {
        executeBatchWrite(script, eventType, *values)
            .subscribe({}, {})
    }

    protected fun writeBatchToRemoteAwait(
        script: String,
        eventType: String,
        vararg values: String,
    ): Mono<VersionRange> {
        return executeBatchWrite(script, eventType, *values)
    }

    private fun executeBatchWrite(
        script: String,
        eventType: String,
        vararg values: String,
    ): Mono<VersionRange> {
        return scriptExecutor.execute<List<Any>>(
            script,
            RScript.Mode.READ_WRITE,
            RScript.ReturnType.LIST,
            scriptKeys,
            instanceId,
            msgDelimiterStr,
            streamMaxLengthStr,
            STREAM_FIELD_TYPE,
            STREAM_FIELD_MSG,
            eventType,
            *values,
        )
            .map { raw ->
                VersionRange(
                    first = raw.getOrNull(0).asLongOrZero(),
                    last = raw.getOrNull(1).asLongOrZero(),
                )
            }
            .doOnNext { range ->
                if (range.first != 0L || range.last != 0L) {
                    applyVersionRange(range.first, range.last)

                    if (range.last > 0L) {
                        wakeupBus.publish(streamKey)
                    }
                }
            }
            .doOnError { throwable ->
                log.atWarning()
                    .withCause(throwable)
                    .log("Error executing batched Lua script '$script' for '$id' ($streamKey)")

                requestResync()
            }
    }

    protected fun readAtomicSnapshot(script: String): Mono<List<Any>> {
        return scriptExecutor.execute(
            script,
            RScript.Mode.READ_WRITE,
            RScript.ReturnType.LIST,
            listOf(dataKey, versionKey),
        )
    }

    private fun Any?.asLongOrZero(): Long = when (this) {
        is Number -> toLong()
        null -> 0L
        else -> toString().toLongOrNull() ?: 0L
    }

    private fun applyVersionRange(first: Long, last: Long) {
        if (versions.applyRange(first, last) == StreamVersionTracker.Outcome.RESYNC) {
            requestResync()
        }
    }

    protected fun applyVersion(ver: Long): Boolean {
        return when (versions.apply(ver)) {
            StreamVersionTracker.Outcome.APPLIED -> true
            StreamVersionTracker.Outcome.SKIPPED -> false
            StreamVersionTracker.Outcome.RESYNC -> {
                requestResync()
                false
            }
        }
    }

    private fun startPolling(): Disposable = stream.pollContinuously(
        cursorId = cursorId,
        wakeups = wakeupSink.asFlux(),
        shouldPoll = { !resyncInFlight.get() },
    ) {
        onSuccess { batch ->
            for ((messageId, fields) in batch) {
                if (resyncInFlight.get()) {
                    break
                }

                val type = fields[STREAM_FIELD_TYPE] ?: continue
                val msg = fields[STREAM_FIELD_MSG] ?: continue

                try {
                    processStreamEvent(type, msg)
                    cursorId.set(messageId)

                    if (resyncInFlight.get()) {
                        break
                    }
                } catch (t: Throwable) {
                    log.atWarning()
                        .withCause(t)
                        .log("Error handling stream event for '$id' ($streamKey)")
                    requestResync()
                }
            }
        }

        onFailure { e ->
            log.atWarning().withCause(e).log("Stream poll failed for '$id' ($streamKey)")
            requestResync()
        }
    }

    protected fun requestResync() {
        if (!resyncInFlight.compareAndSet(false, true)) return

        val disposable = loadFromRemote()
            .retryWhen(
                Retry.backoff(
                    Long.MAX_VALUE,
                    java.time.Duration.ofMillis(100),
                )
                    .maxBackoff(java.time.Duration.ofSeconds(5))
            )
            .doFinally {
                resyncInFlight.set(false)

                if (!isDisposed) {
                    wakeupSink.tryEmitNext(Unit)
                }
            }
            .subscribe()

        trackDisposable(disposable)
    }

    override fun dispose0() {
        wakeupBus.unregister(streamKey, wakeupSink)
        super.dispose0()
    }

    @MustBeInvokedByOverriders
    override fun overrideFromRemote(raw: R) {
        versions.bootstrap(raw.version)
    }

    protected data class StreamEventData(
        val version: Long,
        val origin: String,
        private val encodedPayload: String
    ) {
        fun payload(index: Int): String {
            require(index >= 0) { "Payload index must not be negative: $index" }
            var start = 0
            repeat(index) {
                val delimiter = encodedPayload.indexOf(MESSAGE_DELIMITER, start)
                if (delimiter < 0) throw IndexOutOfBoundsException("Missing stream payload at index $index")
                start = delimiter + 1
            }
            val end = encodedPayload.indexOf(MESSAGE_DELIMITER, start)
            return if (end < 0) encodedPayload.substring(start) else encodedPayload.substring(start, end)
        }

        fun payloadOrNull(index: Int): String? = try {
            payload(index)
        } catch (_: IndexOutOfBoundsException) {
            null
        }
    }

    protected data class VersionRange(
        val first: Long,
        val last: Long,
    )
}
