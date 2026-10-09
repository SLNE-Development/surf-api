package dev.slne.surf.nats.test.common

import dev.slne.surf.api.core.util.requiredService
import org.jetbrains.annotations.MustBeInvokedByOverriders

abstract class NatsTestInstance {

    @MustBeInvokedByOverriders
    open suspend fun load() {
        NatsService.get().start()
    }

    @MustBeInvokedByOverriders
    open suspend fun enable() {
    }

    @MustBeInvokedByOverriders
    open suspend fun disable() {
        NatsService.get().stop()
    }

    companion object {
        val instance = requiredService<NatsTestInstance>()
        fun get() = instance
    }
}