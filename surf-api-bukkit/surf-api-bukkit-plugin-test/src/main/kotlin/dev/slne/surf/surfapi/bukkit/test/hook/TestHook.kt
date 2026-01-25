package dev.slne.surf.surfapi.bukkit.test.hook

import dev.slne.surf.surfapi.bukkit.test.BukkitPluginMain
import dev.slne.surf.surfapi.core.api.hook.AbstractHook
import dev.slne.surf.surfapi.core.api.util.logger
import dev.slne.surf.surfapi.shared.api.hook.HookMeta
import dev.slne.surf.surfapi.shared.api.hook.requirement.*

@HookMeta
@DependsOnClass(BukkitPluginMain::class)
@DependsOnClassName("dev.slne.surf.surfapi.bukkit.test.config.ModernTestConfig")
@DependsOnPlugin("SurfBukkitPluginTest")
@DependsOnOnePlugin(["SurfBukkitPlugin", "surf-bukkit-plugin", "SurfBukkitPluginTest"])
@DependsOnHook(PrimaryTestHook::class)
class TestHook : AbstractHook() {
    private val log = logger()

    override suspend fun onBootstrap() {
        log.atInfo().log("TestHook bootstrapped")
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