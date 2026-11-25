package dev.slne.surf.api.reflection

import dev.slne.surf.api.common.invoker.InvokerMethods
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.invoke.WrongMethodTypeException
import java.lang.reflect.Executable
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * Binds the methods of a proxy interface to members of [target].
 *
 * Every handle [bind] returns has exactly the type of the proxy method it was bound for.
 */
internal class ProxyMemberBinder(private val target: Class<*>) {

    /**
     * Returns the handle that implements [method].
     *
     * @param method an abstract method of the proxy interface
     * @return a handle of exactly [method]'s type
     * @throws ReflectionProxyException if [method] is declared wrongly, matches no member, or the
     * members of [target] name a class that cannot be loaded
     */
    fun bind(method: Method): MethodHandle {
        if (InvokerMethods.isSuspendFunction(method)) {
            fail("suspend functions are not supported")
        }

        val handle = try {
            unadaptedHandle(method)
        } catch (failure: ReflectiveOperationException) {
            fail("cannot access the member of ${target.name}: $failure", failure)
        } catch (failure: LinkageError) {
            fail("cannot resolve the members of ${target.name}: $failure", failure)
        }

        return try {
            handle.asType(MethodType.methodType(method.returnType, method.parameterTypes))
        } catch (failure: WrongMethodTypeException) {
            fail("the member's type ${handle.type()} does not fit the proxy method", failure)
        }
    }

    private fun unadaptedHandle(method: Method): MethodHandle {
        val reflectedMethod = method.getAnnotation(ReflectedMethod::class.java)
        val reflectedField = method.getAnnotation(ReflectedField::class.java)
        val reflectedConstructor = method.getAnnotation(ReflectedConstructor::class.java)
        val isStatic = method.isAnnotationPresent(ReflectedStatic::class.java)

        val kinds = listOfNotNull(reflectedMethod, reflectedField, reflectedConstructor)

        if (kinds.size != 1) {
            fail(
                "annotate it with exactly one of @ReflectedMethod, @ReflectedField and " +
                    "@ReflectedConstructor",
            )
        }

        return when {
            reflectedMethod != null -> methodHandle(method, reflectedMethod, isStatic)
            reflectedField != null -> fieldHandle(method, reflectedField, isStatic)
            else -> {
                if (isStatic) {
                    fail("@ReflectedStatic does not apply to @ReflectedConstructor")
                }

                constructorHandle(method)
            }
        }
    }

    private fun methodHandle(
        method: Method,
        annotation: ReflectedMethod,
        isStatic: Boolean,
    ): MethodHandle {
        val name = annotation.name.ifEmpty { method.name }
        val parameterTypes = memberParameterTypes(method, isStatic)
        val candidates = hierarchy()
            .flatMap { it.declaredMethods.asSequence() }
            .filter { it.name == name && !it.isBridge }
            .filter { Modifier.isStatic(it.modifiers) == isStatic }
            .toList()

        val member = select(candidates, parameterTypes, "${describe(isStatic)}method $name")

        return privateLookup(member.declaringClass).unreflect(member)
    }

    private fun fieldHandle(
        method: Method,
        annotation: ReflectedField,
        isStatic: Boolean,
    ): MethodHandle {
        val name = annotation.name.ifEmpty { method.name }
        val receiverCount = if (isStatic) 0 else 1
        val valueCount = if (annotation.access == FieldAccess.SET) 1 else 0
        checkParameterCount(method, receiverCount + valueCount)

        val field = hierarchy()
            .mapNotNull { type -> type.declaredFields.firstOrNull { it.name == name } }
            .firstOrNull { Modifier.isStatic(it.modifiers) == isStatic }
            ?: fail("${target.name} has no ${describe(isStatic)}field $name")

        val lookup = privateLookup(field.declaringClass)

        return when (annotation.access) {
            FieldAccess.GET -> lookup.unreflectGetter(field)
            FieldAccess.SET -> {
                checkWritable(field)
                lookup.unreflectSetter(field)
            }
        }
    }

