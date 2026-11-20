package dev.slne.surf.surfapi.bukkit.test.hook

import dev.slne.surf.api.common.util.logger
import dev.slne.surf.api.spring.SurfLifecycle
import dev.slne.surf.surfapi.bukkit.test.hook.condition.EnabledCondition
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Service

/**
 * A hook guarded by a custom condition that reads the test config.
 */
@Service
@Conditional(EnabledCondition::class)
class PrimaryTestHook : SurfLifecycle {

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
