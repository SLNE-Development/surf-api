package dev.slne.surf.api.reflection

import java.io.Serial

/**
 * Creates and caches the implementations of [ReflectionProxy] interfaces.
 *
 * Each interface gets one instance, created on first request and kept for as long as the interface
 * class is reachable. The instance is stateless and thread-safe. The bean that a plugin context
 * registers for an interface is the same instance.
 */
object ReflectionProxies {

    private val proxies = object : ClassValue<Any>() {
        override fun computeValue(type: Class<*>): Any = ReflectionProxyFactory.create(type)
    }

    /**
     * Returns the proxy implementing [type].
     *
     * @param type the [ReflectionProxy] interface
     * @return the cached proxy instance
     * @throws ReflectionProxyException if [type] is not a [ReflectionProxy] interface, its target
     * class cannot be loaded, or a proxy method has no matching member
     */
    fun <T : Any> get(type: Class<T>): T = type.cast(proxies.get(type))
}

/**
 * Returns the proxy implementing the [ReflectionProxy] interface [T].
 *
 * @param T the [ReflectionProxy] interface
 * @return the cached proxy instance
 * @throws ReflectionProxyException if [T] is not a [ReflectionProxy] interface, its target class
 * cannot be loaded, or a proxy method has no matching member
 * @see ReflectionProxies.get
 */
inline fun <reified T : Any> reflectionProxy(): T = ReflectionProxies.get(T::class.java)

/**
 * Signals that a [ReflectionProxy] interface cannot be implemented.
 *
 * Thrown when the proxy is created, never when one of its methods is called.
 */
class ReflectionProxyException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause) {
    companion object {
        @Serial
        private const val serialVersionUID: Long = 8098923466127823055L
    }
}
