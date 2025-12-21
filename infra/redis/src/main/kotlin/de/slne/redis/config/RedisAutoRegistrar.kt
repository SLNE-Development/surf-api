package de.slne.redis.config

import de.slne.redis.event.RedisEventBus
import de.slne.redis.event.RedisEventListener
import de.slne.redis.request.RequestResponseBus
import de.slne.redis.request.RedisRequestHandler
import de.slne.redis.stream.RedisStreamEventBus
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Configuration
import jakarta.annotation.PostConstruct

/**
 * Auto-registers all Spring beans annotated with @RedisEventListener and @RedisRequestHandler.
 * This enables automatic discovery and registration of listeners and handlers.
 */
@Configuration
class RedisAutoRegistrar(
    private val applicationContext: ApplicationContext
) {
    
    private val logger = LoggerFactory.getLogger(RedisAutoRegistrar::class.java)
    
    @Autowired(required = false)
    private var eventBus: RedisEventBus? = null
    
    @Autowired(required = false)
    private var streamEventBus: RedisStreamEventBus? = null
    
    @Autowired(required = false)
    private var requestBus: RequestResponseBus? = null
    
    @PostConstruct
    fun registerListenersAndHandlers() {
        // Register all event listeners with regular event bus
        eventBus?.let { bus ->
            val listeners = applicationContext.getBeansWithAnnotation(RedisEventListener::class.java)
            listeners.values.forEach { listener ->
                bus.registerListener(listener)
                logger.info("Auto-registered Redis event listener: {}", listener::class.simpleName)
            }
        }
        
        // Register all event listeners with stream event bus
        streamEventBus?.let { bus ->
            val listeners = applicationContext.getBeansWithAnnotation(RedisEventListener::class.java)
            listeners.values.forEach { listener ->
                bus.registerListener(listener)
                logger.info("Auto-registered Redis stream event listener: {}", listener::class.simpleName)
            }
        }
        
        // Register all request handlers
        requestBus?.let { bus ->
            val handlers = applicationContext.getBeansWithAnnotation(RedisRequestHandler::class.java)
            handlers.values.forEach { handler ->
                bus.registerRequestHandler(handler)
                logger.info("Auto-registered Redis request handler: {}", handler::class.simpleName)
            }
        }
    }
}
