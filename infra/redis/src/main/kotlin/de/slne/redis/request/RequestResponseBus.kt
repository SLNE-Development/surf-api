package de.slne.redis.request

import io.lettuce.core.RedisClient
import io.lettuce.core.pubsub.RedisPubSubListener
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.javaMethod

/**
 * Request-Response bus for Redis-based request/response pattern using Lettuce.
 * Manages sending requests and handling responses across multiple servers asynchronously.
 */
class RequestResponseBus(
    redisUri: String,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val client: RedisClient = RedisClient.create(redisUri)
    private val pubConnection: StatefulRedisPubSubConnection<String, String> = client.connectPubSub()
    private val subConnection: StatefulRedisPubSubConnection<String, String> = client.connectPubSub()
    
    // Maps request types to their handlers
    private val requestHandlers = mutableMapOf<KClass<out RedisRequest>, MutableList<RequestHandlerInfo>>()
    
    // Pending requests waiting for responses
    private val pendingRequests = ConcurrentHashMap<String, CompletableDeferred<RedisResponse>>()
    
    // Lookup for registered types to support polymorphic deserialization
    private val requestTypeRegistry = mutableMapOf<String, KClass<out RedisRequest>>()
    private val responseTypeRegistry = mutableMapOf<String, KClass<out RedisResponse>>()
    
    // Cache serializers for better performance
    private val serializerCache = mutableMapOf<Class<*>, kotlinx.serialization.KSerializer<Any>>()
    
    companion object {
        private const val REQUEST_CHANNEL = "surf-redis:requests"
        private const val RESPONSE_CHANNEL = "surf-redis:responses"
        private const val DEFAULT_TIMEOUT_MS = 3000L
    }
    
    init {
        setupSubscription()
    }
    
    private fun setupSubscription() {
        subConnection.addListener(object : RedisPubSubListener<String, String> {
            override fun message(channel: String, message: String) {
                coroutineScope.launch {
                    when (channel) {
                        REQUEST_CHANNEL -> handleIncomingRequest(message)
                        RESPONSE_CHANNEL -> handleIncomingResponse(message)
                    }
                }
            }
            
            override fun message(pattern: String, channel: String, message: String) {}
            override fun subscribed(channel: String, count: Long) {}
            override fun psubscribed(pattern: String, count: Long) {}
            override fun unsubscribed(channel: String, count: Long) {}
            override fun punsubscribed(pattern: String, count: Long) {}
        })
        
        subConnection.sync().subscribe(REQUEST_CHANNEL, RESPONSE_CHANNEL)
    }
    
    private suspend fun handleIncomingRequest(message: String) {
        try {
            val envelope = Json.decodeFromString<RequestEnvelope>(message)
            val requestClass = requestTypeRegistry[envelope.requestClass]
            
            if (requestClass == null) {
                System.err.println("Unknown request type: ${envelope.requestClass}")
                return
            }
            
            // Deserialize the request
            val request = deserializeRequest(requestClass, envelope.requestData)
            
            // Find and invoke handlers
            requestHandlers[requestClass]?.forEach { handler ->
                coroutineScope.launch {
                    try {
                        val response = handler.invoke(request)
                        // Send response back
                        sendResponse(envelope.requestId, response)
                    } catch (e: Exception) {
                        System.err.println("Error handling request ${request::class.simpleName}: ${e.message}")
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            System.err.println("Error deserializing request: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private suspend fun handleIncomingResponse(message: String) {
        try {
            val envelope = Json.decodeFromString<ResponseEnvelope>(message)
            val responseClass = responseTypeRegistry[envelope.responseClass]
            
            if (responseClass == null) {
                System.err.println("Unknown response type: ${envelope.responseClass}")
                return
            }
            
            // Deserialize the response
            val response = deserializeResponse(responseClass, envelope.responseData)
            
            // Complete the pending request
            pendingRequests.remove(envelope.requestId)?.complete(response)
        } catch (e: Exception) {
            System.err.println("Error deserializing response: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Send a request and wait for a response asynchronously.
     * @param request The request to send
     * @param timeoutMs Timeout in milliseconds (default 3000ms)
     * @return The response
     * @throws RequestTimeoutException if no response is received within the timeout
     */
    suspend fun <T : RedisResponse> sendRequest(
        request: RedisRequest,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS
    ): T {
        val requestId = UUID.randomUUID().toString()
        val deferred = CompletableDeferred<RedisResponse>()
        pendingRequests[requestId] = deferred
        
        try {
            // Publish the request
            val requestClass = request::class.java.name
            val requestData = serializeRequest(request)
            val envelope = RequestEnvelope(requestId, requestClass, requestData)
            val message = Json.encodeToString(RequestEnvelope.serializer(), envelope)
            
            withContext(Dispatchers.IO) {
                pubConnection.async().publish(REQUEST_CHANNEL, message).await()
            }
            
            // Wait for response with timeout
            @Suppress("UNCHECKED_CAST")
            return withTimeout(timeoutMs) {
                deferred.await() as T
            }
        } catch (e: TimeoutCancellationException) {
            pendingRequests.remove(requestId)
            throw RequestTimeoutException("Request timed out after ${timeoutMs}ms: ${request::class.simpleName}")
        } catch (e: Exception) {
            pendingRequests.remove(requestId)
            throw e
        }
    }
    
    /**
     * Send a request and wait for a response synchronously (blocking).
     * @param request The request to send
     * @param timeoutMs Timeout in milliseconds (default 3000ms)
     * @return The response
     * @throws RequestTimeoutException if no response is received within the timeout
     */
    fun <T : RedisResponse> sendRequestBlocking(
        request: RedisRequest,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS
    ): T {
        return runBlocking {
            sendRequest(request, timeoutMs)
        }
    }
    
    private suspend fun sendResponse(requestId: String, response: RedisResponse) {
        val responseClass = response::class.java.name
        val responseData = serializeResponse(response)
        val envelope = ResponseEnvelope(requestId, responseClass, responseData)
        val message = Json.encodeToString(ResponseEnvelope.serializer(), envelope)
        
        withContext(Dispatchers.IO) {
            pubConnection.async().publish(RESPONSE_CHANNEL, message).await()
        }
    }
    
    /**
     * Register a request handler object. All methods annotated with @RequestHandler will be registered.
     * @param handler The handler object containing @RequestHandler annotated methods
     */
    fun registerRequestHandler(handler: Any) {
        val handlerClass = handler::class
        val methods = handlerClass.java.declaredMethods
        
        for (method in methods) {
            if (method.isAnnotationPresent(RequestHandler::class.java)) {
                if (method.parameterCount == 1) {
                    val paramType = method.parameters[0].type
                    val returnType = method.returnType
                    
                    if (RedisRequest::class.java.isAssignableFrom(paramType) &&
                        RedisResponse::class.java.isAssignableFrom(returnType)) {
                        
                        @Suppress("UNCHECKED_CAST")
                        val requestClass = paramType.kotlin as KClass<out RedisRequest>
                        @Suppress("UNCHECKED_CAST")
                        val responseClass = returnType.kotlin as KClass<out RedisResponse>
                        
                        // Register types for deserialization
                        requestTypeRegistry[paramType.name] = requestClass
                        responseTypeRegistry[returnType.name] = responseClass
                        
                        // Check if method is suspend
                        val kFunction = handlerClass.memberFunctions.find { 
                            it.javaMethod == method 
                        }
                        val isSuspend = kFunction?.isSuspend ?: false
                        
                        method.isAccessible = true
                        
                        registerHandler(requestClass, handler, method, isSuspend, kFunction)
                    }
                }
            }
        }
    }
    
    /**
     * Unregister a request handler object and all its handlers.
     * @param handler The handler object to unregister
     */
    fun unregisterRequestHandler(handler: Any) {
        requestHandlers.values.forEach { handlers ->
            handlers.removeIf { it.instance == handler }
        }
    }
    
    private fun registerHandler(
        requestClass: KClass<out RedisRequest>,
        instance: Any,
        method: java.lang.reflect.Method,
        isSuspend: Boolean,
        kFunction: kotlin.reflect.KCallable<*>?
    ) {
        val handlerInfo = RequestHandlerInfo(instance, method, isSuspend, kFunction)
        requestHandlers.getOrPut(requestClass) { mutableListOf() }.add(handlerInfo)
    }
    
    /**
     * Helper function to serialize a request using Kotlin Serialization.
     */
    private fun serializeRequest(request: RedisRequest): String {
        val requestClass = request::class.java
        @Suppress("UNCHECKED_CAST")
        val serializer = serializerCache.getOrPut(requestClass) {
            serializer(requestClass) as kotlinx.serialization.KSerializer<Any>
        }
        return Json.encodeToString(serializer, request)
    }
    
    /**
     * Helper function to deserialize a request using Kotlin Serialization.
     */
    private fun deserializeRequest(requestClass: KClass<out RedisRequest>, requestData: String): RedisRequest {
        @Suppress("UNCHECKED_CAST")
        val serializer = serializerCache.getOrPut(requestClass.java) {
            serializer(requestClass.java) as kotlinx.serialization.KSerializer<Any>
        }
        return Json.decodeFromString(serializer, requestData) as RedisRequest
    }
    
    /**
     * Helper function to serialize a response using Kotlin Serialization.
     */
    private fun serializeResponse(response: RedisResponse): String {
        val responseClass = response::class.java
        @Suppress("UNCHECKED_CAST")
        val serializer = serializerCache.getOrPut(responseClass) {
            serializer(responseClass) as kotlinx.serialization.KSerializer<Any>
        }
        return Json.encodeToString(serializer, response)
    }
    
    /**
     * Helper function to deserialize a response using Kotlin Serialization.
     */
    private fun deserializeResponse(responseClass: KClass<out RedisResponse>, responseData: String): RedisResponse {
        @Suppress("UNCHECKED_CAST")
        val serializer = serializerCache.getOrPut(responseClass.java) {
            serializer(responseClass.java) as kotlinx.serialization.KSerializer<Any>
        }
        return Json.decodeFromString(serializer, responseData) as RedisResponse
    }
    
    /**
     * Close the Redis connections and clean up resources.
     */
    fun close() {
        // Cancel all pending requests
        pendingRequests.values.forEach { 
            it.cancel(CancellationException("RequestResponseBus closed"))
        }
        pendingRequests.clear()
        
        coroutineScope.cancel()
        pubConnection.close()
        subConnection.close()
        client.shutdown()
    }
    
    @Serializable
    private data class RequestEnvelope(
        val requestId: String,
        val requestClass: String,
        val requestData: String
    )
    
    @Serializable
    private data class ResponseEnvelope(
        val requestId: String,
        val responseClass: String,
        val responseData: String
    )
    
    private class RequestHandlerInfo(
        val instance: Any,
        val method: java.lang.reflect.Method,
        val isSuspend: Boolean,
        val kFunction: kotlin.reflect.KCallable<*>?
    ) {
        suspend fun invoke(request: RedisRequest): RedisResponse {
            return if (isSuspend && kFunction != null) {
                // Call suspend function
                kFunction.callSuspend(instance, request) as RedisResponse
            } else {
                // Call regular function
                method.invoke(instance, request) as RedisResponse
            }
        }
    }
}
