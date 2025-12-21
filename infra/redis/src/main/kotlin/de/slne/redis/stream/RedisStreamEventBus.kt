package de.slne.redis.stream

import de.slne.redis.RedisApi
import de.slne.redis.event.RedisEvent
import de.slne.redis.event.Subscribe
import io.lettuce.core.Consumer
import io.lettuce.core.StreamMessage
import io.lettuce.core.XReadArgs
import io.lettuce.core.api.StatefulRedisConnection
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import kotlin.reflect.KClass

/**
 * Redis Streams-based event bus implementation.
 * Uses Redis Streams for more reliable event distribution with message persistence
 * and consumer groups for load balancing.
 * 
 * Advantages over pub/sub:
 * - Message persistence: Events are stored and not lost if no consumer is online
 * - Consumer groups: Multiple instances can share the load
 * - Message acknowledgment: Ensures events are processed
 * - Reprocessing: Failed events can be reprocessed
 * 
 * @param streamName The name of the Redis stream
 * @param consumerGroup The consumer group name (default: "default")
 * @param consumerName The consumer name (default: hostname or UUID)
 * @param coroutineScope The coroutine scope for async operations
 */
class RedisStreamEventBus(
    private val streamName: String = "surf-redis:events",
    private val consumerGroup: String = "default",
    private val consumerName: String = generateConsumerName(),
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val connection: StatefulRedisConnection<String, String> = RedisApi.createConnection()
    private val commands = connection.sync()
    
    private val eventHandlers = mutableMapOf<KClass<out RedisEvent>, MutableList<EventHandler>>()
    private val eventTypeRegistry = mutableMapOf<String, KClass<out RedisEvent>>()
    private val serializerCache = mutableMapOf<Class<*>, kotlinx.serialization.KSerializer<RedisEvent>>()
    
    private var consumerJob: Job? = null
    private var isRunning = false
    
    companion object {
        private val logger = LoggerFactory.getLogger(RedisStreamEventBus::class.java)
        
        private fun generateConsumerName(): String {
            return try {
                java.net.InetAddress.getLocalHost().hostName
            } catch (e: Exception) {
                java.util.UUID.randomUUID().toString()
            }
        }
    }
    
    init {
        initializeStream()
        startConsuming()
    }
    
    /**
     * Initialize the stream and consumer group if they don't exist.
     */
    private fun initializeStream() {
        try {
            // Try to create consumer group (will fail if it already exists)
            val streamOffset = XReadArgs.StreamOffset.from(streamName, "0")
            commands.xgroupCreate(streamOffset, consumerGroup)
        } catch (e: Exception) {
            // Group already exists, this is fine
            logger.debug("Consumer group already exists or error creating: ${e.message}")
        }
    }
    
    /**
     * Start consuming messages from the stream.
     */
    private fun startConsuming() {
        isRunning = true
        consumerJob = coroutineScope.launch {
            while (isActive && isRunning) {
                try {
                    consumeMessages()
                } catch (e: CancellationException) {
                    break
                } catch (e: Exception) {
                    logger.error("Error consuming messages: ${e.message}", e)
                    delay(1000) // Wait before retrying
                }
            }
        }
    }
    
    /**
     * Consume messages from the stream using consumer groups.
     */
    private suspend fun consumeMessages() = withContext(Dispatchers.IO) {
        val messages = commands.xreadgroup(
            Consumer.from(consumerGroup, consumerName),
            XReadArgs.Builder.block(1000), // Block for 1 second max
            XReadArgs.StreamOffset.lastConsumed(streamName)
        )
        
        messages?.forEach { message ->
            handleMessage(message)
            // Acknowledge the message
            commands.xack(streamName, consumerGroup, message.id)
        }
    }
    
    /**
     * Handle an incoming stream message.
     */
    private suspend fun handleMessage(message: StreamMessage<String, String>) {
        try {
            val eventClass = message.body["eventClass"] ?: return
            val eventData = message.body["eventData"] ?: return
            
            val eventKClass = eventTypeRegistry[eventClass]
            if (eventKClass == null) {
                logger.warn("Unknown event type: {}", eventClass)
                return
            }
            
            // Deserialize the event
            val event = deserializeEvent(eventKClass, eventData)
            
            // Invoke handlers asynchronously
            eventHandlers[eventKClass]?.forEach { handler ->
                coroutineScope.launch {
                    try {
                        handler.invoke(event)
                    } catch (e: Exception) {
                        logger.error("Error handling event ${event::class.simpleName}: ${e.message}", e)
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Error processing message: ${e.message}", e)
        }
    }
    
    /**
     * Publish an event to the stream asynchronously.
     * 
     * @param event The event to publish
     */
    suspend fun publish(event: RedisEvent) {
        val eventClass = event::class.java.name
        val eventData = serializeEvent(event)
        
        withContext(Dispatchers.IO) {
            commands.xadd(
                streamName,
                mapOf(
                    "eventClass" to eventClass,
                    "eventData" to eventData
                )
            )
        }
    }
    
    /**
     * Publish an event synchronously (blocking).
     * 
     * @param event The event to publish
     */
    fun publishBlocking(event: RedisEvent) {
        runBlocking {
            publish(event)
        }
    }
    
    /**
     * Register a listener object. All methods annotated with @Subscribe will be registered.
     * 
     * @param listener The listener object containing @Subscribe annotated methods
     */
    fun registerListener(listener: Any) {
        val lookup = MethodHandles.lookup()
        val listenerClass = listener::class
        val methods = listenerClass.java.declaredMethods
        
        for (method in methods) {
            if (method.isAnnotationPresent(Subscribe::class.java)) {
                if (method.parameterCount == 1) {
                    val paramType = method.parameters[0].type
                    if (RedisEvent::class.java.isAssignableFrom(paramType)) {
                        @Suppress("UNCHECKED_CAST")
                        val eventClass = paramType.kotlin as KClass<out RedisEvent>
                        
                        // Register event type for deserialization
                        eventTypeRegistry[paramType.name] = eventClass
                        
                        method.isAccessible = true
                        val methodHandle = lookup.unreflect(method)
                        registerHandler(eventClass, listener, methodHandle)
                    }
                }
            }
        }
    }
    
    /**
     * Unregister a listener object and all its event handlers.
     * 
     * @param listener The listener object to unregister
     */
    fun unregisterListener(listener: Any) {
        eventHandlers.values.forEach { handlers ->
            handlers.removeIf { it.instance == listener }
        }
    }
    
    private fun registerHandler(eventClass: KClass<out RedisEvent>, instance: Any, methodHandle: MethodHandle) {
        val handler = EventHandler(instance, methodHandle)
        eventHandlers.getOrPut(eventClass) { mutableListOf() }.add(handler)
    }
    
    private fun serializeEvent(event: RedisEvent): String {
        val eventClass = event::class.java
        @Suppress("UNCHECKED_CAST")
        val serializer = serializerCache.getOrPut(eventClass) {
            serializer(eventClass) as kotlinx.serialization.KSerializer<RedisEvent>
        }
        return Json.encodeToString(serializer, event)
    }
    
    private fun deserializeEvent(eventClass: KClass<out RedisEvent>, eventData: String): RedisEvent {
        @Suppress("UNCHECKED_CAST")
        val serializer = serializerCache.getOrPut(eventClass.java) {
            serializer(eventClass.java) as kotlinx.serialization.KSerializer<RedisEvent>
        }
        return Json.decodeFromString(serializer, eventData)
    }
    
    /**
     * Close the stream consumer and clean up resources.
     */
    fun close() {
        isRunning = false
        consumerJob?.cancel()
        runBlocking {
            consumerJob?.join()
        }
        coroutineScope.cancel()
        connection.close()
    }
    
    private class EventHandler(
        val instance: Any,
        val methodHandle: MethodHandle
    ) {
        fun invoke(event: RedisEvent) {
            methodHandle.invoke(instance, event)
        }
    }
}
