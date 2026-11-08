package dev.slne.surf.surfapi.bukkit.test.hook

import dev.slne.surf.api.core.component.AbstractComponent
import dev.slne.surf.api.core.util.logger
import dev.slne.surf.api.shared.api.component.SurfComponentMeta
import dev.slne.surf.api.shared.api.component.requirement.ConditionalOn
import dev.slne.surf.surfapi.bukkit.test.hook.condition.EnabledCondition

@ConditionalOn(EnabledCondition::class)
@SurfComponentMeta
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