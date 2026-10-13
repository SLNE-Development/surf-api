package dev.slne.surf.nats.bytecode

import dev.slne.surf.nats.InternalNatsApi
import org.jetbrains.annotations.TestOnly
import java.lang.constant.ClassDesc
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.Method
import java.util.Objects
import kotlin.coroutines.Continuation

@InternalNatsApi
internal object ServiceInvokerFactory {

    /**
     * @see ServiceInvoker
     */
    private val INVOKER_TYPE = MethodType.methodType(
        Any::class.java,
        Array<Any?>::class.java,
        Continuation::class.java
    )

    private val LOOKUP = MethodHandles.lookup()

    private val IS_NULL = LOOKUP.findStatic(
        Objects::class.java,
        "isNull",
        MethodType.methodType(Boolean::class.javaPrimitiveType, Any::class.java)
    )

    private val invokerClassBytes by lazy {
        HandleInvokerClasses.generate(
            ClassDesc.of(ServiceInvoker::class.java.packageName, "ServiceInvokerImpl"),
            ClassDesc.of(ServiceInvoker::class.java.name),
            "invoke",
            INVOKER_TYPE.describeConstable().orElseThrow(),
        )
    }

    fun create(
        implementation: Any,
        method: Method,
        valueClasses: List<ValueClassParameter?> = ValueClassParameters.of(method),
    ): ServiceInvoker {
        val hidden = LOOKUP.defineHiddenClassWithClassData(
            invokerClassBytes,
            adaptHandle(implementation, method, valueClasses),
            true
        )

        return hidden
            .findConstructor(hidden.lookupClass(), MethodType.methodType(Void.TYPE))
            .invoke() as ServiceInvoker
    }

    /**
     * Turns [method] into a [MethodHandle] with the shape `(Object[], Continuation) -> Object`
     * Bound to [impl]
     */
    private fun adaptHandle(impl: Any, method: Method, valueClasses: List<ValueClassParameter?>): MethodHandle {
        val argumentCount = method.parameterCount - 1 // without continuation

        // Object(arg1, arg2, ..., continuation)
        val unreflected = MethodHandles.privateLookupIn(method.declaringClass, LOOKUP)
            .unreflect(method)
            .bindTo(impl)

        // unboxed value class arguments take the value class instead of its underlying type
        val unboxers = valueClasses.map { it?.let(::unboxer) }.toTypedArray()
        val bound = MethodHandles.filterArguments(unreflected, 0, *unboxers)

        // turn all arguments except continuation into object types
        val generic = bound.type()
            .generic()
            .changeParameterType(argumentCount, Continuation::class.java)

        return bound.asType(generic)
            .asSpreader(0, Array<Any?>::class.java, argumentCount) // Object(args[], continuation)
            .asType(INVOKER_TYPE)
    }

    // (ValueClass) -> underlying, mapping null to null for a reference underlying type
    private fun unboxer(parameter: ValueClassParameter): MethodHandle {
        val unbox = LOOKUP.unreflect(parameter.unbox)
        if (parameter.underlying.isPrimitive) return unbox

        val isNull = IS_NULL.asType(unbox.type().changeReturnType(Boolean::class.javaPrimitiveType))
        return MethodHandles.guardWithTest(isNull, MethodHandles.empty(unbox.type()), unbox)
    }

    @TestOnly
    internal fun createInvokerClass(): ByteArray {
        return invokerClassBytes
    }
}