package de.slne.redis.request

import com.google.common.flogger.LogPerBucketingStrategy
import com.google.common.flogger.StackSize
import de.slne.redis.RedisApi
import de.slne.redis.request.RequestResponseBus.Companion.REQUEST_CHANNEL
import de.slne.redis.request.RequestResponseBus.Companion.RESPONSE_CHANNEL
import de.slne.redis.util.KotlinSerializerCache
import dev.slne.surf.surfapi.core.api.serializer.java.uuid.SerializableUUID
import dev.slne.surf.surfapi.core.api.util.logger
import io.lettuce.core.pubsub.RedisPubSubListener
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import kotlinx.coroutines.*
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.future.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import org.jetbrains.annotations.Blocking
import java.lang.invoke.LambdaMetafactory
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.InaccessibleObjectException
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Redis-backed request/response bus based on Redis Pub/Sub.
 *
 * This bus supports:
 * - Publishing requests to [REQUEST_CHANNEL]
 * - Handling incoming requests via methods annotated with [HandleRedisRequest]
 * - Publishing responses to [RESPONSE_CHANNEL]
 * - Awaiting responses for outgoing requests using a per-request correlation ID
 *
 * Handler methods are invoked synchronously on the Redis Pub/Sub thread.
 * If suspending or long-running work is required, the handler must launch its own coroutine.
 */
