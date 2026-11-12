package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.LimitedService
import dev.slne.surf.nats.NatsClient
import dev.slne.surf.nats.NatsInstance
import dev.slne.surf.nats.registerService
import dev.slne.surf.nats.service
import dev.slne.surf.nats.test.proto.v1.Text
import kotlinx.coroutines.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

// LimitedService runs one handler at a time (test config.yml)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HandlerLimitTest {

    private var clientCounter = 0

    @BeforeAll
    fun connect() = runBlocking { NatsInstance.instance.load() }

    @AfterAll
    fun disconnect() = runBlocking { NatsInstance.instance.disable() }

    private class LimitedImpl : LimitedService {
        val holding = CompletableDeferred<Unit>()
        val gate = CompletableDeferred<Unit>()
        val ran = CopyOnWriteArrayList<String>()

        override suspend fun hold(): Text {
            holding.complete(Unit)
            gate.await()
            return Text("hold")
        }

        override suspend fun early(): Text = record("early")

        override suspend fun late(): Text = record("late")

        private fun record(name: String): Text {
            ran += name
            return Text(name)
        }
    }

    @Test
    fun `waiting calls run earliest deadline first`() = withLimitedService { impl, service ->
        val held = async { withNatsCallTimeout(10.seconds) { service.hold() } }
        withTimeout(5.seconds) { impl.holding.await() }

        val late = async { withNatsCallTimeout(8.seconds) { service.late() } }
        delay(200.milliseconds)
        val early = async { withNatsCallTimeout(4.seconds) { service.early() } }
        delay(200.milliseconds)
        assertTrue(impl.ran.isEmpty(), "a call ran while the only handler slot was taken: ${impl.ran}")

        impl.gate.complete(Unit)
        awaitAll(held, late, early)
        assertEquals(listOf("early", "late"), impl.ran)
    }

    @Test
    fun `call whose deadline passes while waiting does not run`() = withLimitedService { impl, service ->
        val held = async { withNatsCallTimeout(10.seconds) { service.hold() } }
        withTimeout(5.seconds) { impl.holding.await() }

        assertFailsWith<NatsCallTimeoutException> { withNatsCallTimeout(300.milliseconds) { service.early() } }

        impl.gate.complete(Unit)
        held.await()
        assertEquals(Text("late"), withNatsCallTimeout(2.seconds) { service.late() }, "the slot was not freed")
        assertEquals(listOf("late"), impl.ran)
    }

    private fun withLimitedService(
        block: suspend CoroutineScope.(LimitedImpl, LimitedService) -> Unit,
    ) = runBlocking(Dispatchers.Default) {
        NatsClient.create("handler-limit-${clientCounter++}").use { client ->
            val impl = LimitedImpl()
            client.registerService<LimitedService>(impl)
            coroutineScope { block(impl, client.service()) }
        }
    }
}
