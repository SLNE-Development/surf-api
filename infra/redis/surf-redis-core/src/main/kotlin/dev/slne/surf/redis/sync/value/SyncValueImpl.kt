package dev.slne.surf.redis.sync.value

import dev.slne.surf.redis.RedisApi
import dev.slne.surf.redis.sync.AbstractStreamSyncStructure
import dev.slne.surf.redis.sync.AbstractSyncStructure
import dev.slne.surf.redis.sync.AbstractSyncStructure.SimpleVersionedSnapshot
import dev.slne.surf.redis.util.LuaScriptRegistry
import dev.slne.surf.surfapi.core.api.util.logger
import kotlinx.serialization.KSerializer
import org.redisson.api.DeletedObjectListener
import org.redisson.api.ExpiredObjectListener
import org.redisson.client.codec.StringCodec
import reactor.core.publisher.Mono
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration
import kotlin.time.toJavaDuration

class SyncValueImpl<T : Any>(
    api: RedisApi,
    id: String,
    private val serializer: KSerializer<T>,
    private val defaultValue: T,
    ttl: Duration
) : AbstractStreamSyncStructure<SyncValueChange, SimpleVersionedSnapshot<String?>>(api, id, ttl, Registry),
    SyncValue<T> {

    companion object {
        private val log = logger()
        private const val NAMESPACE = AbstractSyncStructure.NAMESPACE + "value:"

        private const val EVENT_SET = "S"

        private const val SET_SCRIPT = "set"

        private object Registry : LuaScriptRegistry("lua/sync/value") {
            init {
                load(SET_SCRIPT)
            }
        }
    }

    override val structureNamespace = NAMESPACE

    private val bucket by lazy { api.redissonReactive.getBucket<String>(dataKey, StringCodec.INSTANCE) }
    private val value = AtomicReference(defaultValue)

    override fun registerListeners0(): List<Mono<Int>> = listOf(
        bucket.addListener(ExpiredObjectListener { requestResync() }),
        bucket.addListener(DeletedObjectListener { requestResync() })
    )

    override fun unregisterListener(id: Int): Mono<*> = bucket.removeListener(id)

    override fun get(): T = value.get()

    override fun set(newValue: T) {
        val old = value.getAndSet(newValue)

        setRemote(newValue)
        notifyListeners(SyncValueChange.Updated(newValue, old))
    }

    private fun setRemote(value: T) {
        writeToRemote(SET_SCRIPT, EVENT_SET, encodeValue(value))
    }

    override fun onStreamEvent(type: String, data: StreamEventData) = when (type) {
        EVENT_SET -> onSetEvent(data)
        else -> log.atWarning().log("Unknown message type '$type' received from SyncValue '$id'")
    }

    private fun onSetEvent(data: StreamEventData) {
        val encoded = data.payload[0]
        val decoded = decodeValue(encoded)

        val old = value.getAndSet(decoded)
        notifyListeners(SyncValueChange.Updated(decoded, old))
    }

    override fun refreshTtl0(): Mono<*> = bucket.expire(ttl.toJavaDuration())

    override fun loadFromRemote0(): Mono<SimpleVersionedSnapshot<String?>> = Mono.zip(
        bucket.get(),
        versionCounter.get().onErrorReturn(0L)
    ).map { SimpleVersionedSnapshot.fromTuple(it) }

    override fun overrideFromRemote(raw: SimpleVersionedSnapshot<String?>) {
        val snapshotValue = raw.value
        if (snapshotValue == null) {
            value.set(defaultValue)
            return
        }

        val decoded = decodeValue(snapshotValue)
        value.set(decoded)
        super.overrideFromRemote(raw)
    }

    private fun decodeValue(value: String): T = api.json.decodeFromString(serializer, value)
    private fun encodeValue(value: T): String = api.json.encodeToString(serializer, value)
}