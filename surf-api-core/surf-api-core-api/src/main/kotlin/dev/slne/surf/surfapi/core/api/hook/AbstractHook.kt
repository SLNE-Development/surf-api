package dev.slne.surf.surfapi.core.api.hook

import dev.slne.surf.surfapi.core.api.util.InternalSurfApi
import java.util.concurrent.atomic.AtomicBoolean

abstract class AbstractHook : Comparable<AbstractHook> {
    private val bootstrapped = AtomicBoolean(false)
    private val loaded = AtomicBoolean(false)
    private val enabled = AtomicBoolean(false)
    private val disabled = AtomicBoolean(false)

    private val meta: HookMeta = javaClass.getAnnotation(HookMeta::class.java)
        ?: error("HookMeta annotation is missing on hook class ${this::class.qualifiedName}")

    @InternalSurfApi
    suspend fun bootstrap() {
        if (bootstrapped.compareAndSet(false, true)) {
            onBootstrap()
        }
    }

    @InternalSurfApi
    suspend fun load() {
        if (loaded.compareAndSet(false, true)) {
            bootstrap()
            onLoad()
        }
    }

    @InternalSurfApi
    suspend fun enable() {
        if (enabled.compareAndSet(false, true)) {
            load()
            onEnable()
        }
    }

    @InternalSurfApi
    suspend fun disable() {
        if (disabled.compareAndSet(false, true)) {
            onDisable()
        }
    }

    final override fun compareTo(other: AbstractHook): Int {
        return this.meta.priority.compareTo(other.meta.priority)
    }

    protected open suspend fun onBootstrap() {}
    protected open suspend fun onLoad() {}
    protected open suspend fun onEnable() {}
    protected open suspend fun onDisable() {}
}