package dev.slne.surf.nats.descriptor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ByteArrayContentTest {
    private val none: ByteArray? = null
    private val noList: List<ByteArray>? = null
    private val noMap: Map<String, ByteArray>? = null

    @Test
    fun `byte arrays compare by content`() {
        val a = byteArrayOf(1, 2, 3)
        val b = byteArrayOf(1, 2, 3)

        assertTrue(natsContentEquals(a, b))
        assertEquals(natsContentHashCode(a), natsContentHashCode(b))
        assertFalse(natsContentEquals(a, byteArrayOf(1, 2, 4)))
        assertFalse(natsContentEquals(a, byteArrayOf(1, 2)))
    }

    @Test
    fun `null byte array equals only null`() {
        assertTrue(natsContentEquals(none, none))
        assertFalse(natsContentEquals(none, byteArrayOf()))
        assertFalse(natsContentEquals(byteArrayOf(), none))
        assertEquals(0, natsContentHashCode(none))
    }

    @Test
    fun `byte array toString lists the content`() {
        assertEquals("[1, -2, 3]", natsContentToString(byteArrayOf(1, -2, 3)))
        assertEquals("[]", natsContentToString(byteArrayOf()))
        assertEquals("null", natsContentToString(none))
    }

    @Test
    fun `byte array lists compare element content`() {
        val a = listOf(byteArrayOf(1), byteArrayOf(2, 3))
        val b = listOf(byteArrayOf(1), byteArrayOf(2, 3))

        assertTrue(natsContentEquals(a, b))
        assertEquals(natsContentHashCode(a), natsContentHashCode(b))
        assertFalse(natsContentEquals(a, listOf(byteArrayOf(1), byteArrayOf(2, 4))))
        assertFalse(natsContentEquals(a, listOf(byteArrayOf(2, 3), byteArrayOf(1))))
    }

    @Test
    fun `byte array lists of different length are unequal`() {
        val a = listOf(byteArrayOf(1), byteArrayOf(2))

        assertFalse(natsContentEquals(a, listOf(byteArrayOf(1))))
        assertFalse(natsContentEquals(listOf(byteArrayOf(1)), a))
        assertFalse(natsContentEquals(a, emptyList()))
    }

    @Test
    fun `null byte array list equals only null`() {
        assertTrue(natsContentEquals(noList, noList))
        assertFalse(natsContentEquals(noList, emptyList()))
        assertFalse(natsContentEquals(emptyList(), noList))
        assertTrue(natsContentEquals(emptyList(), emptyList()))
        assertEquals(0, natsContentHashCode(noList))
    }

    @Test
    fun `byte array lists with null elements compare by content`() {
        val a: List<ByteArray?> = listOf(null, byteArrayOf(1))

        assertTrue(natsContentEquals(a, listOf(null, byteArrayOf(1))))
        assertEquals(natsContentHashCode(a), natsContentHashCode(listOf(null, byteArrayOf(1))))
        assertFalse(natsContentEquals(a, listOf(byteArrayOf(), byteArrayOf(1))))
    }

    @Test
    fun `byte array list hash follows the list contract`() {
        val a = byteArrayOf(1, 2)
        val b = byteArrayOf(3)

        assertEquals(listOf(a.contentHashCode(), b.contentHashCode()).hashCode(), natsContentHashCode(listOf(a, b)))
        assertEquals(emptyList<Int>().hashCode(), natsContentHashCode(emptyList<ByteArray>()))
    }

    @Test
    fun `byte array list toString lists the element content`() {
        assertEquals("[[1, 2], [], null]", natsContentToString(listOf(byteArrayOf(1, 2), byteArrayOf(), null)))
        assertEquals("[]", natsContentToString(emptyList<ByteArray>()))
        assertEquals("null", natsContentToString(noList))
    }

    @Test
    fun `byte array maps compare value content`() {
        val a = mapOf("a" to byteArrayOf(1), "b" to byteArrayOf(2, 3))
        val b = mapOf("a" to byteArrayOf(1), "b" to byteArrayOf(2, 3))

        assertTrue(natsContentEquals(a, b))
        assertEquals(natsContentHashCode(a), natsContentHashCode(b))
        assertFalse(natsContentEquals(a, mapOf("a" to byteArrayOf(1), "b" to byteArrayOf(2, 4))))
    }

    @Test
    fun `byte array maps ignore iteration order`() {
        val a = linkedMapOf(1 to byteArrayOf(1), 2 to byteArrayOf(2))
        val b = linkedMapOf(2 to byteArrayOf(2), 1 to byteArrayOf(1))

        assertTrue(natsContentEquals(a, b))
        assertEquals(natsContentHashCode(a), natsContentHashCode(b))
    }

    @Test
    fun `byte array maps with different keys are unequal`() {
        val a = mapOf("a" to byteArrayOf(1), "b" to byteArrayOf(2))

        assertFalse(natsContentEquals(a, mapOf("a" to byteArrayOf(1), "c" to byteArrayOf(2))))
        assertFalse(natsContentEquals(a, mapOf("a" to byteArrayOf(1))))
        assertFalse(natsContentEquals(mapOf("a" to byteArrayOf(1)), a))
    }

    @Test
    fun `null map value differs from a missing key and from an empty value`() {
        val a: Map<String, ByteArray?> = mapOf("a" to null)

        assertTrue(natsContentEquals(a, mapOf("a" to null)))
        assertFalse(natsContentEquals(a, mapOf("b" to null)))
        assertFalse(natsContentEquals(a, mapOf("a" to byteArrayOf())))
        assertFalse(natsContentEquals(mapOf("a" to byteArrayOf()), a))
    }

    @Test
    fun `null byte array map equals only null`() {
        assertTrue(natsContentEquals(noMap, noMap))
        assertFalse(natsContentEquals(noMap, emptyMap()))
        assertFalse(natsContentEquals(emptyMap(), noMap))
        assertTrue(natsContentEquals(emptyMap<String, ByteArray>(), emptyMap()))
        assertEquals(0, natsContentHashCode(noMap))
    }

    @Test
    fun `byte array map hash follows the map contract`() {
        val a = byteArrayOf(1, 2)
        val b = byteArrayOf(3)

        assertEquals(
            mapOf("a" to a.contentHashCode(), "b" to b.contentHashCode()).hashCode(),
            natsContentHashCode(mapOf("a" to a, "b" to b)),
        )
        assertEquals(emptyMap<String, Int>().hashCode(), natsContentHashCode(emptyMap<String, ByteArray>()))
    }

    @Test
    fun `byte array map toString lists the value content`() {
        assertEquals("{a=[1, 2], b=null}", natsContentToString(linkedMapOf("a" to byteArrayOf(1, 2), "b" to null)))
        assertEquals("{}", natsContentToString(emptyMap<String, ByteArray>()))
        assertEquals("null", natsContentToString(noMap))
    }
}
