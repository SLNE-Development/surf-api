package dev.slne.surf.surfapi.core.server.impl.reflection

import dev.slne.surf.surfapi.core.api.reflection.Constructor
import dev.slne.surf.surfapi.core.api.reflection.Field
import dev.slne.surf.surfapi.core.api.reflection.Name
import dev.slne.surf.surfapi.core.api.reflection.Static
import dev.slne.surf.surfapi.core.api.util.mutableObject2ObjectMapOf
import dev.slne.surf.surfapi.core.api.util.setFinalField
import dev.slne.surf.surfapi.core.api.util.setStaticFinalField
import dev.slne.surf.surfapi.core.api.util.synchronize
import it.unimi.dsi.fastutil.objects.Object2ObjectMap
import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.reflect.FieldUtils
import org.apache.commons.lang3.reflect.MethodUtils
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method

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

        private fun findField(clazz: Class<*>, name: String): java.lang.reflect.Field {
            return FieldUtils.getField(clazz, name, true) ?: throw NoSuchFieldException(name)
        }

        private fun findConstructor(
            clazz: Class<*>,
            method: Method
        ): java.lang.reflect.Constructor<*> {
            return clazz.getDeclaredConstructor(*method.parameterTypes)
                .apply { isAccessible = true }
        }

        private fun findMethod(
            clazz: Class<*>,
            original: Method,
            nameAnnotation: Name?,
            staticAnnotation: Static?
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
            nameAnnotation: Name?,
            fieldAnnotation: Field?,
            staticAnnotation: Static?,
            constructorAnnotation: Constructor?
        ): String {
            return when {
                nameAnnotation?.value?.isNotBlank() == true -> nameAnnotation.value
                fieldAnnotation?.name?.isNotBlank() == true -> fieldAnnotation.name
                staticAnnotation?.name?.isNotBlank() == true -> staticAnnotation.name
                constructorAnnotation != null -> method.returnType.simpleName
                else -> method.name
            }
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
    private val staticFinalFieldSetterCache = mutableObject2ObjectMapOf<Method, java.lang.reflect.Field>().synchronize()
    private val finalFieldSetterCache = mutableObject2ObjectMapOf<Method, java.lang.reflect.Field>().synchronize()
    // @formatter:on

    init {
        cacheInterfaceMethods()
    }

    override fun invoke(
        proxy: Any,
        method: Method,
        args: Array<out Any?>?
    ): Any? {
        return when {
            isEqualsMethod(method) -> proxy == args?.get(0)
            isHashCodeMethod(method) -> System.identityHashCode(proxy)
            isToStringMethod(method) -> ToStringBuilder.reflectionToString(proxy)
            method.isDefault -> handleDefault(proxy, method, args)
            else -> invokeFromCache(method, args) ?: throw UnsupportedOperationException("Method $method is not supported")
        }
    }

    private fun invokeFromCache(method: Method, args: Array<out Any?>?): Any? {
        return invoke(normalCache, method, args)
            ?: invoke(getterCache, method, args)
            ?: invoke(setterCache, method, args)
            ?: invoke(staticGetterCache, method, args)
            ?: invoke(staticSetterCache, method, args)
            ?: invokeStaticFinalFieldSetter(method, args)
            ?: invokeFinalFieldSetter(method, args)
    }

    private fun invoke(
        cache: Object2ObjectMap<Method, MethodHandle>,
        method: Method,
        args: Array<out Any?>?
    ): Any? {
        val methodHandle = cache[method] ?: return null
        return if (args == null) methodHandle.invokeExact() else methodHandle.invokeWithArguments(*args)
    }

    private fun invokeStaticFinalFieldSetter(method: Method, args: Array<out Any?>?): Any? {
        val field = staticFinalFieldSetterCache[method] ?: return null
        setStaticFinalField(field, args?.get(0))
        return Void.TYPE
    }

    private fun invokeFinalFieldSetter(method: Method, args: Array<out Any?>?): Any? {
        val field = finalFieldSetterCache[method] ?: return null
        setFinalField(field, args!![0]!!, args[1])
        return Void.TYPE
    }

    private fun handleDefault(proxy: Any, method: Method, args: Array<out Any?>?): Any? {
        val handle = defaultCache.computeIfAbsent(method) {
            normalizeMethodHandleType(
                MethodHandles.privateLookupIn(proxyClass, lookup)
                    .findSpecial(
                        proxyClass,
                        method.name,
                        MethodType.methodType(method.returnType, method.parameterTypes),
                        proxyClass
                    ).bindTo(proxy)
            )
        }

        return if (args == null) handle.invokeExact() else handle.invokeWithArguments(*args)
    }

    private fun cacheInterfaceMethods() {
        for (method in proxyClass.declaredMethods) {
            if (isEqualsMethod(method) || isHashCodeMethod(method) || isToStringMethod(method) || method.isSynthetic || method.isDefault) {
                continue
            }

            val fieldAnnotation = method.getDeclaredAnnotation(Field::class.java)
            val staticAnnotation = method.getDeclaredAnnotation(Static::class.java)
            val constructorAnnotation = method.getDeclaredAnnotation(Constructor::class.java)
            val nameAnnotation = method.getDeclaredAnnotation(Name::class.java)

            if (fieldAnnotation != null) {
                val fieldName =
                    getMethodName(method, nameAnnotation, fieldAnnotation, staticAnnotation, null)
                val field = findField(proxiedClass, fieldName)

                when {
                    fieldAnnotation.overrideFinal -> when {
                        staticAnnotation != null -> when (fieldAnnotation.type) {
                            Field.Type.GETTER -> {
                                checkPramCount(method, 0)
                                staticGetterCache[method] = lookup.unreflectGetter(field)
                            }

                            Field.Type.SETTER -> {
                                checkPramCount(method, 1)
                                staticFinalFieldSetterCache[method] = field
                            }
                        }

                        else -> when (fieldAnnotation.type) {
                            Field.Type.GETTER -> {
                                checkPramCount(method, 1)
                                getterCache[method] = lookup.unreflectGetter(field)
                            }

                            Field.Type.SETTER -> {
                                checkPramCount(method, 2)
                                finalFieldSetterCache[method] = field
                            }
                        }
                    }

                    else -> {
                        val methodHandle = lookup.unreflectGetter(field)
                        when {
                            staticAnnotation != null -> when (fieldAnnotation.type) {
                                Field.Type.GETTER -> {
                                    checkPramCount(method, 0)
                                    staticGetterCache[method] = methodHandle
                                        .asType(MethodType.methodType(Any::class.java))
                                }

                                Field.Type.SETTER -> {
                                    checkPramCount(method, 1)
                                    staticSetterCache[method] = methodHandle
                                        .asType(MethodType.methodType(Any::class.java))
                                }
                            }

                            else -> when (fieldAnnotation.type) {
                                Field.Type.GETTER -> {
                                    checkPramCount(method, 1)
                                    getterCache[method] = methodHandle
                                        .asType(
                                            MethodType.methodType(
                                                Any::class.java,
                                                Any::class.java
                                            )
                                        )
                                }

                                Field.Type.SETTER -> {
                                    checkPramCount(method, 2)
                                    setterCache[method] = methodHandle
                                        .asType(
                                            MethodType.methodType(
                                                Any::class.java,
                                                Any::class.java
                                            )
                                        )
                                }
                            }
                        }
                    }
                }
            } else if (constructorAnnotation != null) {
                normalCache[method] = normalizeMethodHandleType(
                    lookup.unreflectConstructor(findConstructor(proxiedClass, method))
                )
            } else {
                check(staticAnnotation != null || method.parameterCount > 0) {
                    "Method ${method.declaringClass.name}#${method.name}(${
                        method.parameterTypes.joinToString(
                            ", "
                        ) { it.simpleName }
                    }) must have at least 1 parameter"
                }
                normalCache[method] = normalizeMethodHandleType(
                    lookup.unreflect(
                        findMethod(
                            proxiedClass,
                            method,
                            nameAnnotation,
                            staticAnnotation
                        )
                    )
                )
            }
        }
    }
}