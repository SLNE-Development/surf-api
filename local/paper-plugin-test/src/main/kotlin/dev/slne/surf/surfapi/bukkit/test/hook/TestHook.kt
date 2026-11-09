package dev.slne.surf.surfapi.bukkit.test.hook

import dev.slne.surf.api.core.util.logger
import dev.slne.surf.api.di.Service
import dev.slne.surf.api.di.SurfComponent
import dev.slne.surf.api.di.condition.ConditionalOnEnvironment
import dev.slne.surf.api.di.condition.ConditionalOnProperty

/** A stereotype of this plugin's own, to check that `@BeanMeta` is found through one. */
@Service
annotation class TestHookMeta

/** A stereotype built on another one, to check that the chain is followed further than one step. */
@TestHookMeta
annotation class TestHookMeta2

/**
 * A bean that only exists on a production server with `test.property=true` in its `properties.yml`.
 *
 * Carries both halves of what the test plugin is here to exercise: the stereotype chain above, and
 * two independent conditions that both have to hold.
 */
@TestHookMeta2
@ConditionalOnEnvironment(environments = ["production"])
@ConditionalOnProperty(key = ["test", "property"], havingValue = "true")
class TestHook : SurfComponent {
    private val log = logger()

    override suspend fun load() {
        log.atInfo().log("TestHook loaded")
    }

    override suspend fun enable() {
        log.atInfo().log("TestHook enabled")
    }

    override suspend fun disable() {
        log.atInfo().log("TestHook disabled")
    }
}
