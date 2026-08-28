package dev.slne.surf.api.core.inventory.framework.internal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ViewButtonSessionTest {

    private enum class Difficulty { EASY, NORMAL, HARD }

    private fun difficulty(initial: Difficulty = Difficulty.EASY) =
        ViewButtonSession(Difficulty.entries.toList(), initial)

    private fun toggle(initial: Boolean = false) =
        ViewButtonSession(listOf(false, true), initial)

    @Test
    fun `starts on the initial state`() {
        val session = difficulty(Difficulty.NORMAL)

        assertEquals(Difficulty.NORMAL, session.current)
        assertEquals(Difficulty.NORMAL, session.initial)
        assertFalse(session.hasChanged)
    }

    @Test
    fun `advancing moves forward through the declared order`() {
        val session = difficulty()

        assertTrue(session.advance(1))
        assertEquals(Difficulty.NORMAL, session.current)

        assertTrue(session.advance(1))
        assertEquals(Difficulty.HARD, session.current)
    }

    @Test
    fun `advancing wraps around at both ends`() {
        val session = difficulty()

        assertTrue(session.advance(-1))
        assertEquals(Difficulty.HARD, session.current, "stepping back from the first state wraps")

        assertTrue(session.advance(1))
        assertEquals(Difficulty.EASY, session.current, "stepping forward from the last state wraps")
    }

    @Test
    fun `a full cycle counts as unchanged`() {
        val session = difficulty()

        repeat(Difficulty.entries.size) { session.advance(1) }

        assertEquals(Difficulty.EASY, session.current)
        assertFalse(session.hasChanged, "cycling back to the initial state is not a change")
    }

    @Test
    fun `hasChanged reports any state other than the initial one`() {
        val session = difficulty()

        session.advance(1)

        assertTrue(session.hasChanged)
    }

    @Test
    fun `a single state button never moves`() {
        val session = ViewButtonSession(listOf(Difficulty.EASY), Difficulty.EASY)

        assertFalse(session.advance(1))
        assertFalse(session.advance(-1))
        assertEquals(Difficulty.EASY, session.current)
        assertFalse(session.hasChanged)
    }

    @Test
    fun `advancing by zero is a no-op`() {
        val session = difficulty()

        assertFalse(session.advance(0))
        assertEquals(Difficulty.EASY, session.current)
    }

    @Test
    fun `selecting jumps straight to a state`() {
        val session = difficulty()

        assertTrue(session.select(Difficulty.HARD))
        assertEquals(Difficulty.HARD, session.current)
        assertTrue(session.hasChanged)
    }

    @Test
    fun `selecting the current state reports no change`() {
        val session = difficulty()

        assertFalse(session.select(Difficulty.EASY))
        assertEquals(Difficulty.EASY, session.current)
    }

    @Test
    fun `selecting an undeclared state fails`() {
        val session = ViewButtonSession(listOf(Difficulty.EASY, Difficulty.NORMAL), Difficulty.EASY)

        assertFailsWith<IllegalArgumentException> { session.select(Difficulty.HARD) }
    }

    @Test
    fun `an initial value outside the declared states fails`() {
        assertFailsWith<IllegalArgumentException> {
            ViewButtonSession(listOf(Difficulty.EASY, Difficulty.NORMAL), Difficulty.HARD)
        }
    }

    @Test
    fun `a button without states fails`() {
        assertFailsWith<IllegalArgumentException> {
            ViewButtonSession(emptyList<Difficulty>(), Difficulty.EASY)
        }
    }

    @Test
    fun `a toggle flips in both click directions`() {
        val forward = toggle()
        assertTrue(forward.advance(1))
        assertTrue(forward.current)

        val backward = toggle()
        assertTrue(backward.advance(-1))
        assertTrue(backward.current, "with two states a right click lands on the same state")
    }

    @Test
    fun `a toggle switched twice ends up unchanged`() {
        val session = toggle(initial = true)

        session.advance(1)
        assertTrue(session.hasChanged)

        session.advance(1)
        assertTrue(session.current)
        assertFalse(session.hasChanged)
    }
}
