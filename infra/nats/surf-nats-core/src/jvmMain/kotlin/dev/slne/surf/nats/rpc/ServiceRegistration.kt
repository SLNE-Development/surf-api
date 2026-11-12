package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.NatsRegistration
import dev.slne.surf.nats.bytecode.ServiceInvokerFactory
import dev.slne.surf.nats.bytecode.call
import dev.slne.surf.nats.client.NatsClientImpl
import dev.slne.surf.nats.client.awaitRunningHandlers
import dev.slne.surf.nats.codec.NatsHeaders
import dev.slne.surf.nats.codec.deadlineMillis
import dev.slne.surf.nats.codec.header
import dev.slne.surf.nats.config.natsConfig
import dev.slne.surf.nats.connection.NatsConnectionManager
import dev.slne.surf.nats.descriptor.NatsResultKind
import dev.slne.surf.nats.jetstream.JetStreamProvisioner
import dev.slne.surf.nats.protocol.PayloadBucket
import dev.slne.surf.nats.protocol.ProtocolDefaults
import dev.slne.surf.nats.protocol.ProtocolHeaders
import dev.slne.surf.nats.protocol.ServiceNames
import dev.slne.surf.nats.rpc.delivery.DeadlineExpiredException
import dev.slne.surf.nats.rpc.delivery.DeliveryCause
import dev.slne.surf.nats.rpc.delivery.DeliveryDisposition
import dev.slne.surf.nats.rpc.delivery.NoHandlerSlotException
import dev.slne.surf.nats.rpc.delivery.RegistrationDrainingException
import dev.slne.surf.nats.rpc.payload.PayloadStore
import dev.slne.surf.nats.runtime.rpc.CallFlow
import dev.slne.surf.nats.util.rethrowIfFatal
import io.nats.client.*
import io.nats.client.impl.Headers
import io.nats.client.support.NatsJetStreamConstants
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import org.jetbrains.annotations.Blocking
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlin.time.toJavaDuration

/**
 * Serves one service implementation for one client.
 *
 * Core requests arrive through one dispatcher subscription per method, in the queue group of the service for
 * `ANY_INSTANCE` methods. Persistent deliveries arrive through the durable consumer of the work-queue stream of the
 * service. Every request runs in its own coroutine in a scope whose parent is the client scope.
 *
 * One [DeadlineLimiter] per registration bounds how many core and persistent handlers run at once and admits waiting
 * ones earliest deadline first. For a core request with a reply subject and for a persistent RPC, the time spent
 * waiting for a slot counts against the handler timeout. A fire always gets the full [RpcDefaults.FIRE_MAX_DURATION],
 * and a fan-out runs without a time limit. A request that gets no slot before its deadline does not run: a core
 * request is dropped without a reply, a persistent RPC is terminated, and a fire is redelivered with backoff and
 * dead-lettered with [dev.slne.surf.nats.rpc.delivery.NoHandlerSlotException] on its last delivery.
 *
 * A persistent delivery extends its ack wait every half [RpcDefaults.ACK_WAIT] while it waits for a slot and while
 * its handler runs. Dead letters go to the dead-letter stream of the service, see [deadLetter]. The library does not
 * consume that stream.
 *
 * Lifecycle: the client calls [subscribe] once after construction and [close] when the registration ends, or
 * [discard] instead when the registration never subscribed.
 */
