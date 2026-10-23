package dev.slne.surf.nats.js

import dev.slne.surf.nats.js.external.nats.core.ConnectionOptions
import dev.slne.surf.nats.js.external.nats.core.Msg
import dev.slne.surf.nats.js.external.nats.core.NatsConnection
import dev.slne.surf.nats.js.external.nats.core.NatsPublishOptions
import dev.slne.surf.nats.js.external.nats.core.Subscription
import dev.slne.surf.nats.js.external.nats.core.SubscriptionOptions
import dev.slne.surf.nats.js.external.nats.core.credsAuthenticator
import dev.slne.surf.nats.js.external.nats.core.syncIterator
import dev.slne.surf.nats.js.external.nats.jetstream.JetStreamClient
import dev.slne.surf.nats.js.external.nats.jetstream.jetstream
import dev.slne.surf.nats.js.external.protobuf.DescMessage
import dev.slne.surf.nats.js.external.protobuf.DescMethod
import dev.slne.surf.nats.js.external.protobuf.toBinary
import dev.slne.surf.nats.protocol.ProtocolDefaults
import dev.slne.surf.nats.protocol.ProtocolHeaders
import dev.slne.surf.nats.rpc.Recipients
import dev.slne.surf.nats.runtime.ConnectRules
import dev.slne.surf.nats.runtime.ListenerRules
import js.objects.unsafeJso
import js.promise.Promise
import js.promise.PromiseResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.await
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import node.crypto.randomUUID
import node.fs.ReadFileSyncBufferOptions
import node.fs.readFileSync
import kotlin.time.Duration.Companion.seconds
import dev.slne.surf.nats.js.external.nats.core.connect as natsConnect

/**
 * One NATS connection of a surf-nats JS client; the exported [SurfNatsClient] and [SurfNatsTarget] delegate here.
 *
 * `Surf-Origin` and the connection name are `<client name>@<instanceId>`, with a random instance id per
 * connection.
 */
