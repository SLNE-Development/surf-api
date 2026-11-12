package dev.slne.surf.nats.paper.types

import dev.slne.surf.nats.types.UuidSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.protobuf.ProtoNumber
import org.bukkit.Bukkit
import org.bukkit.Location
import java.util.*

/**
 * Serializes a Bukkit [Location] as the protobuf message `surf.types.paper.v1.Location`.
 *
 * The message has the following fields:
 * - `world` (1, optional): the world UID as `surf.types.v1.Uuid`, see [UuidSerializer]. Unset for a location
 *   without a world.
 * - `x` (2), `y` (3) and `z` (4), all required: 64-bit coordinates.
 * - `yaw` (5) and `pitch` (6), both required: 32-bit rotation in degrees.
 *
 * Coordinates and rotation are preserved exactly. Decoding looks the world up by UID with [Bukkit.getWorld], which
 * needs a running server when `world` is set. A world that is not loaded on the reader decodes as a location without
 * a world. Encoding a location whose world was unloaded throws [IllegalArgumentException] from [Location.getWorld].
 * In Paper projects the `dev.slne.surf.nats` Gradle plugin maps fields of this message type to [Location] with this
 * serializer.
 *
 * Decoding throws [kotlinx.serialization.MissingFieldException] when a coordinate or the rotation is absent.
 */
object LocationSerializer : KSerializer<Location> {
    override val descriptor: SerialDescriptor = LocationProto.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Location) {
        encoder.encodeSerializableValue(
            LocationProto.serializer(),
            LocationProto(value.world?.uid, value.x, value.y, value.z, value.yaw, value.pitch)
        )
    }

    override fun deserialize(decoder: Decoder): Location {
        val proto = decoder.decodeSerializableValue(LocationProto.serializer())
        val world = proto.world?.let { Bukkit.getWorld(it) }

        return Location(world, proto.x, proto.y, proto.z, proto.yaw, proto.pitch)
    }
}

@Serializable
@SerialName("surf.types.paper.v1.Location")
private class LocationProto(
    @ProtoNumber(1) val world: @Serializable(with = UuidSerializer::class) UUID?,
    @ProtoNumber(2) val x: Double,
    @ProtoNumber(3) val y: Double,
    @ProtoNumber(4) val z: Double,
    @ProtoNumber(5) val yaw: Float,
    @ProtoNumber(6) val pitch: Float,
)
