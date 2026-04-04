package dev.slne.surf.surfapi.bukkit.test.hook

import dev.slne.surf.surfapi.core.api.component.AbstractComponent
import dev.slne.surf.surfapi.core.api.util.logger
import dev.slne.surf.surfapi.shared.api.component.ComponentMeta
import dev.slne.surf.surfapi.shared.api.component.SurfComponent
import dev.slne.surf.surfapi.shared.api.component.requirement.ConditionalOnMissingComponent
import dev.slne.surf.surfapi.shared.api.component.requirement.ConditionalOnProductionEnvironment
import dev.slne.surf.surfapi.shared.api.component.requirement.ConditionalOnProperty
import dev.slne.surf.surfapi.shared.api.component.types.Service

@Service
annotation class TestHookMeta

@TestHookMeta
annotation class TestHookMeta2

@TestHookMeta2
@ConditionalOnProductionEnvironment
@ConditionalOnProperty(key = ["test", "property"], havingValue = "true")
class TestHook : AbstractComponent() {
    private val log = logger()

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

@ConditionalOnMissingComponent(TestHook::class)
@ComponentMeta
class MissingTestHook : SurfComponent {
    override suspend fun load() {
        println("TestHook is not enabled. Try setting test.property=true in the plugins properties.yml")
    }
}