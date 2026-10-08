package dev.slne.surf.nats.rpc.payload

import dev.slne.surf.api.core.util.logger
import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.codec.NatsHeaders
import dev.slne.surf.nats.connection.NatsConnectionManager
import dev.slne.surf.nats.rpc.RpcDefaults
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
import kotlin.time.Duration
import kotlin.time.toJavaDuration

@InternalNatsApi
internal object PayloadStore {
    const val BUCKET = "surf-nats-payloads"
    private const val REF_RESERVE = 128
    private val EMPTY = ByteArray(0)
    private val log = logger()

    private class Bucket(val connection: Connection, val store: ObjectStore)

    private val mutex = Mutex()

    @Volatile
    private var bucket: Bucket? = null

    /**
     * Offloads [body] if larger than the supported may payload and returns the body to send.
     */
    suspend fun offload(headers: Headers, body: ByteArray, timeout: Duration): ByteArray {
        val connection = NatsConnectionManager.connection()
        if (body.size + headers.serializedLength() + REF_RESERVE <= connection.maxPayload) {
            return body
        }

        require(timeout <= RpcDefaults.PAYLOAD_TTL) {
            "Payload of ${body.size} bytes must be offloaded, but timeout $timeout exceeds " +
                    "the payload TTL of ${RpcDefaults.PAYLOAD_TTL}"
        }

        val name = UUID.randomUUID().toString()
        val store = store(connection)

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
    suspend fun resolve(message: Message, delete: Boolean): ByteArray {
        val headers = if (message.hasHeaders()) message.headers else return message.data
        val name = headers.getFirst(NatsHeaders.PAYLOAD_REF) ?: return message.data

        val store = store(NatsConnectionManager.connection())
        return withContext(Dispatchers.IO) {
            ByteArrayOutputStream().use { out ->
                store.get(name, out)
                if (delete) {
                    try {
                        store.delete(name)
                    } catch (e: Exception) {
                        log.atWarning()
                            .withCause(e)
                            .log("Failed to delete offloaded payload %s", name)
                    }
                }
                out.toByteArray()
            }
        }
    }

    private suspend fun store(connection: Connection): ObjectStore {
        val currentBucket = bucket
        if (currentBucket != null && currentBucket.connection === connection) {
            return currentBucket.store
        }

        return mutex.withLock {
            val currentBucket = bucket
            if (currentBucket != null && currentBucket.connection === connection) {
                return currentBucket.store
            }

            check(connection.serverInfo.isJetStreamAvailable) {
                "Payload exceeds the NATS max payload of ${connection.maxPayload} bytes and " +
                        "cannot be offloaded: JetStream is not enabled on the server"
            }

            val newStore = withContext(Dispatchers.IO) {
                ensureBucket(connection)
                connection.objectStore(BUCKET)
            }
            bucket = Bucket(connection, newStore)

            newStore
        }
    }

    private fun ensureBucket(connection: Connection) {
        val config = ObjectStoreConfiguration.builder(BUCKET)
            .ttl(RpcDefaults.PAYLOAD_TTL.toJavaDuration())
            .storageType(StorageType.File)
            .build()

        val management = connection.objectStoreManagement()
        try {
            management.create(config)
        } catch (e: JetStreamApiException) {
            try {
                management.getStatus(BUCKET) // Throws if bucket does not exist
            } catch (missing: Exception) {
                e.addSuppressed(missing)
                throw e
            }

            log.atWarning()
                .log(
                    "Using existing bucket %s with a different configuration: %s",
                    BUCKET,
                    e.message
                )
        }
    }
}