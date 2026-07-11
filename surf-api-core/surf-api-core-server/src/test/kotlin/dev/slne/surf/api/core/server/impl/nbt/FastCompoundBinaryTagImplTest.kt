package dev.slne.surf.api.core.server.impl.nbt

import net.kyori.adventure.nbt.BinaryTagTypes
import net.kyori.adventure.nbt.IntBinaryTag
import net.kyori.adventure.nbt.ListBinaryTag
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertSame

class FastCompoundBinaryTagImplTest {
    @Test
    fun `typed list lookup checks the element type`() {
        val list = ListBinaryTag.builder(BinaryTagTypes.INT)
            .add(IntBinaryTag.intBinaryTag(1))
            .build()
        val tag = FastCompoundBinaryTagImpl(synchronize = false).put("values", list)

        assertSame(list, tag.getList("values", BinaryTagTypes.INT))
        assertNull(tag.getList("values", BinaryTagTypes.STRING))
    }
}
