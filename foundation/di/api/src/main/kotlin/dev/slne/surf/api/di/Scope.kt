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
     * The container creates the bean, injects it and forgets it. Nothing is cached, no
     * [SurfComponent] lifecycle method is called, and no [BeanPostProcessor] sees it — all three
     * follow from the same thing: the container does not keep the instance, so it could neither shut
     * it down nor unregister it again.
     */
    PROTOTYPE,

    /**
     * Takes the scope of the bean that declares it.
     *
     * Only meaningful on [Bean], where it is the default: a factory on a [SHARED] connection
     * publishes a shared bean without having to repeat the scope, and one on an ordinary bean stays
     * in its container. It exists as a value of its own so that "not stated" and an explicitly
     * stated scope are different things — with `SINGLETON` as the default they would be the same
     * value, and inheritance could only be guessed at.
     *
     * A bean class never carries it; the container resolves it away before anything sees it.
     */
    INHERIT,
}
