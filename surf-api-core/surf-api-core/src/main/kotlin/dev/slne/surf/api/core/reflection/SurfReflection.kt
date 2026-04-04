package dev.slne.surf.api.core.reflection

import dev.slne.surf.api.core.util.requiredService

/**
 * Provides a high-level interface for creating dynamic proxies that enable reflective access to classes.
 *
 * SurfReflection allows you to define an interface annotated with [SurfProxy] and its methods with
 * reflection annotations ([Field], [Constructor], [Static], etc.) to access private or internal
 * members of a target class at runtime without direct reflection calls.
 *
 * Example usage:
 * ```kotlin
 * @SurfProxy(qualifiedName = "com.example.internal.HiddenClass")
 * interface HiddenClassProxy {
 *     // Access private field
 *     @Field(name = "secretValue", type = Field.Type.GETTER)
 *     fun getSecret(instance: Any): String
 *
 *     // Call private static method
 *     @Static
 *     fun staticMethod(param: String): Int
 *
 *     // Invoke constructor
 *     @Constructor
 *     fun create(arg: String): Any
 * }
 *
 * // Create proxy and use it
 * val proxy = surfReflection.createProxy<HiddenClassProxy>()
 * val instance = proxy.create("test")
 * val secret = proxy.getSecret(instance)
 * val result = proxy.staticMethod("value")
 * ```
 */
interface SurfReflection {
    /**
     * Creates a dynamic proxy instance for the specified class using the provided class loader.
     *
     * The class must be an interface annotated with [SurfProxy]. The proxy will intercept method
     * calls and translate them to reflective operations on the target class specified in [SurfProxy].
     *
     * @param T The proxy interface type, must be annotated with [SurfProxy]
     * @param clazz The interface class object for which the proxy is created
     * @param classLoader The ClassLoader to use for loading the target class and defining the proxy
     * @return A dynamic proxy instance implementing the specified interface
     * @throws IllegalArgumentException If the class is not an interface, lacks [SurfProxy] annotation,
     *         or if the target class specified in [SurfProxy] cannot be found
     */
    fun <T : Any> createProxy(clazz: Class<T>, classLoader: ClassLoader): T

    /**
     * Creates a dynamic proxy instance using the class's own ClassLoader.
     *
     * This is a convenience method that delegates to [createProxy] with the class's own ClassLoader.
     *
     * @param T The proxy interface type, must be annotated with [SurfProxy]
     * @param clazz The interface class object for which the proxy is created
     * @return A dynamic proxy instance implementing the specified interface
     * @throws IllegalArgumentException If the class is not an interface, lacks [SurfProxy] annotation,
     *         or if the target class cannot be found or accessed
     */
    fun <T : Any> createProxy(clazz: Class<T>): T = createProxy(clazz, clazz.classLoader)

    companion object : SurfReflection by surfReflection {
        val INSTANCE = surfReflection
    }
}

/**
 * The singleton instance of the SurfReflection interface.
 */
private val surfReflection = requiredService<SurfReflection>()

/**
 * Creates a dynamic proxy for the reified type parameter.
 *
 * This inline extension function provides type-safe proxy creation without explicitly passing the class.
 *
 * @param T The proxy interface type, must be annotated with [SurfProxy]
 * @return A dynamic proxy instance of type T
 * @throws IllegalArgumentException If T is not properly configured for proxy creation
 */
inline fun <reified T : Any> SurfReflection.createProxy(): T = createProxy(T::class.java)