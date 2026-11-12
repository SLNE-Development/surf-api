package dev.slne.surf.nats.js

import dev.slne.surf.nats.js.codec.NatsHeaders
import dev.slne.surf.nats.js.external.nats.core.Msg
import dev.slne.surf.nats.js.fixtures.AuditEntry
import dev.slne.surf.nats.js.fixtures.AuditService
import dev.slne.surf.nats.js.fixtures.BlobData
import dev.slne.surf.nats.js.fixtures.BlobRequestSchema
import dev.slne.surf.nats.js.fixtures.BlobResponseSchema
import dev.slne.surf.nats.js.fixtures.FindPlayerRequest
import dev.slne.surf.nats.js.fixtures.LocateResponse
import dev.slne.surf.nats.js.fixtures.LocateResponseSchema
import dev.slne.surf.nats.js.fixtures.PlayerService
import dev.slne.surf.nats.js.fixtures.fields
import dev.slne.surf.nats.protocol.PayloadBucket
import dev.slne.surf.nats.protocol.ProtocolDefaults
import dev.slne.surf.nats.protocol.ProtocolHeaders
import dev.slne.surf.nats.protocol.ServiceNames
import dev.slne.surf.nats.runtime.headerLineBytes
import js.objects.unsafeJso
import js.typedarrays.Uint8Array
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class PayloadE2eTest {

    private val echo get() = PlayerService.methods.first { it.localName == "echo" }

    private fun blob(data: Uint8Array<*>?): BlobData = unsafeJso { this.data = data }

    private val players = ServiceNames("surf.natsjs.test.v1.PlayerService")

    private val echoSubject get() = players.methodSubject("Echo", persistent = false)

    @Test
    fun largeRequestsAndResponsesGoThroughTheObjectStore() = e2eTest {
        val raw = rawNats()
        val client = TestNats.client(uniqueName("payload-caller"))
        try {
            val requestData = bytesOfSize(1_500_000, seed = 3)
            val responseData = bytesOfSize(1_200_000, seed = 5)
            val seenRequests = mutableListOf<Msg>()
            val replyRefs = mutableListOf<String>()
            val responder = this

            raw.subscribe(echoSubject, queue = players.queueGroup) { request ->
                seenRequests += request
                responder.respondLarge(raw, request, responseData, replyRefs)
            }
            raw.flush()

            val response = client.call(echo, blob(requestData)).await()
            assertTrue(responseData.contentEquals(response.fields<BlobData>().data))

            val request = seenRequests.single()
            assertEquals(0, request.data.length)
            assertNotNull(header(request, ProtocolHeaders.PAYLOAD_REF))
            assertNotNull(header(request, ProtocolHeaders.DEADLINE))

            // The caller deletes the reply object after reading it.
            eventually { replyRefs.size == 1 }
            assertNull(raw.getBlob(ProtocolDefaults.TRANSIENT_PAYLOAD_BUCKET, replyRefs.single()))
        } finally {
            client.close().await()
            raw.close()
        }
    }

    @Test
    fun smallPayloadsStayInline() = e2eTest {
        val raw = rawNats()
        val client = TestNats.client(uniqueName("payload-caller"))
        try {
            raw.subscribe(echoSubject, queue = players.queueGroup) { request ->
                val data = raw.decode(BlobRequestSchema, request.data).fields<BlobData>().data
                raw.respond(request, raw.encode(BlobResponseSchema, blob(data)))
            }
            raw.flush()

            val data = bytesOfSize(1000)
            val response = client.call(echo, blob(data)).await()
            assertTrue(data.contentEquals(response.fields<BlobData>().data))
        } finally {
            client.close().await()
            raw.close()
        }
    }

    @Test
    fun requestsAreOffloadedExactlyAboveTheReservedPayloadSize() = e2eTest {
        val raw = rawNats()
        val client = TestNats.client(uniqueName("payload-caller"))
        try {
            val requests = mutableListOf<Msg>()
            raw.subscribe(echoSubject, queue = players.queueGroup) { request ->
                requests += request
                raw.respond(request, raw.encode(BlobResponseSchema, blob(bytesOfSize(1))))
            }
            raw.flush()

            // Headers at the size check: the base headers and the Surf-Deadline line (13 digits)
            val headerBytes = NatsHeaders.serializedLength(NatsHeaders.forOrigin(client.origin)) +
                    headerLineBytes(ProtocolHeaders.DEADLINE, "0".repeat(13))
            val maxPayload = assertNotNull(raw.connection.info).max_payload.toLong()
            val maxBody = (maxPayload - headerBytes - ProtocolDefaults.PAYLOAD_REF_RESERVE).toInt()
            // BlobRequest.data is the field tag, a 3-byte length varint and the data
            val atThreshold = maxBody - 4

            client.call(echo, blob(bytesOfSize(atThreshold))).await()
            client.call(echo, blob(bytesOfSize(atThreshold + 1))).await()

            val (inline, offloaded) = requests
            assertNull(header(inline, ProtocolHeaders.PAYLOAD_REF))
            assertEquals(maxBody, inline.data.length)
            assertNotNull(header(offloaded, ProtocolHeaders.PAYLOAD_REF))
            assertEquals(0, offloaded.data.length)
        } finally {
            client.close().await()
            raw.close()
        }
    }

    @Test
    fun offloadedPayloadsMustNotOutliveTheBucketTtl() = e2eTest {
        val raw = rawNats()
        val client = TestNats.client(uniqueName("payload-caller"))
        try {
            val requests = mutableListOf<Msg>()
            raw.subscribe(echoSubject, queue = players.queueGroup) { requests += it }
            raw.flush()

            // The transient bucket keeps objects for 10 min; a call waiting longer cannot offload its request
            val error = assertFailsWith<IllegalArgumentException> {
                client.call(echo, blob(bytesOfSize(1_500_000)), callOptions(timeoutMs = 601_000)).await()
            }
            assertTrue(error.message!!.contains("exceeds the TTL of 10m (bucket ${ProtocolDefaults.TRANSIENT_PAYLOAD_BUCKET})"), error.message)

            raw.flush()
            delay(200.milliseconds)
            assertTrue(requests.isEmpty(), "nothing may be published: $requests")
        } finally {
            client.close().await()
            raw.close()
        }
    }

    @Test
    fun payloadBucketsAreCreatedWithTheJvmTtlAndFileStorage() = e2eTest {
        val raw = rawNats()
        val client = TestNats.client(uniqueName("payload-caller"))
        try {
            raw.subscribe(echoSubject, queue = players.queueGroup) { request ->
                raw.respond(request, raw.encode(BlobResponseSchema, blob(bytesOfSize(10))))
            }
            raw.flush()
            val names = ServiceNames("surf.natsjs.test.v1.AuditService")
            raw.addPersistentStreams(names)

            // One offload per bucket; only surf-nats-js creates these buckets on the e2e server
            client.call(echo, blob(bytesOfSize(1_500_000))).await()
            client.call(AuditService.methods.first { it.localName == "record" }, unsafeJso<AuditEntry> {
                action = "big"
                data = bytesOfSize(1_500_000)
            }).await()

            for (bucket in PayloadBucket.entries) {
                val status = raw.bucketStatus(bucket.bucketName)
                assertEquals(bucket.bucketName, status.bucket)
                assertEquals(bucket.ttl.inWholeMilliseconds * 1_000_000.0, status.ttl, "ttl of ${bucket.bucketName}")
                assertEquals("file", status.storage, "storage of ${bucket.bucketName}")
            }
            assertEquals(600_000L, PayloadBucket.TRANSIENT.ttl.inWholeMilliseconds)
            assertEquals(7 * 24 * 3_600_000L, PayloadBucket.PERSISTENT.ttl.inWholeMilliseconds)
        } finally {
            client.close().await()
            raw.close()
        }
    }

    @Test
    fun offloadedAllInstancesAnswersAreResolvedAndDeleted() = e2eTest {
        val raw = rawNats()
        val client = TestNats.client(uniqueName("payload-caller"))
        try {
            val bucket = PayloadBucket.TRANSIENT
            raw.ensureBucket(bucket)
            val refs = mutableListOf<String>()
            val responder = this
            raw.subscribe(players.methodSubject("Locate", persistent = false)) { request ->
                responder.launch {
                    val ref = raw.putBlob(bucket.bucketName, raw.encode(LocateResponseSchema, unsafeJso<LocateResponse> { server = "lobby-7" }))
                    refs += ref
                    val headers = raw.jvmHeaders()
                    headers.set(ProtocolHeaders.PAYLOAD_REF, ref)
                    raw.respond(request, NatsHeaders.EMPTY_BODY, headers)
                }
            }
            raw.flush()

            val locate = PlayerService.methods.first { it.localName == "locate" }
            val answer = client.callAnswer(locate, unsafeJso<FindPlayerRequest> { name = "Steve" }).await()

            assertTrue(answer.known)
            assertEquals("lobby-7", answer.value.fields<LocateResponse>().server)
            // The caller deletes the answer object after reading it
            assertNull(raw.getBlob(bucket.bucketName, refs.single()))
        } finally {
            client.close().await()
            raw.close()
        }
    }

    // Plays the JVM ANY_INSTANCE responder: reads and deletes the request object, offloads the response.
    private fun CoroutineScope.respondLarge(
        raw: RawNats,
        request: Msg,
        data: Uint8Array<*>,
        refs: MutableList<String>,
    ) {
        launch {
            val bucket = ProtocolDefaults.TRANSIENT_PAYLOAD_BUCKET
            val requestRef = assertNotNull(header(request, ProtocolHeaders.PAYLOAD_REF))
            val body = assertNotNull(raw.getBlob(bucket, requestRef))
            assertEquals(1_500_000, raw.decode(BlobRequestSchema, body).fields<BlobData>().data?.length)

            val ref = raw.putBlob(bucket, raw.encode(BlobResponseSchema, blob(data)))
            val headers = raw.jvmHeaders()
            headers.set(ProtocolHeaders.PAYLOAD_REF, ref)
            raw.respond(request, NatsHeaders.EMPTY_BODY, headers)
            refs += ref
        }
    }
}
