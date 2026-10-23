package dev.slne.surf.nats.runtime.rpc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.fail

class AllReplyTest {

    private fun classify(status: Int, error: String?, answer: String?) = classifyAllReply(status, { error }, { answer })

    @Test
    fun repliesAreClassifiedByStatusThenHeaders() {
        assertEquals(AllReply.NO_RESPONDERS, classify(503, "x", null))
        assertEquals(AllReply.OTHER_STATUS, classify(408, null, null))
        assertEquals(AllReply.REMOTE_ERROR, classify(0, "java.lang.IllegalStateException", "unknown"))
        assertEquals(AllReply.UNKNOWN, classify(0, null, "unknown"))
        assertEquals(AllReply.ANSWER, classify(0, null, null))
        assertEquals(AllReply.ANSWER, classify(0, null, "known"))
    }

    @Test
    fun headersOfStatusRepliesAreNotRead() {
        assertEquals(AllReply.NO_RESPONDERS, classifyAllReply(503, { fail("error read") }, { fail("answer read") }))
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
