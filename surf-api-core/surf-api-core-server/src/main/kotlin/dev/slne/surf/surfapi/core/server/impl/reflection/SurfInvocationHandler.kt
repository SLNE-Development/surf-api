package dev.slne.surf.surfapi.core.server.impl.reflection

import dev.slne.surf.surfapi.core.api.reflection.Name
import dev.slne.surf.surfapi.core.api.reflection.Static
import dev.slne.surf.surfapi.core.api.util.*
import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.reflect.FieldUtils
import org.apache.commons.lang3.reflect.MethodUtils
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import dev.slne.surf.surfapi.core.api.reflection.Constructor as ConstructorAnnotation
import dev.slne.surf.surfapi.core.api.reflection.Field as FieldAnnotation

class SurfInvocationHandler<T>(
    private val proxyClass: Class<T>,
    private val proxiedClass: Class<*>,
) : InvocationHandler {
    companion object {
        private val lookup = MethodHandles.lookup()

        private fun normalizeMethodHandleType(handle: MethodHandle): MethodHandle {
            return if (handle.type().parameterCount() == 0) {
                handle.asType(MethodType.methodType(Any::class.java))
            } else {
                handle.asSpreader(Array<Any>::class.java, handle.type().parameterCount())
                    .asType(MethodType.methodType(Any::class.java, Array<Any>::class.java))
            }
        }

        private fun findField(clazz: Class<*>, name: String): Field {
            return FieldUtils.getField(clazz, name, true) ?: throw NoSuchFieldException(name)
        }

        private fun findConstructor(
            clazz: Class<*>,
            method: Method,
        ): Constructor<*> {
            return clazz.getDeclaredConstructor(*method.parameterTypes)
                .apply { isAccessible = true }
        }

        private fun findMethod(
            clazz: Class<*>,
            original: Method,
            nameAnnotation: Name?,
            staticAnnotation: Static?,
        ): Method {
            val params = original.parameters
                .drop(if (staticAnnotation == null) 1 else 0)
                .map { it.type }
                .toTypedArray()
            val methodName = getMethodName(original, nameAnnotation, null, staticAnnotation, null)
            return MethodUtils.getMatchingAccessibleMethod(clazz, methodName, *params)
                ?: throw NoSuchMethodException("Method $methodName with params ${params.joinToString()}")
        }

        private fun getMethodName(
            method: Method,
            nameAnnotation: Name? = null,
            fieldAnnotation: FieldAnnotation? = null,
            staticAnnotation: Static? = null,
            constructorAnnotation: ConstructorAnnotation? = null,
        ) = when {
            nameAnnotation?.value?.isNotBlank() == true -> nameAnnotation.value
            fieldAnnotation?.name?.isNotBlank() == true -> fieldAnnotation.name
            staticAnnotation?.name?.isNotBlank() == true -> staticAnnotation.name
            constructorAnnotation != null -> method.returnType.simpleName
            else -> method.name
        }

        private fun checkPramCount(method: Method, expected: Int) {
            check(method.parameterCount == expected) { "Method ${method.name} must have $expected parameters, found ${method.parameterCount}" }
        }

        private fun isEqualsMethod(method: Method) =
            method.name == "equals" && method.parameterCount == 1 && method.parameterTypes[0] == Any::class.java

        private fun isHashCodeMethod(method: Method) =
            method.name == "hashCode" && method.parameterCount == 0

        private fun isToStringMethod(method: Method) =
            method.name == "toString" && method.parameterCount == 0
    }

    // @formatter:off
    private val normalCache = mutableObject2ObjectMapOf<Method, MethodHandle>().synchronize()
    private val getterCache = mutableObject2ObjectMapOf<Method, MethodHandle>().synchronize()
    private val setterCache = mutableObject2ObjectMapOf<Method, MethodHandle>().synchronize()
    private val staticGetterCache = mutableObject2ObjectMapOf<Method, MethodHandle>().synchronize()
    private val staticSetterCache = mutableObject2ObjectMapOf<Method, MethodHandle>().synchronize()
    private val defaultCache = mutableObject2ObjectMapOf<Method, MethodHandle>().synchronize()
    private val staticFinalFieldSetterCache = mutableObject2ObjectMapOf<Method, Field>().synchronize()
    private val finalFieldSetterCache = mutableObject2ObjectMapOf<Method, Field>().synchronize()
    // @formatter:on

    init {
        cacheInterfaceMethods()
    }

    override fun invoke(
        proxy: Any,
        method: Method,
        args: Array<out Any?>?,
    ): Any? = when {
        isEqualsMethod(method) -> proxy == args?.get(0)
        isHashCodeMethod(method) -> System.identityHashCode(proxy)
        isToStringMethod(method) -> ToStringBuilder.reflectionToString(proxy)
        method.isDefault -> handleDefault(proxy, method, args)
        else -> invokeFromCache(method, args ?: emptyArray())
    }

    private fun invokeFromCache(method: Method, args: Array<out Any?>): Any? {
        val normal = normalCache[method]
        if (normal != null) {
            return if (args.isEmpty()) normal.invokeExact() else normal.invokeWithArguments(args)
        }

        val getter = getterCache[method]
        if (getter != null) {
            return getter.invokeExact(args[0])
        }

        val setter = setterCache[method]
        if (setter != null) {
            return setter.invokeExact(args[0], args[1])
        }

        val staticGetter = staticGetterCache[method]
        if (staticGetter != null) {
            return staticGetter.invokeExact()
        }

        val staticSetter = staticSetterCache[method]
        if (staticSetter != null) {
            return staticSetter.invokeExact(args[0])
        }

        val staticFinalFieldSetter = staticFinalFieldSetterCache[method]
        if (staticFinalFieldSetter != null) {
            return setStaticFinalField(staticFinalFieldSetter, args[0])
        }

        val finalFieldSetter = finalFieldSetterCache[method]
        if (finalFieldSetter != null) {
            return setFinalField(finalFieldSetter, args[0]!!, args[1])
        }

        throw UnsupportedOperationException("Method $method is not supported")
    }

    // @formatter:off - very long compute if absent lambda
    private fun handleDefault(proxy: Any, method: Method, args: Array<out Any?>?): Any? {
        val handle = defaultCache.computeIfAbsent(method) { normalizeMethodHandleType(MethodHandles.privateLookupIn(proxyClass, lookup).findSpecial(proxyClass, method.name, MethodType.methodType(method.returnType, method.parameterTypes), proxyClass).bindTo(proxy)) }
        return if (args == null) handle.invokeExact() else handle.invokeExact(args)
    }
    // @formatter:on

    // @formatter:off - This is a very long method with a lot of type checking,
    // so it's easier to read with the formatting off
    private fun cacheInterfaceMethods() {
        for (method in proxyClass.declaredMethods) {
            if (isEqualsMethod(method) || isHashCodeMethod(method) || isToStringMethod(method) || method.isSynthetic || method.isDefault) {
                continue
            }

            val fieldAnnotation = method.findAnnotation<FieldAnnotation>()
            val staticAnnotation = method.findAnnotation<Static>()
            val constructorAnnotation = method.findAnnotation<ConstructorAnnotation>()
            val nameAnnotation = method.findAnnotation<Name>()

            if (fieldAnnotation != null) {
                val fieldName = getMethodName(method, nameAnnotation, fieldAnnotation, staticAnnotation)
                val field = findField(proxiedClass, fieldName)

                // Handle final fields based on whether they are static or instance fields
                if (fieldAnnotation.overrideFinal) {
                    if (staticAnnotation != null) {
                        if (fieldAnnotation.type == FieldAnnotation.Type.GETTER) {
                            checkPramCount(method, 0)
                            staticGetterCache[method] = lookup.unreflectGetter(field)
                        } else {
                            checkPramCount(method, 1)
                            staticFinalFieldSetterCache[method] = field
                        }
                    } else {
                        if (fieldAnnotation.type == FieldAnnotation.Type.GETTER) {
                            checkPramCount(method, 1)
                            getterCache[method] = lookup.unreflectGetter(field)
                        } else {
                            checkPramCount(method, 2)
                            finalFieldSetterCache[method] = field
                        }
                    }
                } else {
                    val methodHandle = lookup.unreflectGetter(field)

                    if (staticAnnotation != null) {
                        if (fieldAnnotation.type == FieldAnnotation.Type.GETTER) {
                            checkPramCount(method, 0)
                            staticGetterCache[method] = methodHandle.asType(MethodType.methodType(Any::class.java))
                        } else {
                            checkPramCount(method, 1)
                            staticSetterCache[method] = methodHandle.asType(MethodType.methodType(Any::class.java))
                        }
                    } else {
                        if (fieldAnnotation.type == FieldAnnotation.Type.GETTER) {
                            checkPramCount(method, 1)
                            getterCache[method] = methodHandle.asType(MethodType.methodType(Any::class.java, Any::class.java))
                        } else {
                            checkPramCount(method, 2)
                            setterCache[method] = methodHandle.asType(MethodType.methodType(Any::class.java, Any::class.java))
                        }
                    }
                }
            } else if (constructorAnnotation != null) {
                normalCache[method] = normalizeMethodHandleType(lookup.unreflectConstructor(findConstructor(proxiedClass, method)))
            } else {
                check(staticAnnotation != null || method.parameterCount > 0) { "method $method must have at least one parameter" }
                normalCache[method] = normalizeMethodHandleType(lookup.unreflect(findMethod(proxiedClass, method, nameAnnotation, staticAnnotation)))
            }
        }
    }
    // @formatter:on
}