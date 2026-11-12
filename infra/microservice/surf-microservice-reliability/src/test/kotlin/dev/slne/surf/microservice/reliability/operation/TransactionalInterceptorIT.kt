package dev.slne.surf.microservice.reliability.operation

import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.slne.surf.database.reliability.operation.AtomicOperationConflictException
import dev.slne.surf.database.reliability.operation.AtomicOperationInProgressException
import dev.slne.surf.database.reliability.operation.AtomicOperations
import dev.slne.surf.database.reliability.operation.AtomicOperationsConfig
import dev.slne.surf.microservice.reliability.TransactionalEvents
import dev.slne.surf.microservice.reliability.support.FakePersistentCall
import dev.slne.surf.microservice.reliability.support.FakePersistentCall.Companion.valueReply
import dev.slne.surf.microservice.reliability.support.TestDatabase
import dev.slne.surf.microservice.reliability.support.TestVendor
import dev.slne.surf.nats.NatsClient
import dev.slne.surf.nats.descriptor.NatsEvent
import dev.slne.surf.nats.rpc.NatsPersistentReply
import dev.slne.surf.nats.rpc.NatsRedeliveryException
import dev.slne.surf.nats.test.common.proto.v1.TestChatEvent
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Testcontainers(disabledWithoutDocker = true)
class TransactionalInterceptorIT {

    private val runs = AtomicInteger()
    private val events = TransactionalEvents()
    private val published = ConcurrentLinkedQueue<NatsEvent>()
    private val client: NatsClient = mockk {
        every { publish(any()) } answers { published += firstArg<NatsEvent>() }
    }

    @ParameterizedTest
    @EnumSource(TestVendor::class)
    fun `a redelivery receives the stored reply without running the handler`(vendor: TestVendor) =
        withInterceptor(vendor) { database, api, interceptor ->
            val first = interceptor.intercept(call("message-1") { insertAndReply(api, it, "first") })
            val redelivery =
                interceptor.intercept(call("message-1", deliveryCount = 2) { insertAndReply(api, it, "again") })

            assertEquals("first", redelivery.bytes.decodeToString())
            assertEquals(first.format, redelivery.format)
            assertEquals(1, runs.get())
            assertEquals(1, database.entryCount(api, "message-1"))
        }

    @ParameterizedTest
    @EnumSource(TestVendor::class)
    fun `a handler exception propagates unchanged and stores nothing`(vendor: TestVendor) =
        withInterceptor(vendor) { database, api, interceptor ->
            // Coroutine stack-trace recovery may copy the exception, so compare type and message, not identity.
            val thrown = assertFailsWith<IllegalArgumentException> {
                interceptor.intercept(call("message-2") {
                    insertAndReply(api, it, "never stored")
                    throw IllegalArgumentException("rejected")
                })
            }
            assertEquals("rejected", thrown.message)
            assertEquals(0, database.entryCount(api, "message-2"))

            val retry = interceptor.intercept(call("message-2", deliveryCount = 2) { insertAndReply(api, it, "second") })
            assertEquals("second", retry.bytes.decodeToString())
            assertEquals(2, runs.get())
            assertEquals(1, database.entryCount(api, "message-2"))
        }

    @ParameterizedTest
    @EnumSource(TestVendor::class)
    fun `a handler requesting a redelivery is not answered`(vendor: TestVendor) =
        withInterceptor(vendor) { _, _, interceptor ->
            val thrown = assertFailsWith<NatsRedeliveryException> {
                interceptor.intercept(call("message-3") { throw NatsRedeliveryException("later") })
            }

            assertEquals("later", thrown.message)
        }

    @ParameterizedTest
    @EnumSource(TestVendor::class)
    fun `a reused message id with another request is answered with a conflict error`(vendor: TestVendor) =
        withInterceptor(vendor) { _, api, interceptor ->
            interceptor.intercept(call("message-4", request = byteArrayOf(1)) { insertAndReply(api, it, "first") })

            val reply = interceptor.intercept(call("message-4", request = byteArrayOf(2)) { valueReply("other") })

            assertEquals(NatsPersistentReply.error(RuntimeException()).format, reply.format)
            assertTrue(reply.bytes.decodeToString().startsWith(AtomicOperationConflictException::class.java.name))
            assertEquals(1, runs.get())
        }