@InternalNatsApi
internal class ServiceRegistration(
    private val client: NatsClientImpl,
    val descriptor: ServiceDescriptor,
    implementation: Any,
    private val interceptor: NatsPersistentInterceptor?,
) : NatsRegistration {

    private val invokers = descriptor.methods.map {
        ServiceInvokerFactory.create(implementation, it.method)
    }

    private val announcer = descriptor.presenceSubject?.let { subject ->
        PresenceAnnouncer(client, subject)
    }

    private val persistentBySubject = descriptor.methods
        .filter { it.persistent }
        .associateBy { it.subject }

    private val coroutineNames = descriptor.methods.map { method ->
        val kind = if (method.persistent) "persist" else "rpc"
        CoroutineName("surf-nats/${client.name}/$kind/${method.subject}")
    }

    private val scope = client.clientScope + SupervisorJob(client.clientScope.coroutineContext.job)

    private val limiter = natsConfig.handlerLimits(descriptor.name).let { DeadlineLimiter(it.total, it.long) }

    private val lanes = descriptor.methods.map(::laneOf)

    private val notAdmittedLog = NotAdmittedLog(
        serviceName = descriptor.name,
        permits = limiter.permits,
        scope = client.clientScope,
        interval = RpcDefaults.NOT_ADMITTED_LOG_INTERVAL,
        warn = { client.logger.warn(it) },
    )

    private val active = AtomicBoolean(true)
    override val isClosed get() = !active.get()

    @Volatile
    private var dispatcher: Dispatcher? = null

    // Guards messageConsumer: a reconnect swaps it while a drain stops and closes it
    private val consumerLock = Any()

    @Volatile
    private var messageConsumer: MessageConsumer? = null

    private val reconnectListener = ConnectionListener { _, event ->
        if (event == ConnectionListener.Events.RESUBSCRIBED) {
            scope.launch(Dispatchers.IO + CoroutineName("surf-nats/${client.name}/persist/resume")) {
                resumeConsumer()
            }
        }
    }

    /**
     * Subscribes the core methods, provisions the streams and starts the message consumer when the service has
     * persistent methods, starts the presence announcer and flushes, so the server knows every subscription when this
     * returns.
     *
     * Throws when a stream cannot be provisioned or the consumer cannot be created. The caller then closes the
     * registration.
     */
    suspend fun subscribe() {
        val connection = NatsConnectionManager.connection()
        val dispatcher = connection.createDispatcher()
        this.dispatcher = dispatcher

        for (method in descriptor.methods) {
            if (method.persistent) continue
            val handler = MessageHandler { onRequest(method, it) }
            when (method.recipients) {
                Recipients.ANY_INSTANCE -> dispatcher.subscribe(
                    method.subject,
                    descriptor.names.queueGroup,
                    handler
                )

                Recipients.ALL_INSTANCES -> dispatcher.subscribe(method.subject, handler)
            }
        }

        descriptor.streams?.let { streams ->
            JetStreamProvisioner.ensureStreams(streams)
            withContext(Dispatchers.IO) {
                messageConsumer = consume(connection, streams, dispatcher)
            }
            connection.addConnectionListener(reconnectListener)
        }

        announcer?.start(dispatcher)

        client.flush()
    }

    @Blocking
    private fun consume(connection: Connection, streams: ServiceNames, dispatcher: Dispatcher): MessageConsumer {
        return connection.getStreamContext(streams.stream)
            .createOrUpdateConsumer(JetStreamProvisioner.consumerConfig(streams.durable))
            .consume(dispatcher) { onPersistent(it) }
    }

    /**
     * Replaces the message consumer after a reconnect.
     *
     * The server forgets the pull requests of a dropped connection, and jnats pulls again only after its heartbeat
     * alarm, about 45 s later. The old consumer is closed only after the new one pulls. If that fails, the old one
     * stays and recovers through the alarm.
     */
    @Blocking
    private fun resumeConsumer() {
        val streams = descriptor.streams ?: return
        synchronized(consumerLock) {
            val dispatcher = dispatcher
            if (!active.get() || dispatcher == null) return

            val resumed = try {
                consume(NatsConnectionManager.connection(), streams, dispatcher)
            } catch (e: Exception) {
                client.logger.warn("Failed to resume consuming ${streams.stream} after a reconnect", e)
                return
            }
            val previous = messageConsumer
            messageConsumer = resumed
            closeConsumer(previous)
        }
    }

    private fun closeConsumer(consumer: MessageConsumer?) {
        try {
            consumer?.close()
        } catch (e: Exception) {
            client.logger.warn("Failed to close message consumer", e)
        }
    }

    /**
     * Handles a core request on a dispatcher thread by launching its handler.
     *
     * The deadline is the `Surf-Deadline` of the request, else its arrival plus the timeout of [method]. A request
     * past its deadline is dropped with a warning that points at clock skew between the hosts, which this check
     * depends on. A request without a reply subject is a fan-out: it waits for a slot until its deadline like any
     * other request, but its handler then runs without a time limit and nothing is answered. A request that gets no
     * slot is dropped without a reply.
     */
    private fun onRequest(method: MethodDescriptor, message: Message) {
        val deadline = message.deadlineMillis() ?: deadlineAfter(method.timeout)

        val left = remaining(deadline)
        if (!left.isPositive()) return dropExpired(message, left)

        val replyTo = message.replyTo
        val deletePayload = method.recipients == Recipients.ANY_INSTANCE

        scope.launch(coroutineNames[method.index]) {
            // A fan-out runs without a timeout, so it counts as long
            val lane = if (replyTo == null) DeadlineLimiter.Lane.LONG else lanes[method.index]
            val admission = withPermit(deadline, lane) { timeout ->
                // Nobody waits for a fan-out, so its handler runs to completion
                val handlerTimeout = if (replyTo == null) null else timeout
                when (val outcome = invoke(method, message, handlerTimeout, deletePayload)) {
                    is Outcome.Finished -> if (replyTo != null) send(replyTo, replyOf(method, outcome), timeout)

                    Outcome.TimedOut -> logTimedOut(method, timeout)
                }
            }
            when (admission) {
                DeadlineLimiter.Admission.GRANTED -> Unit
                DeadlineLimiter.Admission.EXPIRED -> notAdmittedLog.report(message.subject)
                DeadlineLimiter.Admission.CLOSED -> logDroppedWhileDraining(message)
            }
        }
    }

    /**
     * Runs [handle] with the time left until [deadline] once [limiter] grants a permit.
     *
     * Returns [DeadlineLimiter.Admission.GRANTED] when [handle] ran. Returns the reason without running it when the
     * deadline passes first, also when a permit is granted too late, or when the registration starts draining first.
     */
    private suspend inline fun withPermit(
        deadline: Long,
        lane: DeadlineLimiter.Lane,
        handle: (timeout: Duration) -> Unit,
    ): DeadlineLimiter.Admission {
        val admission = limiter.acquire(deadline, lane)
        if (admission != DeadlineLimiter.Admission.GRANTED) return admission
        try {
            val timeout = remaining(deadline)
            if (!timeout.isPositive()) return DeadlineLimiter.Admission.EXPIRED
            handle(timeout)
            return DeadlineLimiter.Admission.GRANTED
        } finally {
            limiter.release(lane)
        }
    }

    /**
     * Returns [DeadlineLimiter.Lane.LONG] for fire calls and for methods whose timeout exceeds the core default.
     *
     * The declared timeout decides, not the time a caller has left, so clock skew cannot move a call to another lane.
     */
    private fun laneOf(method: MethodDescriptor): DeadlineLimiter.Lane {
        val long = method.flow == CallFlow.FIRE || method.timeout > CORE_TIMEOUT
        return if (long) DeadlineLimiter.Lane.LONG else DeadlineLimiter.Lane.SHORT
    }

    /**
     * Handles a persistent delivery from the message consumer by launching its handler.
     *
     * A delivery on a subject without a registered persistent method fails like a fire, with an
     * [IllegalStateException]. A delivery past its deadline (see [persistentDeadline]) does not run and is settled
     * with [DeliveryCause.EXPIRED]. Otherwise it waits for a handler slot and then runs as a fire or a persistent RPC.
     */
    private fun onPersistent(message: Message) {
        val method = persistentBySubject[message.subject]
        if (method == null) {
            scope.launch(CoroutineName("surf-nats/${client.name}/persist/unknown")) {
                val error =
                    IllegalStateException("No persistent method is registered for ${message.subject}")
                failFire(message, error, DeliveryCause.FAILED)
            }
            return
        }
        val context = NatsPersistentContext(
            messageId = message.persistentMessageId(),
            deliveryCount = message.metaData().deliveredCount(),
        )

        val deadline = message.persistentDeadline(method)
        val left = deadline?.let(::remaining)
        if (left != null && !left.isPositive()) {
            dropExpired(message, left)
            scope.launch(context + coroutineNames[method.index]) { rejectExpired(method, message) }
            return
        }
        // A fire without a deadline ranks as due in FIRE_MAX_DURATION, behind every call due sooner
        val admissionDeadline = deadline ?: deadlineAfter(RpcDefaults.FIRE_MAX_DURATION)

        scope.launch(context + coroutineNames[method.index]) {
            val keepAlive = launch(Dispatchers.IO) {
                while (isActive) {
                    delay(RpcDefaults.ACK_WAIT / 2)
                    try {
                        message.inProgress()
                    } catch (e: Exception) {
                        client.logger.debug(
                            "Failed to extend the ack wait of ${message.subject}",
                            e
                        )
                    }
                }
            }
            try {
                val admission = withPermit(admissionDeadline, lanes[method.index]) {
                    if (method.flow == CallFlow.FIRE) {
                        handleFire(method, message, context)
                    } else {
                        handlePersistentRpc(method, message, admissionDeadline)
                    }
                }
                when (admission) {
                    DeadlineLimiter.Admission.GRANTED -> Unit
                    DeadlineLimiter.Admission.EXPIRED -> rejectNotAdmitted(method, message, deadline)
                    DeadlineLimiter.Admission.CLOSED -> rejectWhileDraining(method, message, deadline)
                }
            } finally {
                keepAlive.cancel()
            }
        }
    }

    /**
     * Runs a fire delivery and settles it.
     *
     * The handler runs for at most [RpcDefaults.FIRE_MAX_DURATION], also when the request carries a deadline, and
     * is cancelled cooperatively after that. Only an empty reply acknowledges the delivery. Every failure, timeout,
     * stored error reply or mismatching reply goes through [failFire]. The payload stays in the object store until
     * the acknowledgement is confirmed, so redeliveries can still read it.
     */
    private suspend fun handleFire(method: MethodDescriptor, message: Message, context: NatsPersistentContext) {
        val data = try {
            PayloadStore.resolve(message, PayloadBucket.PERSISTENT, delete = false)
        } catch (e: Throwable) {
            rethrowIfCancelledOrFatal(e)
            return failFire(message, e, DeliveryCause.ofLoadFailure(e))
        }

        val request = try {
            method.decodeRequest(data, message.header(ProtocolHeaders.CONTENT_TYPE))
        } catch (e: Throwable) {
            rethrowIfCancelledOrFatal(e)
            return failFire(message, e, DeliveryCause.ofDecodeFailure(e))
        }

        val reply = try {
            withTimeoutOrNull(RpcDefaults.FIRE_MAX_DURATION) {
                handle(method, context, request, data)
            }
        } catch (e: Throwable) {
            rethrowIfCancelledOrFatal(e)
            return failFire(message, e, DeliveryCause.ofFailure(e))
        }

        if (reply == null) {
            val error =
                TimeoutException("Handler did not complete within ${RpcDefaults.FIRE_MAX_DURATION}")
            return failFire(message, error, DeliveryCause.TIMED_OUT)
        }

        when (reply.kind) {
            NatsPersistentReply.Kind.EMPTY -> {
                val disposition = DeliveryDisposition.of(
                    DeliveryCause.COMPLETED, CallFlow.FIRE, context.deliveryCount, timeLeft = Duration.INFINITE
                )
                settle(message, disposition, PayloadBucket.PERSISTENT)
            }

            NatsPersistentReply.Kind.ERROR -> {
                val error = NatsRemoteException(reply.errorType.orEmpty(), reply.errorMessage.orEmpty())
                failFire(message, error, DeliveryCause.STORED_ERROR)
            }

            else -> failFire(message, replyMismatch(method, reply), DeliveryCause.REPLY_MISMATCH)
        }
    }

    /**
     * Rethrows the cancellation of this coroutine and fatal errors. Any other [e], including a cancellation raised
     * inside the handler, is left to the caller.
     */
    private suspend fun rethrowIfCancelledOrFatal(e: Throwable) {
        if (e is CancellationException) {
            currentCoroutineContext().ensureActive()
        } else {
            e.rethrowIfFatal()
        }
    }

    /**
     * Runs the handler of a persistent [method] through the interceptor, if any, and returns its encoded reply.
     */
    private suspend fun handle(
        method: MethodDescriptor,
        context: NatsPersistentContext,
        request: Any,
        requestBytes: ByteArray,
    ): NatsPersistentReply {
        val call = PersistentCall(
            serviceName = descriptor.name,
            method = method,
            invoker = invokers[method.index],
            request = request,
            requestBytes = requestBytes,
            messageId = context.messageId,
            deliveryCount = context.deliveryCount,
        )
        return interceptor?.intercept(call) ?: call.proceed()
    }

    /**
     * Acknowledges [message] and deletes its offloaded payload only once the server confirmed the ack, so a
     * redelivery after a lost ack can still resolve the payload.
     */
    private suspend fun ackThenDeletePayload(message: Message, bucket: PayloadBucket) {
        if (!PayloadStore.isOffloaded(message)) {
            message.ack()
            return
        }

        try {
            runInterruptible(Dispatchers.IO) {
                message.ackSync(RpcDefaults.ACK_SYNC_TIMEOUT.toJavaDuration())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            client.logger.warn(
                "Ack of ${message.subject} was not confirmed, keeping its payload",
                e
            )
            return
        }

        PayloadStore.delete(message, bucket)
    }

    private fun replyMismatch(method: MethodDescriptor, reply: NatsPersistentReply): IllegalStateException {
        return IllegalStateException(
            "Reply of format ${reply.format} does not fit the ${method.result} result of ${method.label}"
        )
    }

    private suspend fun failFire(message: Message, e: Throwable, cause: DeliveryCause) {
        val delivered = message.metaData().deliveredCount()
        val permanentSuffix = if (cause.permanent) ", permanent" else ""
        val failure = "Persistent call on ${message.subject} failed (delivery $delivered$permanentSuffix)"
        if (e is NatsRedeliveryException) {
            client.logger.warn("$failure and requested a redelivery", e)
        } else {
            client.logger.error(failure, e)
        }

        val disposition = DeliveryDisposition.of(cause, CallFlow.FIRE, delivered, timeLeft = Duration.INFINITE)
        settle(message, disposition, PayloadBucket.PERSISTENT, e)
    }

    /**
     * Executes [disposition] on [message].
     *
     * [bucket] holds the offloaded payload of the message. [failure] is required for a dead letter and names its
     * `Surf-Error`.
     */
    private suspend fun settle(
        message: Message,
        disposition: DeliveryDisposition,
        bucket: PayloadBucket,
        failure: Throwable? = null,
    ) {
        when (disposition) {
            DeliveryDisposition.Ack -> ackThenDeletePayload(message, bucket)
            DeliveryDisposition.Term -> message.term()
            is DeliveryDisposition.Nak -> message.nakWithDelay(disposition.delay.toJavaDuration())
            is DeliveryDisposition.DeadLetter -> {
                val error = checkNotNull(failure) { "Dead letter of ${message.subject} without a failure" }
                if (deadLetter(message, error)) {
                    message.term()
                } else {
                    message.nakWithDelay(disposition.retryDelay.toJavaDuration())
                }
            }
        }
    }

    /**
     * Returns the identity of a persistent message: its `Surf-Message-Id`, else the `Nats-Msg-Id` of the publish,
     * else the stream sequence.
     *
     * `Surf-Message-Id` carries the idempotency key of the caller or the identity kept by a dead letter. Without it the
     * identity is stable across redeliveries of one publish only.
     */
    private fun Message.persistentMessageId(): String {
        return header(ProtocolHeaders.MESSAGE_ID)
            ?: header(NatsJetStreamConstants.MSG_ID_HDR)
            ?: metaData().streamSequence().toString()
    }

    /**
     * Publishes [message] to the dead-letter stream of the service and returns whether the server confirmed it.
     *
     * The dead letter keeps the body and the headers of the delivery except the `Nats-*` headers, and adds
     * `Surf-Error` (the class name of [e], never its message), `Surf-Dead-Subject` (the original subject) and
     * `Surf-Message-Id` (see [persistentMessageId]). An offloaded payload stays referenced by `Surf-Payload-Ref` and
     * is readable only until the TTL of its bucket, counted from the original upload. The publish ID is derived from
     * the stream sequence, so the server deduplicates a repeated attempt for the same message within the duplicate
     * window of the dead-letter stream. The added headers can push a message close to the max payload over the
     * limit, and then the publish fails.
     */
    private suspend fun deadLetter(message: Message, e: Throwable): Boolean {
        val streams = checkNotNull(descriptor.streams)
        val headers = Headers(message.headers)
            .put(ProtocolHeaders.ERROR, e.javaClass.name)
            .put(ProtocolHeaders.DEAD_SUBJECT, message.subject)
            .put(ProtocolHeaders.MESSAGE_ID, message.persistentMessageId())

        headers.remove(headers.keySet().filter { it.startsWith("Nats-", ignoreCase = true) })

        val options = PublishOptions.builder()
            .messageId("${streams.dlqStream}-${message.metaData().streamSequence()}")
            .build()

        return try {
            NatsConnectionManager.connection()
                .jetStream()
                .publishAsync(streams.dlqSubject, headers, message.data, options)
                .await()
            true
        } catch (dlq: CancellationException) {
            currentCoroutineContext().ensureActive()
            client.logger.error("Failed to dead-letter ${message.subject}: no PubAck", dlq)
            false
        } catch (dlq: Exception) {
            client.logger.error("Failed to dead-letter ${message.subject}", dlq)
            false
        }
    }

    /**
     * Runs a persistent RPC delivery with the time left until [deadline] and settles it.
     *
     * The caller is answered only when the delivery is acknowledged, on the `Surf-Reply-To` subject of the request.
     * A request without that header runs but gets no reply. A handler failure is answered as an error reply, except
     * a requested redelivery that can still happen before the deadline. A timed-out handler gets no reply.
     */
    private suspend fun handlePersistentRpc(
        method: MethodDescriptor,
        message: Message,
        deadline: Long,
    ) {
        val timeout = remaining(deadline)

        // Keep the payload for a redelivery
        val outcome = invoke(method, message, timeout, deletePayload = false)
        val delivered = message.metaData().deliveredCount()
        val disposition =
            DeliveryDisposition.of(causeOf(outcome), CallFlow.PERSISTENT_RPC, delivered, remaining(deadline))

        if (outcome !is Outcome.Finished) {
            logTimedOut(method, timeout)
            return settle(message, disposition, PayloadBucket.TRANSIENT) // Caller has already given up
        }

        if (outcome is Outcome.Failed && outcome.error is NatsRedeliveryException) {
            logRedeliveryRequest(method, delivered, outcome.error, disposition)
        }

        if (disposition == DeliveryDisposition.Ack) {
            message.header(ProtocolHeaders.REPLY_TO)?.let { replyTo ->
                send(replyTo, replyOf(method, outcome), timeout)
            }
        }
        settle(message, disposition, PayloadBucket.TRANSIENT)
    }

    private fun causeOf(outcome: Outcome): DeliveryCause = when (outcome) {
        Outcome.TimedOut -> DeliveryCause.TIMED_OUT
        is Outcome.Failed -> DeliveryCause.ofFailure(outcome.error)
        is Outcome.Completed, is Outcome.Prepared -> DeliveryCause.COMPLETED
    }

    private fun logRedeliveryRequest(
        method: MethodDescriptor,
        delivered: Long,
        error: Throwable,
        disposition: DeliveryDisposition,
    ) {
        if (disposition is DeliveryDisposition.Nak) {
            client.logger.warn("Service method ${method.label} requested a redelivery (delivery $delivered)", error)
            return
        }

        client.logger.error(
            "Service method ${method.label} requested a redelivery that cannot happen before its deadline " +
                    "(delivery $delivered), answering with the error",
            error
        )
    }

    /**
     * Stops accepting requests, waits for the running handlers until the drain timeout and unregisters this
     * registration. The registration is unregistered and its handlers are cancelled even when the drain fails or
     * the calling thread is interrupted.
     *
     * Blocks the calling thread while it drains. Only the first call has an effect. Requests still waiting for a
     * handler slot do not run: core requests are dropped, and persistent deliveries are settled with
     * [DeliveryCause.DRAINING]. That redelivers them, possibly to another instance, but dead-letters a fire on its
     * last delivery and terminates a persistent RPC on its last delivery or when its caller would give up first. A
     * request that finds a free slot during the drain still runs.
     */
    override fun close() {
        if (!active.compareAndSet(true, false)) return
        try {
            removeReconnectListener()
            announcer?.close()
            drain(natsConfig.drainTimeout.seconds)
        } finally {
            scope.cancel("Service registration ${descriptor.type.name} closed")
            client.unregisterService(this)
            client.unregisterRegistration(this)
        }
    }

    private fun drain(timeout: Duration) {
        val deadline = TimeSource.Monotonic.markNow() + timeout

        // Queued deliveries are redelivered and queued core requests dropped instead of being awaited
        limiter.close()

        // A resume running now finishes first. Later ones see the registration inactive
        val consumer = synchronized(consumerLock) { messageConsumer }
        consumer?.let {
            try {
                it.stop()
            } catch (e: Exception) {
                client.logger.warn("Failed to stop message consumer", e)
            }
        }

        dispatcher?.let { client.drainDispatcher(it, timeout, "service dispatcher") }

        closeConsumer(consumer)

        val stillActive = try {
            awaitRunningHandlers(scope, deadline)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            client.logger.warn("Interrupted while draining ${descriptor.type.name}, cancelling its handlers")
            return
        }
        if (stillActive != null) {
            client.logger.warn(
                "$stillActive handlers of ${descriptor.type.name} did not finish " +
                        "within the drain timeout of $timeout and are cancelled"
            )
        }
    }

    private fun removeReconnectListener() {
        if (descriptor.streams == null) return
        try {
            NatsConnectionManager.connection().removeConnectionListener(reconnectListener)
        } catch (e: IllegalStateException) {
            client.logger.debug("Connection gone before removing the reconnect listener", e)
        }
    }

    /**
     * Deactivates a registration that never subscribed and cancels its scope, without draining or unregistering.
     */
    fun discard() {
        active.set(false)
        removeReconnectListener()
        scope.cancel("Service registration ${descriptor.type.name} discarded")
    }

    private fun remaining(deadline: Long): Duration = (deadline - System.currentTimeMillis()).milliseconds

    private fun deadlineAfter(timeout: Duration): Long = System.currentTimeMillis() + timeout.inWholeMilliseconds

    /**
     * Returns the deadline of a persistent delivery in epoch milliseconds: its `Surf-Deadline`, else for a persistent
     * RPC the publish time in the stream plus the timeout of [method], else `null`.
     *
     * The publish time keeps the deadline stable across the wait for a handler slot and across redeliveries. It comes
     * from the clock of the server.
     */
    private fun Message.persistentDeadline(method: MethodDescriptor): Long? {
        deadlineMillis()?.let { return it }
        if (method.flow != CallFlow.PERSISTENT_RPC) return null
        return metaData().timestamp().toInstant().toEpochMilli() + method.timeout.inWholeMilliseconds
    }

    private fun logDroppedWhileDraining(message: Message) {
        client.logger.debug("Dropping request on ${message.subject}: ${descriptor.name} is draining")
    }

    private suspend fun rejectExpired(method: MethodDescriptor, message: Message) {
        val delivered = message.metaData().deliveredCount()
        val error = DeadlineExpiredException(
            "The Surf-Deadline of ${message.subject} had passed when delivery $delivered arrived"
        )
        val disposition = DeliveryDisposition.of(DeliveryCause.EXPIRED, method.flow, delivered, Duration.ZERO)
        settle(message, disposition, method.flow.bucket, error)
    }

    private suspend fun rejectNotAdmitted(method: MethodDescriptor, message: Message, deadline: Long?) {
        notAdmittedLog.report(message.subject)
        val delivered = message.metaData().deliveredCount()
        val disposition =
            DeliveryDisposition.of(DeliveryCause.NOT_ADMITTED, method.flow, delivered, timeLeftUntil(deadline))
        val error = NoHandlerSlotException(
            "All ${limiter.permits} handler slots of ${descriptor.name} stayed busy until the deadline of " +
                    "${message.subject} on delivery $delivered"
        )
        if (disposition is DeliveryDisposition.DeadLetter) {
            client.logger.error("Dead-lettering ${message.subject}", error)
        }
        settle(message, disposition, method.flow.bucket, error)
    }

    private suspend fun rejectWhileDraining(method: MethodDescriptor, message: Message, deadline: Long?) {
        val delivered = message.metaData().deliveredCount()
        val disposition =
            DeliveryDisposition.of(DeliveryCause.DRAINING, method.flow, delivered, timeLeftUntil(deadline))
        val error = RegistrationDrainingException(
            "${descriptor.name} drained while delivery $delivered of ${message.subject} waited for a handler slot"
        )
        if (disposition is DeliveryDisposition.DeadLetter) {
            client.logger.error("Dead-lettering ${message.subject}", error)
        } else {
            client.logger.debug("Releasing ${message.subject} for another instance: ${descriptor.name} is draining")
        }
        settle(message, disposition, method.flow.bucket, error)
    }

    // A fire without a deadline never runs out of time
    private fun timeLeftUntil(deadline: Long?): Duration = deadline?.let(::remaining) ?: Duration.INFINITE

    private fun dropExpired(message: Message, remaining: Duration) {
        client.logger.warn(
            "Dropping request on ${message.subject} that expired ${-remaining.inWholeMilliseconds} ms ago " +
                    "(check the clock synchronization of the hosts if this happens regularly)"
        )
    }

    private fun logTimedOut(method: MethodDescriptor, timeout: Duration) {
        client.logger.warn("Service method ${method.subject} did not complete within $timeout and was cancelled")
    }

    /**
     * Result of [invoke]. [Completed] holds the return value of a core handler, [Prepared] the encoded reply of a
     * persistent RPC handler, [Failed] any failure before or during the handler, and [TimedOut] a handler cancelled
     * after its timeout.
     */
    private sealed interface Outcome {
        sealed interface Finished : Outcome
        class Completed(@JvmField val value: Any?) : Finished
        class Prepared(@JvmField val reply: NatsPersistentReply) : Finished
        class Failed(@JvmField val error: Throwable) : Finished
        data object TimedOut : Outcome
    }

    private class Reply(@JvmField val headers: Headers, @JvmField val body: ByteArray)

    /**
     * Loads and decodes the request of a core or persistent RPC delivery and runs its handler within [timeout], or
     * without a limit when [timeout] is `null`.
     *
     * Failures of loading, decoding and the handler become [Outcome.Failed] and are logged, except a redelivery
     * request of a persistent method. Cancellation of the calling coroutine and fatal errors propagate.
     */
    private suspend fun invoke(
        method: MethodDescriptor,
        message: Message,
        timeout: Duration?,
        deletePayload: Boolean,
    ): Outcome {
        val call = suspend {
            val data = PayloadStore.resolve(message, PayloadBucket.TRANSIENT, deletePayload)
            val request = method.decodeRequest(data, message.header(ProtocolHeaders.CONTENT_TYPE))
            if (method.persistent) {
                Outcome.Prepared(handle(method, natsPersistentContext(), request, data))
            } else {
                Outcome.Completed(invokers[method.index].call(request))
            }
        }

        val outcome = try {
            if (timeout == null) {
                call()
            } else {
                withTimeoutOrNull(timeout) {
                    call()
                } ?: Outcome.TimedOut
            }
        } catch (e: Throwable) {
            rethrowIfCancelledOrFatal(e)
            Outcome.Failed(e)
        }

        if (outcome is Outcome.Failed && !(method.persistent && outcome.error is NatsRedeliveryException)) {
            logFailure(method, outcome.error)
        }

        return outcome
    }

    private fun replyOf(
        method: MethodDescriptor,
        outcome: Outcome.Finished,
    ): Reply {
        return try {
            when (outcome) {
                is Outcome.Completed -> wireReply(method, method.encodeReply(outcome.value))
                is Outcome.Prepared -> wireReply(method, outcome.reply)
                is Outcome.Failed -> errorReply(outcome.error)
            }
        } catch (e: Throwable) {
            e.rethrowIfFatal()
            logFailure(method, e)
            errorReply(e)
        }
    }

    private fun logFailure(method: MethodDescriptor, e: Throwable) {
        client.logger.error(
            "Service method ${method.label} failed",
            e
        )
    }

    /**
     * Returns the wire form of [reply]. Throws [IllegalStateException] when its kind does not fit the result of
     * [method], such as a stored reply of another method.
     */
    private fun wireReply(method: MethodDescriptor, reply: NatsPersistentReply): Reply {
        val fits = when (reply.kind) {
            NatsPersistentReply.Kind.EMPTY -> method.result == NatsResultKind.UNIT
            NatsPersistentReply.Kind.VALUE -> method.result != NatsResultKind.UNIT
            NatsPersistentReply.Kind.UNKNOWN_ANSWER -> method.result == NatsResultKind.ANSWER
            NatsPersistentReply.Kind.ERROR -> true
        }
        if (!fits) throw replyMismatch(method, reply)

        val headers = client.headers()
        return when (reply.kind) {
            NatsPersistentReply.Kind.EMPTY -> Reply(headers, NatsHeaders.EMPTY_BODY)
            NatsPersistentReply.Kind.VALUE -> Reply(headers, reply.bytes)
            NatsPersistentReply.Kind.UNKNOWN_ANSWER -> Reply(
                headers.add(ProtocolHeaders.ANSWER, ProtocolHeaders.ANSWER_UNKNOWN),
                NatsHeaders.EMPTY_BODY
            )

            NatsPersistentReply.Kind.ERROR -> errorReply(reply.errorType.orEmpty(), reply.errorMessage)
        }
    }

    private fun errorReply(throwable: Throwable): Reply = errorReply(throwable.javaClass.name, throwable.message)

    private fun errorReply(type: String, message: String?): Reply {
        val headers = client.headers().add(ProtocolHeaders.ERROR, type)
        return Reply(headers, ProtocolDefaults.errorMessage(message).encodeToByteArray())
    }

    /**
     * Publishes [reply] to [replyTo], offloading a body that exceeds the max payload for [timeout].
     *
     * When offloading or publishing fails, the caller is sent an error reply naming that failure instead. A failure of
     * that fallback publish propagates, and a cancellation is rethrown without any reply.
     */
    private suspend fun send(replyTo: String, reply: Reply, timeout: Duration) {
        val connection = NatsConnectionManager.connection()
        try {
            val body =
                PayloadStore.offload(reply.headers, reply.body, timeout, PayloadBucket.TRANSIENT)
            connection.publish(replyTo, reply.headers, body)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            client.logger.error("Failed to send the reply to $replyTo", e)
            val fallback = errorReply(e)
            connection.publish(replyTo, fallback.headers, fallback.body)
        }
    }

    private companion object {
        val CORE_TIMEOUT = ProtocolDefaults.CORE_TIMEOUT_SECONDS.seconds
    }
}
