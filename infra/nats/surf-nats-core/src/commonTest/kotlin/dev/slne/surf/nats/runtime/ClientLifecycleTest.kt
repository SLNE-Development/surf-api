package dev.slne.surf.nats.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClientLifecycleTest {

    @Test
    fun closingRejectsNewWorkButKeepsTheClientUsable() {
        val lifecycle = ClientLifecycle("surf-test")
        assertTrue(lifecycle.beginClose())
        assertFalse(lifecycle.beginClose())

        lifecycle.requireOpen()
        val rejected = assertFailsWith<IllegalStateException> { lifecycle.requireAccepting() }
        assertEquals("NatsClient[surf-test] is closing", rejected.message)
        assertFalse(lifecycle.isClosed)
    }

    @Test
    fun stopAcceptingRejectsNewWorkWithoutStartingTheClose() {
        val lifecycle = ClientLifecycle("surf-test")
        assertTrue(lifecycle.stopAccepting())
        assertFalse(lifecycle.stopAccepting())

        assertFailsWith<IllegalStateException> { lifecycle.requireAccepting() }
        lifecycle.requireCloseNotStarted()
        assertTrue(lifecycle.beginClose())
        assertFalse(lifecycle.beginClose())
        assertEquals(
            "NatsClient[surf-test] is closing",
            assertFailsWith<IllegalStateException> { lifecycle.requireCloseNotStarted() }.message,
        )
    }

    @Test
    fun admitBeforeCloseKeepsTheEffectAfterStopAccepting() {
        val lifecycle = ClientLifecycle("surf-test")
        lifecycle.stopAccepting()
        var added = false

        lifecycle.admitBeforeClose(add = { added = true }, undo = { added = false })
        assertTrue(added)
    }

    @Test
    fun admitBeforeCloseUndoesWhenTheCloseStartedDuringAdd() {
        val lifecycle = ClientLifecycle("surf-test")
        var added = false

        assertFailsWith<IllegalStateException> {
            lifecycle.admitBeforeClose(
                add = {
                    added = true
                    lifecycle.beginClose()
                },
                undo = { added = false },
            )
        }
        assertFalse(added)
    }

    @Test
    fun closedRejectsEveryUse() {
        val lifecycle = ClientLifecycle("surf-test")
        lifecycle.beginClose()
        lifecycle.markClosed()

        assertTrue(lifecycle.isClosed)
        assertEquals(
            "NatsClient[surf-test] is closed",
            assertFailsWith<IllegalStateException> { lifecycle.requireOpen() }.message,
        )
        assertEquals(
            "NatsClient[surf-test] is closed",
            assertFailsWith<IllegalStateException> { lifecycle.requireAccepting() }.message,
        )
    }

    @Test
    fun admitUndoesWhenClosingStartedDuringAdd() {
        val lifecycle = ClientLifecycle("surf-test")
        var added = false

        assertFailsWith<IllegalStateException> {
            lifecycle.admit(
                add = {
                    added = true
                    lifecycle.beginClose()
                },
                undo = { added = false },
            )
        }
        assertFalse(added)
    }

    @Test
    fun admitKeepsTheEffectWhileOpen() {
        val lifecycle = ClientLifecycle("surf-test")
        var added = false

        lifecycle.admit(add = { added = true }, undo = { added = false })
        assertTrue(added)
    }
}
