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
    private val provisioned = ConcurrentHashMap<String, Connection>()

    suspend fun ensureStreams(streams: PersistentStreams) {
        val connection = NatsConnectionManager.connection()
        if (provisioned[streams.stream] === connection) return

        mutex.withLock {
            if (provisioned[streams.stream] === connection) return
            check(connection.serverInfo.isJetStreamAvailable) {
                "@Persistent requires JetStream to be enabled on the NATS server"
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
            provisioned[streams.stream] = connection
        }
    }

    private fun addOrUpdate(jetStreamManagement: JetStreamManagement, config: StreamConfiguration) {
        try {
            jetStreamManagement.addStream(config)
        } catch (e: JetStreamApiException) {
            if (e.apiErrorCode != STREAM_CONFIG_MISMATCH) throw e
            jetStreamManagement.updateStream(config)
        }
    }

    fun consumerConfig(durable: String): ConsumerConfiguration = ConsumerConfiguration.builder()
        .durable(durable)
        .ackPolicy(AckPolicy.Explicit)
        .ackWait(RpcDefaults.ACK_WAIT.toJavaDuration())
        .maxDeliver(RpcDefaults.MAX_DELIVER.toLong())
        .maxAckPending(256)
        .build()
}