    @ParameterizedTest
    @EnumSource(TestVendor::class)
    fun `concurrent deliveries on two instances run the handler once`(vendor: TestVendor) = runBlocking {
        val database = vendor.freshDatabase()
        database.open("instance-a").use { first ->
            database.open("instance-b").use { second ->
                val instanceA = TransactionalInterceptor(OWNER, AtomicOperations(first), client)
                val instanceB = TransactionalInterceptor(OWNER, AtomicOperations(second), client)
                val handlerStarted = CompletableDeferred<Unit>()
                val releaseHandler = CompletableDeferred<Unit>()

                val replies = withContext(Dispatchers.Default) {
                    val a = async {
                        instanceA.intercept(call("message-5") {
                            handlerStarted.complete(Unit)
                            releaseHandler.await()
                            publishAndReply(first, it, "from a")
                        })
                    }
                    handlerStarted.await()
                    val b = async { instanceB.intercept(call("message-5") { publishAndReply(second, it, "from b") }) }
                    delay(500.milliseconds) // instance B now waits for the claim of instance A
                    releaseHandler.complete(Unit)
                    listOf(a.await(), b.await())
                }

                assertEquals(listOf("from a", "from a"), replies.map { it.bytes.decodeToString() })
                assertEquals(1, runs.get())
                assertEquals(1, database.entryCount(first, "message-5"))
                assertEquals(listOf("from a"), publishedMessages())
            }
        }
    }

    @ParameterizedTest
    @EnumSource(TestVendor::class)
    fun `a claim held beyond the lock timeout requests a redelivery`(vendor: TestVendor) = runBlocking {
        val database = vendor.freshDatabase()
        database.open("holder").use { first ->
            database.open("waiter").use { second ->
                val holder = TransactionalInterceptor(OWNER, AtomicOperations(first), client)
                val waiter = TransactionalInterceptor(
                    OWNER,
                    AtomicOperations(second, AtomicOperationsConfig(lockTimeout = 1.seconds)),
                    client,
                )
                val handlerStarted = CompletableDeferred<Unit>()
                val releaseHandler = CompletableDeferred<Unit>()

                withContext(Dispatchers.Default) {
                    val held = async {
                        holder.intercept(call("message-6") {
                            handlerStarted.complete(Unit)
                            releaseHandler.await()
                            insertAndReply(first, it, "held")
                        })
                    }
                    handlerStarted.await()

                    val failure = assertFailsWith<NatsRedeliveryException> {
                        waiter.intercept(call("message-6") { insertAndReply(second, it, "never") })
                    }
                    assertIs<AtomicOperationInProgressException>(failure.cause)

                    releaseHandler.complete(Unit)
                    held.await()
                }
                assertEquals(1, runs.get())
            }
        }
    }

    @ParameterizedTest
    @EnumSource(TestVendor::class)
    fun `an unavailable database requests a redelivery`(vendor: TestVendor) = runBlocking {
        val database = vendor.freshDatabase()
        val closed = database.open("closed")
        val interceptor = TransactionalInterceptor(OWNER, AtomicOperations(closed.api), client)
        closed.close()

        assertFailsWith<NatsRedeliveryException> {
            interceptor.intercept(call("message-7") { valueReply("never") })
        }
        assertEquals(0, runs.get())
    }

    @ParameterizedTest
    @EnumSource(TestVendor::class)
    fun `events are published in order once the handler's transaction has committed`(vendor: TestVendor) =
        runBlocking {
            val database = vendor.freshDatabase()
            database.open("interceptor").use { api ->
                val committedAtPublish = ConcurrentLinkedQueue<Long>()
                val observingClient: NatsClient = mockk {
                    every { publish(any()) } answers {
                        published += firstArg<NatsEvent>()
                        committedAtPublish += runBlocking { database.entryCount(api, "message-8") }
                    }
                }
                val interceptor = TransactionalInterceptor(OWNER, AtomicOperations(api), observingClient)

                interceptor.intercept(call("message-8") {
                    events.publish(chatEvent("first"))
                    events.publish(chatEvent("second"))
                    assertEquals(emptyList(), publishedMessages(), "events must wait for the commit")
                    insertAndReply(api, it, "committed")
                })

                assertEquals(listOf("first", "second"), publishedMessages())
                assertEquals(listOf(1L, 1L), committedAtPublish.toList(), "the handler's row must be committed")
            }
        }

