package dev.slne.surf.redis.event

import dev.slne.surf.redis.codec.readString
import dev.slne.surf.redis.codec.readVarInt
import dev.slne.surf.redis.codec.writeString
import dev.slne.surf.redis.codec.writeVarInt
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNamingStrategy
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import org.redisson.client.codec.Codec
import org.redisson.client.codec.StringCodec
import java.util.*

/**
 * Compares the complete legacy JSON event wire format with the custom binary event packet.
 *
 * Every benchmark uses the same logical event and includes the transport envelope. Encode, decode,
 * and round-trip costs are separated so regressions are easier to attribute. Payloads contain only
 * ASCII characters so [payloadBytes] is also the UTF-8 payload size for both transports.
 */
open class EventTransportBenchmark {
    @Benchmark
    open fun jsonEncode(state: EventTransportBenchmarkState, blackhole: Blackhole): Int =
        state.encodeJson(blackhole)

    @Benchmark
    open fun binaryEncode(state: EventTransportBenchmarkState, blackhole: Blackhole): Int =
        state.encodeBinary(blackhole)

    @Benchmark
    open fun jsonDecode(state: EventTransportBenchmarkState): BenchmarkEvent = state.decodeJson()

    @Benchmark
    open fun binaryDecode(state: EventTransportBenchmarkState): BenchmarkEvent = state.decodeBinary()

    @Benchmark
    open fun jsonRoundTrip(state: EventTransportBenchmarkState): BenchmarkEvent =
        state.roundTripJson()

    @Benchmark
    open fun binaryRoundTrip(state: EventTransportBenchmarkState): BenchmarkEvent =
        state.roundTripBinary()
}

@State(Scope.Thread)
@OptIn(ExperimentalSerializationApi::class)
open class EventTransportBenchmarkState {
    @Param("0", "32", "256", "1024", "4096", "16384", "65536")
    @JvmField
    var payloadBytes: Int = 0

    private lateinit var event: BenchmarkEvent
    private lateinit var jsonPacket: ByteArray
    private lateinit var binaryPacket: ByteArray
    private lateinit var registration: EventCodecRegistration
    private lateinit var binaryCodec: Codec

    private val json = Json {
        namingStrategy = JsonNamingStrategy.SnakeCase
        encodeDefaults = true
    }
    private val eventSerializer: KSerializer<BenchmarkEvent> = BenchmarkEvent.serializer()
    private val envelopeSerializer: KSerializer<BenchmarkEventEnvelope> =
        BenchmarkEventEnvelope.serializer()

    val jsonWireBytes: Int
        get() = jsonPacket.size

    val binaryWireBytes: Int
        get() = binaryPacket.size

    @Setup(Level.Trial)
    fun setup() {
        val payload = buildString(payloadBytes) {
            repeat(payloadBytes) { append(('a'.code + it % 26).toChar()) }
        }
        event = BenchmarkEvent(
            aggregateId = 0x1020_3040_5060_7080L,
            sequence = 42,
            active = true,
            payload = payload,
        )

        @Suppress("UNCHECKED_CAST")
        val codec = BenchmarkEventCodec as RedisEventCodec<RedisEvent>
        registration = EventCodecRegistration(
            eventType = BenchmarkEvent::class.java,
            codec = codec,
            eventId = BenchmarkEventCodec.eventId,
            version = BenchmarkEventCodec.version,
            explicit = true,
        )
        binaryCodec = CustomEventPacketCodec.redisCodec { eventId ->
            registration.takeIf { it.eventId == eventId }
        }

        jsonPacket = encodeJsonWireBytes()
        binaryPacket = encodeBinaryWireBytes()

        check(decodeJson() == event) { "JSON benchmark fixture does not round-trip" }
        check(decodeBinary() == event) { "Binary benchmark fixture does not round-trip" }
    }

