package dev.slne.surf.surfapi.bukkit.test.hook

import dev.slne.surf.surfapi.bukkit.test.hook.condition.EnabledCondition
import dev.slne.surf.surfapi.core.api.hook.AbstractHook
import dev.slne.surf.surfapi.core.api.util.logger
import dev.slne.surf.surfapi.shared.api.hook.HookMeta
import dev.slne.surf.surfapi.shared.api.hook.requirement.ConditionalOnCustom

@ConditionalOnCustom(EnabledCondition::class)
@HookMeta
class PrimaryTestHook : AbstractHook() {
    private val log = logger()

    override suspend fun onBootstrap() {
        log.atInfo().log("PrimaryTestHook bootstrapped")
    }

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