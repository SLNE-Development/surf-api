package dev.slne.surf.nats.runtime.rpc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class AllReplyTest {

    @Test
    fun repliesAreClassifiedByStatusThenHeaders() {
        assertEquals(AllReply.NO_RESPONDERS, classifyAllReply(503, "x", null))
        assertEquals(AllReply.OTHER_STATUS, classifyAllReply(408, null, null))
        assertEquals(AllReply.REMOTE_ERROR, classifyAllReply(0, "java.lang.IllegalStateException", "unknown"))
        assertEquals(AllReply.UNKNOWN, classifyAllReply(0, null, "unknown"))
        assertEquals(AllReply.ANSWER, classifyAllReply(0, null, null))
        assertEquals(AllReply.ANSWER, classifyAllReply(0, null, "known"))
    }

    @Test
    fun firstFailureIsThrownWithLaterOnesSuppressed() {
        val recorder = FailureRecorder()
        recorder.throwIfAny()

        val first = IllegalStateException("first")
        val second = IllegalArgumentException("second")
        recorder.record(first)
        recorder.record(second)

        val thrown = assertFailsWith<IllegalStateException> { recorder.throwIfAny() }
        assertSame(first, thrown)
        assertEquals(listOf<Throwable>(second), thrown.suppressedExceptions)
    }
}
