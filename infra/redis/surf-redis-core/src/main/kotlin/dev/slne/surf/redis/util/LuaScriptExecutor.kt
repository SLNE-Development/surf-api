package dev.slne.surf.redis.util

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import com.sksamuel.aedile.core.asCache
import dev.slne.surf.redis.RedisApi
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.mono
import org.redisson.api.RScript
import org.redisson.client.RedisNoScriptException
import org.redisson.client.codec.StringCodec
import reactor.core.publisher.Mono
import reactor.util.retry.Retry

class LuaScriptExecutor private constructor(private val api: RedisApi, private val registry: LuaScriptRegistry) {
    private val scriptShas = Caffeine.newBuilder()
        .asCache<String, String>()

    private val script by lazy { api.redissonReactive.getScript(StringCodec.INSTANCE) }

    fun <R : Any> execute(
        id: String,
        mode: RScript.Mode,
        returnType: RScript.ReturnType,
        keys: List<Any>,
        vararg values: Any,
        tries: Int = 3
    ): Mono<R> {
        val sha = mono {
            scriptShas.get(id) {
                script.scriptLoad(registry.get(id)).awaitSingleOrNull() ?: error("Failed to load Lua script: $id")
            }
        }

        return sha
            .flatMap { script.evalSha<R>(mode, it, returnType, keys, *values) }
            .doOnError(RedisNoScriptException::class.java) { scriptShas.invalidate(id) }
            .retryWhen(
                Retry.max(tries.toLong())
                    .filter { it is RedisNoScriptException }
            )
    }

    companion object {
        /**
         * A cache that maps a Redis API instance to a nested cache, which links LuaScriptRegistry objects
         * to their corresponding LuaScriptExecutor instances. The outer cache is keyed by `RedisApi`, while
         * the inner cache is keyed by `LuaScriptRegistry`. Both caches use weak references for their key storage.
         * This design ensures efficient memory usage and avoids retaining unnecessary references to keys.
         */
        private val byApi = Caffeine.newBuilder()
            .weakKeys()
            .build<RedisApi, LoadingCache<LuaScriptRegistry, LuaScriptExecutor>> { api ->
                Caffeine.newBuilder()
                    .weakKeys()
                    .build { registry ->
                        LuaScriptExecutor(api, registry)
                    }
            }


        fun getInstance(api: RedisApi, registry: LuaScriptRegistry) = byApi.get(api).get(registry)
    }
}