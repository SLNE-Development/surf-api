package dev.slne.surf.surfapi.bukkit.test.component.processor

import dev.slne.surf.surfapi.core.api.util.logger
import dev.slne.surf.surfapi.shared.api.component.Component
import dev.slne.surf.surfapi.shared.api.component.processor.ComponentContext
import dev.slne.surf.surfapi.shared.api.component.processor.ComponentPostProcessor

/**
 * Example ComponentPostProcessor implementation that is auto-discovered.
 * No annotation needed - just implementing the interface is enough.
 */
class TestLoggingPostProcessor : ComponentPostProcessor {
    private val log = logger()

    override val priority: Int = 5

    override suspend fun postProcessAfterInitialization(
        component: Component,
        componentName: String,
        context: ComponentContext
    ): Component {
        log.atInfo().log("Component initialized: $componentName")
        return component
    }

    override suspend fun postProcessBeforeDestruction(
        component: Component,
        componentName: String,
        context: ComponentContext
    ) {
        log.atInfo().log("Component being destroyed: $componentName")
    }
}