    private fun constructorHandle(method: Method): MethodHandle {
        if (Modifier.isAbstract(target.modifiers)) {
            fail("${target.name} cannot be instantiated")
        }

        val constructor = select(
            target.declaredConstructors.toList(),
            method.parameterTypes.toList(),
            "constructor",
        )

        return privateLookup(target).unreflectConstructor(constructor)
    }

    private fun memberParameterTypes(method: Method, isStatic: Boolean): List<Class<*>> {
        val parameterTypes = method.parameterTypes.toList()
        if (isStatic) return parameterTypes

        if (parameterTypes.isEmpty()) {
            fail("an instance member takes the receiver first; mark it @ReflectedStatic if static")
        }

        if (!isCompatible(parameterTypes.first(), target)) {
            fail("receiver type ${parameterTypes.first().name} does not fit ${target.name}")
        }

        return parameterTypes.drop(1)
    }

    private fun <E : Executable> select(
        candidates: List<E>,
        parameterTypes: List<Class<*>>,
        description: String,
    ): E {
        val sameCount = candidates
            .filter { it.parameterCount == parameterTypes.size }
            .distinctBy { it.parameterTypes.toList() }

        sameCount.firstOrNull { it.parameterTypes.toList() == parameterTypes }?.let { return it }

        val compatible = sameCount.filter { candidate ->
            candidate.parameterTypes.zip(parameterTypes).all { (member, proxy) ->
                isCompatible(proxy, member)
            }
        }

        return when (compatible.size) {
            1 -> compatible.single()
            0 -> fail(
                "${target.name} has no $description${parameterTypes.signature()}; " +
                    "candidates: ${candidates.map { it.parameterTypes.toList().signature() }}",
            )

            else -> fail(
                "$description${parameterTypes.signature()} is ambiguous in ${target.name}: " +
                    compatible.map { it.parameterTypes.toList().signature() },
            )
        }
    }

    private fun checkParameterCount(method: Method, expected: Int) {
        if (method.parameterCount != expected) {
            fail("expected $expected parameters, found ${method.parameterCount}")
        }
    }

    private fun checkWritable(field: Field) {
        if (Modifier.isFinal(field.modifiers)) {
            val name = "${field.declaringClass.name}.${field.name}"
            fail("$name is final; final writes are unsupported")
        }
    }

    private fun hierarchy(): Sequence<Class<*>> {
        val classes = generateSequence(target) { it.superclass }.toList()
        val interfaces = classes
            .flatMap { allInterfaces(it) }
            .distinct()

        return (classes + interfaces).asSequence()
    }

    private fun allInterfaces(type: Class<*>): List<Class<*>> {
        return type.interfaces.flatMap { listOf(it) + allInterfaces(it) }
    }

    /**
     * Returns a private lookup in [type], or surf-api's own lookup when [type]'s module does not
     * open its package, as for JDK classes; that lookup reaches public members only.
     */
    private fun privateLookup(type: Class<*>): MethodHandles.Lookup {
        return try {
            MethodHandles.privateLookupIn(type, ProxyLookups.LOOKUP)
        } catch (_: IllegalAccessException) {
            ProxyLookups.LOOKUP
        }
    }

    private fun isCompatible(proxy: Class<*>, member: Class<*>): Boolean {
        if (proxy.isPrimitive || member.isPrimitive) {
            return proxy.kotlin.javaObjectType == member.kotlin.javaObjectType
        }

        return proxy.isAssignableFrom(member) || member.isAssignableFrom(proxy)
    }

    private fun describe(isStatic: Boolean) = if (isStatic) "static " else ""

    private fun List<Class<*>>.signature() = joinToString(prefix = "(", postfix = ")") { it.name }

    private fun fail(message: String, cause: Throwable? = null): Nothing {
        throw ReflectionProxyException(message, cause)
    }
}
