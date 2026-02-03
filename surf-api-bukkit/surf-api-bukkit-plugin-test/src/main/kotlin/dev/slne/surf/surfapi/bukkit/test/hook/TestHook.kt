package dev.slne.surf.surfapi.bukkit.test.hook

import dev.slne.surf.surfapi.core.api.component.AbstractComponent
import dev.slne.surf.surfapi.core.api.util.logger
import dev.slne.surf.surfapi.shared.api.component.types.Service

@Service
annotation class TestHookMeta

@TestHookMeta
annotation class TestHookMeta2

@TestHookMeta2
class TestHook : AbstractComponent() {
    private val log = logger()

    override suspend fun onBootstrap() {
        log.atInfo().log("TestHook bootstrapped0")
    }

    override suspend fun onLoad() {
        log.atInfo().log("TestHook loaded")
    }

    override suspend fun onEnable() {
        log.atInfo().log("TestHook enabled")
    }

    override suspend fun onDisable() {
        log.atInfo().log("TestHook disabled")
    }
}