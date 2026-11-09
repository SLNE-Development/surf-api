package dev.slne.surf.api.di

import kotlin.reflect.KClass

/**
 * Holds the beans of one application and resolves them by type.
 *
 * Containers form a hierarchy. The root container belongs to surf-api itself and holds what exists
 * once per process; below it sits one container per application — a plugin, a microservice — holding
 * that application's own beans. Resolution walks upwards: a container answers from its own beans
 * first and then asks its [parent]. It never walks down and never sideways, so an application cannot
 * reach into another one's beans, and a bean crosses that boundary only by being published into the
 * root with [Scope.SHARED].
 *
 * Code inside a bean rarely needs this interface: write what you need into the constructor and let
 * the container resolve it. Reach for the container itself at the edges — where a platform hands
 * control back and there is no constructor to inject into.
 */
interface BeanContainer {

    /** The container asked next when this one has no candidate, or `null` for the root. */
    val parent: BeanContainer?

    /** The application this container belongs to — a plugin instance, a microservice. */
    val owner: Any

    /**
     * Returns the bean providing [type], or `null` if there is none.
     *
     * @param qualifier the name the bean was given with [Qualifier], or `null` for any
     * @throws IllegalStateException if several beans match and none is [Primary]
     */
    fun <T : Any> beanOrNull(type: KClass<T>, qualifier: String? = null): T?

    /**
     * Returns the bean providing [type].
     *
     * @throws IllegalStateException if there is no candidate, or if several match and none is
     * [Primary]. Both messages name what was found, so a wrong wiring is readable from the failure.
     */
    fun <T : Any> bean(type: KClass<T>, qualifier: String? = null): T

    /**
     * Returns every bean providing [type], ordered by `@Priority` and then by class name, with this
     * container's own beans before the ones it inherits.
     *
     * Empty when there is none; this is a collection, not a lookup, so an empty result is an answer
     * rather than a failure. The order is stable across runs, which is what a registrar that cares
     * about registration order depends on.
     */
    fun <T : Any> beansOfType(type: KClass<T>): List<T>

    /**
     * Returns a handle that resolves [type] on first access.
     *
     * Use it where the value is needed later than the container can provide it — a bean that would
     * otherwise close a cycle, or a platform entry point that exists before the container is built.
     */
    fun <T : Any> lazyBean(type: KClass<T>, qualifier: String? = null): Lazy<T>
}

/** Returns the bean providing [T], or `null` if there is none. */
inline fun <reified T : Any> BeanContainer.beanOrNull(qualifier: String? = null): T? =
    beanOrNull(T::class, qualifier)

/** Returns the bean providing [T]. */
inline fun <reified T : Any> BeanContainer.bean(qualifier: String? = null): T =
    bean(T::class, qualifier)

/** Returns every bean providing [T], in dependency order. */
inline fun <reified T : Any> BeanContainer.beansOfType(): List<T> =
    beansOfType(T::class)

/** Returns a handle that resolves [T] on first access. */
inline fun <reified T : Any> BeanContainer.lazyBean(qualifier: String? = null): Lazy<T> =
    lazyBean(T::class, qualifier)
