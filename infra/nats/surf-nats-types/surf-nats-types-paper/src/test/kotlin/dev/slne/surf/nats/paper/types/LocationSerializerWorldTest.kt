package dev.slne.surf.nats.paper.types

import org.bukkit.Location
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import java.nio.ByteBuffer
import java.util.*
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class LocationSerializerWorldTest {

    private lateinit var server: ServerMock

    @BeforeTest
    fun setUp() {
        server = MockBukkit.mock()
    }

    @AfterTest
    fun tearDown() {
        MockBukkit.unmock()
    }

    @Test
    fun `world is written as its uid and resolved on decode`() {
        val world = server.addSimpleWorld("world")
        val location = Location(world, 1.0, 2.0, 3.0, 4f, 5f)
        val hex = lengthDelimited(1, lengthDelimited(1, uuidBytes(world.uid)).hexToByteArray()) +
                "11000000000000f03f190000000000000040210000000000000840" + "2d00008040350000a040"

        assertEquals(hex, encodeHex(LocationSerializer, location))

        val decoded = decodeHex(LocationSerializer, hex)
        assertSame(world, decoded.world)
        assertEquals(location, decoded)
    }

    @Test
    fun `unknown world decodes to a location without a world`() {
        val unknown = UUID.fromString("00112233-4455-6677-8899-aabbccddeeff")
        val hex = "0a120a1000112233445566778899aabbccddeeff" +
                "11000000000000f03f190000000000000040210000000000000840" + "2d00008040350000a040"

        val decoded = decodeHex(LocationSerializer, hex)
        assertNull(decoded.world)
        assertEquals(Location(null, 1.0, 2.0, 3.0, 4f, 5f), decoded)
    }

    private fun uuidBytes(uuid: UUID): ByteArray =
        ByteBuffer.allocate(16).putLong(uuid.mostSignificantBits).putLong(uuid.leastSignificantBits).array()
}
