package dev.slne.surf.surfapi.core.api.util

import dev.slne.surf.surfapi.core.api.util.SurfTypeParameterMatcher.Companion.find
import dev.slne.surf.surfapi.core.api.util.SurfTypeParameterMatcher.Companion.get
import java.lang.reflect.Array
import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.util.concurrent.ConcurrentHashMap

/**
 * A utility class for matching type parameters of generic types at runtime using reflection.
 *
 * This class resolves and matches generic type parameters from parameterized superclasses or interfaces,
 * enabling runtime type checking of generic types. It provides caching mechanisms to optimize repeated
 * lookups and is fully thread-safe.
 *
 * ## Example Usage
 *
 * ```kotlin
 * // Define a generic handler interface
 * interface MessageHandler<T> {
 *     fun handle(message: T)
 * }
 *
 * // Implement the handler with a concrete type
 * class StringMessageHandler : MessageHandler<String> {
 *     override fun handle(message: String) {
 *         println("Handling: $message")
 *     }
 * }
 *
 * // Use the type parameter matcher
 * val handler = StringMessageHandler()
 * val matcher = SurfTypeParameterMatcher.find(handler, MessageHandler::class.java, "T")
 *
 * println(matcher.match("Hello"))  // true - String matches
 * println(matcher.match(123))      // false - Int doesn't match
 * ```
 *
 * ## Thread Safety
 *
 * All caching operations are thread-safe. Multiple threads can concurrently call [get] and [find]
 * without external synchronization.
 */
abstract class SurfTypeParameterMatcher {

    /**
     * Determines whether the provided object matches the expected type parameter.
     *
     * @param any The object to be checked against the type parameter.
     * @return `true` if the object is an instance of the matched type, `false` otherwise.
     */
    abstract fun match(any: Any): Boolean

