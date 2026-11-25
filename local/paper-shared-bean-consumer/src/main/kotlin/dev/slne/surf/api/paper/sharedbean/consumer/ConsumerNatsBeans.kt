package dev.slne.surf.api.paper.sharedbean.consumer

import dev.jorel.commandapi.CommandAPICommand
import dev.slne.surf.api.common.util.logger
import dev.slne.surf.api.paper.command.executors.anyExecutorSuspend
import dev.slne.surf.nats.NatsClient
import dev.slne.surf.nats.NatsService
import dev.slne.surf.nats.event.NatsListener
import dev.slne.surf.nats.paper.types.ItemStackSerializer
import dev.slne.surf.nats.service
import dev.slne.surf.nats.test.proto.v1.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.protobuf.ProtoBuf
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration.Companion.seconds

/**
 * Serves [CalcService] over NATS. Only [mixed] is called; the remaining methods are not part of
 * the probe.
 */
@NatsService
class ConsumerCalcService : CalcService {

    override suspend fun mixed(request: MixedRequest): Text {
        return Text("${request.a + request.b}:${request.label}")
    }

    override suspend fun health(): HealthReply = unused()
    override suspend fun fire(request: FireRequest): Nothing = unused()
    override suspend fun fail(request: FailRequest): Text = unused()
    override suspend fun innerTimeout(): Text = unused()
    override suspend fun count(): Count = unused()
    override suspend fun slowFire(): Nothing = unused()
    override suspend fun gated(): Text = unused()
    override suspend fun unsigned(request: UnsignedRequest): UnsignedReply = unused()
    override suspend fun inline(request: InlineRequest): Text = unused()
    override suspend fun slow(): Text = unused()
    override suspend fun missing(): Text = unused()

    private fun unused(): Nothing {
        throw UnsupportedOperationException("Not part of the NATS probe")
    }
}

/**
 * Proves that the NATS integration bundled into surf-api serves a plugin on a real server, over a
 * real broker, and that the Paper serializers of surf-nats read what they write on that server.
 *
 * The plugin serves [CalcService] through [ConsumerCalcService] and listens for [Created]. surf-api
 * registers both after the enable hooks, so [roundTrip] runs once the server is up.
 */
@Service
class ConsumerNatsProbe(private val client: NatsClient) {

    private val log = logger()

    private val nextEventId = AtomicLong(System.nanoTime())

    @Volatile
    private var awaitedEvent = CompletableDeferred<Long>()

    @NatsListener
    suspend fun onCreated(event: Created) {
        awaitedEvent.complete(event.id)
    }

    /**
     * Calls [CalcService] through a service proxy and publishes an event to this class's own
     * listener.
     *
     * A service proxy obtained from the client reaches the service through the broker. The injected
     * [CalcService] would be [ConsumerCalcService] itself, because a local implementation wins.
     *
     * @return `ok` for each round trip that came back as expected, otherwise the failure
     */
    suspend fun roundTrip(): Result {
        val remote = client.service<CalcService>()

        return Result(
            rpc = check("rpc") {
                remote.mixed(
                    MixedRequest(
                        1,
                        2L,
                        0.5,
                        true,
                        "probe",
                    ),
                ).value == "3:probe"
            },
            event = check("event") { publishAndAwaitEvent() },
            itemStack = check("itemStack") { roundTripItemStack() },
        )
    }

    /**
     * Writes an item stack with [ItemStackSerializer] and reads it back, which needs the server's
     * own item format.
     */
    private fun roundTripItemStack(): Boolean {
        val item = ItemStack.of(Material.DIAMOND, 3)
        val bytes = ProtoBuf.encodeToByteArray(ItemStackSerializer, item)

        return ProtoBuf.decodeFromByteArray(ItemStackSerializer, bytes) == item
    }

    private suspend fun publishAndAwaitEvent(): Boolean {
        val id = nextEventId.incrementAndGet()
        awaitedEvent = CompletableDeferred()
        client.publish(Created(id))

        return withTimeout(EVENT_TIMEOUT) { awaitedEvent.await() } == id
    }

    private suspend fun check(name: String, probe: suspend () -> Boolean): String {
        return try {
            if (probe()) "ok" else "mismatch"
        } catch (failure: Exception) {
            log.atWarning()
                .withCause(failure)
                .log("The nats probe '%s' failed", name)
            "failed:${failure.javaClass.simpleName}"
        }
    }

    /**
     * The outcome of [roundTrip]: one entry per round trip.
     */
    data class Result(val rpc: String, val event: String, val itemStack: String)

    private companion object {
        val EVENT_TIMEOUT = 10.seconds
    }
}

/**
 * A command bean whose console run executes the NATS round trip. The boot test runs it once the
 * server is up, because the NATS services register only after the enable phase.
 */
@Service
class SpikeNatsCommand(private val probe: ConsumerNatsProbe) :
    CommandAPICommand(SharedBeanReport.NATS_COMMAND) {

    private val log = logger()

    init {
        anyExecutorSuspend { _, _ ->
            val result = probe.roundTrip()
            log.atInfo()
                .log(
                    "%s nats=%s natsEvent=%s natsItemStack=%s",
                    SharedBeanReport.SPIKE_MARKER,
                    result.rpc,
                    result.event,
                    result.itemStack,
                )
        }
    }
}
