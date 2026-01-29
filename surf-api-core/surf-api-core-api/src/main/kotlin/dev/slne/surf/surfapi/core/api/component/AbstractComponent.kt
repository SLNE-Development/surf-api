package dev.slne.surf.surfapi.core.api.component

import dev.slne.surf.surfapi.shared.api.component.Component
import dev.slne.surf.surfapi.shared.api.component.ComponentMeta
import dev.slne.surf.surfapi.shared.api.util.InternalSurfApi
import java.util.concurrent.atomic.AtomicBoolean

abstract class AbstractComponent : Component {
    private val bootstrapped = AtomicBoolean(false)
    private val loaded = AtomicBoolean(false)
    private val enabled = AtomicBoolean(false)
    private val disabled = AtomicBoolean(false)

    private val meta: ComponentMeta = javaClass.getAnnotation(ComponentMeta::class.java)
        ?: findMetaAnnotation()
        ?: error("ComponentMeta annotation is missing on component class ${this::class.qualifiedName}")

    final override val priority = meta.priority

    /**
     * Recursively searches for @ComponentMeta on the class's annotations (meta-annotation support)
     */
    private fun findMetaAnnotation(): ComponentMeta? {
        return javaClass.annotations
            .mapNotNull { it.annotationClass.java.getAnnotation(ComponentMeta::class.java) }
            .firstOrNull()
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

    final override fun compareTo(other: Component): Int {
        return this.priority.compareTo(other.priority)
    }

    protected open suspend fun onBootstrap() {}
    protected open suspend fun onLoad() {}
    protected open suspend fun onEnable() {}
    protected open suspend fun onDisable() {}
}
