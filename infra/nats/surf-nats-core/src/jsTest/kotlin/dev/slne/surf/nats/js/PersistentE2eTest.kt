package dev.slne.surf.nats.js

import dev.slne.surf.nats.js.client.JsRuntimeDefaults
import dev.slne.surf.nats.js.error.NatsCallTimeoutError
import dev.slne.surf.nats.js.error.NatsRpcError
import dev.slne.surf.nats.js.fixtures.AuditEntry
import dev.slne.surf.nats.js.fixtures.AuditEntrySchema
import dev.slne.surf.nats.js.fixtures.AuditQuerySchema
import dev.slne.surf.nats.js.fixtures.AuditResult
import dev.slne.surf.nats.js.fixtures.AuditResultSchema
import dev.slne.surf.nats.js.fixtures.AuditService
import dev.slne.surf.nats.js.fixtures.fields
import dev.slne.surf.nats.protocol.ProtocolDefaults
import dev.slne.surf.nats.protocol.ProtocolHeaders
import dev.slne.surf.nats.protocol.ServiceNames
import dev.slne.surf.nats.runtime.RuntimeDefaults
import dev.slne.surf.nats.runtime.epochMillis
import js.objects.unsafeJso
import js.typedarrays.Uint8Array
import kotlinx.coroutines.async
import kotlinx.coroutines.await
import kotlinx.coroutines.coroutineScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class PersistentE2eTest {

    private fun method(name: String) = AuditService.methods.first { it.localName == name }

    private fun entry(action: String, data: Uint8Array<*>? = null): AuditEntry = unsafeJso {
        this.action = action
        if (data != null) this.data = data
    }

    private val names = ServiceNames("surf.natsjs.test.v1.AuditService")

    @Test
    fun fireIsStoredWithMsgIdAndWithoutDeadline() = e2eTest {
        val raw = rawNats()
        val client = TestNats.client(uniqueName("persist-caller"))
        try {
            raw.addPersistentStreams(names)

            val result = client.call(method("record"), entry("ban")).await()
            assertEquals("undefined", jsTypeOf(result))

            val message = assertNotNull(raw.nextPersistent(names))
            assertEquals(names.methodSubject("Record", true), message.subject)
            val msgId = assertNotNull(header(message, ProtocolHeaders.NATS_MSG_ID))
            assertEquals(36, msgId.length)
            assertEquals(ProtocolHeaders.PROTOBUF_CONTENT_TYPE, header(message, ProtocolHeaders.CONTENT_TYPE))
            assertEquals(client.origin, header(message, ProtocolHeaders.ORIGIN))
            assertNull(header(message, ProtocolHeaders.DEADLINE))
            assertNull(header(message, ProtocolHeaders.NATS_TTL))
            assertNull(header(message, ProtocolHeaders.REPLY_TO))
            assertEquals("ban", raw.decode(AuditEntrySchema, message.data).fields<AuditEntry>().action)
            message.ack()
        } finally {
            client.close().await()
            raw.close()
        }
    }

    @Test
    fun fireWithoutStreamIsRejected() = e2eTest {
        val raw = rawNats()
        val client = TestNats.client(uniqueName("persist-caller"))
        try {
            raw.deletePersistentStreams(names)
            val error = assertFailsWith<NatsRpcError> {
                client.call(method("record"), entry("x")).await()
            }
            assertTrue(error.message!!.startsWith("JetStream publish on "), error.message)

            val tooLong = assertFails {
                client.call(method("record"), entry("x"), callOptions(timeoutMs = 121_000)).await()
            }
            assertTrue(tooLong.message!!.contains("duplicate window"), tooLong.message)
        } finally {
            client.close().await()
            raw.close()
        }
    }

    @Test
    fun persistentRpcRepliesThroughTheReplyInbox() = e2eTest {
        val raw = rawNats()
        val client = TestNats.client(uniqueName("persist-caller"))
        try {
            raw.addPersistentStreams(names)

            coroutineScope {
                val call = async {
                    client.call(method("query"), entry("ban")).await()
                }

                val message = assertNotNull(raw.nextPersistent(names))
                assertEquals(names.methodSubject("Query", true), message.subject)
                val replyTo = assertNotNull(header(message, ProtocolHeaders.REPLY_TO))
                assertNotNull(header(message, ProtocolHeaders.DEADLINE)?.toLongOrNull())
                assertNotNull(header(message, ProtocolHeaders.NATS_MSG_ID))
                // AuditService has call_timeout_seconds: 10
                assertEquals("10s", header(message, ProtocolHeaders.NATS_TTL))
                assertEquals("ban", raw.decode(AuditQuerySchema, message.data).fields<AuditEntry>().action)

                raw.publish(replyTo, raw.encode(AuditResultSchema, unsafeJso<AuditResult> { count = 3 }), raw.jvmHeaders())
                message.ack()

                assertEquals(3, call.await().fields<AuditResult>().count)
            }
        } finally {
            client.close().await()
            raw.close()
        }
    }

    @Test
    fun persistentRpcTimesOutWithoutReply() = e2eTest {
        val raw = rawNats()
        val client = TestNats.client(uniqueName("persist-caller"))
        try {
            raw.addPersistentStreams(names)
            coroutineScope {
                val call = async {
                    assertFails {
                        client.call(method("query"), entry("x"), callOptions(timeoutMs = 1500)).await()
                    }
                }

                // The stored request expires with Nats-TTL = ceil(timeout) = 2 s; nobody replies.
                val message = assertNotNull(raw.nextPersistent(names))
                assertEquals("2s", header(message, ProtocolHeaders.NATS_TTL))
                message.ack()

                assertEquals(1500, assertIs<NatsCallTimeoutError>(call.await()).timeoutMs)
            }
        } finally {
            client.close().await()
            raw.close()
        }
    }

    @Test
    fun lostPubAcksAreRepublishedWithTheSameMsgIdUntilTheFireTimeout() = e2eTest {
        val raw = rawNats()
        val client = TestNats.client(uniqueName("persist-caller"))
        val previousTimeout = JsRuntimeDefaults.jetStreamPublishTimeout
        try {
            // The stream stores and deduplicates, but never acknowledges a publish
            raw.addPersistentStreams(names, noAck = true)
            val subject = names.methodSubject("Record", true)
            val attempts = mutableListOf<String?>()
            raw.subscribe(subject) { attempts += header(it, ProtocolHeaders.NATS_MSG_ID) }
            raw.flush()
            JsRuntimeDefaults.jetStreamPublishTimeout = 400.milliseconds

            val start = epochMillis()
            val error = assertFails {
                client.call(method("record"), entry("ban"), callOptions(timeoutMs = 2500)).await()
            }
            val took = epochMillis() - start

            assertIs<NatsCallTimeoutError>(error)
            assertEquals(subject, error.subject)
            assertEquals(2500, error.timeoutMs)
            assertTrue(took in 2400..6000, "took $took ms")
            eventually { attempts.size >= 2 }
            assertNotNull(attempts.first())
            assertEquals(1, attempts.toSet().size, "every attempt must carry the same Nats-Msg-Id: $attempts")
            assertEquals(1, raw.streamMessages(names.stream), "the stream must deduplicate the attempts")
        } finally {
            JsRuntimeDefaults.jetStreamPublishTimeout = previousTimeout
            client.close().await()
            raw.close()
        }
    }

    @Test
    fun thePublishPhaseOfAPersistentRpcEndsAtTheDuplicateWindow() = e2eTest {
        val raw = rawNats()
        val client = TestNats.client(uniqueName("persist-caller"))
        val previousTimeout = JsRuntimeDefaults.jetStreamPublishTimeout
        val previousWindow = RuntimeDefaults.dedupeWindow
        try {
            raw.addPersistentStreams(names, noAck = true)
            JsRuntimeDefaults.jetStreamPublishTimeout = 400.milliseconds
            RuntimeDefaults.dedupeWindow = 1.seconds

            val start = epochMillis()
            val error = assertFails {
                client.call(method("query"), entry("ban"), callOptions(timeoutMs = 8000)).await()
            }
            val took = epochMillis() - start

            // Without a PubAck the call ends after the publish phase, not after its own timeout
            assertEquals(8000, assertIs<NatsCallTimeoutError>(error).timeoutMs)
            assertTrue(took in 900..5000, "took $took ms")
        } finally {
            JsRuntimeDefaults.jetStreamPublishTimeout = previousTimeout
            RuntimeDefaults.dedupeWindow = previousWindow
            client.close().await()
            raw.close()
        }
    }

    @Test
    fun largeFireUsesThePersistentPayloadBucket() = e2eTest {
        val raw = rawNats()
        val client = TestNats.client(uniqueName("persist-caller"))
        try {
            raw.addPersistentStreams(names)
            val data = bytesOfSize(1_500_000)

            client.call(method("record"), entry("big", data)).await()

            val message = assertNotNull(raw.nextPersistent(names))
            val ref = assertNotNull(header(message, ProtocolHeaders.PAYLOAD_REF))
            assertEquals(0, message.data.length)
            val storedBytes = assertNotNull(raw.getBlob(ProtocolDefaults.PERSISTENT_PAYLOAD_BUCKET, ref))
            val stored = raw.decode(AuditEntrySchema, storedBytes).fields<AuditEntry>()
            assertEquals("big", stored.action)
            assertTrue(data.contentEquals(stored.data))
            message.ack()
        } finally {
            client.close().await()
            raw.close()
        }
    }
}
