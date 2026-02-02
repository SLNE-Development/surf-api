package dev.slne.surf.surfapi.bukkit.test.hook

import dev.slne.surf.surfapi.core.api.component.AbstractComponent
import dev.slne.surf.surfapi.core.api.util.logger
import dev.slne.surf.surfapi.shared.api.component.ComponentMeta

@ComponentMeta
//@DependsOnClass(BukkitPluginMain::class)
//@DependsOnClassName("dev.slne.surf.surfapi.bukkit.test.config.ModernTestConfig")
//@DependsOnPlugin("SurfBukkitPluginTest")
//@DependsOnOnePlugin(["SurfBukkitPlugin", "surf-bukkit-plugin", "SurfBukkitPluginTest"])
//@DependsOnComponent(PrimaryTestHook::class)
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