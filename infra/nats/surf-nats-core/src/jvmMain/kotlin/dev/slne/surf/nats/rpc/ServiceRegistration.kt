package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.NatsRegistration
import dev.slne.surf.nats.bytecode.ServiceInvokerFactory
import dev.slne.surf.nats.bytecode.call
import dev.slne.surf.nats.client.NatsClientImpl
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
import dev.slne.surf.nats.rpc.payload.PayloadStore
import dev.slne.surf.nats.util.rethrowIfFatal
import io.nats.client.*
import io.nats.client.impl.Headers
import io.nats.client.support.NatsJetStreamClientError
import io.nats.client.support.NatsJetStreamConstants
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlin.time.toJavaDuration

@InternalNatsApi
internal class ServiceRegistration(
    private val client: NatsClientImpl,
    val descriptor: ServiceDescriptor,
    implementation: Any,
) : NatsRegistration {

    private val invokers = descriptor.methods.map {
        ServiceInvokerFactory.create(implementation, it.method, it.valueClasses)
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

    private val active = AtomicBoolean(true)
    override val isClosed get() = !active.get()

    @Volatile
    private var dispatcher: Dispatcher? = null

    @Volatile
    private var messageConsumer: MessageConsumer? = null

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
                val consumer = connection.getStreamContext(streams.stream)
                    .createOrUpdateConsumer(JetStreamProvisioner.consumerConfig(streams.durable))
                messageConsumer = consumer.consume(dispatcher) { onPersistent(it) }
            }
        }

        announcer?.start(dispatcher)

        client.flush()
    }

    private fun onRequest(method: MethodDescriptor, message: Message) {
        val deadline = message.deadlineMillis()

        val left = remaining(deadline, method.timeout)
        if (!left.isPositive()) return dropExpired(message, left)

        val replyTo = message.replyTo
        val deletePayload = method.recipients == Recipients.ANY_INSTANCE

        scope.launch(coroutineNames[method.index]) {
            val timeout = remaining(deadline, method.timeout)
            if (!timeout.isPositive()) return@launch dropExpired(message, timeout)

            if (replyTo == null) {
                // Nobody waits for a response, so the handle may take as long as needed
                invoke(method, message, timeout = null, deletePayload)
                return@launch
            }

            when (val outcome = invoke(method, message, timeout, deletePayload)) {
                is Outcome.Finished -> send(replyTo, replyOf(method, outcome), timeout)

                Outcome.TimedOut -> logTimedOut(method, timeout)
            }
        }
    }

    private fun onPersistent(message: Message) {
        val method = persistentBySubject[message.subject]
        if (method == null) {
            scope.launch(CoroutineName("surf-nats/${client.name}/persist/unknown")) {
                val error =
                    IllegalStateException("No persistent method is registered for ${message.subject}")
                failFire(message, error, permanent = false)
            }
            return
        }
        val meta = message.metaData()
        val context = NatsPersistentContext(
            messageId = message.header(NatsJetStreamConstants.MSG_ID_HDR)
                ?: meta.streamSequence().toString(),
            deliveryCount = meta.deliveredCount(),
        )

        val deadline = message.deadlineMillis()
        val left = remaining(deadline, method.timeout)
        if (!left.isPositive()) {
            dropExpired(message, left)
            return message.term()
        }

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
                if (method.result == NatsResultKind.UNIT) {
                    handleFire(method, message)
                } else {
                    handlePersistentRpc(method, message, deadline)
                }
            } finally {
                keepAlive.cancel()
            }
        }
    }

    private suspend fun handleFire(method: MethodDescriptor, message: Message) {
        val data = try {
            PayloadStore.resolve(message, PayloadBucket.PERSISTENT, delete = false)
        } catch (e: CancellationException) {
            currentCoroutineContext().ensureActive()
            return failFire(message, e, permanent = false)
        } catch (e: Throwable) {
            e.rethrowIfFatal()
            return failFire(message, e, permanent = isObjectNotFound(e))
        }

        val arguments = try {
            method.decodeArguments(data, message.header(ProtocolHeaders.CONTENT_TYPE))
        } catch (e: CancellationException) {
            currentCoroutineContext().ensureActive()
            return failFire(message, e, permanent = false)
        } catch (e: Throwable) {
            e.rethrowIfFatal()
            return failFire(message, e, permanent = true)
        }

        val completed: Unit? = try {
            withTimeoutOrNull(RpcDefaults.FIRE_MAX_DURATION) {
                invokers[method.index].call(arguments)
                Unit
            }
        } catch (e: CancellationException) {
            currentCoroutineContext().ensureActive()
            return failFire(message, e, permanent = false)
        } catch (e: Throwable) {
            e.rethrowIfFatal()
            return failFire(message, e, permanent = false)
        }

        if (completed == null) {
            val error =
                TimeoutException("Handler did not complete within ${RpcDefaults.FIRE_MAX_DURATION}")
            return failFire(message, error, permanent = false)
        }

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

        PayloadStore.delete(message, PayloadBucket.PERSISTENT)
    }

    private fun isObjectNotFound(e: Throwable): Boolean {
        return e is IllegalArgumentException &&
                e.message?.startsWith("[${NatsJetStreamClientError.OsObjectNotFound.id()}]") == true
    }

    private suspend fun failFire(message: Message, e: Throwable, permanent: Boolean) {
        val delivered = message.metaData().deliveredCount()
        client.logger.error(
            "Persistent call on ${message.subject} failed (delivery $delivered${if (permanent) ", permanent" else ""})",
            e
        )
        if ((permanent || delivered >= RpcDefaults.MAX_DELIVER) && deadLetter(message, e)) {
            message.term()
        } else {
            val backoff = RpcDefaults.NAK_BACKOFF[(delivered - 1).toInt()
                .coerceIn(0, RpcDefaults.NAK_BACKOFF.lastIndex)]
            message.nakWithDelay(backoff.toJavaDuration())
        }
    }

    private suspend fun deadLetter(message: Message, e: Throwable): Boolean {
        val streams = checkNotNull(descriptor.streams)
        val headers = Headers(message.headers)
            .put(ProtocolHeaders.ERROR, e.javaClass.name)
            .put(ProtocolHeaders.DEAD_SUBJECT, message.subject)

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
        if (outcome !is Outcome.Finished) {
            logTimedOut(method, timeout)
            message.term()
            return // Caller has already given up
        }

        message.header(ProtocolHeaders.REPLY_TO)?.let { replyTo ->
            send(replyTo, replyOf(method, outcome), timeout)
        }
        message.ack()

        PayloadStore.delete(message, PayloadBucket.TRANSIENT)
    }

    override fun close() {
        if (!active.compareAndSet(true, false)) return
        announcer?.close()
        drain(natsConfig.drainTimeout.seconds)
        scope.cancel("Service registration ${descriptor.type.name} closed")
        client.unregisterService(this)
        client.unregisterRegistration(this)
    }

    private fun drain(timeout: Duration) {
        val deadline = TimeSource.Monotonic.markNow() + timeout

        messageConsumer?.let {
            try {
                it.stop()
            } catch (e: Exception) {
                client.logger.warn("Failed to stop message consumer", e)
            }
        }

        dispatcher?.let { dispatcher ->
            val drained = try {
                // jnats unsubscribes and flushes before drain returns, so the lock does not cover the wait
                NatsConnectionManager.withFlushLock(timeout.toJavaDuration()) { dispatcher.drain(it) }
                    .get(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                false
            } catch (e: Exception) {
                client.logger.warn("Failed to drain service dispatcher", e)
                false
            }
            if (!drained) client.closeDispatcher(dispatcher, "service dispatcher")
        }

        messageConsumer?.let {
            try {
                it.close()
            } catch (e: Exception) {
                client.logger.warn("Failed to close message consumer", e)
            }
        }

        val handlers = scope.coroutineContext.job.children.toList()
        if (handlers.isEmpty()) return
        val finished = runBlocking {
            withTimeoutOrNull(deadline.timeLeft()) { handlers.joinAll() } != null
        }
        if (!finished) {
            client.logger.warn(
                "${handlers.count { it.isActive }} handlers of ${descriptor.type.name} did not finish " +
                        "within the drain timeout of $timeout and are cancelled"
            )
        }
    }

    private fun TimeMark.timeLeft(): Duration = (-elapsedNow()).coerceAtLeast(Duration.ZERO)

    fun discard() {
        active.set(false)
        scope.cancel("Service registration ${descriptor.type.name} discarded")
    }

    private fun remaining(deadline: Long?, fallback: Duration): Duration {
        return deadline?.let { (it - System.currentTimeMillis()).milliseconds } ?: fallback
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
            val args = method.decodeArguments(data, message.header(ProtocolHeaders.CONTENT_TYPE))
            Outcome.Completed(invokers[method.index].call(args))
        }

        val outcome = try {
            if (timeout == null) {
                call()
            } else {
                withTimeoutOrNull(timeout) {
                    call()
                } ?: Outcome.TimedOut
            }
        } catch (e: CancellationException) {
            currentCoroutineContext().ensureActive()
            Outcome.Failed(e)
        } catch (e: Throwable) {
            e.rethrowIfFatal()
            Outcome.Failed(e)
        }

        if (outcome is Outcome.Failed) {
            logFailure(method, outcome.error)
        }

        return outcome
    }

    private fun replyOf(
        method: MethodDescriptor,
        outcome: Outcome.Finished,
    ): Reply {
        return when (outcome) {
            is Outcome.Completed -> try {
                encodeResult(method, outcome.value)
            } catch (e: Throwable) {
                e.rethrowIfFatal()
                logFailure(method, e)
                errorReply(e)
            }

            is Outcome.Failed -> errorReply(outcome.error)
        }
    }

    private fun logFailure(method: MethodDescriptor, e: Throwable) {
        client.logger.error(
            "Service method ${method.label} failed",
            e
        )
    }

    private fun encodeResult(
        method: MethodDescriptor,
        value: Any?,
    ): Reply {
        val headers = client.headers()
        return when (method.result) {
            NatsResultKind.UNIT -> Reply(headers, NatsHeaders.EMPTY_BODY)
            NatsResultKind.VALUE -> Reply(headers, method.encodeResponse(value as Any))

            NatsResultKind.ANSWER -> when (val answer = value as Answer<*>) {
                is Answer.Known -> Reply(headers, method.encodeResponse(answer.value as Any))

                Answer.Unknown -> Reply(
                    headers.add(ProtocolHeaders.ANSWER, ProtocolHeaders.ANSWER_UNKNOWN),
                    NatsHeaders.EMPTY_BODY
                )
            }
        }
    }

    private fun errorReply(throwable: Throwable): Reply {
        val headers = client.headers()
            .add(ProtocolHeaders.ERROR, throwable.javaClass.name)

        return Reply(
            headers,
            ProtocolDefaults.errorMessage(throwable.message).encodeToByteArray()
        )
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
}