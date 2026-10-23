package dev.slne.surf.nats.js

import dev.slne.surf.nats.js.external.nats.core.Msg
import dev.slne.surf.nats.js.external.nats.core.NatsConnection
import dev.slne.surf.nats.js.external.nats.obj.ObjectStore
import dev.slne.surf.nats.js.external.nats.obj.ObjectStoreMeta
import dev.slne.surf.nats.js.external.nats.obj.ObjectStoreOptions
import dev.slne.surf.nats.js.external.nats.obj.Objm
import dev.slne.surf.nats.protocol.PayloadBucket
import dev.slne.surf.nats.protocol.ProtocolDefaults
import dev.slne.surf.nats.protocol.ProtocolHeaders
import dev.slne.surf.nats.runtime.deadlineHeader
import js.objects.unsafeJso
import js.typedarrays.Uint8Array
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.await
import node.crypto.randomUUID
import kotlin.time.Duration

/**
 * Offloads bodies above the server max payload to the payload buckets and resolves `Surf-Payload-Ref` bodies.
 *
 * A missing bucket is created with the TTL and file storage of the JVM runtime; an existing one is used as is.
 */
internal class PayloadStore(
    private val connection: NatsConnection,
    private val scope: CoroutineScope,
) {
    private val stores = mutableMapOf<PayloadBucket, Deferred<ObjectStore>>()

    /**
     * Returns the body to send: [body] itself, or an empty body after uploading it and adding `Surf-Payload-Ref`.
     *
     * [extraHeaderBytes] covers headers that nats.js adds after this call (JetStream publish options). With
     * [stampDeadline], `Surf-Deadline` (now + [lifetime]) is set after a possible upload and counted in the size check.
     */
    suspend fun offload(
        headers: OutgoingHeaders,
        body: Uint8Array<*>,
        lifetime: Duration,
        bucket: PayloadBucket,
        extraHeaderBytes: Int = 0,
        stampDeadline: Boolean = false,
    ): Uint8Array<*> {
        val maxPayload = maxPayload()
        val deadline = if (stampDeadline) deadlineHeader(lifetime) else null
        val headerBytes = headers.bytes +
                if (deadline == null) 0 else Wire.headerLineBytes(ProtocolHeaders.DEADLINE, deadline)
        if (!ProtocolDefaults.requiresOffload(body.length, headerBytes, extraHeaderBytes, maxPayload)) {
            if (deadline != null) headers.set(ProtocolHeaders.DEADLINE, deadline)
            return body
        }

        require(lifetime <= bucket.ttl) { bucket.lifetimeExceededMessage(body.length, lifetime) }

        val name = randomUUID()
        store(bucket).putBlob(unsafeJso<ObjectStoreMeta> { this.name = name }, body).await()
        // The upload used up part of the lifetime
        if (stampDeadline) headers.set(ProtocolHeaders.DEADLINE, deadlineHeader(lifetime))
        headers.set(ProtocolHeaders.PAYLOAD_REF, name)
        return Wire.emptyBody()
    }

    suspend fun resolve(message: Msg, bucket: PayloadBucket, delete: Boolean): Uint8Array<*> {
        val name = Wire.header(message, ProtocolHeaders.PAYLOAD_REF) ?: return message.data
        val store = store(bucket)
        val data = store.getBlob(name).await()
            ?: throw IllegalStateException("Offloaded payload $name not found in bucket ${bucket.bucketName}")
        if (delete) {
            try {
                store.delete(name).await()
            } catch (e: Throwable) {
                console.warn("surf-nats: failed to delete offloaded payload $name", e)
            }
        }
        return data
    }

    fun maxPayload(): Long = (connection.info?.max_payload ?: DEFAULT_MAX_PAYLOAD).toLong()

    private suspend fun store(bucket: PayloadBucket): ObjectStore {
        val existing = stores[bucket]
        // A failed creation (a cancelled Deferred) is retried by the next caller.
        if (existing != null && !existing.isCancelled) return existing.await()

        check(connection.info?.jetstream == true) { PayloadBucket.jetStreamDisabledMessage(maxPayload()) }

        val created = scope.async {
            val options = unsafeJso<ObjectStoreOptions> {
                ttl = bucket.ttl.inWholeNanoseconds.toDouble()
                storage = "file"
            }
            Objm(connection).create(bucket.bucketName, options).await()
        }
        stores[bucket] = created
        return created.await()
    }

    private companion object {
        const val DEFAULT_MAX_PAYLOAD = 1024 * 1024
    }
}
