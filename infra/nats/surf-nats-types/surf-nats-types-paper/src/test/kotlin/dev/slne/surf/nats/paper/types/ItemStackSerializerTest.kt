package dev.slne.surf.nats.paper.types

import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.protobuf.ProtoNumber
import org.bukkit.inventory.ItemStack
import kotlin.test.Test
import kotlin.test.assertEquals

// Runs without a server: the presence and length checks fail before Paper's item codec is reached.
class ItemStackSerializerTest {

    @Serializable
    private data class Holder(
        @ProtoNumber(1) val single: @Serializable(ItemStackSerializer::class) ItemStack,
    )

    @Test
    fun `descriptor is the surf ItemStack message`() {
        val descriptor = ItemStackSerializer.descriptor

        assertEquals("surf.types.paper.v1.ItemStack", descriptor.serialName)
        assertEquals(StructureKind.CLASS, descriptor.kind)
        assertEquals(listOf("data"), descriptor.elementNames.toList())
        assertEquals(1, descriptor.getElementAnnotations(0).filterIsInstance<ProtoNumber>().single().number)
    }

    @Test
    fun `absent data is a missing field`() {
        assertMissingField("surf.types.paper.v1.ItemStack", "data") { decodeHex(ItemStackSerializer, "") }
        assertMissingField("surf.types.paper.v1.ItemStack", "data") { decodeHex(Holder.serializer(), "0a00") }
    }

    @Test
    fun `empty data is rejected`() {
        assertInvalid { decodeHex(ItemStackSerializer, "0a00") }
    }
}
