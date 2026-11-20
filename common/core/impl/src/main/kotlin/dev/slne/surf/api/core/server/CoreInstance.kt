package dev.slne.surf.api.core.server

import dev.slne.surf.api.core.server.listener.CoreListenerManager
import org.jetbrains.annotations.MustBeInvokedByOverriders
import java.util.concurrent.atomic.AtomicBoolean

abstract class CoreInstance {
    private val bootstrapping = AtomicBoolean(true)

    @MustBeInvokedByOverriders
    open suspend fun bootstrap() {
    }

    @MustBeInvokedByOverriders
    open suspend fun onLoad() {
        bootstrapping.set(false)
    }

    @MustBeInvokedByOverriders
    open suspend fun onEnable() {
        CoreListenerManager.registerListeners()
    }

    @MustBeInvokedByOverriders
    open suspend fun onDisable() {
        CoreListenerManager.unregisterListeners()
    }

    fun isBootstrapping(): Boolean = bootstrapping.get()
}