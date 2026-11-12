package dev.slne.surf.redis.internal

import dev.slne.surf.redis.util.InternalRedisAPI
import org.intellij.lang.annotations.Language
import org.jetbrains.annotations.Blocking
import org.redisson.Redisson
import org.redisson.api.RScript
import org.redisson.api.RedissonClient
import org.redisson.codec.BaseEventCodec
import org.redisson.config.Config

@InternalRedisAPI
interface RedissonConnector {

    @Blocking
    fun connect(config: Config): RedissonClient

    @Blocking
    fun detectOsType(redisson: RedissonClient): BaseEventCodec.OSType? {
        @Language("Redis")
        val lua = """
            local info = redis.call('INFO', 'server')
            return string.match(info, 'os:([^\r\n]+)')
        """.trimIndent()

        val os = redisson.script.eval<String?>(
            RScript.Mode.READ_ONLY,
            lua,
            RScript.ReturnType.STRING,
        )

        return when {
            os == null || os.contains("Windows") -> BaseEventCodec.OSType.WINDOWS
            os.contains("NONSTOP") -> BaseEventCodec.OSType.HPNONSTOP
            else -> null
        }
    }

    @InternalRedisAPI
    companion object {
        val DEFAULT: RedissonConnector = object : RedissonConnector {
            override fun connect(config: Config): RedissonClient = Redisson.create(config)
        }
    }
}
