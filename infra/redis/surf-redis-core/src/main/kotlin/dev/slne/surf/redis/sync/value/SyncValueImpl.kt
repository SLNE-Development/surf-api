package dev.slne.surf.redis.sync.value

import dev.slne.surf.api.core.util.logger
import dev.slne.surf.redis.RedisApi
import dev.slne.surf.redis.sync.AbstractStreamSyncStructure
import dev.slne.surf.redis.sync.AbstractSyncStructure
import dev.slne.surf.redis.sync.AbstractSyncStructure.SimpleVersionedSnapshot
import dev.slne.surf.redis.sync.SyncValueCodec
import dev.slne.surf.redis.util.LuaScriptRegistry
import dev.slne.surf.redis.util.RedisExpirableUtils
import kotlinx.coroutines.reactor.awaitSingle
import org.redisson.api.DeletedObjectListener
import org.redisson.api.ExpiredObjectListener
import org.redisson.client.codec.StringCodec
import reactor.core.publisher.Mono
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration

class SyncValueImpl<T : Any> internal constructor(
    api: RedisApi,
    id: String,
    private val valueCodec: SyncValueCodec<T>,
    private val defaultValue: T,
    ttl: Duration
) : AbstractStreamSyncStructure<SyncValueChange, SimpleVersionedSnapshot<String?>>(
    api,
    id,
    ttl,
    Registry,
    NAMESPACE,
    valueCodec.descriptor
), SyncValue<T> {

    companion object {
        private val log = logger()
        private const val NAMESPACE = AbstractSyncStructure.NAMESPACE + "value:"

        private const val EVENT_SET = "S"

        private const val SET_SCRIPT = "set"
        private const val SNAPSHOT_SCRIPT = "snapshot"
        private const val COMPARE_AND_SET_SCRIPT = "compare-and-set"
        private const val GET_AND_SET_SCRIPT = "get-and-set"

        private object Registry : LuaScriptRegistry("lua/sync/value") {
            init {
                load(SET_SCRIPT)
                load(SNAPSHOT_SCRIPT)
                load(COMPARE_AND_SET_SCRIPT)
                load(GET_AND_SET_SCRIPT)
            }
        }
    }

    private val bucket by lazy {
        api.redissonReactive.getBucket<String>(
            dataKey,
            StringCodec.INSTANCE
        )
    }
    private val value = AtomicReference(defaultValue)
    private val encodedDefault by lazy { encodeValue(defaultValue) }

    override fun init(): Mono<Void> {
        return super.init()
            .doOnSuccess {
                trackDisposable(RedisExpirableUtils.refreshContinuously(ttl, bucket))
            }
            .then()
    }

    override fun registerListeners0(): List<Mono<Int>> = listOf(
        bucket.addListener(ExpiredObjectListener { requestResync() }),
        bucket.addListener(DeletedObjectListener { requestResync() })
    )

    override fun unregisterListener(id: Int): Mono<*> = bucket.removeListener(id)

    override fun get(): T = value.get()

    override suspend fun getRemote(): T = decodeOrDefault(readRemoteEncoded())

    override fun set(newValue: T) {
        val old = value.getAndSet(newValue)

        notifyListeners(SyncValueChange.Updated(newValue, old))
        setRemoteAsync(newValue)
    }

    override suspend fun setAndAwait(newValue: T) {
        val old = value.getAndSet(newValue)

        notifyListeners(
            SyncValueChange.Updated(
                newValue,
                old,
            )
        )

        writeToRemoteAwait(
            SET_SCRIPT,
            EVENT_SET,
            encodeValue(newValue),
        ).awaitSingle()
    }

    override suspend fun compareAndSetRemote(expectedValue: T, newValue: T): Boolean =
        compareAndSetRemoteEncoded(encodeValue(expectedValue), encodeValue(newValue))

    override suspend fun getAndSetRemote(newValue: T): T {
        val result = writeToRemoteWithPayloadAwait(
            GET_AND_SET_SCRIPT,
            EVENT_SET,
            encodeValue(newValue),
        ).awaitSingle()

        val hadPreviousValue = result.extra.getOrNull(0) == "1"
        return if (hadPreviousValue) decodeValue(result.extra[1]) else defaultValue
    }

    override suspend fun updateAndGetRemote(transform: (T) -> T): T =
        updateRemote(transform).updated

    override suspend fun getAndUpdateRemote(transform: (T) -> T): T =
        updateRemote(transform).previous

    private suspend fun updateRemote(transform: (T) -> T): RemoteUpdate<T> {
        while (true) {
            val encodedCurrent = readRemoteEncoded()
            val current = decodeOrDefault(encodedCurrent)
            val next = transform(current)

            if (compareAndSetRemoteEncoded(encodedCurrent ?: encodedDefault, encodeValue(next))) {
                return RemoteUpdate(current, next)
            }
        }
    }

    private suspend fun compareAndSetRemoteEncoded(
        encodedExpected: String,
        encodedNew: String,
    ): Boolean {
        val absentMatches = encodedExpected == encodedDefault

        return writeToRemoteWithPayloadAwait(
            COMPARE_AND_SET_SCRIPT,
            EVENT_SET,
            encodedExpected,
            encodedNew,
            if (absentMatches) "1" else "0",
        ).awaitSingle().applied
    }
    
    private suspend fun readRemoteEncoded(): String? = pullRemoteSnapshot().awaitSingle().value

    private fun decodeOrDefault(encoded: String?): T = encoded?.let(::decodeValue) ?: defaultValue

    private fun setRemoteAsync(value: T) {
        writeToRemote(SET_SCRIPT, EVENT_SET, encodeValue(value))
    }

    override fun onStreamEvent(type: String, data: StreamEventData) = when (type) {
        EVENT_SET -> onSetEvent(data)
        else -> log.atWarning().log("Unknown message type '$type' received from SyncValue '$id'")
    }

    private fun onSetEvent(data: StreamEventData) {
        val encoded = data.payload(0)
        val decoded = decodeValue(encoded)

        val old = value.getAndSet(decoded)
        notifyListeners(SyncValueChange.Updated(decoded, old))
    }

    override fun loadFromRemote0(): Mono<SimpleVersionedSnapshot<String?>> {
        return readAtomicSnapshot(SNAPSHOT_SCRIPT)
            .map { raw ->
                require(raw.size == 3) {
                    "Malformed snapshot result for SyncValue '$id': $raw"
                }

                val present = when (raw[0].toString()) {
                    "0" -> false
                    "1" -> true
                    else -> error(
                        "Malformed presence flag in SyncValue '$id' snapshot: ${raw[0]}"
                    )
                }

                val snapshotValue = if (present) {
                    raw[1].toString()
                } else {
                    null
                }

                val version = raw[2].toString().toLong()

                SimpleVersionedSnapshot(
                    snapshotValue,
                    version,
                )
            }
    }

    override fun overrideFromRemote(raw: SimpleVersionedSnapshot<String?>) {
        val snapshotValue = raw.value
        if (snapshotValue == null) {
            value.set(defaultValue)
            super.overrideFromRemote(raw)
            return
        }

        val decoded = decodeValue(snapshotValue)
        value.set(decoded)
        super.overrideFromRemote(raw)
    }

    private fun decodeValue(value: String): T = valueCodec.decode(value)
    private fun encodeValue(value: T): String = valueCodec.encode(value)

    private data class RemoteUpdate<T>(val previous: T, val updated: T)
}
