package dev.slne.surf.nats.test.common

import dev.slne.surf.api.core.util.requiredService
import dev.slne.surf.nats.NatsClient
import org.jetbrains.annotations.MustBeInvokedByOverriders

abstract class NatsService {

    val client = NatsClient.create("surf-nats-test")

    @MustBeInvokedByOverriders
    open suspend fun start() {

    }

    fun stop() {
        client.close()
    }

    companion object {
        val instance = requiredService<NatsService>()
        fun get() = instance
    }
}

val natsClient get() = NatsService.get().client