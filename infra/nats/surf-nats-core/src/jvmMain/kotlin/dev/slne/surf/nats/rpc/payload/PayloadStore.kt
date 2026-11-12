package dev.slne.surf.nats.rpc.payload

import dev.slne.surf.api.common.util.logger
import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.codec.NatsHeaders
import dev.slne.surf.nats.codec.header
import dev.slne.surf.nats.connection.NatsConnectionManager
import dev.slne.surf.nats.protocol.PayloadBucket
import dev.slne.surf.nats.protocol.ProtocolHeaders
import dev.slne.surf.nats.runtime.rpc.offloadPayload
import io.nats.client.Connection
import io.nats.client.JetStreamApiException
import io.nats.client.Message
import io.nats.client.ObjectStore
import io.nats.client.api.ObjectStoreConfiguration
import io.nats.client.api.StorageType
import io.nats.client.impl.Headers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.toJavaDuration

/**
 * Moves message bodies that exceed the max payload of the connection to a JetStream object store and back.
 *
 * Each [PayloadBucket] is created on first use with its TTL and file storage. An existing bucket with a different
 * configuration is used as it is, with a warning. Object store handles are cached per connection instance, so a new
 * connection checks the bucket again. Thread-safe.
 */
@InternalNatsApi
internal object PayloadStore {
    private val log = logger()

    private class Bucket(val connection: Connection, val store: ObjectStore)

    private val mutex = Mutex()
    private val buckets = ConcurrentHashMap<PayloadBucket, Bucket>()

    /**
     * Offloads [body] to [bucket] when the message would exceed the max payload of the connection, and returns the
     * body to send: [body] itself, or an empty body when it was offloaded.
     *
     * Adds `Surf-Payload-Ref` to [headers] when offloading. [extraHeaderBytes] accounts for headers that are added
     * after this call, such as the JetStream publish options. With [stampDeadline], `Surf-Deadline` (now +
     * [lifetime]) is added after a possible upload and counted in the size check.
     *
     * Throws [IllegalArgumentException] when the body must be offloaded and [lifetime] exceeds the TTL of [bucket],
     * and [IllegalStateException] when it must be offloaded and JetStream is disabled on the server. A failing
     * upload propagates.
     */
    suspend fun offload(
        headers: Headers,
        body: ByteArray,
        lifetime: Duration,
        bucket: PayloadBucket,
        extraHeaderBytes: Int = 0,
        stampDeadline: Boolean = false,
    ): ByteArray {
        val connection = NatsConnectionManager.connection()
        val offloaded = offloadPayload(
            bodyBytes = body.size,
            headerBytes = headers.serializedLength(),
            extraHeaderBytes = extraHeaderBytes,
            maxPayload = connection.maxPayload,
            lifetime = lifetime,
            bucket = bucket,
            stampDeadline = stampDeadline,
            writeHeader = headers::add,
            upload = { name ->
                val store = store(connection, bucket)
                withContext(Dispatchers.IO) {
                    store.put(name, body)
                }
            },
        )
        return if (offloaded) NatsHeaders.EMPTY_BODY else body
    }

    /**
     * Returns the body of [message], loaded from [bucket] when the message carries `Surf-Payload-Ref`.
     *
     * With [delete], the object is deleted after reading, and a failed delete is only logged. A failed read
     * propagates, such as the [IllegalArgumentException] of jnats for an object that no longer exists.
     */
    suspend fun resolve(
        message: Message,
        bucket: PayloadBucket,
        delete: Boolean,
    ): ByteArray {
        val name = payloadRef(message) ?: return message.data
        val store = store(NatsConnectionManager.connection(), bucket)

        return withContext(Dispatchers.IO) {
            ByteArrayOutputStream().use { out ->
                store.get(name, out)
                if (delete) {
                    deleteQuietly(store, name)
                }
                out.toByteArray()
            }
        }
    }

    /**
     * Deletes the offloaded payload of [message] from [bucket], if any.
     *
     * Never throws except for the cancellation of the calling coroutine. Other failures are logged, and the object
     * then expires with the TTL of the bucket.
     */
    suspend fun delete(message: Message, bucket: PayloadBucket) {
        val name = payloadRef(message) ?: return
        try {
            val store = store(NatsConnectionManager.connection(), bucket)
            withContext(Dispatchers.IO) {
                deleteQuietly(store, name)
            }
        } catch (e: CancellationException) {
            currentCoroutineContext().ensureActive()
            logDeleteFailure(name, e)
        } catch (e: Exception) {
            logDeleteFailure(name, e)
        }
    }

    fun isOffloaded(message: Message): Boolean = payloadRef(message) != null

    private fun payloadRef(message: Message): String? = message.header(ProtocolHeaders.PAYLOAD_REF)

    private fun deleteQuietly(store: ObjectStore, name: String) {
        try {
            store.delete(name)
        } catch (e: Exception) {
            logDeleteFailure(name, e)
        }
    }

    private fun logDeleteFailure(name: String, e: Exception) {
        log.atWarning()
            .withCause(e)
            .log("Failed to delete offloaded payload %s", name)
    }

    private suspend fun store(connection: Connection, bucket: PayloadBucket): ObjectStore {
        buckets[bucket]?.takeIf { it.connection === connection }?.let { return it.store }

        return mutex.withLock {
            buckets[bucket]?.takeIf { it.connection === connection }?.let { return it.store }

            check(connection.serverInfo.isJetStreamAvailable) {
                PayloadBucket.jetStreamDisabledMessage(connection.maxPayload)
            }

            val store = withContext(Dispatchers.IO) {
                ensureBucket(connection, bucket)
                connection.objectStore(bucket.bucketName)
            }
            buckets[bucket] = Bucket(connection, store)

            store
        }
    }

    private fun ensureBucket(connection: Connection, bucket: PayloadBucket) {
        val config = ObjectStoreConfiguration.builder(bucket.bucketName)
            .ttl(bucket.ttl.toJavaDuration())
            .storageType(StorageType.File)
            .build()

        val management = connection.objectStoreManagement()
        try {
            management.create(config)
        } catch (e: JetStreamApiException) {
            try {
                management.getStatus(bucket.bucketName) // Throws if bucket does not exist
            } catch (missing: Exception) {
                e.addSuppressed(missing)
                throw e
            }

            log.atWarning()
                .log(
                    "Using existing bucket %s with a different configuration: %s",
                    bucket.bucketName,
                    e.message
                )
        }
    }
}