package dev.slne.surf.nats.rpc.payload

import dev.slne.surf.api.core.util.logger
import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.codec.NatsHeaders
import dev.slne.surf.nats.connection.NatsConnectionManager
import io.nats.client.Connection
import io.nats.client.JetStreamApiException
import io.nats.client.Message
import io.nats.client.ObjectStore
import io.nats.client.api.ObjectStoreConfiguration
import io.nats.client.api.StorageType
import io.nats.client.impl.Headers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.toJavaDuration

@InternalNatsApi
internal object PayloadStore {
    private const val REF_RESERVE = 128
    private val EMPTY = ByteArray(0)
    private val log = logger()

    private class Bucket(val connection: Connection, val store: ObjectStore)

    private val mutex = Mutex()
    private val buckets = ConcurrentHashMap<PayloadBucket, Bucket>()

    /**
     * Offloads [body] if larger than the supported max payload and returns the body to send.
     */
    suspend fun offload(
        headers: Headers,
        body: ByteArray,
        lifetime: Duration,
        bucket: PayloadBucket
    ): ByteArray {
        val connection = NatsConnectionManager.connection()
        if (body.size + headers.serializedLength() + REF_RESERVE <= connection.maxPayload) {
            return body
        }

        require(lifetime <= bucket.ttl) {
            "Payload of ${body.size} bytes must be offloaded, but its lifetime $lifetime exceeds " +
                    "the TTL of ${bucket.ttl} (${bucket.ttl})"
        }

        val name = UUID.randomUUID().toString()
        val store = store(connection, bucket)

        withContext(Dispatchers.IO) {
            store.put(name, body)
        }
        headers.add(NatsHeaders.PAYLOAD_REF, name)

        return EMPTY
    }

    /**
     * Resolves the body from [message] which may be loaded from the object store.
     * Optionally deletes the payload from the object store.
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

    suspend fun delete(message: Message, bucket: PayloadBucket) {
        val name = payloadRef(message) ?: return
        val store = store(NatsConnectionManager.connection(), bucket)
        withContext(Dispatchers.IO) {
            deleteQuietly(store, name)
        }
    }

    private fun payloadRef(message: Message): String? {
        return if (message.hasHeaders()) message.headers.getFirst(NatsHeaders.PAYLOAD_REF) else null
    }

    private fun deleteQuietly(store: ObjectStore, name: String) {
        try {
            store.delete(name)
        } catch (e: Exception) {
            log.atWarning()
                .withCause(e)
                .log("Failed to delete offloaded payload %s", name)
        }
    }

    private suspend fun store(connection: Connection, bucket: PayloadBucket): ObjectStore {
        buckets[bucket]?.takeIf { it.connection === connection }?.let { return it.store }

        return mutex.withLock {
            buckets[bucket]?.takeIf { it.connection === connection }?.let { return it.store }

            check(connection.serverInfo.isJetStreamAvailable) {
                "Payload exceeds the NATS max payload of ${connection.maxPayload} bytes and " +
                        "cannot be offloaded: JetStream is not enabled on the server"
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