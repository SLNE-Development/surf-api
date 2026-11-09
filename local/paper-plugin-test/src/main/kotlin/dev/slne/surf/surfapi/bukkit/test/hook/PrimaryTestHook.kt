package dev.slne.surf.surfapi.bukkit.test.hook

import dev.slne.surf.api.core.util.logger
import dev.slne.surf.api.di.Service
import dev.slne.surf.api.di.SurfComponent
import dev.slne.surf.api.di.condition.ConditionalOn
import dev.slne.surf.surfapi.bukkit.test.hook.condition.EnabledCondition

/** A bean whose existence a condition of this plugin's own decides. */
@Service
@ConditionalOn(EnabledCondition::class)
class PrimaryTestHook : SurfComponent {
    private val log = logger()

    override suspend fun load() {
        log.atInfo().log("PrimaryTestHook loaded")
    }

    override suspend fun enable() {
        log.atInfo().log("PrimaryTestHook enabled")
    }

    override suspend fun disable() {
        log.atInfo().log("PrimaryTestHook disabled")
    }
}
