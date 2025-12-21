package de.slne.redis.config

import de.slne.redis.RedisApi
import de.slne.redis.event.RedisEventBus
import de.slne.redis.request.RequestResponseBus
import de.slne.redis.stream.RedisStreamEventBus
import de.slne.redis.sync.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import jakarta.annotation.PostConstruct

/**
 * Spring Boot auto-configuration for surf-redis.
 * Automatically configures Redis connections and buses when Spring Boot is present.
 * 
 * Configuration properties:
 * - surf.redis.url: Redis connection URL (default: redis://localhost:6379)
 * - surf.redis.enabled: Enable auto-configuration (default: true)
 */
@Configuration
@ConditionalOnClass(RedisApi::class)
@ConditionalOnProperty(
    prefix = "surf.redis",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class RedisAutoConfiguration {
    
    @Value("\${surf.redis.url:redis://localhost:6379}")
    private lateinit var redisUrl: String
    
    @PostConstruct
    fun init() {
        // Initialize global Redis connection
        RedisApi.init(redisUrl)
        
        // Initialize SyncManager with the same URL
        SyncManager.init(redisUrl)
    }
    
    /**
     * Create a shared coroutine scope for Redis operations.
     */
    @Bean
    fun redisCoroutineScope(): CoroutineScope {
        return CoroutineScope(Dispatchers.Default + SupervisorJob())
    }
    
    /**
     * Create the RedisEventBus bean for event distribution.
     * Uses the global Redis connection.
     */
    @Bean
    @ConditionalOnProperty(
        prefix = "surf.redis.event-bus",
        name = ["enabled"],
        havingValue = "true",
        matchIfMissing = true
    )
    fun redisEventBus(coroutineScope: CoroutineScope): RedisEventBus {
        return RedisEventBus(redisUrl, coroutineScope)
    }
    
    /**
     * Create the RequestResponseBus bean for request-response patterns.
     * Uses the global Redis connection.
     */
    @Bean
    @ConditionalOnProperty(
        prefix = "surf.redis.request-bus",
        name = ["enabled"],
        havingValue = "true",
        matchIfMissing = true
    )
    fun requestResponseBus(coroutineScope: CoroutineScope): RequestResponseBus {
        return RequestResponseBus(redisUrl, coroutineScope)
    }
    
    /**
     * Create the RedisStreamEventBus bean for stream-based event distribution.
     * Uses Redis Streams for reliable event delivery with persistence.
     * Only enabled when surf.redis.stream-bus.enabled is set to true.
     */
    @Bean
    @ConditionalOnProperty(
        prefix = "surf.redis.stream-bus",
        name = ["enabled"],
        havingValue = "true",
        matchIfMissing = false
    )
    fun redisStreamEventBus(
        coroutineScope: CoroutineScope,
        @Value("\${surf.redis.stream-bus.stream-name:surf-redis:events}") streamName: String,
        @Value("\${surf.redis.stream-bus.consumer-group:default}") consumerGroup: String,
        @Value("\${surf.redis.stream-bus.consumer-name:}") consumerName: String?
    ): RedisStreamEventBus {
        // Use explicit constructor call with proper parameter order
        return if (consumerName.isNullOrBlank()) {
            // Generate consumer name automatically
            RedisStreamEventBus(
                streamName = streamName,
                consumerGroup = consumerGroup,
                coroutineScope = coroutineScope
            )
        } else {
            // Use provided consumer name
            RedisStreamEventBus(
                streamName = streamName,
                consumerGroup = consumerGroup,
                consumerName = consumerName,
                coroutineScope = coroutineScope
            )
        }
    }
}
