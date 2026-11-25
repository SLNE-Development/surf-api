package dev.slne.surf.api.nbt

import net.kyori.adventure.nbt.BinaryTagTypes
import net.kyori.adventure.nbt.CompoundBinaryTag
import net.kyori.adventure.nbt.IntBinaryTag
import net.kyori.adventure.nbt.StringBinaryTag
import kotlin.test.*

class FastCompoundBinaryTagTest {

    private val source = CompoundBinaryTag.builder()
        .putInt("count", 3)
        .putString("name", "surf")
        .build()

    @Test
    fun `copies the entries and round trips`() {
        for (concurrent in listOf(false, true)) {
            val tag = source.fast(concurrent)

            assertEquals(3, tag.getInt("count"))
            assertEquals("surf", tag.getString("name"))
            assertTrue(tag.contains("count", BinaryTagTypes.INT))
            assertEquals(source, tag.asBinaryTag())
        }
    }

    @Test
    fun `mutations do not touch the source tag`() {
        val tag = source.fast(concurrent = true)
            .put("extra", IntBinaryTag.intBinaryTag(1))
            .remove("name")

        assertEquals(setOf("count", "extra"), tag.keySet())
        assertEquals(StringBinaryTag.stringBinaryTag("surf"), source.get("name"))
        assertFalse(source.keySet().contains("extra"))
    }

    @Test
    fun `key set is a read-only view`() {
        for (concurrent in listOf(false, true)) {
            val tag = source.fast(concurrent)
            val keys = tag.keySet()

            assertFailsWith<UnsupportedOperationException> {
                (keys as MutableSet<String>).remove("count")
            }
            tag.put("later", IntBinaryTag.intBinaryTag(0))
            assertTrue("later" in keys)
        }
    }
}
