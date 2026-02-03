package dev.slne.surf.surfapi.core.api.component

import dev.slne.surf.surfapi.shared.api.annotation.AnnotationUtils
import dev.slne.surf.surfapi.shared.api.component.Component
import dev.slne.surf.surfapi.shared.api.component.ComponentMeta
import dev.slne.surf.surfapi.shared.api.component.Priority
import dev.slne.surf.surfapi.shared.api.util.InternalSurfApi
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Abstract base class for components that provides lifecycle management.
 *
 * This class handles the component lifecycle by ensuring each phase is only
 * executed once and in the correct order. Subclasses should override the
 * protected `on*` methods to implement their functionality.
 *
 * The priority is determined by the [@Priority][Priority] annotation on the class
 * or its meta-annotations. If no priority is specified, the default is 0.
 *
 * Example:
 * ```kotlin
 * @ComponentMeta
 * @Priority(10)
 * class MyComponent : AbstractComponent() {
 *     override suspend fun onEnable() {
 *         // Initialize component
 *     }
 *
 *     override suspend fun onDisable() {
 *         // Cleanup
 *     }
 * }
 * ```
 *
 * @see Component
 * @see ComponentMeta
 * @see Priority
 */
abstract class AbstractComponent : Component {
    private val bootstrapped = AtomicBoolean(false)
    private val loaded = AtomicBoolean(false)
    private val enabled = AtomicBoolean(false)
    private val disabled = AtomicBoolean(false)

    init {
        AnnotationUtils.findAnnotation(javaClass, ComponentMeta::class.java)
            ?: error("ComponentMeta annotation is missing on component class ${this::class.qualifiedName}")
    }

    @InternalSurfApi
    final override suspend fun bootstrap() {
        if (bootstrapped.compareAndSet(false, true)) {
            onBootstrap()
        }
    }

    @InternalSurfApi
    final override suspend fun load() {
        if (loaded.compareAndSet(false, true)) {
            bootstrap()
            onLoad()
        }
    }

    @InternalSurfApi
    final override suspend fun enable() {
        if (enabled.compareAndSet(false, true)) {
            load()
            onEnable()
        }
    }

    @InternalSurfApi
    final override suspend fun disable() {
        if (disabled.compareAndSet(false, true)) {
            onDisable()
        }
    }

    /**
     * Called during the bootstrap phase.
     * Override to perform early initialization.
     */
    protected open suspend fun onBootstrap() {}

    /**
     * Called during the load phase.
     * Override to load configuration and resources.
     */
    protected open suspend fun onLoad() {}

    /**
     * Called during the enable phase.
     * Override to activate component functionality.
     */
    protected open suspend fun onEnable() {}

    /**
     * Called during the disable phase.
     * Override to clean up resources.
     */
    protected open suspend fun onDisable() {}
}