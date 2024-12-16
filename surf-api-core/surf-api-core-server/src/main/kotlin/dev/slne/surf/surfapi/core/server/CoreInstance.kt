package dev.slne.surf.surfapi.core.server

import dev.slne.surf.surfapi.core.api.messages.Colors
import dev.slne.surf.surfapi.core.server.listener.CoreListenerManager
import dev.slne.surf.surfapi.core.server.util.PlayerSkinFetcher
import org.jetbrains.annotations.MustBeInvokedByOverriders

abstract class CoreInstance {

    @MustBeInvokedByOverriders
    open suspend fun bootstrap() {
        initObjects()
    }

    @MustBeInvokedByOverriders
    open suspend fun onLoad() {
    }

    @MustBeInvokedByOverriders
    open suspend fun onEnable() {
        CoreListenerManager.registerListeners()
    }

    @MustBeInvokedByOverriders
    open suspend fun onDisable() {
        CoreListenerManager.unregisterListeners()
    }

    private fun initObjects() {
        PlayerSkinFetcher
        Colors
    }
}