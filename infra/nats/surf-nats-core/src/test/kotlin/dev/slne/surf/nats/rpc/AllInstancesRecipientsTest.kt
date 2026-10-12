package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.*
import dev.slne.surf.nats.client.NatsClientImpl
import dev.slne.surf.nats.codec.NatsHeaders
import dev.slne.surf.nats.codec.presenceKind
import dev.slne.surf.nats.connection.NatsConnectionManager
import io.nats.client.Connection
import io.nats.client.Message
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AllInstancesRecipientsTest {

    @NatsService("finder")
    @NatsRecipients(Recipients.ALL_INSTANCES)
    interface FinderService {
        suspend fun find(id: Long): Answer<String>
        suspend fun broadcast(message: String)
    }

    class FinderImpl(private val answer: (Long) -> Answer<String> = { Answer.Unknown }) :
        FinderService {
        val broadcasts = Channel<String>(Channel.UNLIMITED)

        override suspend fun find(id: Long): Answer<String> = answer(id)

        override suspend fun broadcast(message: String) {
            broadcasts.send(message)
        }
    }

    @NatsService("notifier")
    @NatsRecipients(Recipients.ALL_INSTANCES)
    interface NotifierService {
        suspend fun announce(message: String)
    }

    sealed interface Behaviour {
        data class Known(val value: String) : Behaviour
        data object Unknown : Behaviour
        data class Fail(val type: String, val message: String) : Behaviour
        data object Hang : Behaviour

        /** A reply without `Surf-Error` and `Surf-Answer: unknown` whose body cannot be decoded. */
        data class Garbage(val withContentType: Boolean = true) : Behaviour
    }

    internal class FakeInstance(
        private val client: NatsClientImpl,
        private val announce: Boolean = true,
        private val behaviour: (Long) -> Behaviour,
    ) : AutoCloseable {
        val origin = "${client.name}@${UUID.randomUUID()}"
        val broadcasts = Channel<String>(Channel.UNLIMITED)

        private val connection = NatsConnectionManager.connection()
        private val dispatcher = connection.createDispatcher()
        private val descriptor = client.serviceDescriptor(FinderService::class.java)
        private val find = descriptor.methods.single { it.method.name == "find" }
        private val broadcast = descriptor.methods.single { it.method.name == "broadcast" }
        private val presenceSubject = descriptor.presenceSubject!!

        init {
            dispatcher.subscribe(find.subject) { request -> onFind(request) }
            dispatcher.subscribe(broadcast.subject) { request ->
                broadcasts.trySend(
                    client.codec.decode(
                        broadcast.arguments,
                        request.data,
                        request.headers?.getFirst(NatsHeaders.CONTENT_TYPE)
                    )[0] as String
                )
            }
            if (announce) {
                dispatcher.subscribe(presenceSubject) { message ->
                    if (message.presenceKind() == NatsHeaders.PRESENCE_QUERY) presence(NatsHeaders.PRESENCE_BEAT)
                }
                presence(NatsHeaders.PRESENCE_JOIN)
            }
            connection.flush(Duration.ofSeconds(5))
        }

        private fun onFind(request: Message) {
            val contentType = request.headers?.getFirst(NatsHeaders.CONTENT_TYPE)
            val id = client.codec.decode(find.arguments, request.data, contentType)[0] as Long
            val headers =
                NatsHeaders.base(client.name, WireFormat.CBOR).put(NatsHeaders.ORIGIN, origin)

            when (val answer = behaviour(id)) {
                is Behaviour.Known -> {
                    val serializer = (find.result as ResultKind.Answer).serializer
                    val data = client.codec.encode(serializer, answer.value, WireFormat.CBOR)
                    connection.publish(request.replyTo, headers, data)
                }

                Behaviour.Unknown -> connection.publish(
                    request.replyTo,
                    headers.add(NatsHeaders.ANSWER, NatsHeaders.ANSWER_UNKNOWN),
                    ByteArray(0)
                )

                is Behaviour.Fail -> connection.publish(
                    request.replyTo,
                    headers.add(NatsHeaders.ERROR, answer.type),
                    answer.message.encodeToByteArray()
                )

                is Behaviour.Garbage -> {
                    if (!answer.withContentType) headers.remove(NatsHeaders.CONTENT_TYPE)
                    connection.publish(request.replyTo, headers, byteArrayOf(0x7f, 0x01))
                }

                Behaviour.Hang -> Unit
            }
        }

        private fun presence(kind: String) {
            val headers = NatsHeaders.base(client.name, WireFormat.CBOR)
                .put(NatsHeaders.ORIGIN, origin)
                .add(NatsHeaders.PRESENCE, kind)
            connection.publish(presenceSubject, headers, ByteArray(0))
        }

        override fun close() {
            if (announce) presence(NatsHeaders.PRESENCE_LEAVE)
            connection.closeDispatcher(dispatcher)
        }
    }

    private var clientCounter = 0

    @BeforeAll
    fun connect() = runBlocking { NatsInstance.instance.load() }

    @AfterAll
    fun disconnect() = runBlocking { NatsInstance.instance.disable() }

    @Test
    fun `first Known answer wins immediately`() = withClient { client ->
        withInstances(
            FakeInstance(client) { Behaviour.Unknown },
            FakeInstance(client) { Behaviour.Known("server-$it") },
            FakeInstance(client) { Behaviour.Hang },
        ) {
            val finder = warmFinder(client)
            val (answer, elapsed) = timed { withNatsCallTimeout(5.seconds) { finder.find(7) } }

            assertEquals(Answer.Known("server-7"), answer)
            assertTrue(elapsed < 2.seconds, "took $elapsed")
        }
    }

    @Test
    fun `all known instances answering Unknown ends before the timeout`() = withClient { client ->
        withInstances(
            FakeInstance(client) { Behaviour.Unknown },
            FakeInstance(client) { Behaviour.Unknown },
            FakeInstance(client) { Behaviour.Unknown },
        ) {
            val finder = warmFinder(client)
            val (answer, elapsed) = timed { withNatsCallTimeout(5.seconds) { finder.find(1) } }

            assertEquals(Answer.Unknown, answer)
            assertTrue(elapsed < 2.seconds, "took $elapsed")
        }
    }

    @Test
    fun `hanging instance delays Unknown until the timeout`() = withClient { client ->
        withInstances(
            FakeInstance(client) { Behaviour.Unknown },
            FakeInstance(client) { Behaviour.Unknown },
            FakeInstance(client) { Behaviour.Hang },
        ) {
            val finder = warmFinder(client)
            val (answer, elapsed) = timed { withNatsCallTimeout(2.seconds) { finder.find(1) } }

            assertEquals(Answer.Unknown, answer)
            assertTrue(elapsed >= 1.5.seconds, "ended early after $elapsed")
        }
    }

    @Test
    fun `failure without any Known becomes NatsRemoteException once all answered`() =
        withClient { client ->
            withInstances(
                FakeInstance(client) { Behaviour.Fail("java.lang.IllegalStateException", "boom") },
                FakeInstance(client) { Behaviour.Unknown },
                FakeInstance(client) { Behaviour.Unknown },
            ) {
                val finder = warmFinder(client)
                val start = TimeSource.Monotonic.markNow()
                val exception = assertFailsWith<NatsRemoteException> {
                    withNatsCallTimeout(5.seconds) { finder.find(1) }
                }

                assertEquals("java.lang.IllegalStateException", exception.remoteType)
                assertEquals("boom", exception.remoteMessage)
                assertTrue(start.elapsedNow() < 2.seconds, "took ${start.elapsedNow()}")
            }
        }

    @Test
    fun `failure is reported after the timeout when an instance hangs`() = withClient { client ->
        withInstances(
            FakeInstance(client) { Behaviour.Fail("java.lang.IllegalStateException", "boom") },
            FakeInstance(client) { Behaviour.Hang },
        ) {
            val finder = warmFinder(client)
            assertFailsWith<NatsRemoteException> { withNatsCallTimeout(1.seconds) { finder.find(1) } }
        }
    }

    @Test
    fun `Known wins over a failure`() = withClient { client ->
        withInstances(
            FakeInstance(client) { Behaviour.Fail("java.lang.IllegalStateException", "boom") },
            FakeInstance(client) { Behaviour.Known("found") },
        ) {
            val finder = warmFinder(client)
            assertEquals(Answer.Known("found"), withNatsCallTimeout(5.seconds) { finder.find(1) })
        }
    }

    @Test
    fun `undecodable answer does not hide an Known from another instance`() = withClient { client ->
        for (garbage in listOf(Behaviour.Garbage(), Behaviour.Garbage(withContentType = false))) {
            withInstances(
                FakeInstance(client) { garbage },
                FakeInstance(client) { Behaviour.Known("found-$it") },
            ) {
                val finder = warmFinder(client)
                assertEquals(Answer.Known("found-5"), withNatsCallTimeout(5.seconds) { finder.find(5) }, "$garbage")
            }
        }
    }

    @Test
    fun `undecodable answer without any Known is reported instead of Unknown`() = withClient { client ->
        for (garbage in listOf(Behaviour.Garbage(), Behaviour.Garbage(withContentType = false))) {
            withInstances(
                FakeInstance(client) { garbage },
                FakeInstance(client) { Behaviour.Unknown },
                FakeInstance(client) { Behaviour.Unknown },
            ) {
                val finder = warmFinder(client)
                val start = TimeSource.Monotonic.markNow()
                val exception = assertFailsWith<Exception>("$garbage") {
                    withNatsCallTimeout(5.seconds) { finder.find(1) }
                }

                if (garbage.withContentType) {
                    assertIs<SerializationException>(exception)
                } else {
                    assertContains(exception.message.orEmpty(), "Content-Type")
                }
                assertTrue(start.elapsedNow() < 2.seconds, "$garbage took ${start.elapsedNow()}")
            }
        }
    }

    @Test
    fun `cold tracker does not end early`() = withClient { client ->
        withInstances(
            FakeInstance(client) { Behaviour.Unknown },
            FakeInstance(client) { Behaviour.Unknown },
        ) {
            val finder = client.service<FinderService>() // no warm-up
            val (answer, elapsed) = timed { withNatsCallTimeout(2.seconds) { finder.find(1) } }

            assertEquals(Answer.Unknown, answer)
            assertTrue(elapsed >= 1.5.seconds, "ended early after $elapsed")
        }
    }

    @Test
    fun `instance that left is no longer expected`() = withClient { client ->
        withInstances(
            FakeInstance(client) { Behaviour.Unknown },
            FakeInstance(client) { Behaviour.Unknown },
        ) {
            val leaving = FakeInstance(client) { Behaviour.Unknown }
            val finder = warmFinder(client)
            leaving.close()
            delay(200.milliseconds) // let the leave arrive

            val (answer, elapsed) = timed { withNatsCallTimeout(5.seconds) { finder.find(1) } }

            assertEquals(Answer.Unknown, answer)
            assertTrue(elapsed < 2.seconds, "took $elapsed")
        }
    }

    @Test
    fun `unannounced instance that answers is counted for later calls`() = withClient { client ->
        withInstances(FakeInstance(client) { if (it == 1L) Behaviour.Hang else Behaviour.Unknown }) {
            val hang = AtomicBoolean(false)
            FakeInstance(
                client,
                announce = false
            ) { if (hang.get()) Behaviour.Hang else Behaviour.Unknown }.use {
                val finder = warmFinder(client)
                assertEquals(Answer.Unknown, withNatsCallTimeout(1.seconds) { finder.find(1) })

                hang.set(true)
                val (answer, elapsed) = timed { withNatsCallTimeout(2.seconds) { finder.find(2) } }
                assertEquals(Answer.Unknown, answer)
                assertTrue(elapsed >= 1.5.seconds, "ended early after $elapsed")
            }
        }
    }

    @Test
    fun `no responders ends immediately with Unknown`() = withClient { client ->
        val finder = warmFinder(client)
        val (answer, elapsed) = timed { withNatsCallTimeout(5.seconds) { finder.find(1) } }

        assertEquals(Answer.Unknown, answer)
        assertTrue(elapsed < 2.seconds, "took $elapsed")
    }

    @Test
    fun `registered service answers its own client`() = withClient { client ->
        client.registerService<FinderService>(FinderImpl { Answer.Known("self-$it") })
        val finder = warmFinder(client)

        assertEquals(Answer.Known("self-3"), withNatsCallTimeout(5.seconds) { finder.find(3) })
    }

    @Test
    fun `registered service answering Unknown ends early through its own presence`() =
        withClient { client ->
            client.registerService<FinderService>(FinderImpl())
            val finder = warmFinder(client)

            val (answer, elapsed) = timed { withNatsCallTimeout(5.seconds) { finder.find(3) } }
            assertEquals(Answer.Unknown, answer)
            assertTrue(elapsed < 2.seconds, "took $elapsed")
        }

    @Test
    fun `unit method fans out to every instance without waiting`() = withClient { client ->
        val impl = FinderImpl()
        client.registerService<FinderService>(impl)
        val first = FakeInstance(client) { Behaviour.Unknown }
        val second = FakeInstance(client) { Behaviour.Unknown }

        withInstances(first, second) {
            val finder = client.service<FinderService>()
            val (_, elapsed) = timed { finder.broadcast("hello") }
            assertTrue(elapsed < 1.seconds, "took $elapsed")

            withTimeout(5.seconds) {
                assertEquals("hello", impl.broadcasts.receive())
                assertEquals("hello", first.broadcasts.receive())
                assertEquals("hello", second.broadcasts.receive())
            }
        }
    }

    @Test
    fun `parallel calls keep their answers apart`() = withClient { client ->
        withInstances(FakeInstance(client) { Behaviour.Known("answer-$it") }) {
            val finder = warmFinder(client)
            val answers = withNatsCallTimeout(5.seconds) {
                (1L..20L).map { id -> async { finder.find(id) } }.awaitAll()
            }

            assertEquals((1L..20L).map { Answer.Known("answer-$it") }, answers)
        }
    }

    @Test
    fun `registration announces join, answers queries and leaves on close`() =
        withClient { client ->
            val subject = client.serviceDescriptor(FinderService::class.java).presenceSubject!!
            val connection = NatsConnectionManager.connection()
            val presence = connection.subscribe(subject)
            connection.flush(Duration.ofSeconds(5))

            val registration = client.registerService<FinderService>(FinderImpl())
            assertEquals(NatsHeaders.PRESENCE_JOIN, presence.nextPresence())

            val query = NatsHeaders.base("external", WireFormat.CBOR)
                .add(NatsHeaders.PRESENCE, NatsHeaders.PRESENCE_QUERY)
            connection.publish(subject, query, ByteArray(0))
            assertEquals(
                NatsHeaders.PRESENCE_QUERY,
                presence.nextPresence()
            ) // own query echoed back
            assertEquals(NatsHeaders.PRESENCE_BEAT, presence.nextPresence())

            registration.close()
            assertEquals(NatsHeaders.PRESENCE_LEAVE, presence.nextPresence())
            presence.unsubscribe()
        }

    @Test
    fun `presence subject only exists for services with ALL_INSTANCES answers`() = withClient { client ->
        assertEquals(
            "${client.name}.\$presence.finder",
            client.serviceDescriptor(FinderService::class.java).presenceSubject
        )
        assertNull(client.serviceDescriptor(ServiceResponderTest.CalcService::class.java).presenceSubject)
        assertNull(
            client.serviceDescriptor(NotifierService::class.java).presenceSubject,
            "fan-out does not wait for answers and needs no presence"
        )
    }

    @Test
    fun `reconnect restarts the presence warm-up`() = withClient { client ->
        withInstances(
            FakeInstance(client) { Behaviour.Unknown },
            FakeInstance(client) { Behaviour.Unknown },
        ) {
            val finder = warmFinder(client)
            val connection = NatsConnectionManager.connection()
            val reconnects = connection.statistics.reconnects

            withContext(Dispatchers.IO) { TestNatsServer.restart() }
            eventually("reconnect after the server restart", timeout = 30.seconds) {
                connection.status == Connection.Status.CONNECTED && connection.statistics.reconnects > reconnects
            }
            withContext(Dispatchers.IO) { connection.flush(Duration.ofSeconds(5)) } // subscriptions are restored

            // Members can expire while the connection is down, the old list must not end the call early
            val (answer, elapsed) = timed { withNatsCallTimeout(2.seconds) { finder.find(1) } }
            assertEquals(Answer.Unknown, answer)
            assertTrue(elapsed >= 1.5.seconds, "ended early after $elapsed")

            // The members answered the new query, so calls end early again
            val (again, quick) = timed { withNatsCallTimeout(5.seconds) { finder.find(1) } }
            assertEquals(Answer.Unknown, again)
            assertTrue(quick < 2.seconds, "took $quick")
        }
    }

    private fun withClient(block: suspend CoroutineScope.(NatsClientImpl) -> Unit) = runTest {
        (NatsClient.create("rpc-all-${clientCounter++}") as NatsClientImpl).use { client ->
            withContext(Dispatchers.Default) { block(client) }
        }
    }

    private suspend fun withInstances(vararg instances: FakeInstance, block: suspend () -> Unit) {
        try {
            block()
        } finally {
            instances.forEach { it.close() }
        }
    }

    private suspend fun warmFinder(client: NatsClientImpl): FinderService {
        val finder = client.service<FinderService>()
        delay(RpcDefaults.PRESENCE_WARMUP + 300.milliseconds)
        return finder
    }

    private suspend fun <T> timed(block: suspend () -> T): Pair<T, kotlin.time.Duration> {
        val start = TimeSource.Monotonic.markNow()
        val result = block()
        return result to start.elapsedNow()
    }

    private fun io.nats.client.Subscription.nextPresence(): String? =
        nextMessage(Duration.ofSeconds(5))?.presenceKind()
}
