package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.*
import dev.slne.surf.nats.client.NatsClientImpl
import dev.slne.surf.nats.connection.NatsConnectionManager
import dev.slne.surf.nats.jetstream.JetStreamProvisioner
import dev.slne.surf.nats.protocol.ProtocolHeaders
import dev.slne.surf.nats.protocol.ServiceNames
import dev.slne.surf.nats.rpc.delivery.DeadlineExpiredException
import dev.slne.surf.nats.rpc.delivery.NoHandlerSlotException
import dev.slne.surf.nats.test.proto.v1.Text
import io.nats.client.api.PublishAck
import io.nats.client.impl.Headers
import io.nats.client.support.NatsJetStreamConstants
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

// LimitedPersistentService runs one handler at a time, LaneService two with one long (test config.yml)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ServiceAdmissionTest {

    private var clientCounter = 0
    private val defaultBackoff = RpcDefaults.NAK_BACKOFF
    private val defaultFireMaxDuration = RpcDefaults.FIRE_MAX_DURATION

    @BeforeAll
    fun connect() = runBlocking { NatsInstance.instance.load() }

    @AfterAll
    fun disconnect() = runBlocking { NatsInstance.instance.disable() }

    @AfterEach
    fun resetDefaults() {
        RpcDefaults.NAK_BACKOFF = defaultBackoff
        RpcDefaults.FIRE_MAX_DURATION = defaultFireMaxDuration
    }

    private class LimitedImpl : LimitedPersistentService {
        val blocking = CompletableDeferred<Unit>()
        val gate = CompletableDeferred<Unit>()
        val blockCancelled = CompletableDeferred<Unit>()
        val blockCalls = AtomicInteger()
        val quickRuns = Channel<QuickRun>(Channel.UNLIMITED)
        val recorded = Channel<Long>(Channel.UNLIMITED)

        override suspend fun block(): Text {
            blockCalls.incrementAndGet()
            blocking.complete(Unit)
            try {
                gate.await()
            } catch (e: CancellationException) {
                blockCancelled.complete(Unit)
                throw e
            }
            return Text("block")
        }

        // Runs until its timeout cancels it
        override suspend fun quick(): Text {
            val startedAt = System.currentTimeMillis()
            try {
                awaitCancellation()
            } finally {
                quickRuns.trySend(QuickRun(startedAt, System.currentTimeMillis()))
            }
        }

        override suspend fun record() {
            recorded.send(natsPersistentContext().deliveryCount)
        }
    }

    // Epoch milliseconds of the host clock
    private class QuickRun(val startedAt: Long, val endedAt: Long)

    private class LaneImpl : LaneService {
        val holds = AtomicInteger()
        val gate = CompletableDeferred<Unit>()

        override suspend fun hold() {
            holds.incrementAndGet()
            gate.await()
        }

        override suspend fun short(): Text = Text("short")

        override suspend fun slow(): Text = Text("slow")
    }

    @Test
    fun `fire without a handler slot on its last delivery is dead-lettered`() = withClient { client, streams ->
        RpcDefaults.FIRE_MAX_DURATION = 200.milliseconds
        RpcDefaults.NAK_BACKOFF = listOf(100.milliseconds)
        val impl = LimitedImpl()
        client.registerService<LimitedPersistentService>(impl)
        val service = client.service<LimitedPersistentService>()
        val blocked = async { withNatsCallTimeout(30.seconds) { service.block() } }
        withTimeout(5.seconds) { impl.blocking.await() }

        service.record()

        eventually("the fire must be dead-lettered", timeout = 20.seconds) { messageCount(streams.dlqStream) == 1L }
        val dead = jsm().getLastMessage(streams.dlqStream, streams.dlqSubject)
        val error = assertNotNull(dead.headers).getFirst(ProtocolHeaders.ERROR)
        assertEquals(NoHandlerSlotException::class.java.name, error)
        eventually("the work queue must be empty after the dead letter") { messageCount(streams.stream) == 0L }
        assertNull(impl.recorded.tryReceive().getOrNull(), "the fire must not run")

        impl.gate.complete(Unit)
        blocked.await()
    }

    @Test
    fun `fire past its deadline is dead-lettered without running`() = withClient { client, streams ->
        val impl = LimitedImpl()
        client.registerService<LimitedPersistentService>(impl)

        publishExternal(
            client.method("record").subject,
            ProtocolHeaders.DEADLINE to (System.currentTimeMillis() - 1_000).toString(),
        )

        eventually("the expired fire must be dead-lettered") { messageCount(streams.dlqStream) == 1L }
        val dead = jsm().getLastMessage(streams.dlqStream, streams.dlqSubject)
        assertEquals(
            DeadlineExpiredException::class.java.name,
            assertNotNull(dead.headers).getFirst(ProtocolHeaders.ERROR)
        )
        eventually("the work queue must be empty after the dead letter") { messageCount(streams.stream) == 0L }
        assertNull(impl.recorded.tryReceive().getOrNull(), "the expired fire must not run")
    }

    @Test
    fun `persistent rpc without a deadline header loses the time it waited for a handler slot`() =
        withClient { client, streams ->
            val impl = LimitedImpl()
            client.registerService<LimitedPersistentService>(impl)
            val service = client.service<LimitedPersistentService>()
            val blocked = async { withNatsCallTimeout(30.seconds) { service.block() } }
            withTimeout(5.seconds) { impl.blocking.await() }

            val quick = client.method("quick")
            val timeoutMillis = quick.timeout.inWholeMilliseconds
            val ack = publishExternal(quick.subject)
            // The responder counts from the publish time the server stamps, so the test reads the same timestamp
            val publishedAt = jsm().getMessage(streams.stream, ack.seqno).time!!.toInstant().toEpochMilli()
            val deadline = publishedAt + timeoutMillis
            assumeTrue(deadline - System.currentTimeMillis() > 1_500, "the server clock is too far behind")

            // Free the slot halfway to the deadline
            delay((publishedAt + timeoutMillis / 2 - System.currentTimeMillis()).milliseconds)
            impl.gate.complete(Unit)
            blocked.await()

            val run = withTimeout(5.seconds) { impl.quickRuns.receive() }
            val ranFor = run.endedAt - run.startedAt
            val leftAtStart = deadline - run.startedAt
            assertTrue(
                ranFor in (leftAtStart - 300)..(leftAtStart + 300),
                "the rpc ran for $ranFor ms instead of the $leftAtStart ms left until its deadline"
            )
            assertTrue(ranFor < timeoutMillis - 500, "the rpc ran for $ranFor ms, close to its full timeout")
            eventually("the timed out rpc must be removed from the work queue") { messageCount(streams.stream) == 0L }
        }

    @Test
    fun `draining registration drops core requests waiting for a handler slot`() = withClient { client, _ ->
        val impl = LimitedImpl()
        val registration = client.registerService<LimitedPersistentService>(impl)
        val service = client.service<LimitedPersistentService>()
        val running = async { withNatsCallTimeout(30.seconds) { service.block() } }
        withTimeout(5.seconds) { impl.blocking.await() }
        val queued = async { runCatching { withNatsCallTimeout(3.seconds) { service.block() } } }
        delay(500.milliseconds) // the second request waits for the slot

        launch {
            delay(500.milliseconds)
            impl.gate.complete(Unit)
        }
        val closeTime = measureTime { withContext(Dispatchers.IO) { registration.close() } }
        running.await()

        assertTrue(closeTime < 5.seconds, "close took $closeTime, close to the drain timeout")
        assertIs<NatsCallTimeoutException>(queued.await().exceptionOrNull())
        assertEquals(1, impl.blockCalls.get(), "the draining registration ran the queued request")
    }

    @Test
    fun `fire calls and methods with a long timeout share the long handler slots`() =
        withClient(LaneService::class.java) { client, _ ->
            val impl = LaneImpl()
            client.registerService<LaneService>(impl)
            val service = client.service<LaneService>()

            service.hold()
            service.hold()
            eventually("the first fire must hold the long slot") { impl.holds.get() == 1 }
            delay(500.milliseconds)
            assertEquals(1, impl.holds.get(), "the second fire ran while the long slot was taken")

            assertEquals(Text("short"), withNatsCallTimeout(2.seconds) { service.short() })
            // The declared 30 s timeout makes the call long, not the second its caller waits
            assertFailsWith<NatsCallTimeoutException> { withNatsCallTimeout(1.seconds) { service.slow() } }

            impl.gate.complete(Unit)
            eventually("the second fire must run once the long slot is free") { impl.holds.get() == 2 }
        }

    @Test
    fun `draining registration redelivers fires waiting for a handler slot`() = withClient { client, _ ->
        val draining = LimitedImpl()
        val registration = client.registerService<LimitedPersistentService>(draining)
        val service = client.service<LimitedPersistentService>()
        val blocked = async { withNatsCallTimeout(30.seconds) { service.block() } }
        withTimeout(5.seconds) { draining.blocking.await() }
        service.record()
        delay(500.milliseconds) // the fire waits for the slot

        launch {
            delay(500.milliseconds)
            draining.gate.complete(Unit)
        }
        withContext(Dispatchers.IO) { registration.close() }
        blocked.await()
        assertNull(draining.recorded.tryReceive().getOrNull(), "the draining registration must not run the fire")

        val next = LimitedImpl()
        client.registerService<LimitedPersistentService>(next).use {
            assertEquals(2, withTimeout(10.seconds) { next.recorded.receive() }, "the fire was redelivered")
        }
    }

    @Test
    fun `interrupted close still unregisters the registration`() = withClient { client, _ ->
        val impl = LimitedImpl()
        val registration = client.registerService<LimitedPersistentService>(impl)
        val service = client.service<LimitedPersistentService>()
        val blocked = launch { withNatsCallTimeout(30.seconds) { service.block() } }
        withTimeout(5.seconds) { impl.blocking.await() }

        val failure = AtomicReference<Throwable?>()
        val closing = thread { runCatching { registration.close() }.onFailure(failure::set) }
        delay(500.milliseconds) // close waits for the running handler
        closing.interrupt()
        withContext(Dispatchers.IO) { closing.join(5_000) }

        assertFalse(closing.isAlive, "close did not return after the interrupt")
        assertNull(failure.get(), "close failed")
        withTimeout(5.seconds) { impl.blockCancelled.await() }
        assertTrue(registration.isClosed)
        client.registerService<LimitedPersistentService>(LimitedImpl()).close()
        blocked.cancel() // the cancelled handler never replies
    }

    private fun NatsClientImpl.method(name: String): MethodDescriptor =
        serviceDescriptor(LimitedPersistentService::class.java).methods.single { it.kotlinName == name }

    // Publishes like an external producer without Surf-Deadline unless extraHeaders carries one
    private fun publishExternal(subject: String, vararg extraHeaders: Pair<String, String>): PublishAck {
        val headers = Headers()
            .add(NatsJetStreamConstants.MSG_ID_HDR, UUID.randomUUID().toString())
            .add(ProtocolHeaders.CONTENT_TYPE, ProtocolHeaders.PROTOBUF_CONTENT_TYPE)
        extraHeaders.forEach { (name, value) -> headers.add(name, value) }
        return NatsConnectionManager.connection().jetStream().publish(subject, headers, ByteArray(0))
    }

    private fun withClient(
        serviceType: Class<*> = LimitedPersistentService::class.java,
        block: suspend CoroutineScope.(NatsClientImpl, ServiceNames) -> Unit,
    ) = runBlocking(Dispatchers.Default) {
        val client = NatsClient.create("admission-${clientCounter++}") as NatsClientImpl
        val streams = client.serviceDescriptor(serviceType).streams!!
        try {
            coroutineScope { block(client, streams) }
        } finally {
            withContext(Dispatchers.IO) { client.close() }
            for (stream in listOf(streams.stream, streams.dlqStream)) runCatching { jsm().deleteStream(stream) }
            JetStreamProvisioner.invalidate(streams)
        }
    }
}
