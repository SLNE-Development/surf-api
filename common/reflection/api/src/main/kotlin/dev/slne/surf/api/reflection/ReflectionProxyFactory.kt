package dev.slne.surf.api.reflection

import dev.slne.surf.api.common.invoker.HandleInvokerClasses
import dev.slne.surf.api.common.invoker.InvokerMethod
import java.lang.constant.ClassDesc
import java.lang.constant.MethodTypeDesc
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * Implements [ReflectionProxy] interfaces with one hidden class each.
 *
 * Every member is resolved before the class is generated, so a broken interface fails as a whole
 * with one [ReflectionProxyException] that lists every problem.
 */
internal object ReflectionProxyFactory {

    private const val PROXY_CLASS_SUFFIX = $$"$ReflectionProxy"

    /**
     * Creates a new instance of a hidden class implementing [type].
     *
     * @param type the [ReflectionProxy] interface
     * @return the proxy instance
     * @throws ReflectionProxyException if [type] cannot be implemented
     */
    fun create(type: Class<*>): Any {
        val declaration = declarationOf(type)
        val definingLookup = lazy { ProxyLookups.definingLookup(type) }
        val binder = ProxyMemberBinder(targetOf(type, declaration), type, definingLookup)
        val methods = proxyMethods(type)

        val failures = mutableListOf<String>()
        val handles = methods.mapNotNull { method ->
            try {
                binder.bind(method)
            } catch (failure: ReflectionProxyException) {
                failures += "${method.name}: ${failure.message}"
                null
            }
        }

        if (failures.isNotEmpty()) {
            throw ReflectionProxyException(
                "Cannot implement ${type.name}:\n" + failures.joinToString("\n") { "  - $it" },
            )
        }

        return define(type, definingLookup, methods, handles)
    }

    private fun declarationOf(type: Class<*>): ReflectionProxy {
        if (!type.isInterface) throw ReflectionProxyException("${type.name} is not an interface")

        return type.getAnnotation(ReflectionProxy::class.java)
            ?: throw ReflectionProxyException("${type.name} is not annotated with @ReflectionProxy")
    }

    private fun targetOf(type: Class<*>, declaration: ReflectionProxy): Class<*> {
        val target = declaredTarget(type, declaration)
        val hasTarget = target != Unit::class.java
        val hasTargetName = declaration.targetName.isNotEmpty()

        if (hasTarget == hasTargetName) {
            throw ReflectionProxyException(
                "@ReflectionProxy on ${type.name} must set exactly one of target and targetName",
            )
        }

        if (hasTarget) return target

        return try {
            Class.forName(declaration.targetName, false, type.classLoader)
        } catch (failure: ClassNotFoundException) {
            throw targetNotFound(type, declaration.targetName, failure)
        } catch (failure: LinkageError) {
            throw targetNotFound(type, declaration.targetName, failure)
        }
    }

    private fun declaredTarget(type: Class<*>, declaration: ReflectionProxy): Class<*> {
        return try {
            declaration.target.java
        } catch (failure: TypeNotPresentException) {
            throw targetNotFound(type, failure.typeName(), failure)
        }
    }

    private fun targetNotFound(
        type: Class<*>,
        targetName: String,
        cause: Throwable,
    ): ReflectionProxyException {
        return ReflectionProxyException("Target class $targetName of ${type.name} not found", cause)
    }

    private fun proxyMethods(type: Class<*>): List<Method> {
        return type.methods
            .filter { Modifier.isAbstract(it.modifiers) && !isObjectMethod(it) }
            .distinctBy { it.name to it.parameterTypes.toList() }
    }

    private fun isObjectMethod(method: Method): Boolean {
        return try {
            Any::class.java.getMethod(method.name, *method.parameterTypes)
            true
        } catch (_: NoSuchMethodException) {
            false
        }
    }

    private fun define(
        type: Class<*>,
        definingLookup: Lazy<MethodHandles.Lookup>,
        methods: List<Method>,
        handles: List<MethodHandle>,
    ): Any {
        val classBytes = HandleInvokerClasses.generate(
            ClassDesc.of(type.name + PROXY_CLASS_SUFFIX),
            type.describeConstable().orElseThrow(),
            methods.map { InvokerMethod(it.name, descriptorOf(it)) },
        )

        return try {
            val lookup = definingLookup.value
            HandleInvokerClasses.instantiate(lookup, classBytes, handles)
        } catch (failure: ReflectiveOperationException) {
            throw definitionFailure(type, failure)
        } catch (failure: LinkageError) {
            throw definitionFailure(type, failure)
        } catch (failure: IllegalStateException) {
            throw definitionFailure(type, failure)
        }
    }

    private fun definitionFailure(type: Class<*>, cause: Throwable): ReflectionProxyException {
        return ReflectionProxyException("Cannot define the proxy class of ${type.name}", cause)
    }

    private fun descriptorOf(method: Method): MethodTypeDesc {
        return MethodTypeDesc.of(
            method.returnType.describeConstable().orElseThrow(),
            *method.parameterTypes.map { it.describeConstable().orElseThrow() }.toTypedArray(),
        )
    }
}
