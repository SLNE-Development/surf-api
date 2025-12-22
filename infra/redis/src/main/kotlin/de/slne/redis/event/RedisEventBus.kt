package de.slne.redis.event

import io.lettuce.core.RedisClient
import io.lettuce.core.pubsub.RedisPubSubListener
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import kotlin.reflect.KClass

/**
 * Main event bus for Redis events using Lettuce.
 * Manages publishing and subscribing to Redis events across multiple servers asynchronously.
 */
class RedisEventBus(
    redisUri: String,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val client: RedisClient = RedisClient.create(redisUri)
    private val pubConnection: StatefulRedisPubSubConnection<String, String> = client.connectPubSub()
    private val subConnection: StatefulRedisPubSubConnection<String, String> = client.connectPubSub()
    
    private val eventHandlers = mutableMapOf<KClass<out RedisEvent>, MutableList<EventHandler>>()
    
    // Lookup for registered event types to support polymorphic deserialization
    private val eventTypeRegistry = mutableMapOf<String, KClass<out RedisEvent>>()
    
    // Cache serializers for better performance
    private val serializerCache = mutableMapOf<Class<*>, kotlinx.serialization.KSerializer<RedisEvent>>()
    
    companion object {
        private const val REDIS_CHANNEL = "surf-redis:events"
    }
    
    init {
        setupSubscription()
    }
    
    private fun setupSubscription() {
        subConnection.addListener(object : RedisPubSubListener<String, String> {
            override fun message(channel: String, message: String) {
                if (channel == REDIS_CHANNEL) {
                    // Handle message asynchronously
                    coroutineScope.launch {
                        handleIncomingMessage(message)
                    }
                }
            }
            
            override fun message(pattern: String, channel: String, message: String) {}
            override fun subscribed(channel: String, count: Long) {}
            override fun psubscribed(pattern: String, count: Long) {}
            override fun unsubscribed(channel: String, count: Long) {}
            override fun punsubscribed(pattern: String, count: Long) {}
        })
        
        subConnection.sync().subscribe(REDIS_CHANNEL)
    }
    
    private suspend fun handleIncomingMessage(message: String) {
        try {
            val envelope = Json.decodeFromString<EventEnvelope>(message)
            val eventClass = eventTypeRegistry[envelope.eventClass]
            
            if (eventClass == null) {
                System.err.println("Unknown event type: ${envelope.eventClass}")
                return
            }
            
            // Deserialize using the registered serializer
            val event = deserializeEvent(eventClass, envelope.eventData)
            
            // Invoke handlers asynchronously
            eventHandlers[eventClass]?.forEach { handler ->
                coroutineScope.launch {
                    try {
                        handler.invoke(event)
                    } catch (e: Exception) {
                        System.err.println("Error handling event ${event::class.simpleName}: ${e.message}")
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            System.err.println("Error deserializing event: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Publish an event to all subscribed servers/listeners asynchronously.
     * @param event The event to publish
     */
    suspend fun publish(event: RedisEvent) {
        val eventClass = event::class.java.name
        val eventData = serializeEvent(event)
        val envelope = EventEnvelope(eventClass, eventData)
        val message = Json.encodeToString(EventEnvelope.serializer(), envelope)
        
        // Publish asynchronously using coroutines with Lettuce async API
        withContext(Dispatchers.IO) {
            pubConnection.async().publish(REDIS_CHANNEL, message).await()
        }
    }
    
    /**
     * Publish an event synchronously (blocking).
     * @param event The event to publish
     */
    fun publishBlocking(event: RedisEvent) {
        runBlocking {
            publish(event)
        }
    }
    
    /**
     * Register a listener object. All methods annotated with @Subscribe will be registered.
     * Uses MethodHandles for better performance compared to reflection.
     * @param listener The listener object containing @Subscribe annotated methods
     */
    fun registerListener(listener: Any) {
        val lookup = MethodHandles.lookup()
        val listenerClass = listener::class
        val methods = listenerClass.java.declaredMethods
        
        // Register event types from listener methods
        for (method in methods) {
            if (method.isAnnotationPresent(OnRedisEvent::class.java)) {
                if (method.parameterCount == 1) {
                    val paramType = method.parameters[0].type
                    if (RedisEvent::class.java.isAssignableFrom(paramType)) {
                        @Suppress("UNCHECKED_CAST")
                        val eventClass = paramType.kotlin as KClass<out RedisEvent>
                        
                        // Register event type for deserialization
                        eventTypeRegistry[paramType.name] = eventClass
                        
                        // Set accessible before creating MethodHandle for private methods
                        method.isAccessible = true
                        
                        // Create MethodHandle for better performance
                        val methodHandle = lookup.unreflect(method)
                        registerHandler(eventClass, listener, methodHandle)
                    }
                }
            }
        }
    }
    
    /**
     * Unregister a listener object and all its event handlers.
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
    
    /**
     * Helper function to serialize an event using Kotlin Serialization.
     * Caches serializers for better performance.
     */
    private fun serializeEvent(event: RedisEvent): String {
        val eventClass = event::class.java
        @Suppress("UNCHECKED_CAST")
        val serializer = serializerCache.getOrPut(eventClass) {
            serializer(eventClass) as kotlinx.serialization.KSerializer<RedisEvent>
        }
        return Json.encodeToString(serializer, event)
    }
    
    /**
     * Helper function to deserialize an event using Kotlin Serialization.
     * Caches serializers for better performance.
     */
    private fun deserializeEvent(eventClass: KClass<out RedisEvent>, eventData: String): RedisEvent {
        @Suppress("UNCHECKED_CAST")
        val serializer = serializerCache.getOrPut(eventClass.java) {
            serializer(eventClass.java) as kotlinx.serialization.KSerializer<RedisEvent>
        }
        return Json.decodeFromString(serializer, eventData)
    }
    
    /**
     * Close the Redis connections and clean up resources.
     */
    fun close() {
        coroutineScope.cancel()
        pubConnection.close()
        subConnection.close()
        client.shutdown()
    }
    
    @Serializable
    private data class EventEnvelope(
        val eventClass: String,
        val eventData: String
    )
    
    private class EventHandler(
        val instance: Any,
        val methodHandle: MethodHandle
    ) {
        fun invoke(event: RedisEvent) {
            methodHandle.invoke(instance, event)
        }
    }
}