    companion object {
        /**
         * Thread-safe cache for storing matchers based on parameterized types and type parameter names.
         * The outer map is concurrent, and each inner map is synchronized during creation.
         */
        private val findCache = ConcurrentHashMap<Class<*>, ConcurrentHashMap<String, SurfTypeParameterMatcher>>()

        /**
         * A no-operation matcher that always returns `true`, used for [Object] type parameters.
         */
        private val noop = object : SurfTypeParameterMatcher() {
            override fun match(any: Any): Boolean = true
        }

        /**
         * Thread-safe cache for storing matchers based on parameter types.
         * Uses [ClassValue] to ensure that the cache is automatically cleaned up when classes are unloaded,
         * preventing memory leaks.
         */
        private val getCache = object : ClassValue<SurfTypeParameterMatcher>() {
            @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
            override fun computeValue(type: Class<*>): SurfTypeParameterMatcher {
                if (type == Object::class.java) return noop
                return ReflectiveMatcher(type)
            }
        }

        /**
         * Retrieves a [SurfTypeParameterMatcher] for the given parameter type.
         *
         * Results are cached for performance. For `Object` type, returns a no-op matcher
         * that always matches any object.
         *
         * ## Example
         *
         * ```kotlin
         * val stringMatcher = SurfTypeParameterMatcher[String::class.java]
         * println(stringMatcher.match("test"))  // true
         * println(stringMatcher.match(42))      // false
         *
         * val objectMatcher = SurfTypeParameterMatcher[Object::class.java]
         * println(objectMatcher.match("anything"))  // true (always matches)
         * ```
         *
         * @param parameterType The class representing the type parameter to match.
         * @return A [SurfTypeParameterMatcher] for the provided type.
         */
        operator fun get(parameterType: Class<*>): SurfTypeParameterMatcher = getCache.get(parameterType)

        /**
         * Finds and retrieves a [SurfTypeParameterMatcher] for a specified type parameter name
         * from a parameterized superclass or interface.
         *
         * This method analyzes the object's class hierarchy to resolve the actual runtime type
         * of the specified generic type parameter. Results are cached per class and type parameter name.
         *
         * ## Example
         *
         * ```kotlin
         * // Generic repository interface
         * interface Repository<T, ID> {
         *     fun findById(id: ID): T?
         * }
         *
         * // Concrete implementation
         * class UserRepository : Repository<User, Long> {
         *     override fun findById(id: Long): User? = null
         * }
         *
         * val repo = UserRepository()
         *
         * // Match the entity type parameter
         * val entityMatcher = SurfTypeParameterMatcher.find(
         *     repo,
         *     Repository::class.java,
         *     "T"
         * )
         * println(entityMatcher.match(User()))  // true
         *
         * // Match the ID type parameter
         * val idMatcher = SurfTypeParameterMatcher.find(
         *     repo,
         *     Repository::class.java,
         *     "ID"
         * )
         * println(idMatcher.match(123L))  // true
         * println(idMatcher.match("not-a-long"))  // false
         * ```
         *
         * @param any The object whose type parameters are being analyzed.
         * @param parametrizedType The class representing the parameterized superclass or interface.
         * @param typeParamName The name of the type parameter to resolve (e.g., "T", "E", "K").
         * @return A [SurfTypeParameterMatcher] for the resolved type parameter.
         * @throws IllegalStateException If the type parameter cannot be resolved from the class hierarchy.
         */
        fun find(
            any: Any,
            parametrizedType: Class<*>,
            typeParamName: String
        ): SurfTypeParameterMatcher {
            val thisClass = any.javaClass
            val innerMap = findCache.computeIfAbsent(thisClass) { ConcurrentHashMap() }
            return innerMap.computeIfAbsent(typeParamName) {
                get(find0(any, parametrizedType, typeParamName))
            }
        }

        /**
         * Resolves the runtime type of a specified type parameter from the parameterized superclass or interface.
         *
         * @param any The object whose type parameter is being resolved.
         * @param parametrizedType The class representing the parameterized superclass or interface.
         * @param typeParamName The name of the type parameter to resolve.
         * @return The resolved [Class] representing the runtime type of the parameter.
         * @throws IllegalStateException If the type parameter cannot be resolved.
         */
        private fun find0(
            any: Any,
            parametrizedType: Class<*>,
            typeParamName: String
        ): Class<*> {
            val thisClass: Class<*> = any.javaClass

            val result = resolveTypeFromSuperclass(thisClass, parametrizedType, typeParamName)
                ?: resolveTypeFromInterfaces(thisClass, parametrizedType, typeParamName)

            return result ?: fail(thisClass, typeParamName)
        }

        /**
         * Attempts to resolve the type parameter by traversing the superclass hierarchy.
         *
         * @param currentClass The class to start traversal from.
         * @param parametrizedType The target parameterized type.
         * @param typeParamName The name of the type parameter.
         * @return The resolved class or `null` if not found in the superclass hierarchy.
         */
        private fun resolveTypeFromSuperclass(
            currentClass: Class<*>,
            parametrizedType: Class<*>,
            typeParamName: String
        ): Class<*>? {
            var currentClass = currentClass
            while (currentClass.superclass != null) {
                if (currentClass.superclass == parametrizedType) {
                    return resolveTypeFromGenericInfo(
                        currentClass.genericSuperclass as? ParameterizedType,
                        parametrizedType,
                        typeParamName
                    )
                }
                currentClass = currentClass.superclass ?: return null
            }
            return null
        }

        /**
         * Attempts to resolve the type parameter by examining implemented interfaces.
         *
         * @param currentClass The class whose interfaces are being examined.
         * @param parametrizedType The target parameterized type.
         * @param typeParamName The name of the type parameter.
         * @return The resolved class or `null` if not found in the interfaces.
         */
        private fun resolveTypeFromInterfaces(
            currentClass: Class<*>,
            parametrizedType: Class<*>,
            typeParamName: String
        ): Class<*>? {
            for (interfaceType in currentClass.genericInterfaces) {
                if (interfaceType is ParameterizedType && interfaceType.rawType == parametrizedType) {
                    return resolveTypeFromGenericInfo(interfaceType, parametrizedType, typeParamName)
                }
                if (interfaceType is Class<*>) {
                    val resolved = resolveTypeFromInterfaces(interfaceType, parametrizedType, typeParamName)
                    if (resolved != null) return resolved
                }
            }
            return null
        }

        /**
         * Extracts the actual type argument from a parameterized type.
         *
         * @param parameterizedType The parameterized type containing type arguments.
         * @param parametrizedType The base parameterized class/interface.
         * @param typeParamName The name of the type parameter to extract.
         * @return The resolved class or `null` if extraction fails.
         */
        private fun resolveTypeFromGenericInfo(
            parameterizedType: ParameterizedType?,
            parametrizedType: Class<*>,
            typeParamName: String
        ): Class<*>? {
            parameterizedType ?: return null
            val typeParamIndex = parametrizedType.typeParameters.indexOfFirst { it.name == typeParamName }
            if (typeParamIndex == -1) return null

            val actualTypeArgument = parameterizedType.actualTypeArguments[typeParamIndex]
            if (actualTypeArgument is Class<*>) {
                return actualTypeArgument
            }

            if (actualTypeArgument is GenericArrayType) {
                val componentType = actualTypeArgument.genericComponentType as? Class<*>
                return componentType?.let { Array.newInstance(it, 0).javaClass }
            }

            return null
        }

        /**
         * Throws an exception when the type parameter cannot be resolved.
         *
         * @param type The class being analyzed.
         * @param typeParamName The name of the type parameter.
         * @throws IllegalStateException Always thrown with a descriptive error message.
         */
        private fun fail(type: Class<*>, typeParamName: String): Nothing {
            throw IllegalStateException(
                "cannot determine the type of the type parameter '$typeParamName': $type"
            )
        }

        /**
         * A [SurfTypeParameterMatcher] implementation that matches objects based on their runtime type
         * using [Class.isInstance].
         *
         * @param type The class representing the type to match against.
         */
        private class ReflectiveMatcher(private val type: Class<*>) : SurfTypeParameterMatcher() {
            override fun match(any: Any) = type.isInstance(any)
        }
    }
}
