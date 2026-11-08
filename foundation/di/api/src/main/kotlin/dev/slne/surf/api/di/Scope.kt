package dev.slne.surf.api.di

/**
 * How many instances of a bean exist and who sees them.
 */
enum class Scope {
    /**
     * One instance per container.
     *
     * A plugin's container holds its own instance; another plugin's container holds a different
     * one. This is the default, and the right choice for anything that belongs to one application.
     */
    SINGLETON,

    /**
     * One instance for the whole process, published into the root container.
     *
     * Every container below the root resolves the same instance, which makes it the way a bean
     * crosses application boundaries. Use it for what genuinely exists once per process — a broker
     * connection, a platform service — and never for state that belongs to one application, since
     * every other application in the process sees it too.
     */
    SHARED,

    /**
     * A new instance for every injection point.
     *
     * The container creates the bean, injects it and forgets it: nothing is cached and
     * [SurfComponent] lifecycle methods are not called, because the container does not keep the
     * instance long enough to shut it down.
     */
    PROTOTYPE,
}
