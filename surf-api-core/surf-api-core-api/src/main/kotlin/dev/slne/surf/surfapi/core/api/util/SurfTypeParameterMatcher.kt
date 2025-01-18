package dev.slne.surf.surfapi.core.api.util

import it.unimi.dsi.fastutil.objects.Object2ObjectMap
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import java.lang.reflect.Array
import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.util.*

/**
 * A utility class for matching type parameters of generic types at runtime.
 */
abstract class SurfTypeParameterMatcher {

    /**
     * Determines whether the provided object matches the criteria defined by the matcher.
     *
     * @param any The object to be checked.
     * @return `true` if the object matches the criteria, `false` otherwise.
     */
    abstract fun match(any: Any): Boolean

    companion object {
        /**
         * Cache for storing matchers based on their parameter types.
         */
        private val getCache = IdentityHashMap<Class<*>, SurfTypeParameterMatcher>()

        /**
         * Cache for storing matchers based on their parameterized superclass and type parameter names.
         */
        private val findCache =
            IdentityHashMap<Class<*>, Object2ObjectMap<String, SurfTypeParameterMatcher>>()

        /**
         * A no-operation matcher that always returns `true`.
         */
        private val noop = object : SurfTypeParameterMatcher() {
            override fun match(any: Any): Boolean = true
        }

        /**
         * Retrieves a [SurfTypeParameterMatcher] for the given parameter type.
         *
         * @param parameterType The class representing the type parameter.
         * @return A [SurfTypeParameterMatcher] for the provided type.
         */
        operator fun get(parameterType: Class<*>): SurfTypeParameterMatcher =
            getCache.computeIfAbsent(parameterType) {
                if (parameterType == Object::class.java) noop else ReflectiveMatcher(parameterType)
            }

        /**
         * Finds and retrieves a [SurfTypeParameterMatcher] for a specified type parameter name of
         * a parameterized superclass or interface.
         *
         * @param any The object whose type parameters are being analyzed.
         * @param parametrizedType The class representing the parameterized superclass or interface.
         * @param typeParamName The name of the type parameter to resolve.
         * @return A [SurfTypeParameterMatcher] for the resolved type parameter.
         */
        fun find(
            any: Any,
            parametrizedType: Class<*>,
            typeParamName: String
        ): SurfTypeParameterMatcher {
            val thisClass = any.javaClass
            val map = findCache.computeIfAbsent(thisClass) { Object2ObjectOpenHashMap() }
            return map.computeIfAbsent(typeParamName) {
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
         * @throws IllegalStateException Always thrown with a message indicating the unresolved parameter.
         */
        private fun fail(type: Class<*>, typeParamName: String): Nothing {
            throw IllegalStateException(
                "cannot determine the type of the type parameter '$typeParamName': $type"
            )
        }

        /**
         * A [SurfTypeParameterMatcher] implementation that matches objects based on their runtime type.
         *
         * @param type The class representing the type to match.
         */
        private class ReflectiveMatcher(private val type: Class<*>) : SurfTypeParameterMatcher() {
            override fun match(any: Any) = type.isInstance(any)
        }
    }
}