internal class NatsRuntime(
    val name: String,
    val instanceId: String,
    val connection: NatsConnection,
) {
    val origin: String = ProtocolHeaders.origin(name, instanceId)
    val scope = CoroutineScope(SupervisorJob())
    val payloads = PayloadStore(connection, scope)
    val jetStream: JetStreamClient by lazy { jetstream(connection) }

    private val caller = RpcCaller(this)
    private val registrations = mutableSetOf<NatsRegistration>()
    private val baseHeaderBytes = Wire.headerBytes(Wire.baseHeaders(origin))

    // Listener handlers whose returned promise or thenable has not settled yet; close() waits for them.
    private var runningHandlers = 0

    // Completed while no handler runs; replaced when the first handler starts.
    private var handlersIdle = CompletableDeferred(Unit)

    private val lifecycle = ClientLifecycle(name)

    // Completed when close() has finished; a second close() waits for it
    private val closed = CompletableDeferred<Unit>()
    val isClosed: Boolean get() = lifecycle.isClosed

    init {
        logConnectionEvents()
    }

    fun headers(): OutgoingHeaders = OutgoingHeaders(Wire.baseHeaders(origin), baseHeaderBytes)

    private fun logConnectionEvents() {
        scope.launch {
            val events = syncIterator(connection.status())
            while (true) {
                val status = events.next().await() ?: break
                when (val type = status.type) {
                    "disconnect", "error", "staleConnection", "slowConsumer" ->
                        console.warn("surf-nats[$name]: NATS connection $type", status.server ?: status.error ?: "")

                    "reconnect" -> console.info("surf-nats[$name]: NATS connection reconnect", status.server)
                }
            }
        }
    }

    suspend fun publish(schema: DescMessage, message: ProtoMessageInit?, owner: String?, timeoutMs: Int?) {
        checkOpen()
        val contract = Contracts.event(schema)
        val subject = contract.subject(owner)
        val timeout = positiveTimeoutOrNull(timeoutMs, "Timeout") ?: ProtocolDefaults.CORE_TIMEOUT_SECONDS.seconds
        val data = toBinary(schema, MessageRules.prepare(schema, message))
        val headers = headers()

        val size = data.length + headers.bytes
        val max = payloads.maxPayload()
        require(size <= max) { ProtocolDefaults.eventTooLargeMessage(contract.typeName, size, max, subject) }

        connection.publish(subject, data, unsafeJso<NatsPublishOptions> { this.headers = headers.msgHdrs })
        withTimeoutOrNull(timeout) { connection.flush().await() }
            ?: throw NatsCallTimeoutError(subject, timeout.inWholeMilliseconds.toInt())
    }

    suspend fun listen(
        schema: DescMessage,
        handler: (ProtoMessage, EventContext) -> Any?,
        owner: String?,
        options: ListenOptions?,
    ): NatsRegistration {
        lifecycle.requireAccepting()
        val contract = Contracts.event(schema)
        val subject = contract.subject(owner)
        // ?. and ?: also map an omitted option (undefined); a string `when` would compile to a strict JS switch
        val recipients = options?.recipients?.let { value ->
            Recipients.entries.firstOrNull { it.name == value } ?: throw IllegalArgumentException(
                "Unknown recipients '$value': use ${Recipients.entries.joinToString(" or ")}"
            )
        } ?: ListenerRules.DEFAULT_RECIPIENTS
        val ignoreFromThisClient = options?.ignoreFromThisClient == true
        ListenerRules.problem(recipients, ignoreFromThisClient)?.let { problem ->
            throw IllegalArgumentException("$problem (listener for ${contract.typeName} on $subject)")
        }

        val subscriptionOptions = unsafeJso<SubscriptionOptions> {
            ListenerRules.queueGroup(recipients, name)?.let { queue = it }
            callback = { error, message ->
                if (error != null) {
                    console.warn("surf-nats: listener for ${contract.typeName} on $subject stopped", error)
                } else {
                    dispatch(schema, subject, message, handler, ignoreFromThisClient)
                }
            }
        }
        val subscription = connection.subscribe(subject, subscriptionOptions)
        val registration = NatsRegistration(this, subscription)
        lifecycle.admit(
            add = {
                registrations += registration
                // close() may start during the flush and drain this registration
                connection.flush().await()
            },
            undo = { registration.close() },
        )
        return registration
    }

    suspend fun call(method: DescMethod, request: ProtoMessageInit?, owner: String?, options: CallOptions?): ProtoMessage? {
        checkOpen()
        return caller.call(method, owner, request, options?.timeoutMs)
    }

    suspend fun callAnswer(method: DescMethod, request: ProtoMessageInit?, owner: String?, options: CallOptions?): Answer {
        checkOpen()
        return caller.callAnswer(method, owner, request, options?.timeoutMs)
    }

    suspend fun close() {
        if (!lifecycle.beginClose()) return closed.await()
        try {
            // Running handlers may still publish and call through this client while the listeners drain
            drainListeners()
            if (!connection.isClosed()) connection.drain().await()
        } finally {
            lifecycle.markClosed()
            scope.cancel("SurfNatsClient[$name] closed")
            closed.complete(Unit)
        }
    }

    private suspend fun drainListeners() {
        val drains = registrations.toList().mapNotNull(NatsRegistration::drain)
        for (drain in drains) {
            try {
                drain.await()
            } catch (e: Throwable) {
                console.warn("surf-nats[$name]: failed to drain a listener", e)
            }
        }

        val drainTimeout = JsRuntimeDefaults.drainTimeout
        withTimeoutOrNull(drainTimeout) {
            while (runningHandlers > 0) handlersIdle.await()
        } ?: console.warn(
            "surf-nats[$name]: $runningHandlers listener handlers still running after $drainTimeout, closing anyway"
        )
    }

    fun unregister(registration: NatsRegistration) {
        registrations -= registration
    }

    private fun dispatch(
        schema: DescMessage,
        subject: String,
        message: Msg,
        handler: (ProtoMessage, EventContext) -> Any?,
        ignoreFromThisClient: Boolean,
    ) {
        val origin = Wire.header(message, ProtocolHeaders.ORIGIN)
        val fromThisClient = origin == this.origin
        if (fromThisClient && ignoreFromThisClient) return

        val event = try {
            val contentType = Wire.header(message, ProtocolHeaders.CONTENT_TYPE)
            decodeMessage(schema, message.data, contentType, "event on ${message.subject}")
        } catch (e: Throwable) {
            console.warn("surf-nats: dropping undecodable ${schema.typeName} on ${message.subject}", e)
            return
        }

        val context = unsafeJso<EventContextInit> {
            this.origin = origin
            isFromThisClient = fromThisClient
        }
        // Reading `then` of the result may throw like the handler itself.
        val pending = try {
            handler(event, context).takeIf(::isThenable)
        } catch (e: Throwable) {
            logListenerFailure(schema, subject, e)
            return
        }
        if (pending != null) track(schema, subject, pending)
    }

    // Adopts a returned promise or thenable like await does; other return values are done at once.
    private fun track(schema: DescMessage, subject: String, pending: Any) {
        if (runningHandlers++ == 0) handlersIdle = CompletableDeferred()
        Promise.resolve(PromiseResult(pending)).then(
            onFulfilled = { handlerSettled() },
            onRejected = { e ->
                logListenerFailure(schema, subject, e)
                handlerSettled()
            },
        )
    }

    private fun handlerSettled() {
        if (--runningHandlers == 0) handlersIdle.complete(Unit)
    }

    private fun logListenerFailure(schema: DescMessage, subject: String, error: Any?) {
        console.error("surf-nats: listener for ${schema.typeName} failed on $subject", error)
    }

    fun checkOpen() = lifecycle.requireOpen()

    companion object {
        suspend fun connect(options: ConnectOptions): NatsRuntime {
            val settings = ConnectSettings.of(options)
            val instanceId = randomUUID()

            val natsOptions = unsafeJso<ConnectionOptions> {
                servers = settings.servers.toTypedArray()
                name = ProtocolHeaders.origin(settings.name, instanceId)
                timeout = settings.connectTimeoutMs
                maxReconnectAttempts = ConnectRules.MAX_RECONNECTS
                pingInterval = ConnectRules.PING_INTERVAL_SECONDS * MILLIS_PER_SECOND
                when (val auth = settings.auth) {
                    is ConnectSettings.Auth.Token -> token = auth.token
                    is ConnectSettings.Auth.UserPass -> {
                        user = auth.user
                        pass = auth.pass
                    }

                    is ConnectSettings.Auth.CredsFile -> {
                        val creds = readFileSync(auth.path, unsafeJso<ReadFileSyncBufferOptions>())
                        authenticator = credsAuthenticator(creds)
                    }

                    null -> Unit
                }
            }

            return NatsRuntime(settings.name, instanceId, natsConnect(natsOptions).await())
        }

        private const val MILLIS_PER_SECOND = 1000
    }
}

@OptIn(ExperimentalJsExport::class)
@JsExport
class NatsRegistration internal constructor(
    private val runtime: NatsRuntime,
    private val subscription: Subscription,
) {
    private var closed = false
    val isClosed: Boolean get() = closed

    fun close() {
        if (closed) return
        closed = true
        runtime.unregister(this)
        if (!subscription.isClosed()) subscription.unsubscribe()
    }

    // Stops the delivery like close(); messages already received are still dispatched before the promise resolves.
    internal fun drain(): kotlin.js.Promise<Unit>? {
        if (closed) return null
        closed = true
        runtime.unregister(this)
        return if (subscription.isClosed()) null else subscription.drain()
    }
}
