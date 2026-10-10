package dev.slne.surf.nats.jetstream

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.connection.NatsConnectionManager
import dev.slne.surf.nats.rpc.RpcDefaults
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
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.toJavaDuration

@InternalNatsApi
internal object JetStreamProvisioner {
    private const val STREAM_CONFIG_MISMATCH = 10058
    private val mutex = Mutex()
    private val provisioned = ConcurrentHashMap<String, Provisioned>()

    private class Provisioned(val connection: Connection, val filter: String)

    suspend fun ensureStreams(streams: PersistentStreams) {
        val connection = NatsConnectionManager.connection()
        if (isProvisioned(streams, connection)) return

        mutex.withLock {
            if (isProvisioned(streams, connection)) return
            check(connection.serverInfo.isJetStreamAvailable) {
                "@NatsPersistent requires JetStream to be enabled on the NATS server"
            }

            withContext(Dispatchers.IO) {
                val jetStreamManagement = connection.jetStreamManagement()
                addOrUpdate(
                    jetStreamManagement, StreamConfiguration.builder()
                        .name(streams.stream)
                        .subjects(streams.filter)
                        .retentionPolicy(RetentionPolicy.WorkQueue)
                        .storageType(StorageType.File)
                        .allowMessageTtl()
                        .maxAge(RpcDefaults.PERSIST_MAX_AGE.toJavaDuration())
                        .duplicateWindow(Duration.ofMinutes(2))
                        .build()
                )
                addOrUpdate(
                    jetStreamManagement, StreamConfiguration.builder()
                        .name(streams.dlqStream)
                        .subjects(streams.dlqSubject)
                        .retentionPolicy(RetentionPolicy.Limits)
                        .storageType(StorageType.File)
                        .maxAge(RpcDefaults.PERSIST_MAX_AGE.toJavaDuration())
                        .build()
                )
            }
            provisioned[streams.stream] = Provisioned(connection, streams.filter)
        }
    }

    private fun isProvisioned(streams: PersistentStreams, connection: Connection): Boolean {
        val entry = provisioned[streams.stream] ?: return false
        if (entry.connection !== connection) return false
        check(entry.filter == streams.filter) {
            collisionMessage(streams.stream, listOf(entry.filter), listOf(streams.filter))
        }
        return true
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
                "Two services map to the same stream name. Rename one of them."
    }

    fun consumerConfig(durable: String): ConsumerConfiguration = ConsumerConfiguration.builder()
        .durable(durable)
        .ackPolicy(AckPolicy.Explicit)
        .ackWait(RpcDefaults.ACK_WAIT.toJavaDuration())
        .maxDeliver(RpcDefaults.MAX_DELIVER.toLong())
        .maxAckPending(256)
        .build()
}