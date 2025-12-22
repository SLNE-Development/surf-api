package de.slne.redis.sync

import de.slne.redis.RedisApi
import dev.slne.surf.surfapi.core.api.util.logger
import io.lettuce.core.pubsub.RedisPubSubListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * Base class for all synchronized structures.
 * Handles Redis pub/sub communication and change notifications.
 */
abstract class SyncStructure<TDelta : Any>(
    protected val api: RedisApi,
    protected val id: String,
    protected val scope: CoroutineScope
) {
    protected val lock = ReentrantReadWriteLock()
    protected abstract val redisChannel: String

    companion object {
        private val log = logger()

    }

    internal open suspend fun init() {
        loadSnapshot()
        setupSubscription()
    }

    private suspend fun setupSubscription() {
        api.pubSubConnection.addListener(object : RedisPubSubListener<String, String> {
            override fun message(channel: String, message: String) {
                if (channel == redisChannel) {
                    handleIncoming(message)
                }
            }

            override fun message(pattern: String, channel: String, message: String) {}
            override fun subscribed(channel: String, count: Long) {}
            override fun psubscribed(pattern: String, count: Long) {}
            override fun unsubscribed(channel: String, count: Long) {}
            override fun punsubscribed(pattern: String, count: Long) {}
        })

        api.pubSubConnection.async().subscribe(redisChannel).await()
    }


    protected abstract suspend fun loadSnapshot()
    protected abstract fun handleIncoming(message: String)

    protected fun <T> notifyListeners(listeners: List<(T) -> Unit>, change: T) {
        for (l in listeners) {
            try {
                l(change)
            } catch (e: Throwable) {
                log.atWarning()
                    .withCause(e)
                    .log("Error notifying listener of change: $change")
            }
        }
    }
}
