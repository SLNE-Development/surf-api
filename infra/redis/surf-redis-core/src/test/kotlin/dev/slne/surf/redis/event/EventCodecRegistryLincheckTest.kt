package dev.slne.surf.redis.event

import io.netty.buffer.ByteBuf
import org.jetbrains.lincheck.datastructures.ModelCheckingOptions
import org.jetbrains.lincheck.datastructures.Operation
import org.jetbrains.lincheck.datastructures.forClasses
import org.junit.jupiter.api.Tag
import kotlin.test.Test

/**
 * Verifies that codec registration, freezing, and lock-free frozen lookups are linearizable.
 *
 * This intentionally targets the registry lifecycle rather than codec implementations: codecs are
 * consumer-provided and must supply their own thread-safety guarantees.
 */
@Tag("lincheck")
class EventCodecRegistryLincheckTest {
    private val registry = EventCodecRegistry()

    @Operation
    fun registerAlpha(): String = outcome {
        registry.registerExplicit(AlphaEvent::class.java, AlphaCodec)
        "registered"
    }

    @Operation
    fun registerBeta(): String = outcome {
        registry.registerExplicit(BetaEvent::class.java, BetaCodec)
        "registered"
    }

    @Operation
    fun freeze(): String = outcome {
        registry.freeze()
        "frozen"
    }

    @Operation
    fun lookupAlphaByClass(): String? =
        registry.codecForPublishing(AlphaEvent::class.java)?.eventId

    @Operation
    fun lookupBetaByClass(): String? =
        registry.codecForPublishing(BetaEvent::class.java)?.eventId

    @Operation
    fun lookupSharedId(): String? =
        registry.codecForEventId(SHARED_EVENT_ID)?.eventType?.simpleName

    @Test
    fun modelChecking() {
        ModelCheckingOptions()
            .iterations(40)
            .threads(3)
            .actorsPerThread(3)
            .addGuarantee(
                forClasses { className -> className.startsWith("it.unimi.dsi.fastutil.") }
                    .allMethods()
                    .treatAsAtomic(),
            )
            .check(this::class)
    }

    private inline fun outcome(block: () -> String): String = try {
        block()
    } catch (exception: IllegalArgumentException) {
        "rejected:${exception::class.simpleName}"
    } catch (exception: IllegalStateException) {
        "rejected:${exception::class.simpleName}"
    }

    private class AlphaEvent(val value: Int) : RedisEvent()
    private class BetaEvent(val value: Int) : RedisEvent()

    private object AlphaCodec : RedisEventCodec<AlphaEvent> {
        override val eventId: String = SHARED_EVENT_ID

        override fun encode(buffer: ByteBuf, value: AlphaEvent) {
            buffer.writeInt(value.value)
        }

        override fun decode(buffer: ByteBuf) = AlphaEvent(buffer.readInt())
    }

    private object BetaCodec : RedisEventCodec<BetaEvent> {
        override val eventId: String = SHARED_EVENT_ID

        override fun encode(buffer: ByteBuf, value: BetaEvent) {
            buffer.writeInt(value.value)
        }

        override fun decode(buffer: ByteBuf) = BetaEvent(buffer.readInt())
    }

    private companion object {
        const val SHARED_EVENT_ID = "lincheck-shared-event"
    }
}
