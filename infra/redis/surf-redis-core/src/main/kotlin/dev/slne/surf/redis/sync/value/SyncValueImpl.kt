package dev.slne.surf.redis.sync.value

import dev.slne.surf.redis.RedisApi
import dev.slne.surf.redis.sync.AbstractSyncStructure
import dev.slne.surf.surfapi.core.api.util.logger
import kotlinx.serialization.KSerializer
import org.redisson.api.DeletedObjectListener
import org.redisson.api.ExpiredObjectListener
import org.redisson.api.listener.SetObjectListener
import org.redisson.api.map.event.MapEntryListener
import org.redisson.client.codec.StringCodec
import reactor.core.publisher.Mono
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.time.Duration
import kotlin.time.toJavaDuration

/**
 * SyncValue implementation backed by a single Redis snapshot key.
 *
 * Keeps an in-memory copy and mirrors it to Redis. On remote changes, reloads the snapshot
 * and updates local state, notifying listeners.
 */
class SyncValueImpl<T : Any>(
    api: RedisApi,
    id: String,
    private val serializer: KSerializer<T>,
    private val defaultValue: T,
    ttl: Duration
) : AbstractSyncStructure<SyncValueChange, String>(api, id, ttl),
    SyncValue<T> {

    companion object {
        private val log = logger()
    }

    private val dataKey = "surf-redis:sync:value:$id:snapshot"
    private val bucket by lazy { api.redissonReactive.getBucket<String>(dataKey, StringCodec.INSTANCE) }
    private var value: T = defaultValue

    override fun registerListeners0(): List<Mono<Int>> = listOf(
        bucket.addListener(SetListener()),
        bucket.addListener(ExpiredListener()),
        bucket.addListener(DeletedListener())
    )

    override fun unregisterListener(id: Int): Mono<*> = bucket.removeListener(id)

    override fun get(): T = lock.read { value }

    override fun set(newValue: T) {
        lock.write {
            value = newValue
        }

        bucket.set(encodeValue(newValue), ttl.toJavaDuration())
            .subscribe(
                { /* Success */ },
                {
                    log.atWarning()
                        .withCause(it)
                        .log("Failed to set SyncValue snapshot key '$dataKey'")
                }
            )
    }

    inner class SetListener : SetObjectListener {
        override fun onSet(name: String?) {
            onChange()
        }
    }

    inner class ExpiredListener : ExpiredObjectListener {
        override fun onExpired(name: String?) {
            onChange()
        }
    }

    inner class DeletedListener : DeletedObjectListener {
        override fun onDeleted(name: String?) {
            onChange()
        }
    }

    private fun onChange() {
        bucket.get()
            .subscribe(
                { raw ->
                    val next = try {
                        if (raw.isNullOrBlank()) defaultValue else decodeValue(raw)
                    } catch (e: Throwable) {
                        log.atWarning()
                            .withCause(e)
                            .log("Failed to decode SyncValue '$dataKey'")
                        return@subscribe
                    }

                    val old = lock.write {
                        val old = value
                        value = next
                        old
                    }

                    notifyListeners(SyncValueChange.Updated(next, old))
                },
                { e ->
                    log.atWarning().withCause(e).log("Failed to reload SyncValue '$dataKey'")
                }
            )

    }

    override fun refreshTtl(): Mono<*> = bucket.expire(ttl.toJavaDuration())
    override fun loadFromRemote0(): Mono<String> = bucket.get()

    override fun overrideFromRemote(raw: String) {
        lock.write { value = decodeValue(raw) }
    }

    private fun decodeValue(value: String): T = api.json.decodeFromString(serializer, value)
    private fun encodeValue(value: T): String = api.json.encodeToString(serializer, value)

}