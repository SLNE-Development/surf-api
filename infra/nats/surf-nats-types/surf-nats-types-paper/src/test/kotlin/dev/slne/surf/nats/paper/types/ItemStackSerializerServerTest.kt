package dev.slne.surf.nats.paper.types

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.mockbukkit.mockbukkit.MockBukkit
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

// MockBukkit stands in for the server; its item byte format differs from Paper's, the adapter passes it through.
class ItemStackSerializerServerTest {

    @Serializable
    private data class Holder(
        @ProtoNumber(1) val single: @Serializable(ItemStackSerializer::class) ItemStack,
        @ProtoNumber(2) val list: List<@Serializable(ItemStackSerializer::class) ItemStack> = emptyList(),
        @ProtoNumber(3) val map: Map<String, @Serializable(ItemStackSerializer::class) ItemStack> = emptyMap(),
    )

    @BeforeTest
    fun setUp() {
        MockBukkit.mock()
    }

    @AfterTest
    fun tearDown() {
        MockBukkit.unmock()
    }

    @Test
    fun `top-level wire bytes carry serializeAsBytes`() {
        val item = ItemStack.of(Material.DIAMOND, 3)
        val hex = lengthDelimited(1, item.serializeAsBytes())

        assertEquals(hex, encodeHex(ItemStackSerializer, item))
        assertEquals(item, decodeHex(ItemStackSerializer, hex))
    }

    @Test
    fun `nested field, list elements and map values`() {
        val diamond = ItemStack.of(Material.DIAMOND, 3)
        val stone = ItemStack.of(Material.STONE)
        val value = Holder(single = diamond, list = listOf(stone, diamond), map = mapOf("k" to stone))
        val diamondMessage = lengthDelimited(1, diamond.serializeAsBytes()).hexToByteArray()
        val stoneMessage = lengthDelimited(1, stone.serializeAsBytes()).hexToByteArray()
        val entry = lengthDelimited(1, "k".encodeToByteArray()) + lengthDelimited(2, stoneMessage)
        val hex = lengthDelimited(1, diamondMessage) +
                lengthDelimited(2, stoneMessage) +
                lengthDelimited(2, diamondMessage) +
                lengthDelimited(3, entry.hexToByteArray())

        assertEquals(hex, encodeHex(Holder.serializer(), value))
        assertEquals(value, decodeHex(Holder.serializer(), hex))
    }
}
