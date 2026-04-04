@file:OptIn(ExperimentalSerializationApi::class)

package dev.slne.surf.api.paper.serializer.bukkit.location

import dev.slne.surf.api.core.serializer.java.uuid.JavaUUIDSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.*
import org.bukkit.Bukkit
import org.bukkit.Location
import java.util.*

typealias SerializableLocation = @Serializable(with = LocationSerializer::class) Location

object LocationSerializer : KSerializer<Location> {
    override val descriptor = buildClassSerialDescriptor("surfapi.bukkit.Location") {
        element("worldUuid", JavaUUIDSerializer.descriptor, isOptional = true)
        element<Double>("x")
        element<Double>("y")
        element<Double>("z")
        element<Float>("yaw")
        element<Float>("pitch")
    }

    override fun serialize(
        encoder: Encoder,
        value: Location,
    ) = encoder.encodeStructure(descriptor) {
        encodeNullableSerializableElement(descriptor, 0, JavaUUIDSerializer, value.world?.uid)
        encodeDoubleElement(descriptor, 1, value.x)
        encodeDoubleElement(descriptor, 2, value.y)
        encodeDoubleElement(descriptor, 3, value.z)
        encodeFloatElement(descriptor, 4, value.yaw)
        encodeFloatElement(descriptor, 5, value.pitch)
    }

    override fun deserialize(decoder: Decoder) = decoder.decodeStructure(descriptor) {
        var worldUuid: UUID? = null
        var x = 0.0
        var y = 0.0
        var z = 0.0
        var yaw = 0f
        var pitch = 0f

        if (decodeSequentially()) {
            worldUuid = decodeNullableSerializableElement(descriptor, 0, JavaUUIDSerializer)
            x = decodeDoubleElement(descriptor, 1)
            y = decodeDoubleElement(descriptor, 2)
            z = decodeDoubleElement(descriptor, 3)
            yaw = decodeFloatElement(descriptor, 4)
            pitch = decodeFloatElement(descriptor, 5)
        } else while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                0 -> worldUuid =
                    decodeNullableSerializableElement(descriptor, 0, JavaUUIDSerializer)

                1 -> x = decodeDoubleElement(descriptor, 1)
                2 -> y = decodeDoubleElement(descriptor, 2)
                3 -> z = decodeDoubleElement(descriptor, 3)
                4 -> yaw = decodeFloatElement(descriptor, 4)
                5 -> pitch = decodeFloatElement(descriptor, 5)
                CompositeDecoder.DECODE_DONE -> break
                else -> error("Unexpected index: $index")
            }
        }

        val world = worldUuid?.let { Bukkit.getWorld(it) }

        Location(world, x, y, z, yaw, pitch)
    }
}