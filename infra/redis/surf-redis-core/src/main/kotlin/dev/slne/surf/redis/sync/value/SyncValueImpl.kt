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

        private object Registry : LuaScriptRegistry("lua/sync/value") {
            init {
                load(SET_SCRIPT)
                load(SNAPSHOT_SCRIPT)
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

    override fun set(newValue: T) {
        val old = value.getAndSet(newValue)

        notifyListeners(SyncValueChange.Updated(newValue, old))
        setRemote(newValue)
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


    private fun setRemote(value: T) {
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
}
