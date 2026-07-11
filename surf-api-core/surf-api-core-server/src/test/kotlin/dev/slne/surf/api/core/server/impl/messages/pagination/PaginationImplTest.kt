package dev.slne.surf.api.core.server.impl.messages.pagination

import java.util.RandomAccess
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PaginationImplTest {
    @Test
    fun `page count does not overflow for large collections`() {
        assertEquals(Int.MAX_VALUE, PaginationImpl.pages(pageSize = 1, count = Int.MAX_VALUE))
        assertEquals(1, PaginationImpl.pages(pageSize = Int.MAX_VALUE, count = Int.MAX_VALUE))
    }

    @Test
    fun `page count validates inputs`() {
        assertEquals(0, PaginationImpl.pages(pageSize = 1, count = 0))
        assertFailsWith<IllegalArgumentException> { PaginationImpl.pages(0, 1) }
        assertFailsWith<IllegalArgumentException> { PaginationImpl.pages(1, -1) }
    }

    @Test
    fun `last page bounds do not overflow`() {
        val hugeList = object : AbstractList<Int>(), RandomAccess {
            override val size: Int = Int.MAX_VALUE
            override fun get(index: Int) = index
        }
        val entries = mutableListOf<Int>()

        PaginationImpl.forEachPageEntry(
            hugeList,
            pageSize = 2,
            page = 1_073_741_824,
        ) { _, index -> entries += index }

        assertEquals(listOf(Int.MAX_VALUE - 1), entries)
    }
}
