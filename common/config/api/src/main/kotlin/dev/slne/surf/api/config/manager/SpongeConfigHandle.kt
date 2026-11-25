package dev.slne.surf.api.config.manager

import dev.slne.surf.api.config.ConfigHandle
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * A [ConfigHandle] backed by the [SpongeConfigManager] that [open] creates on the first load.
 *
 * The manager is not thread-safe on its own: every operation that loads the file or touches the
 * manager's file or node holds [lock]. [manager] is volatile, so [value] reads a loaded handle
 * without the lock. A failing [open], or a first [reload] whose check rejects the opened value,
 * leaves the handle unloaded.
 *
 * @param configClass the configuration class
 * @param open reads the file; called under [lock] until its result is installed
 */
@InternalSurfApi
class SpongeConfigHandle<C : Any>(
    private val configClass: Class<C>,
    private val open: () -> SpongeConfigManager<C>,
) : ConfigHandle<C> {

    private val lock = ReentrantLock()

    @Volatile
    private var manager: SpongeConfigManager<C>? = null
    private var isClosed = false

    override val value: C
        get() {
            val loaded = manager ?: return load()

            return loaded.config
        }

    override fun load(): C = withLoadedManager { current -> current.config }

    override fun reload(validate: (C) -> Unit): C {
        return lock.withLock {
            checkOpen()
            val loaded = manager
            if (loaded != null) return loaded.reload(validate)

            val opened = open()
            validate(opened.config)
            manager = opened
            opened.config
        }
    }

    override fun edit(block: C.() -> Unit) {
        withLoadedManager { current ->
            current.config.block()
            current.save()
        }
    }

    override fun update(transform: (C) -> C): C {
        return withLoadedManager { current ->
            val updated = transform(current.config)
            current.config = updated
            current.save()
            updated
        }
    }

    override fun save() {
        withLoadedManager { current -> current.save() }
    }

    override fun close() {
        lock.withLock { isClosed = true }
    }

    override fun toString(): String = "ConfigHandle(${configClass.name})"

    private inline fun <R> withLoadedManager(action: (SpongeConfigManager<C>) -> R): R {
        return lock.withLock {
            checkOpen()
            val loaded = manager ?: open().also { manager = it }

            action(loaded)
        }
    }

    private fun checkOpen() {
        check(!isClosed) { "The config handle of ${configClass.name} is closed" }
    }
}