    fun encodeJson(blackhole: Blackhole): Int {
        val wire = StringCodec.INSTANCE.valueEncoder.encode(encodeJsonMessage())
        try {
            blackhole.consume(wire)
            return wire.readableBytes()
        } finally {
            wire.release()
        }
    }

    fun encodeBinary(blackhole: Blackhole): Int {
        val wire = binaryCodec.valueEncoder.encode(
            CustomEventPacketCodec.outbound(event, registration)
        )
        try {
            blackhole.consume(wire)
            return wire.readableBytes()
        } finally {
            wire.release()
        }
    }

    fun decodeJson(): BenchmarkEvent {
        val wire = Unpooled.wrappedBuffer(jsonPacket)
        val message = try {
            StringCodec.INSTANCE.valueDecoder.decode(wire, null) as String
        } finally {
            wire.release()
        }
        return decodeJsonMessage(message)
    }

    fun decodeBinary(): BenchmarkEvent {
        val wire = Unpooled.wrappedBuffer(binaryPacket)
        val result = try {
            binaryCodec.valueDecoder.decode(wire, null) as CustomEventPacketCodec.DecodeResult
        } finally {
            wire.release()
        }
        return decodedEvent(result)
    }

    fun roundTripJson(): BenchmarkEvent {
        val wire = StringCodec.INSTANCE.valueEncoder.encode(encodeJsonMessage())
        val message = try {
            StringCodec.INSTANCE.valueDecoder.decode(wire, null) as String
        } finally {
            wire.release()
        }
        return decodeJsonMessage(message)
    }

    fun roundTripBinary(): BenchmarkEvent {
        val wire = binaryCodec.valueEncoder.encode(
            CustomEventPacketCodec.outbound(event, registration)
        )
        val result = try {
            binaryCodec.valueDecoder.decode(wire, null) as CustomEventPacketCodec.DecodeResult
        } finally {
            wire.release()
        }
        return decodedEvent(result)
    }

    private fun encodeJsonWireBytes(): ByteArray {
        val wire = StringCodec.INSTANCE.valueEncoder.encode(encodeJsonMessage())
        try {
            return ByteBufUtil.getBytes(wire, wire.readerIndex(), wire.readableBytes(), false)
        } finally {
            wire.release()
        }
    }

    private fun encodeJsonMessage(): String {
        val eventData = json.encodeToJsonElement(eventSerializer, event)
        return json.encodeToString(
            envelopeSerializer,
            BenchmarkEventEnvelope(BenchmarkEvent::class.java.name, eventData),
        )
    }

    private fun decodeJsonMessage(message: String): BenchmarkEvent {
        val envelope = json.decodeFromString(envelopeSerializer, message)
        return json.decodeFromJsonElement(eventSerializer, envelope.eventData)
    }

    private fun encodeBinaryWireBytes(): ByteArray {
        val wire = binaryCodec.valueEncoder.encode(
            CustomEventPacketCodec.outbound(event, registration)
        )
        try {
            return ByteBufUtil.getBytes(wire, wire.readerIndex(), wire.readableBytes(), false)
        } finally {
            wire.release()
        }
    }

    private fun decodedEvent(result: CustomEventPacketCodec.DecodeResult): BenchmarkEvent {
        return (result as CustomEventPacketCodec.DecodeResult.Event).event as BenchmarkEvent
    }
}

/** Exercises the adaptive capacity estimate with payload sizes that change between operations. */
open class VariableEventPacketBenchmark {
    @Benchmark
    open fun binaryVariableEncode(
        state: VariableEventPacketBenchmarkState,
        blackhole: Blackhole
    ): Int = state.encodeNext(blackhole)
}

@State(Scope.Thread)
open class VariableEventPacketBenchmarkState {
    @Param("alternating", "bursty", "mixed")
    @JvmField
    var payloadPattern: String = "alternating"

    private lateinit var events: Array<BenchmarkEvent>
    private lateinit var registration: EventCodecRegistration
    private lateinit var binaryCodec: Codec
    private var index = 0

