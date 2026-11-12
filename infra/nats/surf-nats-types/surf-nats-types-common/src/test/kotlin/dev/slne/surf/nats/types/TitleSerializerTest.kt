package dev.slne.surf.nats.types

import kotlinx.serialization.descriptors.nonNullOriginal
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals

class TitleSerializerTest {

    private val texts = "0a060a04226869221205" + "0a03226122" // title "hi", subtitle "a"

    @Test
    fun `descriptor is the surf Title message`() {
        assertMessageDescriptor(
            TitleSerializer.descriptor,
            "surf.types.v1.Title",
            "title" to 1, "subtitle" to 2, "times" to 3,
        )
        assertMessageDescriptor(
            TitleSerializer.descriptor.getElementDescriptor(2).nonNullOriginal,
            "surf.types.v1.Title.Times",
            "fadeIn" to 1, "stay" to 2, "fadeOut" to 3,
        )
    }

    @Test
    fun `title with times`() {
        val value = Title.title(
            Component.text("hi"),
            Component.text("a"),
            Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofSeconds(1)),
        )
        val hex = texts + "1a10" + "0a061080cab5ee01" + "12020803" + "1a020801"

        assertEquals(hex, encodeHex(TitleSerializer, value))
        assertEquals(value, decodeHex(TitleSerializer, hex))
    }

    @Test
    fun `title without times omits the field`() {
        val value = Title.title(Component.text("hi"), Component.text("a"), null)

        assertEquals(texts, encodeHex(TitleSerializer, value))
        assertEquals(value, decodeHex(TitleSerializer, texts))
    }

    @Test
    fun `absent fields are missing`() {
        assertMissingField("surf.types.v1.Title", "title", "subtitle") { decodeHex(TitleSerializer, "") }
        assertMissingField("surf.types.v1.Title.Times", "fadeIn", "stay", "fadeOut") {
            decodeHex(TitleSerializer, texts + "1a00")
        }
    }
}