class RequestResponseBus internal constructor(
    private val api: RedisApi
) {

    private val requestHandlers =
        Object2ObjectOpenHashMap<Class<out RedisRequest>, RequestConsumer>()
    private val pendingRequests = ConcurrentHashMap<UUID, CompletableDeferred<RedisResponse>>()

    private val requestTypeRegistry = Object2ObjectOpenHashMap<String, Class<out RedisRequest>>()
    private val responseTypeRegistry = ConcurrentHashMap<String, Class<out RedisResponse>>()

    private val serializerCache = KotlinSerializerCache<Any>(api.json.serializersModule)

    companion object {
        private val log = logger()
        private const val REQUEST_CHANNEL = "surf-redis:requests"
        private const val RESPONSE_CHANNEL = "surf-redis:responses"

        /**
         * Default timeout for awaiting responses in milliseconds.
         */
        const val DEFAULT_TIMEOUT_MS = 5000L

        private val lookup = MethodHandles.lookup()
    }

    /**
     * Initializes the bus by subscribing to request/response channels.
     *
     * This method is blocking and should only be called during startup.
     */
    @Blocking
    internal fun init() {
        setupSubscription()
    }

    /**
     * Installs the Redis Pub/Sub listener and subscribes to request/response channels.
     *
     * Incoming messages are handled synchronously on the Pub/Sub thread.
     */
    @Blocking
    private fun setupSubscription() {
        api.pubSubConnection.addListener(object : RedisPubSubListener<String, String> {
            override fun message(channel: String, message: String) {
                if (channel == REQUEST_CHANNEL) handleIncomingRequest(message)
                else if (channel == RESPONSE_CHANNEL) handleIncomingResponse(message)
            }

            override fun message(pattern: String, channel: String, message: String) = Unit
            override fun subscribed(channel: String, count: Long) = Unit
            override fun psubscribed(pattern: String, count: Long) = Unit
            override fun unsubscribed(channel: String, count: Long) = Unit
            override fun punsubscribed(pattern: String, count: Long) = Unit
        })

        api.pubSubConnection.sync().subscribe(REQUEST_CHANNEL, RESPONSE_CHANNEL)
    }

    /**
     * Handles an incoming request message.
     *
     * The request envelope is decoded, the request is deserialized, and the registered handler for the
     * request type is invoked. The provided [RequestContext] exposes a response callback which publishes
     * a response back to Redis.
     */
    private fun handleIncomingRequest(message: String) {
        val envelope = try {
            api.json.decodeFromString<RequestEnvelope>(message)
        } catch (e: SerializationException) {
            log.atWarning()
                .withCause(e)
                .log("Unable to deserialize request envelope: ${e.message}")
            return
        }

        val requestClass = requestTypeRegistry[envelope.requestClass]

        if (requestClass == null) {
            log.atSevere()
                .per(envelope.requestClass, LogPerBucketingStrategy.byHashCode(128))
                .atMostEvery(1, TimeUnit.MINUTES)
                .log("No registered request class for name: ${envelope.requestClass} - ignoring request.")
            return
        }

        val request = deserializeRequest(requestClass, envelope.requestData) ?: return
        val handler = requestHandlers[requestClass] ?: return
        val context = RequestContext(
            request = request,
            respondCallback = { response ->
                sendResponse(envelope.requestId, response)
            }
        )

        try {
            handler.accept(context)
        } catch (e: Throwable) {
            log.atWarning()
                .withCause(e)
                .log("Error handling request ${request::class.simpleName}: ${e.message}")

        }
    }

    /**
     * Handles an incoming response message.
     *
     * The response envelope is decoded and the corresponding pending request is completed.
     * Responses without a matching pending request are ignored.
     */
    private fun handleIncomingResponse(message: String) {
        val envelope = try {
            api.json.decodeFromString<ResponseEnvelope>(message)
        } catch (e: SerializationException) {
            log.atWarning()
                .withCause(e)
                .log("Unable to deserialize response envelope: ${e.message}")
            return
        }

        val responseClass = responseTypeRegistry[envelope.responseClass]

        if (responseClass == null) {
            log.atWarning()
                .per(envelope.responseClass, LogPerBucketingStrategy.byHashCode(128))
                .atMostEvery(1, TimeUnit.MINUTES)
                .log("No registered response class for name: ${envelope.responseClass} - ignoring response.")
            return
        }

        val response = deserializeResponse(responseClass, envelope.responseData) ?: return
        val deferred = pendingRequests.remove(envelope.requestId)
        if (deferred == null) {
            log.atWarning()
                .log("No pending request found for response with ID: ${envelope.requestId} - ignoring response.")
            return
        }

        deferred.complete(response)
    }

    /**
     * Sends a request and awaits a response of type [T].
     *
     * The response type is registered for deserialization using the fully-qualified class name.
     *
     * @param request request payload to publish
     * @param timeoutMs timeout for awaiting the response
     * @return the response payload
     * @throws RequestTimeoutException if no response is received within [timeoutMs]
     * @throws ClassCastException if the received response does not match [T]
     */
    suspend inline fun <reified T : RedisResponse> sendRequest(
        request: RedisRequest,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS
    ): T {
        return sendRequest(request, T::class.java, timeoutMs)
    }

    /**
     * Sends a request and awaits a response of the given [responseType].
     *
     * @param request request payload to publish
     * @param responseType expected response type
     * @param timeoutMs timeout for awaiting the response
     * @return the response payload
     * @throws RequestTimeoutException if no response is received within [timeoutMs]
     * @throws ClassCastException if the received response does not match [responseType]
     */
    suspend fun <T : RedisResponse> sendRequest(
        request: RedisRequest,
        responseType: Class<T>,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS
    ): T {
        val requestId = UUID.randomUUID()
        val requestData = serializeRequest(request) ?: throw IllegalStateException()
        val deferred = CompletableDeferred<RedisResponse>()

        pendingRequests[requestId] = deferred
        responseTypeRegistry[responseType.name] = responseType

        val envelope = RequestEnvelope.forRequest(request, requestId, requestData)
        val message = api.json.encodeToString(envelope)

        api.pubSubConnection.async()
            .publish(REQUEST_CHANNEL, message)
            .await()

        try {
            return withTimeout(timeoutMs) {
                val response = deferred.await()
                if (!responseType.isInstance(response)) {
                    throw ClassCastException(
                        "Expected response type ${responseType.name} but got ${response::class.java.name}"
                    )
                }
                @Suppress("UNCHECKED_CAST")
                response as T
            }
        } catch (_: TimeoutCancellationException) {
            pendingRequests.remove(requestId)
            throw RequestTimeoutException("Request timed out after ${timeoutMs}ms: ${request::class.simpleName}")
        } catch (e: Exception) {
            pendingRequests.remove(requestId)
            throw e
        }
    }

    /**
     * Publishes a response for a given request ID.
     *
     * @return a [Deferred] containing the number of receiving subscribers,
     *         or `0` if the response could not be serialized
     */
    private fun sendResponse(requestId: UUID, response: RedisResponse): Deferred<Long> {
        val responseData = serializeResponse(response) ?: return CompletableDeferred(0L)
        val envelope = ResponseEnvelope.forResponse(response, requestId, responseData)
        val message = api.json.encodeToString(envelope)

        return api.pubSubConnection.async()
            .publish(RESPONSE_CHANNEL, message)
            .asDeferred()
    }

    /**
     * Registers all request handler methods on the given object.
     *
     * Methods annotated with [HandleRedisRequest] must:
     * - Have exactly one parameter
     * - The parameter type must be `RequestContext<T>`
     * - `T` must be a subtype of [RedisRequest]
     * - Not be a `suspend` function
     *
     * Only a single handler may be registered per request type; duplicates are ignored.
     *
     * @param handler object containing [HandleRedisRequest]-annotated methods
     */
    fun registerRequestHandler(handler: Any) {
        require(!api.isFrozen()) { "Cannot register request handler after RedisApi has been frozen." }

        val handlerClass = handler.javaClass
        val methods = handlerClass.declaredMethods

        for (method in methods) {
            if (!method.isAnnotationPresent(HandleRedisRequest::class.java)) continue

            if (method.parameterCount != 1) {
                log.atWarning()
                    .withStackTrace(StackSize.MEDIUM)
                    .log("Method ${method.name} in class ${handlerClass.name} is annotated with @RequestHandler but has ${method.parameterCount} parameters. Expected exactly 1 parameter of type RequestContext<T>.")
                continue
            }

            val paramRaw = method.parameterTypes[0]
            if (!RequestContext::class.java.isAssignableFrom(paramRaw)) {
                log.atWarning()
                    .withStackTrace(StackSize.MEDIUM)
                    .log("Method ${method.name} in class ${handlerClass.name} is annotated with @RequestHandler but has parameter of type ${paramRaw.name}. Expected parameter of type RequestContext<T>.")
                continue
            }

            val generic = method.genericParameterTypes[0]
            if (generic !is ParameterizedType) {
                log.atWarning()
                    .withStackTrace(StackSize.MEDIUM)
                    .log("Method ${method.name} in class ${handlerClass.name} has non-parameterized RequestContext parameter.")
                continue
            }

            val requestType = generic.actualTypeArguments[0] as? Class<*>

            if (requestType == null) {
                log.atWarning()
                    .withStackTrace(StackSize.MEDIUM)
                    .log("Method ${method.name} in class ${handlerClass.name} is annotated with @RequestHandler but has parameter of type RequestContext<T> but T is not a class.")
                continue
            }

            if (!RedisRequest::class.java.isAssignableFrom(requestType)) {
                log.atWarning()
                    .withStackTrace(StackSize.MEDIUM)
                    .log("Method ${method.name} in class ${handlerClass.name} is annotated with @RequestHandler but has parameter of type RequestContext<${requestType.name}>. Expected parameter of type RequestContext<T> where T is a subclass of RedisRequest.")
                continue
            }

            @Suppress("UNCHECKED_CAST")
            requestType as Class<out RedisRequest>

            requestTypeRegistry[requestType.name] = requestType

            try {
                method.isAccessible = true
            } catch (e: InaccessibleObjectException) {
                log.atWarning()
                    .withCause(e)
                    .log("Unable to make method ${method.name} in class ${handlerClass.name} accessible.")
                continue
            }

            val consumer = createRequestConsumer(handler, method)
            val current = requestHandlers.putIfAbsent(requestType, consumer)

            if (current != null) {
                log.atWarning()
                    .withStackTrace(StackSize.MEDIUM)
                    .log("A request handler is already registered for request type: ${requestType.name} - ignoring duplicate registration in method ${method.name} of class ${handlerClass.name}.")
            }
        }
    }

    /**
     * Creates a fast [RequestConsumer] for a handler method.
     *
     * The method is converted into a JVM-generated lambda via [LambdaMetafactory], resulting in
     * near-direct invocation performance.
     */
    private fun createRequestConsumer(instance: Any, method: Method): RequestConsumer {
        val consumerMethodType = MethodType.methodType(Void.TYPE, RequestContext::class.java)

        val impl = lookup.unreflect(method)
            .bindTo(instance)
            .asType(consumerMethodType)

        val callSite = LambdaMetafactory.metafactory(
            lookup,
            "accept",
            MethodType.methodType(RequestConsumer::class.java),
            consumerMethodType,
            impl,
            consumerMethodType
        )

        val target = callSite.target

        return target.invokeExact() as RequestConsumer
    }

    /**
     * Serializes a request to JSON.
     *
     * @return the serialized request, or `null` if no serializer is available or serialization fails
     */
    private fun serializeRequest(request: RedisRequest): String? {
        val serializer = serializerCache.get(request.javaClass)

        if (serializer == null) {
            log.atWarning()
                .log("No serializer found for request class: ${request::class.simpleName} - ignoring request.")
            return null
        }

        try {
            return api.json.encodeToString(serializer, request)
        } catch (e: SerializationException) {
            log.atWarning()
                .withCause(e)
                .log("Unable to serialize request: ${e.message}")
            return null
        }
    }

    /**
     * Deserializes a request of the given type from JSON.
     *
     * @return the deserialized request, or `null` if no serializer is available or deserialization fails
     */
    private fun deserializeRequest(
        requestClass: Class<out RedisRequest>,
        requestData: String
    ): RedisRequest? {
        val serializer = serializerCache.get(requestClass)

        if (serializer == null) {
            log.atWarning()
                .log("No serializer found for request class: ${requestClass.name}")
            return null
        }

        try {
            val deserialized = api.json.decodeFromString(serializer, requestData)
            if (deserialized !is RedisRequest) {
                log.atWarning()
                    .log("Deserialized object is not a RedisRequest: ${deserialized::class.simpleName}")
                return null
            }

            return deserialized
        } catch (e: SerializationException) {
            log.atWarning()
                .withCause(e)
                .log("Unable to deserialize request: ${e.message}")
            return null
        }
    }

    /**
     * Serializes a response to JSON.
     *
     * @return the serialized response, or `null` if no serializer is available or serialization fails
     */
    private fun serializeResponse(response: RedisResponse): String? {
        val serializer = serializerCache.get(response.javaClass)

        if (serializer == null) {
            log.atWarning()
                .log("No serializer found for response class: ${response::class.simpleName} - ignoring response.")
            return null
        }

        try {
            return api.json.encodeToString(serializer, response)
        } catch (e: SerializationException) {
            log.atWarning()
                .withCause(e)
                .log("Unable to serialize response: ${e.message}")
            return null
        }
    }

    /**
     * Deserializes a response of the given type from JSON.
     *
     * @return the deserialized response, or `null` if no serializer is available or deserialization fails
     */
    private fun deserializeResponse(
        responseClass: Class<out RedisResponse>,
        responseData: String
    ): RedisResponse? {
        val serializer = serializerCache.get(responseClass)

        if (serializer == null) {
            log.atWarning()
                .log("No serializer found for response class: ${responseClass.name} - ignoring response.")
            return null
        }

        try {
            val deserialized = api.json.decodeFromString(serializer, responseData)
            if (deserialized !is RedisResponse) {
                log.atWarning()
                    .log("Deserialized object is not a RedisResponse: ${deserialized::class.simpleName}")
                return null
            }

            return deserialized
        } catch (e: SerializationException) {
            log.atWarning()
                .withCause(e)
                .log("Unable to deserialize response: ${e.message}")
            return null
        }
    }

    /**
     * Cancels all pending requests and clears internal state.
     *
     * This does not unsubscribe from Redis channels; connection lifecycle is owned by [RedisApi].
     */
    fun close() {
        pendingRequests.values.forEach { deferred ->
            deferred.cancel("RequestResponseBus closed")
        }
        pendingRequests.clear()
    }

    /**
     * Wire format for request messages published to Redis.
     */
    @Serializable
    private data class RequestEnvelope(
        val requestId: SerializableUUID,
        val requestClass: String,
        val requestData: String
    ) {
        companion object {
            fun forRequest(request: RedisRequest, requestId: UUID, data: String): RequestEnvelope {
                return RequestEnvelope(
                    requestId = requestId,
                    requestClass = request.javaClass.name,
                    requestData = data
                )
            }
        }
    }

    /**
     * Wire format for response messages published to Redis.
     */
    @Serializable
    private data class ResponseEnvelope(
        val requestId: SerializableUUID,
        val responseClass: String,
        val responseData: String
    ) {
        companion object {
            fun forResponse(
                response: RedisResponse,
                requestId: UUID,
                data: String
            ): ResponseEnvelope {
                return ResponseEnvelope(
                    requestId = requestId,
                    responseClass = response.javaClass.name,
                    responseData = data
                )
            }
        }
    }

    /**
     * Internal functional interface used for fast request dispatch.
     */
    @Suppress("unused")
    @FunctionalInterface
    private fun interface RequestConsumer {
        fun accept(ctx: RequestContext<out RedisRequest>)
    }
}