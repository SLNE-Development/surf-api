package dev.slne.surf.redis.sync

import dev.slne.surf.api.core.util.logger
import dev.slne.surf.redis.RedisApi
import dev.slne.surf.redis.RedisInstance
import dev.slne.surf.redis.util.LuaScriptExecutor
import dev.slne.surf.redis.util.LuaScriptRegistry
import dev.slne.surf.redis.util.RedisExpirableUtils
import dev.slne.surf.redis.util.fetchLatestStreamId
import org.jetbrains.annotations.MustBeInvokedByOverriders
import org.redisson.api.RAtomicLongReactive
import org.redisson.api.RScript
import org.redisson.api.RStreamReactive
import org.redisson.api.stream.StreamMessageId
import org.redisson.api.stream.StreamReadArgs
import org.redisson.client.codec.StringCodec
import reactor.core.Disposable
import reactor.core.publisher.Mono
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
    scriptRegistry: LuaScriptRegistry,
    structureNamespace: String
) : AbstractSyncStructure<L, R>(api, id, ttl) {
    companion object {
        private val log = logger()

        const val STREAM_FIELD_TYPE = "T"
        const val STREAM_FIELD_MSG = "M"

        private const val MESSAGE_DELIMITER = '\u0000'
        private const val MAX_STREAM_LENGTH = 10_000
    }

    protected val instanceId: String = api.clientId

    private val lastVersion = AtomicLong(0L)
    private val bootstrapped = AtomicBoolean(false)

    protected val stream: RStreamReactive<String, String> by lazy {
        api.redissonReactive.getStream(streamKey, StringCodec.INSTANCE)
    }

    private val cursorId = AtomicReference(StreamMessageId(0, 0))
    private val resyncInFlight = AtomicBoolean(false)

    protected val namespace: String = "$structureNamespace${this.id}:"
    protected val dataKey = "${namespace}snapshot"
    protected val versionKey = "${namespace}version"
    protected val streamKey: String = "${namespace}stream"

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

    @MustBeInvokedByOverriders
    override fun init(): Mono<Void> {
        return stream.fetchLatestStreamId()
            .doOnNext { cursorId.set(it) }
            .then(super.init())
            .doOnSuccess {
                trackDisposable(startPolling())
                trackDisposable(
                    RedisExpirableUtils.refreshContinuously(
                        ttl,
                        stream,
                        versionCounter
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

        val versionPart = msg.substring(0, firstDelim)
        val origin = msg.substring(firstDelim + 1)

        if (secondDelim < 0) { // No payload
            processParsedStreamEvent(type, msg, versionPart, origin, "")
        } else {
            val payloadPart = msg.substring(secondDelim + 1)
            processParsedStreamEvent(type, msg, versionPart, origin, payloadPart)
        }
    }

    private fun processParsedStreamEvent(
        type: String,
        msg: String,
        versionPart: String,
        origin: String,
        payloadPart: String
    ) {
        if (versionPart.isBlank()) {
            log.atWarning()
                .log(
                    "Malformed stream message for type %s: empty version part in message: %s",
                    type,
                    msg
                )
            return
        }

        val version = versionPart.toLongOrNull()
        if (version == null) {
            log.atWarning()
                .log(
                    "Malformed stream message for type %s: invalid version '%s' in message: %s",
                    type,
                    versionPart,
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

        val payload = if (payloadPart.isEmpty()) emptyList() else payloadPart.split(MESSAGE_DELIMITER)

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
            scriptKeys,
            instanceId,
            msgDelimiterStr,
            streamMaxLengthStr,
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
            { e ->
                log.atWarning().withCause(e)
                    .log("Error executing Lua script '$script' for '$id' ($streamKey)")
            }
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

    private fun startPolling(): Disposable {
        return pollOnce()
            .delayElement(
                250.milliseconds.toJavaDuration(),
                RedisInstance.get().streamPollScheduler
            )
            .onErrorResume { e ->
                log.atWarning().withCause(e).log("Stream poll failed for '$id' ($streamKey)")
                requestResync()
                Mono.empty()
            }
            .repeat()
            .subscribe()
    }

    private fun pollOnce(): Mono<Void> = Mono.defer {
        val from = cursorId.get()
        val args = StreamReadArgs.greaterThan(from)
            .count(200)

        stream.read(args)
            .filter { it.isNotEmpty() }
            .flatMap { batch ->
                for ((messageId, fields) in batch) {
                    val type = fields[STREAM_FIELD_TYPE] ?: continue
                    val msg = fields[STREAM_FIELD_MSG] ?: continue
                    try {
                        processStreamEvent(type, msg)
                        cursorId.set(messageId)
                    } catch (t: Throwable) {
                        log.atWarning().withCause(t)
                            .log("Error handling stream event for '$id' ($streamKey)")
                        requestResync()
                    }
                }

                Mono.empty()
            }
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

    protected data class StreamEventData(
        val version: Long,
        val origin: String,
        val payload: List<String>
    )
}