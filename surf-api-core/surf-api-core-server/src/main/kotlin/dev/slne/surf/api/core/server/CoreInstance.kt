package dev.slne.surf.api.core.server

import dev.slne.surf.api.core.actionbar.ActionbarService
import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.server.listener.CoreListenerManager
import dev.slne.surf.api.core.server.util.PlayerSkinFetcher
import org.jetbrains.annotations.MustBeInvokedByOverriders
import java.util.concurrent.atomic.AtomicBoolean

abstract class CoreInstance {
    private val bootstrapping = AtomicBoolean(true)

    @MustBeInvokedByOverriders
    open suspend fun bootstrap() {
        initObjects()
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
        ActionbarService.cancelAll()
    }

    private fun initObjects() {
        PlayerSkinFetcher
        Colors
    }

    fun isBootstrapping(): Boolean = bootstrapping.get()
}