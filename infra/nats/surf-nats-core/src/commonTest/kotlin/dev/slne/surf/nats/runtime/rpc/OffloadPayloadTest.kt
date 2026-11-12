package dev.slne.surf.nats.runtime.rpc

import dev.slne.surf.nats.protocol.PayloadBucket
import dev.slne.surf.nats.protocol.ProtocolDefaults
import dev.slne.surf.nats.protocol.ProtocolHeaders
import dev.slne.surf.nats.runtime.epochMillis
import dev.slne.surf.nats.runtime.headerLineBytes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class OffloadPayloadTest {

    // Uploads and header writes in call order
    private val events = mutableListOf<Pair<String, String>>()

    @Test
    fun bodyAtTheLimitIsLeftAlone() {
        assertFalse(offload(maxPayload = FITTING_MAX_PAYLOAD))

        assertEquals(emptyList(), events)
    }

    @Test
    fun bodyAboveTheLimitIsUploadedThenReferenced() {
        assertTrue(offload(maxPayload = FITTING_MAX_PAYLOAD - 1))

        val name = uploadedNames().single()
        assertEquals(listOf(UPLOAD to name, ProtocolHeaders.PAYLOAD_REF to name), events)
    }

    @Test
    fun inlineBodyGetsOneDeadline() {
        val before = epochMillis()
        assertFalse(offload(maxPayload = FITTING_MAX_PAYLOAD + DEADLINE_LINE_BYTES, stampDeadline = true))
        val after = epochMillis()

        assertEquals(listOf(ProtocolHeaders.DEADLINE), events.map { it.first })
        assertTrue(deadline() in (before + LIFETIME_MILLIS)..(after + LIFETIME_MILLIS))
    }

    @Test
    fun sizeCheckCountsTheDeadlineLine() {
        val maxPayload = FITTING_MAX_PAYLOAD + DEADLINE_LINE_BYTES - 1

        assertFalse(offload(maxPayload))
        assertTrue(offload(maxPayload, stampDeadline = true))
    }

    @Test
    fun sizeCheckCountsExtraHeaderBytes() {
        assertFalse(offload(maxPayload = FITTING_MAX_PAYLOAD, extraHeaderBytes = 0))
        assertTrue(offload(maxPayload = FITTING_MAX_PAYLOAD, extraHeaderBytes = 1))
    }

    @Test
    fun offloadStampsOneDeadlineBetweenUploadAndReference() {
        assertTrue(offload(maxPayload = FITTING_MAX_PAYLOAD - 1, stampDeadline = true))

        assertEquals(
            listOf(UPLOAD, ProtocolHeaders.DEADLINE, ProtocolHeaders.PAYLOAD_REF),
            events.map { it.first },
        )
    }

    @Test
    fun deadlineIsTakenAfterTheUpload() {
        var uploadedAt = 0L

        assertTrue(offload(maxPayload = FITTING_MAX_PAYLOAD - 1, stampDeadline = true) { uploadedAt = nextClockTick() })
        val after = epochMillis()

        assertTrue(deadline() in (uploadedAt + LIFETIME_MILLIS)..(after + LIFETIME_MILLIS))
    }

    @Test
    fun everyOffloadUsesAFreshUuidName() {
        assertTrue(offload(maxPayload = FITTING_MAX_PAYLOAD - 1))
        assertTrue(offload(maxPayload = FITTING_MAX_PAYLOAD - 1))

        val names = uploadedNames()
        assertEquals(2, names.toSet().size)
        for (name in names) assertTrue(UUID_FORM.matches(name), name)
    }

    @Test
    fun lifetimeAboveTheBucketTtlFailsBeforeTheUpload() {
        for (bucket in PayloadBucket.entries) {
            val lifetime = bucket.ttl + 1.seconds

            val error = assertFailsWith<IllegalArgumentException> {
                offload(maxPayload = FITTING_MAX_PAYLOAD - 1, lifetime = lifetime, bucket = bucket)
            }

            assertEquals(bucket.lifetimeExceededMessage(BODY_BYTES, lifetime), error.message)
        }
        assertEquals(emptyList(), events)
    }

    @Test
    fun lifetimeEqualToTheBucketTtlIsOffloaded() {
        for (bucket in PayloadBucket.entries) {
            assertTrue(offload(maxPayload = FITTING_MAX_PAYLOAD - 1, lifetime = bucket.ttl, bucket = bucket))
        }
    }

    @Test
    fun inlineBodyIgnoresTheBucketTtl() {
        val lifetime = PayloadBucket.TRANSIENT.ttl + 1.seconds

        assertFalse(offload(maxPayload = FITTING_MAX_PAYLOAD, lifetime = lifetime))
    }

    @Test
    fun failedUploadLeavesTheHeadersUntouched() {
        val failure = IllegalStateException("upload failed")

        val thrown = assertFailsWith<IllegalStateException> {
            offload(maxPayload = FITTING_MAX_PAYLOAD - 1, stampDeadline = true) { throw failure }
        }

        assertSame(failure, thrown)
        assertEquals(listOf(UPLOAD), events.map { it.first })
    }

    private fun offload(
        maxPayload: Long,
        stampDeadline: Boolean = false,
        extraHeaderBytes: Int = 0,
        lifetime: Duration = LIFETIME,
        bucket: PayloadBucket = PayloadBucket.TRANSIENT,
        duringUpload: () -> Unit = {},
    ): Boolean = offloadPayload(
        bodyBytes = BODY_BYTES,
        headerBytes = HEADER_BYTES,
        extraHeaderBytes = extraHeaderBytes,
        maxPayload = maxPayload,
        lifetime = lifetime,
        bucket = bucket,
        stampDeadline = stampDeadline,
        writeHeader = { name, value -> events += name to value },
        upload = { name ->
            events += UPLOAD to name
            duringUpload()
        },
    )

    private fun uploadedNames(): List<String> = events.filter { it.first == UPLOAD }.map { it.second }

    private fun deadline(): Long = events.single { it.first == ProtocolHeaders.DEADLINE }.second.toLong()

    // A deadline taken before this returns is smaller than one taken after it
    private fun nextClockTick(): Long {
        val start = epochMillis()
        var now = epochMillis()
        while (now <= start) now = epochMillis()
        return now
    }

    private companion object {
        const val UPLOAD = "upload"
        const val BODY_BYTES = 1000
        const val HEADER_BYTES = 200
        val LIFETIME = 5.seconds
        val LIFETIME_MILLIS = LIFETIME.inWholeMilliseconds

        // Smallest max payload the body fits into without a deadline and without extra headers
        val FITTING_MAX_PAYLOAD = (BODY_BYTES + HEADER_BYTES + ProtocolDefaults.PAYLOAD_REF_RESERVE).toLong()

        // Surf-Deadline is epoch milliseconds: 13 digits
        val DEADLINE_LINE_BYTES = headerLineBytes(ProtocolHeaders.DEADLINE, "0".repeat(13))

        val UUID_FORM = Regex("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}")
    }
}
