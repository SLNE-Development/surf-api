package dev.slne.surf.nats.types

import kotlin.test.Test
import kotlin.test.assertEquals

class EmptySerializerTest {

    @Test
    fun `descriptor is the google Empty message`() {
        assertMessageDescriptor(EmptySerializer.descriptor, "google.protobuf.Empty")
    }

    @Test
    fun `unit is an empty message`() {
        assertEquals("", encodeHex(EmptySerializer, Unit))
        assertEquals(Unit, decodeHex(EmptySerializer, ""))
    }

    @Test
    fun `unknown fields are skipped`() {
        assertEquals(Unit, decodeHex(EmptySerializer, "08011202aabb"))
    }
}
