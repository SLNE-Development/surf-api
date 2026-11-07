package dev.slne.surf.nats.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class ProtocolHeadersTest {

    @Test
    fun headerNamesAndValues() {
        assertEquals("Content-Type", ProtocolHeaders.CONTENT_TYPE)
        assertEquals("Surf-Origin", ProtocolHeaders.ORIGIN)
        assertEquals("Surf-Sender-Version", ProtocolHeaders.SENDER_VERSION)
        assertEquals("Surf-Deadline", ProtocolHeaders.DEADLINE)
        assertEquals("Surf-Error", ProtocolHeaders.ERROR)
        assertEquals("Surf-Payload-Ref", ProtocolHeaders.PAYLOAD_REF)
        assertEquals("Surf-Reply-To", ProtocolHeaders.REPLY_TO)
        assertEquals("Surf-Dead-Subject", ProtocolHeaders.DEAD_SUBJECT)
        assertEquals("Surf-Event-Id", ProtocolHeaders.EVENT_ID)
        assertEquals("Surf-Message-Id", ProtocolHeaders.MESSAGE_ID)
        assertEquals("Nats-Msg-Id", ProtocolHeaders.NATS_MSG_ID)
        assertEquals("Nats-TTL", ProtocolHeaders.NATS_TTL)
        assertEquals("Surf-Answer", ProtocolHeaders.ANSWER)
        assertEquals("unknown", ProtocolHeaders.ANSWER_UNKNOWN)
        assertEquals("Surf-Presence", ProtocolHeaders.PRESENCE)
        assertEquals("join", ProtocolHeaders.PRESENCE_JOIN)
        assertEquals("beat", ProtocolHeaders.PRESENCE_BEAT)
        assertEquals("leave", ProtocolHeaders.PRESENCE_LEAVE)
        assertEquals("query", ProtocolHeaders.PRESENCE_QUERY)
        assertEquals("application/protobuf", ProtocolHeaders.PROTOBUF_CONTENT_TYPE)
    }

    @Test
    fun eventIdsAreVisibleAsciiOfBoundedLength() {
        assertNull(ProtocolHeaders.eventIdProblem("0f8fad5b-d9cb-469f-a165-70867728950e"))
        assertNull(ProtocolHeaders.eventIdProblem("x".repeat(ProtocolHeaders.MAX_EVENT_ID_LENGTH)))

        for (invalid in listOf("", "x".repeat(ProtocolHeaders.MAX_EVENT_ID_LENGTH + 1), "a b", "line\r\n", "ü")) {
            assertTrue(ProtocolHeaders.eventIdProblem(invalid) != null, "'$invalid' must be rejected")
        }
    }

    @Test
    fun idempotencyKeysAreVisibleAsciiOfBoundedLength() {
        assertNull(ProtocolHeaders.idempotencyKeyProblem("ban:0f8fad5b-d9cb-469f-a165-70867728950e"))
        assertNull(ProtocolHeaders.idempotencyKeyProblem("x".repeat(ProtocolHeaders.MAX_IDEMPOTENCY_KEY_LENGTH)))

        for (invalid in listOf("", "x".repeat(ProtocolHeaders.MAX_IDEMPOTENCY_KEY_LENGTH + 1), "a b", "line\r\n", "ü")) {
            assertTrue(ProtocolHeaders.idempotencyKeyProblem(invalid) != null, "'$invalid' must be rejected")
        }
    }

    @Test
    fun protobufContentTypesAreAccepted() {
        val accepted = listOf(
            "application/protobuf",
            "Application/Protobuf",
            "APPLICATION/PROTOBUF",
            " application/protobuf ",
            "application/protobuf;proto=surf.punish.v1.PlayerBanned",
            "application/protobuf ; charset=binary",
        )
        for (contentType in accepted) {
            assertTrue(ProtocolHeaders.isProtobufContentType(contentType), "content type '$contentType'")
            ProtocolHeaders.requireProtobufContentType(contentType)
        }
    }

    @Test
    fun otherContentTypesAreRejected() {
        val rejected = listOf(
            "",
            ";application/protobuf",
            "application/cbor",
            "application/json",
            "application/x-protobuf",
            "application/protobuffer",
            "application/proto buf",
        )
        for (contentType in rejected) {
            assertFalse(ProtocolHeaders.isProtobufContentType(contentType), "content type '$contentType'")
            val error = assertFailsWith<IllegalArgumentException>("content type '$contentType'") {
                ProtocolHeaders.requireProtobufContentType(contentType)
            }
            assertTrue("application/protobuf" in error.message.orEmpty(), error.message)
        }
    }

    @Test
    fun missingContentTypeIsRejected() {
        assertFalse(ProtocolHeaders.isProtobufContentType(null))
        val error = assertFailsWith<IllegalArgumentException> { ProtocolHeaders.requireProtobufContentType(null) }
        assertTrue("application/protobuf" in error.message.orEmpty(), error.message)
    }

    @Test
    fun originAndDeadline() {
        assertEquals("surf-punish@0b6c", ProtocolHeaders.origin("surf-punish", "0b6c"))
        assertEquals("1700000005000", ProtocolHeaders.deadline(1_700_000_000_000, 5.seconds))
        assertEquals(1_700_000_005_000, ProtocolHeaders.deadlineMillis("1700000005000"))
        assertNull(ProtocolHeaders.deadlineMillis("soon"))
        assertNull(ProtocolHeaders.deadlineMillis(""))
        assertNull(ProtocolHeaders.deadlineMillis(null))
    }

    @Test
    fun emptyHeaderValuesAreAbsent() {
        assertNull(ProtocolHeaders.valueOrNull(null))
        assertNull(ProtocolHeaders.valueOrNull(""))
        assertEquals(" ", ProtocolHeaders.valueOrNull(" "))
        assertEquals("unknown", ProtocolHeaders.valueOrNull("unknown"))
    }
}
