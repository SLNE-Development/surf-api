package dev.slne.surf.nats.paper.types

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import java.nio.ByteBuffer
import java.util.*
import kotlin.test.*

// Bukkit.getWorld is the only server call of the serializer, so mocking it replaces a running
// server.
class LocationSerializerWorldTest {

    @BeforeTest
    fun setUp() {
        mockkStatic(Bukkit::class)
    }

    @AfterTest
    fun tearDown() {
        unmockkStatic(Bukkit::class)
    }

    @Test
    fun `world is written as its uid and resolved on decode`() {
        val worldUid = UUID.fromString("00112233-4455-6677-8899-aabbccddeeff")
        val world = mockk<World> { every { uid } returns worldUid }
        every { Bukkit.getWorld(worldUid) } returns world
        val location = Location(world, 1.0, 2.0, 3.0, 4f, 5f)
        val hex = lengthDelimited(1, lengthDelimited(1, uuidBytes(worldUid)).hexToByteArray()) +
            "11000000000000f03f190000000000000040210000000000000840" + "2d00008040350000a040"

        assertEquals(hex, encodeHex(LocationSerializer, location))

        val decoded = decodeHex(LocationSerializer, hex)
        assertSame(world, decoded.world)
        assertEquals(location, decoded)
    }

    @Test
    fun `unknown world decodes to a location without a world`() {
        val unknown = UUID.fromString("00112233-4455-6677-8899-aabbccddeeff")
        every { Bukkit.getWorld(unknown) } returns null
        val hex = "0a120a1000112233445566778899aabbccddeeff" +
            "11000000000000f03f190000000000000040210000000000000840" + "2d00008040350000a040"

        val decoded = decodeHex(LocationSerializer, hex)
        assertNull(decoded.world)
        assertEquals(Location(null, 1.0, 2.0, 3.0, 4f, 5f), decoded)
    }

    private fun uuidBytes(uuid: UUID): ByteArray {
        return ByteBuffer.allocate(16)
            .putLong(uuid.mostSignificantBits)
            .putLong(uuid.leastSignificantBits)
            .array()
    }
}
