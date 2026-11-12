package dev.slne.surf.nats.js.rpc.payload

import dev.slne.surf.nats.js.codec.NatsHeaders
import dev.slne.surf.nats.js.codec.header
import dev.slne.surf.nats.js.external.nats.core.Msg
import dev.slne.surf.nats.js.external.nats.core.MsgHdrs
import dev.slne.surf.nats.js.external.nats.core.NatsConnection
import dev.slne.surf.nats.js.external.nats.obj.ObjectStore
import dev.slne.surf.nats.js.external.nats.obj.ObjectStoreMeta
import dev.slne.surf.nats.js.external.nats.obj.ObjectStoreOptions
import dev.slne.surf.nats.js.external.nats.obj.Objm
import dev.slne.surf.nats.protocol.PayloadBucket
import dev.slne.surf.nats.protocol.ProtocolHeaders
import dev.slne.surf.nats.runtime.rpc.offloadPayload
import js.typedarrays.Uint8Array
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.await
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.time.Duration

/**
 * Offloads bodies above the server max payload to the payload buckets and resolves `Surf-Payload-Ref` bodies.
 *
 * A missing bucket is created with the TTL and file storage of the JVM runtime. An existing one is used as is.
 */
internal class PayloadStore(
    private val connection: NatsConnection,
    private val scope: CoroutineScope,
) {
    private val stores = mutableMapOf<PayloadBucket, Deferred<ObjectStore>>()

    val maxPayload: Long get() = (connection.info?.max_payload ?: DEFAULT_MAX_PAYLOAD).toLong()

    /**
     * Offloads [body] if larger than the supported max payload and returns the body to send.
     *
     * [extraHeaderBytes] accounts for headers that are added after this call, such as the JetStream publish options.
     * With [stampDeadline], `Surf-Deadline` (now + [lifetime]) is added after a possible upload and counted in the
     * size check.
     */
    suspend fun offload(
        headers: MsgHdrs,
        body: Uint8Array<*>,
        lifetime: Duration,
        bucket: PayloadBucket,
        extraHeaderBytes: Int = 0,
        stampDeadline: Boolean = false,
    ): Uint8Array<*> {
        val offloaded = offloadPayload(
            bodyBytes = body.length,
            headerBytes = NatsHeaders.serializedLength(headers),
            extraHeaderBytes = extraHeaderBytes,
            maxPayload = maxPayload,
            lifetime = lifetime,
            bucket = bucket,
            stampDeadline = stampDeadline,
            writeHeader = headers::set,
            upload = { name -> store(bucket).putBlob(ObjectStoreMeta(name = name), body).await() },
        )
        return if (offloaded) NatsHeaders.EMPTY_BODY else body
    }

    /**
     * Returns the body of [message], loaded from [bucket] when the message carries `Surf-Payload-Ref`.
     *
     * With [delete], removes the object afterwards. A failed deletion is only logged.
     */
    suspend fun resolve(message: Msg, bucket: PayloadBucket, delete: Boolean): Uint8Array<*> {
        val name = message.header(ProtocolHeaders.PAYLOAD_REF) ?: return message.data
        val store = store(bucket)
        val data = store.getBlob(name).await()
            ?: error("Offloaded payload $name not found in bucket ${bucket.bucketName}")

        if (delete) {
            deleteQuietly(store, name)
        }

        return data
    }

    private suspend fun deleteQuietly(store: ObjectStore, name: String) {
        try {
            store.delete(name).await()
        } catch (e: Throwable) {
            if (e is CancellationException) {
                currentCoroutineContext().ensureActive()
            }
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
