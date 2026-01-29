package dev.slne.surf.surfapi.bukkit.test.component

import dev.slne.surf.surfapi.bukkit.test.component.condition.EnabledCondition
import dev.slne.surf.surfapi.core.api.component.AbstractComponent
import dev.slne.surf.surfapi.core.api.util.logger
import dev.slne.surf.surfapi.shared.api.component.ComponentMeta
import dev.slne.surf.surfapi.shared.api.component.requirement.ConditionalOn

@ConditionalOn(EnabledCondition::class)
@ComponentMeta
class PrimaryTestComponent : AbstractComponent() {
    private val log = logger()

    override suspend fun onBootstrap() {
        log.atInfo().log("PrimaryTestComponent bootstrapped")
    }

    override suspend fun onLoad() {
        log.atInfo().log("PrimaryTestComponent loaded")
    }

    override suspend fun onEnable() {
        log.atInfo().log("PrimaryTestComponent enabled")
    }

    override suspend fun onDisable() {
        log.atInfo().log("PrimaryTestComponent disabled")
    }
}
