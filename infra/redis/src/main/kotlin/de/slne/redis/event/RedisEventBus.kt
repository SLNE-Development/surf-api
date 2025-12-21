package de.slne.redis.event

import com.google.gson.Gson
import io.lettuce.core.RedisClient
import io.lettuce.core.pubsub.RedisPubSubListener
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands
import java.lang.reflect.Method
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.javaMethod

/**
 * Main event bus for Redis events using Lettuce.
 * Manages publishing and subscribing to Redis events across multiple servers.
 */
class RedisEventBus(redisUri: String) {
    private val client: RedisClient = RedisClient.create(redisUri)
    private val pubConnection: StatefulRedisPubSubConnection<String, String> = client.connectPubSub()
    private val subConnection: StatefulRedisPubSubConnection<String, String> = client.connectPubSub()
    private val gson = Gson()
    
    private val eventHandlers = mutableMapOf<KClass<out RedisEvent>, MutableList<EventHandler>>()
    
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
                    handleIncomingMessage(message)
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
    
    private fun handleIncomingMessage(message: String) {
        try {
            val envelope = gson.fromJson(message, EventEnvelope::class.java)
            val eventClass = Class.forName(envelope.eventClass).kotlin as KClass<out RedisEvent>
            val event = gson.fromJson(envelope.eventData, eventClass.java) as RedisEvent
            
            eventHandlers[eventClass]?.forEach { handler ->
                try {
                    handler.invoke(event)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Publish an event to all subscribed servers/listeners.
     * @param event The event to publish
     */
    fun publish(event: RedisEvent) {
        val eventClass = event::class.java.name
        val eventData = gson.toJson(event)
        val envelope = EventEnvelope(eventClass, eventData)
        val message = gson.toJson(envelope)
        
        pubConnection.sync().publish(REDIS_CHANNEL, message)
    }
    
    /**
     * Register a listener object. All methods annotated with @Subscribe will be registered.
     * @param listener The listener object containing @Subscribe annotated methods
     */
    fun registerListener(listener: Any) {
        val methods = listener::class.java.declaredMethods
        
        for (method in methods) {
            if (method.isAnnotationPresent(Subscribe::class.java)) {
                if (method.parameterCount == 1) {
                    val paramType = method.parameters[0].type
                    if (RedisEvent::class.java.isAssignableFrom(paramType)) {
                        @Suppress("UNCHECKED_CAST")
                        val eventClass = paramType.kotlin as KClass<out RedisEvent>
                        registerHandler(eventClass, listener, method)
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
    
    private fun registerHandler(eventClass: KClass<out RedisEvent>, instance: Any, method: Method) {
        method.isAccessible = true
        val handler = EventHandler(instance, method)
        eventHandlers.getOrPut(eventClass) { mutableListOf() }.add(handler)
    }
    
    /**
     * Close the Redis connections and clean up resources.
     */
    fun close() {
        pubConnection.close()
        subConnection.close()
        client.shutdown()
    }
    
    private data class EventEnvelope(
        val eventClass: String,
        val eventData: String
    )
    
    private class EventHandler(
        val instance: Any,
        val method: Method
    ) {
        fun invoke(event: RedisEvent) {
            method.invoke(instance, event)
        }
    }
}
