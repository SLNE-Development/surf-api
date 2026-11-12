package dev.slne.surf.nats.paper.types

import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.protobuf.ProtoNumber
import org.bukkit.Location
import kotlin.test.Test
import kotlin.test.assertEquals

// Runs without a server: a location without a world never reaches Bukkit.getWorld.
class LocationSerializerTest {

    @Serializable
    private data class Holder(
        @ProtoNumber(1) val single: @Serializable(LocationSerializer::class) Location,
        @ProtoNumber(2) val list: List<@Serializable(LocationSerializer::class) Location> = emptyList(),
        @ProtoNumber(3) val map: Map<String, @Serializable(LocationSerializer::class) Location> = emptyMap(),
    )

    private val location = Location(null, 1.0, 2.0, 3.0, 4f, 5f)
    private val locationHex = "11000000000000f03f" + "190000000000000040" + "210000000000000840" +
            "2d00008040" + "350000a040"

    @Test
    fun `descriptor is the surf Location message`() {
        val descriptor = LocationSerializer.descriptor

        assertEquals("surf.types.paper.v1.Location", descriptor.serialName)
        assertEquals(StructureKind.CLASS, descriptor.kind)
        assertEquals(listOf("world", "x", "y", "z", "yaw", "pitch"), descriptor.elementNames.toList())
        assertEquals(
            listOf(1, 2, 3, 4, 5, 6),
            (0 until descriptor.elementsCount).map { index ->
                descriptor.getElementAnnotations(index).filterIsInstance<ProtoNumber>().single().number
            }
        )
    }

    @Test
    fun `top-level wire bytes without a world`() {
        assertEquals(locationHex, encodeHex(LocationSerializer, location))
        assertEquals(location, decodeHex(LocationSerializer, locationHex))
    }

    @Test
    fun `zero coordinates are written`() {
        val zero = Location(null, 0.0, 0.0, 0.0, 0f, 0f)
        val hex = "110000000000000000" + "190000000000000000" + "210000000000000000" +
                "2d00000000" + "3500000000"

        assertEquals(hex, encodeHex(LocationSerializer, zero))
        assertEquals(zero, decodeHex(LocationSerializer, hex))
    }

    @Test
    fun `nested field, list elements and map values`() {
        val zero = Location(null, 0.0, 0.0, 0.0, 0f, 0f)
        val other = Location(null, -1.5, 64.0, 0.25, 90f, -45f)
        val value = Holder(single = location, list = listOf(zero, other), map = mapOf("spawn" to location))
        val hex = "0a25" + locationHex +
                "1225110000000000000000190000000000000000210000000000000000" + "2d000000003500000000" +
                "122511000000000000f8bf19000000000000504021000000000000d03f" + "2d0000b44235000034c2" +
                "1a2e0a05737061776e1225" + locationHex

        assertEquals(hex, encodeHex(Holder.serializer(), value))
        assertEquals(value, decodeHex(Holder.serializer(), hex))
    }

    @Test
    fun `absent coordinates are missing fields`() {
        val withoutPitch = "11000000000000f03f190000000000000040210000000000000840" + "2d00008040"

        assertMissingField("surf.types.paper.v1.Location", "pitch") {
            decodeHex(LocationSerializer, withoutPitch)
        }
        assertMissingField("surf.types.paper.v1.Location", "x", "y", "z", "yaw", "pitch") {
            decodeHex(LocationSerializer, "")
        }
        assertMissingField("surf.types.paper.v1.Location", "x", "y", "z", "yaw", "pitch") {
            decodeHex(Holder.serializer(), "0a00")
        }
    }
}
