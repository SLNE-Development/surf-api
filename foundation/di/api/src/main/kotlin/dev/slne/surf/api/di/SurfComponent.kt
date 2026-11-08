package dev.slne.surf.api.di

import org.jetbrains.annotations.ApiStatus

/**
 * Opt-in lifecycle for a bean that has to start and stop.
 *
 * A bean does not need this interface — most beans are created, injected and used. Implement it
 * when the bean owns something that must be opened and closed, and the container will drive it.
 *
 * The container runs the phases as separate passes over all beans, each in dependency order:
 *
 * 1. every bean is constructed and injected
 * 2. every bean passes through the [BeanPostProcessor] chain
 * 3. [load] on every bean
 * 4. [enable] on every bean
 * 5. [disable] on every bean, in reverse
 *
 * The gap between [load] and [enable] is what makes two-phase resources work: a connection can be
 * created in [load] while the beans that register against it are still being set up, and only be
 * frozen or opened in [enable], once every [load] has run. [disable] running in reverse means a
 * bean is torn down before the things it depends on, so a client closes before its connection.
 *
 * All three are suspending and are called from the container's scope. A phase that throws stops
 * that phase; the container reports which bean failed.
 */
interface SurfComponent {

    /** Prepares the bean. Called after every bean exists, before any [enable]. */
    @ApiStatus.OverrideOnly
    suspend fun load() = Unit

    /** Activates the bean. Called after every [load] has run. */
    @ApiStatus.OverrideOnly
    suspend fun enable() = Unit

    /** Releases what the bean owns. Called in reverse dependency order during shutdown. */
    @ApiStatus.OverrideOnly
    suspend fun disable() = Unit
}
