package dev.slne.surf.microservice.reliability.integration

import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.microservice.reliability.ReliabilityConfig
import dev.slne.surf.microservice.reliability.support.OriginListener
import dev.slne.surf.microservice.reliability.support.ReceivedEvents
import dev.slne.surf.microservice.reliability.support.ReceivedOrigin
import dev.slne.surf.microservice.reliability.support.RecordingTestService
import dev.slne.surf.microservice.reliability.support.TestClients
import dev.slne.surf.microservice.reliability.support.TestDatabase
import dev.slne.surf.microservice.reliability.support.TestNats
import dev.slne.surf.microservice.reliability.support.TestNats.streamMessageCount
import dev.slne.surf.microservice.reliability.support.TestVendor
import dev.slne.surf.microservice.reliability.support.eventually
import dev.slne.surf.nats.NatsClient
import dev.slne.surf.nats.registerService
import dev.slne.surf.nats.rpc.NatsPersistentCall
import dev.slne.surf.nats.rpc.NatsPersistentInterceptor
import dev.slne.surf.nats.rpc.NatsPersistentReply
import dev.slne.surf.nats.rpc.NatsRedeliveryException
import dev.slne.surf.nats.service
import dev.slne.surf.nats.test.common.proto.v1.MicroserviceTestService
import dev.slne.surf.nats.test.common.proto.v1.StoreRequest
import dev.slne.surf.nats.test.common.proto.v1.StoredCountRequest
import io.nats.client.impl.Headers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * Transactional services over a real NATS server and database: replies stored per message, redeliveries,
 * dead-letter replays, several instances and events published after the commit.
 */
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TransactionalServiceIT {

    @BeforeAll
    fun connect() = runBlocking { TestNats.start() }

    @AfterAll
    fun disconnect() = runBlocking { TestNats.stop() }

    @ParameterizedTest
    @EnumSource(TestVendor::class)
    fun `a persistent rpc whose reply was lost after the commit is answered with the stored reply`(vendor: TestVendor) =
        runBlocking {
            val deliveries = ConcurrentLinkedQueue<NatsPersistentCall>()
            val replyLost = AtomicBoolean(true)
            val loseFirstReply: (NatsPersistentInterceptor) -> NatsPersistentInterceptor = { transactional ->
                NatsPersistentInterceptor { call ->
                    deliveries += call
                    val reply = transactional.intercept(call)
                    if (replyLost.compareAndSet(true, false)) throw NatsRedeliveryException("reply lost after commit")
                    reply
                }
            }

            withInstance(vendor, loseFirstReply) { instance, caller ->
                val response = caller.service<MicroserviceTestService>().storedCount(StoredCountRequest())

                assertEquals(1, response.count)
                assertEquals(1, instance.service.storedCountRuns.get())
                assertEquals(1, instance.database.entryCount(instance.api, RecordingTestService.RPC_ENTRY))
                assertEquals(listOf(1L, 2L), deliveries.map { it.deliveryCount })
                assertEquals(1, deliveries.map { it.messageId }.distinct().size)
            }
        }

    @ParameterizedTest
    @EnumSource(TestVendor::class)
    fun `a dead-letter replay of a committed fire call does not run the handler again`(vendor: TestVendor) =
        runBlocking {
            val deliveries = ConcurrentLinkedQueue<NatsPersistentCall>()
            val deadLetterFirst = AtomicBoolean(true)
            val deadLetterAfterCommit: (NatsPersistentInterceptor) -> NatsPersistentInterceptor = { transactional ->
                NatsPersistentInterceptor { call ->
                    deliveries += call
                    val reply = transactional.intercept(call)
                    if (deadLetterFirst.compareAndSet(true, false)) {
                        NatsPersistentReply.error(IllegalStateException("acknowledgement lost after commit"))
                    } else {
                        reply
                    }
                }
            }
            val entry = "dlq-${vendor.name.lowercase()}"

            TestNats.rawConnection().use { raw ->
                withInstance(vendor, deadLetterAfterCommit) { instance, caller ->
                    val deadBefore = raw.streamMessageCount(DLQ_STREAM)
                    caller.service<MicroserviceTestService>().store(StoreRequest(entry))
                    eventually("the message must be dead-lettered") {
                        raw.streamMessageCount(DLQ_STREAM) == deadBefore + 1
                    }

                    val dead = raw.jetStreamManagement().getLastMessage(DLQ_STREAM, DLQ_SUBJECT)
                    val deadHeaders = checkNotNull(dead.headers)
                    raw.jetStream().publish(deadHeaders.getFirst(DEAD_SUBJECT_HEADER), Headers(deadHeaders), dead.data)

                    eventually("the replay must be delivered") { deliveries.size == 2 }
                    eventually("the replay must be acknowledged") { raw.streamMessageCount(WORK_QUEUE_STREAM) == 0L }
                    assertEquals(1, deliveries.map { it.messageId }.distinct().size)
                    assertEquals(1, instance.service.storeRunsOf(entry))
                    assertEquals(1, instance.database.entryCount(instance.api, entry))
                }
            }
        }

    @ParameterizedTest
    @EnumSource(TestVendor::class)
    fun `events of a handler are published once per commit`(vendor: TestVendor) = runBlocking {
        withInstance(vendor) { _, caller ->
            val received = ReceivedEvents()
            caller.registerListener(received)
            val entries = (1..20).map { "event-$it" }

            val service = caller.service<MicroserviceTestService>()
            entries.forEach { service.store(StoreRequest(it)) }

            eventually("all events must arrive") { received.eventIds.size >= entries.size }
            delay(1.seconds) // a duplicate would arrive within this window
            assertEquals(entries.size, received.eventIds.size)
            assertEquals(entries.toSet(), received.messages.toSet())
        }
    }

    @ParameterizedTest
    @EnumSource(TestVendor::class)
    fun `events of a handler carry the origin of the client the service is registered on`(vendor: TestVendor) =
        runBlocking {
            withInstance(vendor) { instance, caller ->
                val own = OriginListener()
                val foreign = OriginListener()
                instance.serviceClient.registerListener(own)
                caller.registerListener(foreign)
                val entry = "origin-${vendor.name.lowercase()}"

                caller.service<MicroserviceTestService>().store(StoreRequest(entry))

                eventually("the event must reach both clients") { own.all.isNotEmpty() && foreign.all.isNotEmpty() }
                delay(1.seconds) // an event the own client does not ignore would arrive within this window
                val serviceOrigin = own.all.single().origin.orEmpty()
                assertTrue(serviceOrigin.startsWith("${instance.serviceClient.name}@"), serviceOrigin)
                assertEquals(listOf(ReceivedOrigin(entry, serviceOrigin, isFromThisClient = true)), own.all.toList())
                assertEquals(emptyList(), own.ignoringOwn.toList(), "ignoreFromThisClient must skip the event")
                val foreignView = ReceivedOrigin(entry, serviceOrigin, isFromThisClient = false)
                assertEquals(listOf(foreignView), foreign.all.toList())
                assertEquals(listOf(foreignView), foreign.ignoringOwn.toList())
            }
        }

    @ParameterizedTest
    @EnumSource(TestVendor::class)
    fun `the event of a handler cancelled after its commit is published once`(vendor: TestVendor) = runBlocking {
        val deliveries = ConcurrentLinkedQueue<NatsPersistentCall>()
        val cancelFirst = AtomicBoolean(true)
        val cancelAfterCommit: (NatsPersistentInterceptor) -> NatsPersistentInterceptor = { transactional ->
            NatsPersistentInterceptor { call ->
                deliveries += call
                val reply = transactional.intercept(call)
                if (cancelFirst.compareAndSet(true, false)) throw CancellationException("timed out after the commit")
                reply
            }
        }
        val entry = "cancelled-${vendor.name.lowercase()}"

        TestNats.rawConnection().use { raw ->
            withInstance(vendor, cancelAfterCommit) { instance, caller ->
                val received = ReceivedEvents()
                caller.registerListener(received)

                caller.service<MicroserviceTestService>().store(StoreRequest(entry))

                eventually("the committed event must arrive") { entry in received.messages }
                eventually("the cancelled message must be redelivered") { deliveries.size == 2 }
                eventually("the redelivery must be acknowledged") { raw.streamMessageCount(WORK_QUEUE_STREAM) == 0L }
                delay(1.seconds) // a duplicate from the redelivery would arrive within this window
                assertEquals(listOf(entry), received.messages)
                assertEquals(1, instance.service.storeRunsOf(entry))
                assertEquals(1, instance.database.entryCount(instance.api, entry))
            }
        }
    }

    @ParameterizedTest
    @EnumSource(TestVendor::class)
    fun `two instances of a service apply every message once`(vendor: TestVendor) = runBlocking {
        val database = vendor.freshDatabase()
        withInstance(database) { first, caller ->
            withInstance(database) { second, _ ->
                val received = ReceivedEvents()
                caller.registerListener(received)
                val entries = (1..30).map { "shared-$it" }

                val service = caller.service<MicroserviceTestService>()
                entries.forEach { service.store(StoreRequest(it)) }

                eventually("every entry must be stored") {
                    entries.all { first.service.storeRunsOf(it) + second.service.storeRunsOf(it) == 1 }
                }
                eventually("every event must arrive") { received.eventIds.size >= entries.size }
                delay(1.seconds)
                entries.forEach { assertEquals(1, database.entryCount(first.api, it), it) }
                assertEquals(entries.size, received.eventIds.distinct().size)
                assertEquals(entries.size, received.eventIds.size)
            }
        }
    }

    private class Instance(
        val database: TestDatabase,
        val api: DatabaseApi,
        val service: RecordingTestService,
        val serviceClient: NatsClient,
    )

    private suspend fun withInstance(
        vendor: TestVendor,
        wrap: (NatsPersistentInterceptor) -> NatsPersistentInterceptor = { it },
        block: suspend (Instance, NatsClient) -> Unit,
    ) = withInstance(vendor.freshDatabase(), wrap, block)

    private suspend fun withInstance(
        database: TestDatabase,
        wrap: (NatsPersistentInterceptor) -> NatsPersistentInterceptor = { it },
        block: suspend (Instance, NatsClient) -> Unit,
    ) = database.open("instance").use { api ->
        val runtime = ReliabilityRuntime.start(OWNER, api, ReliabilityConfig())
        val service = RecordingTestService(api, runtime.events)
        val serviceClient = TestClients.create("service")
        val caller = TestClients.create("caller")
        try {
            serviceClient.registerService<MicroserviceTestService>(service, wrap(runtime.interceptorFor(serviceClient)))
            withContext(Dispatchers.Default) { block(Instance(database, api, service, serviceClient), caller) }
        } finally {
            withContext(Dispatchers.IO) {
                caller.close()
                serviceClient.close()
            }
            runtime.stop()
        }
    }

    private companion object {
        const val OWNER = "reliability-test"
        const val SERVICE = "surf.nats.testing.v1.MicroserviceTestService"
        const val WORK_QUEUE_STREAM = "SURF_PERSIST_surf_nats_testing_v1_MicroserviceTestService"
        const val DLQ_STREAM = "SURF_DLQ_surf_nats_testing_v1_MicroserviceTestService"
        const val DLQ_SUBJECT = "surf.dlq.$SERVICE"
        const val DEAD_SUBJECT_HEADER = "Surf-Dead-Subject"
    }
}
