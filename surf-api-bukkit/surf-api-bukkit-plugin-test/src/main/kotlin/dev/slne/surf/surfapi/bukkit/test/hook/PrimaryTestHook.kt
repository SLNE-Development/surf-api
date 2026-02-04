package dev.slne.surf.surfapi.bukkit.test.hook

import dev.slne.surf.surfapi.bukkit.test.hook.condition.EnabledCondition
import dev.slne.surf.surfapi.core.api.component.AbstractComponent
import dev.slne.surf.surfapi.core.api.util.logger
import dev.slne.surf.surfapi.shared.api.component.ComponentMeta
import dev.slne.surf.surfapi.shared.api.component.requirement.ConditionalOn

@ConditionalOn(EnabledCondition::class)
@ComponentMeta
class PrimaryTestHook : AbstractComponent() {
    private val log = logger()

    override suspend fun onLoad() {
        log.atInfo().log("PrimaryTestHook loaded")
    }

    override suspend fun onEnable() {
        log.atInfo().log("PrimaryTestHook enabled")
    }

    override suspend fun onDisable() {
        log.atInfo().log("PrimaryTestHook disabled")
    }
}