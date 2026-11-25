package dev.slne.surf.api.config

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Returns the handle of the [SurfConfig] class [C] from the context of the plugin that owns [C].
 *
 * For code that is not a bean; beans inject `ConfigHandle<C>`. The owning context is found
 * through the class loader of [C]. While that context is starting, the handle is created on
 * demand, so the call works from bean constructors as well.
 *
 * @return the handle of [C]
 * @throws IllegalStateException if [C] is not a [SurfConfig] class, or its plugin's context has
 * not started yet or is closed
 */
inline fun <reified C : Any> configHandle(): ConfigHandle<C> = configHandle(C::class.java)

/**
 * Returns the handle of the [SurfConfig] class [configClass] from the context of the plugin that
 * owns it.
 *
 * @param configClass the [SurfConfig] class
 * @return the handle of [configClass]
 * @throws IllegalStateException if [configClass] is not a [SurfConfig] class, or its plugin's
 * context has not started yet or is closed
 * @see configHandle
 */
fun <C : Any> configHandle(configClass: Class<C>): ConfigHandle<C> {
    return SurfConfigContexts.resolve(configClass).handle
}

/**
 * Returns a property that reads the current value of the [SurfConfig] class [C]:
 *
 * ```kotlin
 * object ShopPrices {
 *     private val config by surfConfig<ShopConfig>()
 *
 *     fun price(base: Double) = base * (1 - config.discount)
 * }
 * ```
 *
 * Every read returns the handle's current value, so a reload is visible at once. The handle is
 * resolved on the first read, as [configHandle] does, and again once its plugin's context has
 * closed, so a restarted plugin reads its new handle.
 *
 * @return the property
 */
inline fun <reified C : Any> surfConfig(): ReadOnlyProperty<Any?, C> = surfConfig(C::class.java)

/**
 * Returns a property that reads the current value of the [SurfConfig] class [configClass].
 *
 * @param configClass the [SurfConfig] class
 * @return the property
 * @see surfConfig
 */
fun <C : Any> surfConfig(configClass: Class<C>): ReadOnlyProperty<Any?, C> {
    return SurfConfigProperty(configClass)
}

/**
 * Caches the resolved handle together with its context, published through one volatile field, and
 * resolves it again once that context is no longer active.
 */
private class SurfConfigProperty<C : Any>(
    private val configClass: Class<C>,
) : ReadOnlyProperty<Any?, C> {

    @Volatile
    private var resolved: ResolvedHandle<C>? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>): C {
        val cached = resolved
        if (cached != null && cached.context.isActive) return cached.handle.value

        return SurfConfigContexts.resolve(configClass)
            .also { resolved = it }
            .handle
            .value
    }
}
