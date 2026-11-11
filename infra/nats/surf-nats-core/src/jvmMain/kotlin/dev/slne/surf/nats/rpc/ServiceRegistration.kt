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
import dev.slne.surf.nats.rpc.delivery.DeliveryCause
import dev.slne.surf.nats.rpc.delivery.DeliveryDisposition
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

    private val limiter = DeadlineLimiter(natsConfig.handlerLimit(descriptor.name))

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
     * alarm, about 45 s later. The old consumer is closed only after the new one pulls; if that fails, the old one
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

    private fun onRequest(method: MethodDescriptor, message: Message) {
        val deadline = message.deadlineMillis() ?: deadlineAfter(method.timeout)

        val left = remaining(deadline)
        if (!left.isPositive()) return dropExpired(message, left)

        val replyTo = message.replyTo
        val deletePayload = method.recipients == Recipients.ANY_INSTANCE

        scope.launch(coroutineNames[method.index]) {
            // A fan-out runs without a timeout, so it counts as long
            val lane = if (replyTo == null) DeadlineLimiter.Lane.LONG else laneOf(deadline)
            val admitted = withPermit(deadline, lane) { timeout ->
                // Nobody waits for a fan-out, so its handler runs to completion
                val handlerTimeout = if (replyTo == null) null else timeout
                when (val outcome = invoke(method, message, handlerTimeout, deletePayload)) {
                    is Outcome.Finished -> if (replyTo != null) send(replyTo, replyOf(method, outcome), timeout)

                    Outcome.TimedOut -> logTimedOut(method, timeout)
                }
            }
            if (!admitted) logNotAdmitted(message)
        }
    }

    /**
     * Runs [handle] with the time left until [deadline] once [limiter] grants a permit, and returns `false` without
     * running it when the deadline passes first.
     */
    private suspend inline fun withPermit(
        deadline: Long,
        lane: DeadlineLimiter.Lane,
        handle: (timeout: Duration) -> Unit,
    ): Boolean {
        if (!limiter.acquire(deadline, lane)) return false
        try {
            val timeout = remaining(deadline)
            if (!timeout.isPositive()) return false
            handle(timeout)
            return true
        } finally {
            limiter.release(lane)
        }
    }

    /**
     * Returns [DeadlineLimiter.Lane.LONG] when the caller waits longer than the default core timeout.
     */
    private fun laneOf(deadline: Long): DeadlineLimiter.Lane {
        return if (remaining(deadline) > CORE_TIMEOUT) DeadlineLimiter.Lane.LONG else DeadlineLimiter.Lane.SHORT
    }

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

        val deadline = message.deadlineMillis()
        val left = remaining(deadline, method.timeout)
        if (!left.isPositive()) {
            dropExpired(message, left)
            return message.term()
        }
        // Fire calls carry no deadline and rank behind every call whose caller waits
        val admissionDeadline = deadline ?: deadlineAfter(
            if (method.result == NatsResultKind.UNIT) RpcDefaults.FIRE_MAX_DURATION else method.timeout
        )

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
                val admitted = withPermit(admissionDeadline, laneOf(admissionDeadline)) {
                    if (method.result == NatsResultKind.UNIT) {
                        handleFire(method, message, context)
                    } else {
                        handlePersistentRpc(method, message, deadline)
                    }
                }
                if (!admitted) rejectNotAdmitted(method, message, deadline)
            } finally {
                keepAlive.cancel()
            }
        }
    }

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
     * Executes [disposition] on [message]. [bucket] holds the offloaded payload of the message; [failure] is recorded
     * with a dead letter.
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
     * Returns the identity of a persistent message: the identity carried over by a dead-letter replay, else the
     * `Nats-Msg-Id` of the publish, else the stream sequence.
     */
    private fun Message.persistentMessageId(): String {
        return header(ProtocolHeaders.MESSAGE_ID)
            ?: header(NatsJetStreamConstants.MSG_ID_HDR)
            ?: metaData().streamSequence().toString()
    }

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

    private suspend fun handlePersistentRpc(
        method: MethodDescriptor,
        message: Message,
        deadline: Long?,
    ) {
        val timeout = remaining(deadline, method.timeout)

        // Keep the payload for a redelivery
        val outcome = invoke(method, message, timeout, deletePayload = false)
        val delivered = message.metaData().deliveredCount()
        val timeLeft = remaining(deadline, method.timeout)
        val disposition = DeliveryDisposition.of(causeOf(outcome), CallFlow.PERSISTENT_RPC, delivered, timeLeft)

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

    override fun close() {
        if (!active.compareAndSet(true, false)) return
        removeReconnectListener()
        announcer?.close()
        drain(natsConfig.drainTimeout.seconds)
        scope.cancel("Service registration ${descriptor.type.name} closed")
        client.unregisterService(this)
        client.unregisterRegistration(this)
    }

    private fun drain(timeout: Duration) {
        val deadline = TimeSource.Monotonic.markNow() + timeout

        // A resume running now finishes first; later ones see the registration inactive
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

        val stillActive = awaitRunningHandlers(scope, deadline)
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

    fun discard() {
        active.set(false)
        removeReconnectListener()
        scope.cancel("Service registration ${descriptor.type.name} discarded")
    }

    private fun remaining(deadline: Long?, fallback: Duration): Duration {
        return deadline?.let(::remaining) ?: fallback
    }

    private fun remaining(deadline: Long): Duration = (deadline - System.currentTimeMillis()).milliseconds

    private fun deadlineAfter(timeout: Duration): Long = System.currentTimeMillis() + timeout.inWholeMilliseconds

    private fun logNotAdmitted(message: Message) {
        client.logger.warn(
            "Dropping request on ${message.subject}: all ${limiter.permits} handler slots of " +
                    "${descriptor.name} stayed busy until its deadline"
        )
    }

    private suspend fun rejectNotAdmitted(method: MethodDescriptor, message: Message, deadline: Long?) {
        logNotAdmitted(message)
        val disposition = DeliveryDisposition.of(
            DeliveryCause.NOT_ADMITTED,
            method.flow,
            message.metaData().deliveredCount(),
            timeLeft = remaining(deadline, method.timeout),
        )
        settle(message, disposition, method.flow.bucket)
    }

    private fun dropExpired(message: Message, remaining: Duration) {
        client.logger.warn(
            "Dropping request on ${message.subject} that expired ${-remaining.inWholeMilliseconds} ms ago " +
                    "(check the clock synchronization of the hosts if this happens regularly)"
        )
    }

    private fun logTimedOut(method: MethodDescriptor, timeout: Duration) {
        client.logger.warn("Service method ${method.subject} did not complete within $timeout and was cancelled")
    }

    private sealed interface Outcome {
        sealed interface Finished : Outcome
        class Completed(@JvmField val value: Any?) : Finished
        class Prepared(@JvmField val reply: NatsPersistentReply) : Finished
        class Failed(@JvmField val error: Throwable) : Finished
        data object TimedOut : Outcome
    }

    private class Reply(@JvmField val headers: Headers, @JvmField val body: ByteArray)

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
     * Returns the wire form of [reply]; throws [IllegalStateException] when its kind does not fit the result of
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
