package dev.slne.surf.nats.paper.types

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.inventory.ItemStack
import java.util.*
import kotlin.test.*
import dev.slne.surf.nats.paper.types.legacy.ItemStackSerializer as LegacyItemStackSerializer
import dev.slne.surf.nats.paper.types.legacy.LocationSerializer as LegacyLocationSerializer

/**
 * Pins the wire format of the Paper serializers to the implementation they replaced.
 *
 * The `legacy` package holds verbatim copies of the serializers before they were rebuilt on top of
 * the surf-api serializers. [Bukkit.getWorld] and the item codec are mocked in place of a server.
 */
class LegacyWireCompatibilityTest {

    private val worldUid = UUID.fromString("00112233-4455-6677-8899-aabbccddeeff")
    private val world = mockk<World> { every { uid } returns worldUid }

    @BeforeTest
    fun setUp() {
        mockkStatic(Bukkit::class, ItemStack::class)
        every { Bukkit.getWorld(worldUid) } returns world
    }

    @AfterTest
    fun tearDown() {
        unmockkStatic(Bukkit::class, ItemStack::class)
    }

    @Test
    fun location() {
        val locations = listOf(
            Location(null, 0.0, 0.0, 0.0, 0f, 0f),
            Location(null, -1.5, 64.0, 0.25, 90f, -45f),
            Location(world, 1.0, 2.0, 3.0, 4f, 5f),
        )

        for (location in locations) {
            val legacyHex = encodeHex(LegacyLocationSerializer, location)

            assertEquals(legacyHex, encodeHex(LocationSerializer, location))
            assertEquals(location, decodeHex(LocationSerializer, legacyHex))
        }
    }

    @Test
    fun itemStack() {
        val data = byteArrayOf(10, 0, 0, 3, 0, 4, 1, 2, 3, 4, 0)
        val item = mockk<ItemStack> { every { serializeAsBytes() } returns data }
        val decoded = mockk<ItemStack>()
        every { ItemStack.deserializeBytes(any()) } answers {
            assertContentEquals(data, firstArg<ByteArray>())
            decoded
        }

        val legacyHex = encodeHex(LegacyItemStackSerializer, item)

        assertEquals(legacyHex, encodeHex(ItemStackSerializer, item))
        assertEquals(decoded, decodeHex(ItemStackSerializer, legacyHex))
    }
}
