package dev.slne.surf.surfapi.core.api.reflection

import dev.slne.surf.surfapi.core.api.util.requiredService

/**
 * Represents a high-level interface for creating dynamic proxies for classes.
 * The SurfReflection interface enables creation of proxy instances for given
 * classes at runtime, typically for facilitating reflective operations or interception.
 */
interface SurfReflection {
    /**
     * Creates a dynamic proxy instance for the specified class using the provided class loader.
     * This function is typically used for creating runtime proxies to enable reflective operations
     * or to intercept method calls on the provided class type.
     *
     * @param T The type of the class for which the proxy is created.
     * @param clazz The `Class` object representing the class for which the proxy needs to be created.
     * @param classLoader The `ClassLoader` to be used for defining the proxy class.
     * @return A dynamic proxy instance of the specified class type `T`.
     * @throws IllegalArgumentException If the provided class or class loader is invalid, or if a proxy cannot be created.
     */
    fun <T> createProxy(clazz: Class<T>, classLoader: ClassLoader): T

    /**
     * Creates a dynamic proxy instance for the specified class using its class loader.
     * This method simplifies the creation of runtime proxies by leveraging the provided class.
     *
     * @param T The type of the class for which the proxy is created.
     * @param clazz The `Class` object representing the class for which the proxy needs to be created.
     * @return A dynamic proxy instance of the specified class type `T`.
     * @throws IllegalArgumentException If the provided class or class loader is invalid, or if a proxy cannot be created.
     */
    fun <T> createProxy(clazz: Class<T>): T = createProxy<T>(clazz, clazz.getClassLoader())

    companion object {
        /**
         * The singleton instance of the SurfReflection interface.
         */
        @JvmStatic
        val instance = requiredService<SurfReflection>()
    }
}

/**
 * The singleton instance of the SurfReflection interface.
 */
val surfReflection get() = SurfReflection.instance

/**
 * Creates a dynamic proxy instance for the specified class type using its class loader.
 * This method provides a concise inline approach by leveraging the reified type parameter.
 *
 * @return A dynamic proxy instance of the specified class type `T`.
 * @throws IllegalArgumentException If the proxy cannot be created for the provided class type `T`.
 */
inline fun <reified T> SurfReflection.createProxy(): T = createProxy<T>(T::class.java)