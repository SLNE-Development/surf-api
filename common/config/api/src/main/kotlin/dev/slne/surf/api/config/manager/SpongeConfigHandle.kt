package dev.slne.surf.api.config.manager

import dev.slne.surf.api.config.ConfigHandle
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * A [ConfigHandle] backed by a [SpongeConfigManager], which is not thread-safe on its own: every
 * operation that touches the manager's file or node holds [lock].
 */
internal class SpongeConfigHandle<C : Any>(
    private val configClass: Class<C>,
    private val manager: SpongeConfigManager<C>,
) : ConfigHandle<C> {

    private val lock = ReentrantLock()
    private var isClosed = false

    override val value: C get() = manager.config

    override fun reload(validate: (C) -> Unit): C = withOpenLock { manager.reload(validate) }

    override fun edit(block: C.() -> Unit) {
        withOpenLock {
            manager.config.block()
            manager.save()
        }
    }

    override fun update(transform: (C) -> C): C {
        return withOpenLock {
            val updated = transform(manager.config)
            manager.config = updated
            manager.save()
            updated
        }
    }

    override fun save() {
        withOpenLock { manager.save() }
    }

    override fun close() {
        lock.withLock { isClosed = true }
    }

    override fun toString(): String = "ConfigHandle(${configClass.name})"

    private inline fun <R> withOpenLock(action: () -> R): R {
        return lock.withLock {
            check(!isClosed) { "The config handle of ${configClass.name} is closed" }

            action()
        }
    }
}