    @ParameterizedTest
    @EnumSource(TestVendor::class)
    fun `a failing handler publishes nothing`(vendor: TestVendor) = withInterceptor(vendor) { _, api, interceptor ->
        assertFailsWith<IllegalStateException> {
            interceptor.intercept(call("message-9") {
                publishAndReply(api, it, "rolled-back")
                error("handler failed after publishing")
            })
        }
        assertEquals(emptyList(), publishedMessages())

        interceptor.intercept(call("message-9", deliveryCount = 2) { publishAndReply(api, it, "retried") })
        assertEquals(listOf("retried"), publishedMessages())
    }

    @ParameterizedTest
    @EnumSource(TestVendor::class)
    fun `a redelivery answered with the stored reply publishes nothing`(vendor: TestVendor) =
        withInterceptor(vendor) { _, api, interceptor ->
            interceptor.intercept(call("message-10") { publishAndReply(api, it, "first") })
            interceptor.intercept(call("message-10", deliveryCount = 2) { publishAndReply(api, it, "again") })

            assertEquals(listOf("first"), publishedMessages())
            assertEquals(1, runs.get())
        }

    @ParameterizedTest
    @EnumSource(TestVendor::class)
    fun `a failed publish after the commit keeps the reply`(vendor: TestVendor) = runBlocking {
        val database = vendor.freshDatabase()
        database.open("interceptor").use { api ->
            val failingClient: NatsClient = mockk {
                every { publish(any()) } throws IllegalStateException("NATS is not connected")
            }
            val interceptor = TransactionalInterceptor(OWNER, AtomicOperations(api), failingClient)

            val reply = interceptor.intercept(call("message-11") { publishAndReply(api, it, "kept") })

            assertEquals("kept", reply.bytes.decodeToString())
            assertEquals(1, database.entryCount(api, "message-11"))
        }
    }

    @ParameterizedTest
    @EnumSource(TestVendor::class)
    fun `events outside a transactional handler are rejected`(vendor: TestVendor) = runBlocking {
        val failure = assertFailsWith<IllegalStateException> {
            events.publish(chatEvent(vendor.name))
        }

        assertTrue("registerTransactionalService" in failure.message.orEmpty())
    }

    private fun call(
        messageId: String,
        request: ByteArray = messageId.encodeToByteArray(),
        deliveryCount: Long = 1,
        handler: suspend (messageId: String) -> NatsPersistentReply,
    ) = FakePersistentCall(messageId, request, deliveryCount) {
        runs.incrementAndGet()
        handler(messageId)
    }

    private suspend fun insertAndReply(api: DatabaseApi, messageId: String, reply: String): NatsPersistentReply {
        suspendTransaction(db = api.database) {
            exec("INSERT INTO test_entries (entry) VALUES ('$messageId')")
        }
        return valueReply(reply)
    }

    private suspend fun publishAndReply(api: DatabaseApi, messageId: String, message: String): NatsPersistentReply {
        events.publish(chatEvent(message), aggregateId = messageId)
        return insertAndReply(api, messageId, message)
    }

    private fun chatEvent(message: String) = TestChatEvent(origin = OWNER, message = message)

    private fun publishedMessages(): List<String> = published.map { (it as TestChatEvent).message }

    private fun withInterceptor(
        vendor: TestVendor,
        block: suspend (TestDatabase, DatabaseApi, TransactionalInterceptor) -> Unit,
    ) = runBlocking {
        val database = vendor.freshDatabase()
        database.open("interceptor").use { api ->
            block(database, api, TransactionalInterceptor(OWNER, AtomicOperations(api), client))
        }
    }

    private companion object {
        const val OWNER = "interceptor-test"
    }
}
