package dev.slne.surf.nats.jetstream

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.connection.NatsConnectionManager
import dev.slne.surf.nats.protocol.ServiceNames
import dev.slne.surf.nats.protocol.ProtocolDefaults
import dev.slne.surf.nats.rpc.RpcDefaults
import dev.slne.surf.nats.runtime.RuntimeDefaults
import io.nats.client.Connection
import io.nats.client.JetStreamApiException
import io.nats.client.JetStreamManagement
import io.nats.client.api.AckPolicy
import io.nats.client.api.ConsumerConfiguration
import io.nats.client.api.RetentionPolicy
import io.nats.client.api.StorageType
import io.nats.client.api.StreamConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.toJavaDuration

@InternalNatsApi
internal object JetStreamProvisioner {
    private const val STREAM_CONFIG_MISMATCH = 10058
    private val mutex = Mutex()
    private val provisioned = ConcurrentHashMap<String, Connection>()

    /**
     * Creates or updates the work-queue and dead-letter streams of [streams] once per connection.
     *
     * Throws [IllegalStateException] when JetStream is disabled or a stream of that name exists for other subjects.
     */
    suspend fun ensureStreams(streams: ServiceNames) {
        val connection = NatsConnectionManager.connection()
        if (isProvisioned(streams, connection)) return

        mutex.withLock {
            if (isProvisioned(streams, connection)) return
            check(connection.serverInfo.isJetStreamAvailable) {
                "Persistent service methods require JetStream to be enabled on the NATS server"
            }

            withContext(Dispatchers.IO) {
                val jetStreamManagement = connection.jetStreamManagement()
                addOrUpdate(
                    jetStreamManagement, StreamConfiguration.builder()
                        .name(streams.stream)
                        .subjects(streams.streamFilter)
                        .retentionPolicy(RetentionPolicy.WorkQueue)
                        .storageType(StorageType.File)
                        .allowMessageTtl()
                        .maxAge(ProtocolDefaults.PERSIST_MAX_AGE.toJavaDuration())
                        .duplicateWindow(RuntimeDefaults.dedupeWindow.toJavaDuration())
                        .build()
                )
                addOrUpdate(
                    jetStreamManagement, StreamConfiguration.builder()
                        .name(streams.dlqStream)
                        .subjects(streams.dlqSubject)
                        .retentionPolicy(RetentionPolicy.Limits)
                        .storageType(StorageType.File)
                        .maxAge(ProtocolDefaults.PERSIST_MAX_AGE.toJavaDuration())
                        .build()
                )
            }
            provisioned[streams.stream] = connection
        }
    }

    /**
     * Forgets that the streams of [streams] were provisioned, so the next [ensureStreams] creates them again after
     * they were deleted on the server.
     *
     * Does not take the provisioning lock: an [ensureStreams] running at the same time may record the streams again.
     */
    @TestOnly
    fun invalidate(streams: ServiceNames) {
        provisioned.remove(streams.stream)
    }

    private fun isProvisioned(streams: ServiceNames, connection: Connection): Boolean {
        return provisioned[streams.stream] === connection
    }

    private fun addOrUpdate(jetStreamManagement: JetStreamManagement, config: StreamConfiguration) {
        try {
            jetStreamManagement.addStream(config)
        } catch (e: JetStreamApiException) {
            if (e.apiErrorCode != STREAM_CONFIG_MISMATCH) throw e

            val existing = jetStreamManagement.getStreamInfo(config.name).configuration.subjects
            check(existing.toSet() == config.subjects.toSet()) {
                collisionMessage(config.name, existing, config.subjects)
            }
            jetStreamManagement.updateStream(config)
        }
    }

    private fun collisionMessage(stream: String, existing: List<String>, requested: List<String>): String {
        return "Stream $stream already exists for subjects $existing and cannot be used for $requested. " +
                "It was created by another tool or with an older subject filter; delete it to let surf-nats recreate it."
    }

    fun consumerConfig(durable: String): ConsumerConfiguration = ConsumerConfiguration.builder()
        .durable(durable)
        .ackPolicy(AckPolicy.Explicit)
        .ackWait(RpcDefaults.ACK_WAIT.toJavaDuration())
        .maxDeliver(RpcDefaults.MAX_DELIVER.toLong())
        .maxAckPending(256)
        .build()
}