    @Setup(Level.Trial)
    fun setup() {
        val payloadSizes = when (payloadPattern) {
            "alternating" -> IntArray(256) { if (it and 1 == 0) 256 else 65_536 }
            "bursty" -> IntArray(256) { if (it % 32 == 31) 65_536 else 1_024 }
            "mixed" -> {
                val sizes = intArrayOf(0, 32, 256, 1_024, 4_096, 16_384, 65_536)
                IntArray(256) { sizes[(it * 73 + 19) % sizes.size] }
            }

            else -> error("Unknown payload pattern: $payloadPattern")
        }
        events = Array(payloadSizes.size) { eventIndex ->
            BenchmarkEvent(
                aggregateId = eventIndex.toLong(),
                sequence = eventIndex,
                active = true,
                payload = benchmarkPayload(payloadSizes[eventIndex]),
            )
        }

        @Suppress("UNCHECKED_CAST")
        val codec = BenchmarkEventCodec as RedisEventCodec<RedisEvent>
        registration = EventCodecRegistration(
            eventType = BenchmarkEvent::class.java,
            codec = codec,
            eventId = BenchmarkEventCodec.eventId,
            version = BenchmarkEventCodec.version,
            explicit = true,
        )
        binaryCodec = CustomEventPacketCodec.redisCodec { eventId ->
            registration.takeIf { it.eventId == eventId }
        }
    }

    fun encodeNext(blackhole: Blackhole): Int {
        val event = events[index]
        index = (index + 1) and (events.size - 1)
        val wire = binaryCodec.valueEncoder.encode(
            CustomEventPacketCodec.outbound(event, registration)
        )
        try {
            blackhole.consume(wire)
            return wire.readableBytes()
        } finally {
            wire.release()
        }
    }
}

@Serializable
data class BenchmarkEvent(
    val aggregateId: Long,
    val sequence: Int,
    val active: Boolean,
    val payload: String,
) : RedisEvent()

@Serializable
private data class BenchmarkEventEnvelope(
    val eventClass: String,
    val eventData: JsonElement,
)

private object BenchmarkEventCodec : RedisEventCodec<BenchmarkEvent> {
    private const val MAX_PAYLOAD_BYTES = 1024 * 1024

    override val eventId: String = "benchmark-event"
    override val codecId: String = "benchmark-event-binary-v1"

    override fun encode(buffer: ByteBuf, value: BenchmarkEvent) {
        buffer.writeLong(value.aggregateId)
        buffer.writeVarInt(value.sequence)
        buffer.writeBoolean(value.active)
        buffer.writeString(value.payload, MAX_PAYLOAD_BYTES)
    }

    override fun decode(buffer: ByteBuf) = BenchmarkEvent(
        aggregateId = buffer.readLong(),
        sequence = buffer.readVarInt(),
        active = buffer.readBoolean(),
        payload = buffer.readString(MAX_PAYLOAD_BYTES),
    )
}

private fun benchmarkPayload(size: Int) = buildString(size) {
    repeat(size) { append(('a'.code + it % 26).toChar()) }
}

/** Prints exact wire sizes without mixing size calculation into timed JMH operations. */
object EventWireSizeReport {
    @JvmStatic
    fun main(args: Array<String>) {
        println("payload_bytes\tjson_wire_bytes\tbinary_wire_bytes\tjson_over_binary")
        for (payloadBytes in PAYLOAD_SIZES) {
            val state = EventTransportBenchmarkState().apply {
                this.payloadBytes = payloadBytes
                setup()
            }
            println(
                "$payloadBytes\t${state.jsonWireBytes}\t${state.binaryWireBytes}\t" +
                        "%.3f".format(
                            Locale.ROOT,
                            state.jsonWireBytes.toDouble() / state.binaryWireBytes,
                        ),
            )
        }
    }

    private val PAYLOAD_SIZES = intArrayOf(0, 32, 256, 1024, 4096, 16384, 65536)
}
