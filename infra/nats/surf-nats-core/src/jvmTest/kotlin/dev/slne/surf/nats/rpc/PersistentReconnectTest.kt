package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.NatsClient
import dev.slne.surf.nats.NatsInstance
import dev.slne.surf.nats.TestNatsServer
import dev.slne.surf.nats.client.NatsClientImpl
import dev.slne.surf.nats.connection.NatsConnectionManager
import dev.slne.surf.nats.eventually
import dev.slne.surf.nats.jetstream.JetStreamProvisioner
import dev.slne.surf.nats.jsm
import dev.slne.surf.nats.registerService
import dev.slne.surf.nats.service
import dev.slne.surf.nats.test.proto.v1.OrderService
import dev.slne.surf.nats.test.proto.v1.PlaceRequest
import io.nats.client.Connection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class PersistentReconnectTest {

    @Test
    fun `persistent message published after a server restart is processed promptly`() = runBlocking {
        NatsInstance.instance.load()
        val client = NatsClient.create("persistent-reconnect") as NatsClientImpl
        val streams = client.serviceDescriptor(OrderService::class.java).streams!!
        try {
            val impl = NatsPersistentServiceTest.OrderImpl()
            client.registerService<OrderService>(impl)
            val orders = client.service<OrderService>()

            orders.place(PlaceRequest(1))
            assertEquals(1, withTimeoutOrNull(10.seconds) { impl.deliveries.receive() }?.id)

            restartServerAndAwaitReconnect()

            val published = TimeSource.Monotonic.markNow()
            orders.place(PlaceRequest(2))
            val delivery = withTimeoutOrNull(PROCESSING_BOUND) { impl.deliveries.receive() }
            assertEquals(
                2,
                delivery?.id,
                "persistent message was not processed within $PROCESSING_BOUND after the reconnect " +
                        "(waited ${published.elapsedNow()})",
            )
        } finally {
            client.close()
            for (stream in listOf(streams.stream, streams.dlqStream)) runCatching { jsm().deleteStream(stream) }
            JetStreamProvisioner.invalidate(streams)
            NatsInstance.instance.disable()
        }
    }

    private suspend fun restartServerAndAwaitReconnect() {
        val connection = NatsConnectionManager.connection()
        val reconnectsBefore = connection.statistics.reconnects
        withContext(Dispatchers.IO) { TestNatsServer.restart() }
        eventually("reconnect after the server restart", timeout = 30.seconds) {
            connection.status == Connection.Status.CONNECTED &&
                    connection.statistics.reconnects > reconnectsBefore
        }
    }

    private companion object {
        val PROCESSING_BOUND = 10.seconds
    }
}
