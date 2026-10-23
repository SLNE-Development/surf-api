package dev.slne.surf.nats.rpc.payload

import dev.slne.surf.api.core.util.logger
import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.codec.NatsHeaders
import dev.slne.surf.nats.codec.header
import dev.slne.surf.nats.connection.NatsConnectionManager
import dev.slne.surf.nats.protocol.PayloadBucket
import dev.slne.surf.nats.protocol.ProtocolDefaults
import dev.slne.surf.nats.protocol.ProtocolHeaders
import dev.slne.surf.nats.runtime.deadlineHeader
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
import kotlin.coroutines.cancellation.CancellationException
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.toJavaDuration

@InternalNatsApi
internal object PayloadStore {
    private val log = logger()

    private class Bucket(val connection: Connection, val store: ObjectStore)

    private val mutex = Mutex()
    private val buckets = ConcurrentHashMap<PayloadBucket, Bucket>()

    /**
     * Offloads [body] if larger than the supported max payload and returns the body to send.
     *
     * [extraHeaderBytes] accounts for headers that are added after this call, such as the JetStream publish options.
     * With [stampDeadline], `Surf-Deadline` (now + [lifetime]) is added after a possible upload and counted in the
     * size check.
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
        val deadline = if (stampDeadline) deadlineHeader(lifetime) else null
        val headerBytes = headers.serializedLength() + NatsHeaders.lineBytes(ProtocolHeaders.DEADLINE, deadline)
        if (!ProtocolDefaults.requiresOffload(body.size, headerBytes, extraHeaderBytes, connection.maxPayload)) {
            if (deadline != null) headers.add(ProtocolHeaders.DEADLINE, deadline)
            return body
        }

        require(lifetime <= bucket.ttl) { bucket.lifetimeExceededMessage(body.size, lifetime) }

        val name = UUID.randomUUID().toString()
        val store = store(connection, bucket)

        withContext(Dispatchers.IO) {
            store.put(name, body)
        }
        // The upload used up part of the lifetime
        if (stampDeadline) headers.add(ProtocolHeaders.DEADLINE, deadlineHeader(lifetime))
        headers.add(ProtocolHeaders.PAYLOAD_REF, name)

        return NatsHeaders.EMPTY_BODY
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