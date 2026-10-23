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
