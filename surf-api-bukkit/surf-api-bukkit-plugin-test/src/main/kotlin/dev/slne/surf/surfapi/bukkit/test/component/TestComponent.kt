package dev.slne.surf.surfapi.bukkit.test.component

import dev.slne.surf.surfapi.bukkit.test.BukkitPluginMain
import dev.slne.surf.surfapi.core.api.component.AbstractComponent
import dev.slne.surf.surfapi.core.api.util.logger
import dev.slne.surf.surfapi.shared.api.component.ComponentMeta
import dev.slne.surf.surfapi.shared.api.component.requirement.*

@ComponentMeta
@DependsOnClass(BukkitPluginMain::class)
@DependsOnClassName("dev.slne.surf.surfapi.bukkit.test.config.ModernTestConfig")
@DependsOnPlugin("SurfBukkitPluginTest")
@DependsOnOnePlugin(["SurfBukkitPlugin", "surf-bukkit-plugin", "SurfBukkitPluginTest"])
@DependsOnComponent(PrimaryTestComponent::class)
class TestComponent : AbstractComponent() {
    private val log = logger()

    override suspend fun onBootstrap() {
        log.atInfo().log("TestComponent bootstrapped")
    }

    override suspend fun onLoad() {
        log.atInfo().log("TestComponent loaded")
    }

    override suspend fun onEnable() {
        log.atInfo().log("TestComponent enabled")
    }

    override suspend fun onDisable() {
        log.atInfo().log("TestComponent disabled")
    }
}
