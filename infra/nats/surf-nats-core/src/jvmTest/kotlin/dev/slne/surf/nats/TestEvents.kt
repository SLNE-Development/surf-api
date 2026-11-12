@file:OptIn(ExperimentalSerializationApi::class)

package dev.slne.surf.nats

import dev.slne.surf.nats.descriptor.NatsEvent
import dev.slne.surf.nats.descriptor.NatsEventDescriptor
import dev.slne.surf.nats.test.fixtures.Tag
import dev.slne.surf.nats.test.proto.v1.Color
import dev.slne.surf.nats.test.proto.v1.Created
import dev.slne.surf.nats.test.proto.v1.Everything
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import java.time.Instant
import java.util.*
import kotlin.time.Duration.Companion.milliseconds

val TEST_UUID: UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")

/**
 * An [Everything] with a non-default value in every field.
 */
fun everything() = Everything(
    small = -7,
    large = Long.MIN_VALUE,
    unsignedSmall = -1,
    unsignedLarge = -1L,
    signedLarge = -123_456_789L,
    fixedSmall = 42,
    fixedLarge = 1L shl 40,
    signedFixedSmall = -42,
    signedFixedLarge = -(1L shl 40),
    flag = true,
    ratio = 0.25,
    scale = 1.5f,
    label = "surf",
    payload = byteArrayOf(1, 2, 3),
    names = listOf("a", "b"),
    counts = mapOf("a" to 1, "b" to 2),
    nested = Everything.Nested("nested", listOf(1, 2, 3)),
    color = Color.GREEN,
    choice = Everything.Choice.Number(9),
    createdAt = Instant.ofEpochSecond(1_700_000_000, 123),
    duration = (-1_500).milliseconds,
    uuid = TEST_UUID,
    component = Component.text("hello"),
    key = Key.key("surf", "nats"),
    nullableLabel = "maybe",
    defaulted = 8,
    tag = Tag("tag"),
    nullableTag = Tag("nullable-tag"),
    nestedList = listOf(Everything.Nested("first"), Everything.Nested("second", listOf(4))),
    nestedById = mapOf(5L to Everything.Nested("by-id", listOf(5))),
)

// Event classes with hand-written descriptors. They stand in for contracts of other modules, which one generator run
// never sees together, and for classes that are not generated events.

/**
 * Uses the proto name, and therefore the subject, of the generated [Created] with another type.
 */
@Serializable
@SerialName("surf.nats.test.v1.CreatedImpostor")
data class CreatedImpostor(@ProtoNumber(1) val id: Long) : NatsEvent {
    override val natsDescriptor: NatsEventDescriptor<*> get() = NATS_DESCRIPTOR

    companion object {
        @JvmField
        val NATS_DESCRIPTOR = NatsEventDescriptor<CreatedImpostor>("surf.nats.test.v1.Created", serializer())
    }
}

/**
 * Declares a proto name that is no proto full name.
 */
@Serializable
@SerialName("surf.nats.test.v1.InvalidProtoNameEvent")
data class InvalidProtoNameEvent(@ProtoNumber(1) val id: Long) : NatsEvent {
    override val natsDescriptor: NatsEventDescriptor<*> get() = NATS_DESCRIPTOR

    companion object {
        @JvmField
        val NATS_DESCRIPTOR = NatsEventDescriptor<InvalidProtoNameEvent>("surf.nats.test.v1.Invalid Name", serializer())
    }
}

open class DescriptorHolder {
    companion object {
        @JvmField
        val NATS_DESCRIPTOR = Created.NATS_DESCRIPTOR
    }
}

/**
 * Inherits a `NATS_DESCRIPTOR` field instead of declaring one.
 */
class InheritedDescriptorEvent : DescriptorHolder(), NatsEvent {
    override val natsDescriptor: NatsEventDescriptor<*> get() = Created.NATS_DESCRIPTOR
}
