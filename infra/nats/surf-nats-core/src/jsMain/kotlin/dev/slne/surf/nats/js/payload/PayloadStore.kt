package dev.slne.surf.nats.js.payload

import dev.slne.surf.nats.js.codec.NatsHeaders
import dev.slne.surf.nats.js.codec.OutgoingHeaders
import dev.slne.surf.nats.js.codec.emptyBody
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

    val maxPayload: Long get() = (connection.info?.max_payload ?: DEFAULT_MAX_PAYLOAD).toLong()

    /**
     * Returns the body to send: [body] itself, or an empty body after uploading it and adding `Surf-Payload-Ref`.
     *
     * [extraHeaderBytes] covers headers that nats.js adds after this call (JetStream publish options). With
     * [stampDeadline], `Surf-Deadline` (now + [lifetime]) is set and counted in the size check.
     */
    suspend fun offload(
        headers: OutgoingHeaders,
        body: Uint8Array<*>,
        lifetime: Duration,
        bucket: PayloadBucket,
        extraHeaderBytes: Int = 0,
        stampDeadline: Boolean = false,
    ): Uint8Array<*> {
        val deadline = if (stampDeadline) deadlineHeader(lifetime) else null
        val deadlineBytes =
            deadline?.let { NatsHeaders.lineSize(ProtocolHeaders.DEADLINE, it) } ?: 0

        val offloadRequired = ProtocolDefaults.requiresOffload(
            bodyBytes = body.length,
            headerBytes = headers.bytes + deadlineBytes,
            extraHeaderBytes = extraHeaderBytes,
            maxPayload = maxPayload,
        )

        if (!offloadRequired) {
            if (deadline != null) {
                headers.set(ProtocolHeaders.DEADLINE, deadline)
            }
            return body
        }

        require(lifetime <= bucket.ttl) {
            bucket.lifetimeExceededMessage(body.length, lifetime)
        }

        val name = upload(bucket, body)

        // Stamped after the upload, which used up part of the lifetime
        if (stampDeadline) {
            headers.set(ProtocolHeaders.DEADLINE, deadlineHeader(lifetime))
        }

        headers.set(ProtocolHeaders.PAYLOAD_REF, name)
        return emptyBody()
    }

    suspend fun resolve(message: Msg, bucket: PayloadBucket, delete: Boolean): Uint8Array<*> {
        val name = NatsHeaders.get(message, ProtocolHeaders.PAYLOAD_REF) ?: return message.data
        val store = store(bucket)
        val data = store.getBlob(name).await()
            ?: error("Offloaded payload $name not found in bucket ${bucket.bucketName}")

        if (delete) {
            deleteQuietly(store, name)
        }

        return data
    }

    private suspend fun upload(bucket: PayloadBucket, body: Uint8Array<*>): String {
        val name = randomUUID()
        store(bucket).putBlob(ObjectStoreMeta(name = name), body).await()
        return name
    }

    private suspend fun deleteQuietly(store: ObjectStore, name: String) {
        try {
            store.delete(name).await()
        } catch (e: Throwable) {
            console.warn("surf-nats: failed to delete offloaded payload $name", e)
        }
    }

    /**
     * A failed creation leaves a cancelled [Deferred], which the next caller replaces.
     */
    private suspend fun store(bucket: PayloadBucket): ObjectStore {
        val existing = stores[bucket]?.takeUnless { it.isCancelled }
        val store = existing ?: createStore(bucket).also { stores[bucket] = it }
        return store.await()
    }

    private fun createStore(bucket: PayloadBucket): Deferred<ObjectStore> {
        check(connection.info?.jetstream == true) {
            PayloadBucket.jetStreamDisabledMessage(maxPayload)
        }

        val options = ObjectStoreOptions(
            ttl = bucket.ttl.inWholeNanoseconds.toDouble(),
            storage = "file",
        )

        return scope.async { Objm(connection).create(bucket.bucketName, options).await() }
    }

    private companion object {
        const val DEFAULT_MAX_PAYLOAD = 1024 * 1024
    }
}
