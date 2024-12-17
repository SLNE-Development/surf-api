package dev.slne.surf.surfapi.core.api.util

import it.unimi.dsi.fastutil.objects.Object2ObjectMap
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import java.lang.reflect.Array
import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.TypeVariable
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
         * a parameterized superclass.
         *
         * @param any The object whose type parameters are being analyzed.
         * @param parametrizedSuperclass The class representing the parameterized superclass.
         * @param typeParamName The name of the type parameter to resolve.
         * @return A [SurfTypeParameterMatcher] for the resolved type parameter.
         */
        fun find(
            any: Any,
            parametrizedSuperclass: Class<*>,
            typeParamName: String
        ): SurfTypeParameterMatcher {
            val thisClass = any.javaClass
            val map = findCache.computeIfAbsent(thisClass) { Object2ObjectOpenHashMap() }
            return map.computeIfAbsent(typeParamName) {
                get(find0(any, parametrizedSuperclass, typeParamName))
            }
        }

        /**
         * Resolves the runtime type of a specified type parameter from the parameterized superclass.
         *
         * @param any The object whose type parameter is being resolved.
         * @param parametrizedSuperclass The class representing the parameterized superclass.
         * @param typeParamName The name of the type parameter to resolve.
         * @return The resolved [Class] representing the runtime type of the parameter.
         * @throws IllegalStateException If the type parameter cannot be resolved.
         */
        private fun find0(
            any: Any,
            parametrizedSuperclass: Class<*>,
            typeParamName: String
        ): Class<*> {
            var parametrizedSuperclass = parametrizedSuperclass
            var typeParamName = typeParamName

            val thisClass: Class<*> = any.javaClass
            var currentClass: Class<*> = thisClass

            while (true) {
                if (currentClass.superclass == parametrizedSuperclass) {
                    val typeParamIndex =
                        currentClass.typeParameters.indexOfFirst { it.name == typeParamName }
                    check(typeParamIndex != -1) { "unknown type parameter '$typeParamName': $parametrizedSuperclass" }

                    val genericSuperType = currentClass.genericSuperclass as? ParameterizedType
                        ?: return Object::class.java
                    val actualTypeArguments = genericSuperType.actualTypeArguments
                    var actualTypeParam = actualTypeArguments[typeParamIndex]

                    if (actualTypeParam is ParameterizedType) {
                        actualTypeParam = actualTypeParam.rawType
                    }

                    if (actualTypeParam is Class<*>) {
                        return actualTypeParam
                    }

                    if (actualTypeParam is GenericArrayType) {
                        var componentType = actualTypeParam.genericComponentType
                        if (componentType is ParameterizedType) {
                            componentType = componentType.rawType
                        }
                        if (componentType is Class<*>) {
                            return Array.newInstance(componentType, 0).javaClass
                        }
                    }

                    if (actualTypeParam is TypeVariable<*>) {
                        if (actualTypeParam.genericDeclaration !is Class<*>) {
                            return Object::class.java
                        }

                        currentClass = thisClass
                        parametrizedSuperclass = actualTypeParam.genericDeclaration as Class<*>
                        typeParamName = actualTypeParam.name
                        if (parametrizedSuperclass.isAssignableFrom(thisClass)) {
                            continue
                        }
                        return Object::class.java
                    }

                    return fail(thisClass, typeParamName)
                }
                currentClass = currentClass.superclass ?: return fail(thisClass, typeParamName)
            }
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