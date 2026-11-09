package dev.slne.surf.api.shared.internal.nms

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Pins that a dotted Minecraft version is compared as a version and not as text.
 *
 * Both of the things this guards against were being relied on, and both pick the *wrong* NMS
 * bindings rather than failing: a server then starts, and the first reflected member that moved
 * between the two versions throws somewhere unrelated to the cause. There is no test that could
 * catch that downstream, which is why it is caught here.
 */
class NmsVersionTest {

    @Test
    fun `a two-digit minor does not match a single-digit one`() {
        // The bug, and it needs no hypothetical entry: only a server reporting 26.10 while the
        // enum holds 26.1 — the next minor release. `"26.10".startsWith("26.1")` is true.
        assertFalse(
            NmsVersion.V26_1.matches("26.10"),
            "26.10 is not 26.1, however the two strings begin",
        )
        assertNull(
            NmsVersion.matching("26.10"),
            "with no bindings for 26.10 the answer is none, not the 26.1 bindings",
        )
    }

    @Test
    fun `a server matches the entry for its own version`() {
        assertSame(NmsVersion.V26_3, NmsVersion.matching("26.3"))
        assertSame(NmsVersion.V26_1, NmsVersion.matching("26.1"))
        assertSame(NmsVersion.V1_21_11, NmsVersion.matching("1.21.11"))
    }

    @Test
    fun `a patch release matches the entry for its minor`() {
        // Minecraft adds a component for a patch, and bindings are per minor, so 26.3.1 is 26.3.
        assertSame(NmsVersion.V26_3, NmsVersion.matching("26.3.1"))
        assertTrue(NmsVersion.V26_3.matches("26.3.17"))
    }

    @Test
    fun `a version with fewer components than an entry does not match it`() {
        assertFalse(NmsVersion.V26_3.matches("26"), "26 is not 26.3")
        assertFalse(NmsVersion.V1_21_11.matches("1.21"), "1.21 is not 1.21.11")
    }

    @Test
    fun `an unparseable version matches nothing rather than throwing`() {
        // Whatever a fork reports, the resolver has to reach its fallback instead of dying in a
        // lazy initialiser that every NMS call then re-enters.
        assertNull(NmsVersion.matching("not-a-version"))
        assertNull(NmsVersion.matching(""))
        assertFalse(NmsVersion.V26_3.matches("26.x"))
    }

    @Test
    fun `newest first orders by number, not by text`() {
        assertEquals(
            listOf(NmsVersion.V26_3, NmsVersion.V26_2, NmsVersion.V26_1, NmsVersion.V1_21_11),
            NmsVersion.NEWEST_FIRST,
        )
        assertSame(NmsVersion.V26_3, NmsVersion.NEWEST)

        // The property that matters and that a string sort does not have: every entry is above the
        // one after it when compared component by component.
        NmsVersion.NEWEST_FIRST.zipWithNext { newer, older ->
            assertTrue(
                isAbove(newer.components, older.components),
                "${newer.versionPrefix} has to rank above ${older.versionPrefix}",
            )
        }
    }

    @Test
    fun `the component order puts a two-digit minor above a single-digit one`() {
        // Asserted on the comparator rather than on the enum, because with the four entries that
        // exist today a plain text sort produces the same order -- so nothing asserted about the
        // enum could tell a correct implementation from the one this replaces. `26.10` against
        // `26.3` is the case that can.
        val versions = listOf(
            listOf(26, 3),
            listOf(26, 10),
            listOf(26, 2),
            listOf(1, 21, 11),
        )

        assertEquals(
            listOf(listOf(26, 10), listOf(26, 3), listOf(26, 2), listOf(1, 21, 11)),
            versions.sortedWith(COMPONENT_ORDER.reversed()),
            "26.10 is the newest of these; a text sort answers 26.3",
        )
    }

    @Test
    fun `the component order puts a bare version below its own patches`() {
        assertEquals(
            listOf(listOf(26, 3), listOf(26, 3, 1), listOf(26, 3, 17)),
            listOf(listOf(26, 3, 17), listOf(26, 3), listOf(26, 3, 1)).sortedWith(COMPONENT_ORDER),
        )
    }

    @Test
    fun `every entry parses and keeps its own prefix`() {
        // A new entry is added by hand next to the generated modules, so a typo like "26..4" or
        // "26.4-rc" would otherwise surface as a failure to initialise the enum at runtime.
        NmsVersion.entries.forEach { version ->
            assertTrue(version.components.isNotEmpty(), version.name)
            assertEquals(
                version.versionPrefix,
                version.components.joinToString("."),
                "${version.name} does not round-trip, so its prefix is not plain numbers",
            )
            assertTrue(version.matches(version.versionPrefix), version.name)
        }
    }

    /** Whether [left] is a later version than [right], component by component. */
    private fun isAbove(left: List<Int>, right: List<Int>): Boolean {
        for (index in 0 until minOf(left.size, right.size)) {
            if (left[index] != right[index]) return left[index] > right[index]
        }

        return left.size > right.size
    }
}
