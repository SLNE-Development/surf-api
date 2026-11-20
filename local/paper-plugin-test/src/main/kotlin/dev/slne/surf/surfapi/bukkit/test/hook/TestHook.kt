package dev.slne.surf.surfapi.bukkit.test.hook

import dev.slne.surf.api.common.util.logger
import dev.slne.surf.api.spring.SurfLifecycle
import dev.slne.surf.surfapi.bukkit.test.hook.condition.TestPropertyCondition
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

/**
 * A stereotype meta-annotated with `@Service`.
 */
@Service
annotation class TestHookMeta

/**
 * A stereotype two levels removed from `@Service`.
 */
@TestHookMeta
annotation class TestHookMeta2

/**
 * A hook behind a two-level stereotype, a profile and a property condition.
 */
@TestHookMeta2
@Profile("production")
@Conditional(TestPropertyCondition::class)
class TestHook : SurfLifecycle {

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
