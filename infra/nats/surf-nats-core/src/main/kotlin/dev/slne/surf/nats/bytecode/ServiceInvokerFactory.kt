package dev.slne.surf.nats.bytecode

import dev.slne.surf.nats.InternalNatsApi
import org.jetbrains.annotations.TestOnly
import java.lang.constant.ClassDesc
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.Method
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

    private val invokerClassBytes by lazy {
        HandleInvokerClasses.generate(
            ClassDesc.of(ServiceInvoker::class.java.packageName, "ServiceInvokerImpl"),
            ClassDesc.of(ServiceInvoker::class.java.name),
            "invoke",
            INVOKER_TYPE.describeConstable().orElseThrow(),
        )
    }

    fun create(implementation: Any, method: Method): ServiceInvoker {
        val hidden = LOOKUP.defineHiddenClassWithClassData(
            invokerClassBytes,
            adaptHandle(implementation, method),
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
    private fun adaptHandle(impl: Any, method: Method): MethodHandle {
        val argumentCount = method.parameterCount - 1 // without continuation

        // Object(arg1, arg2, ..., continuation)
        val bound = MethodHandles.privateLookupIn(method.declaringClass, LOOKUP)
            .unreflect(method)
            .bindTo(impl)

        // turn all arguments except continuation into object types
        val generic = bound.type()
            .generic()
            .changeParameterType(argumentCount, Continuation::class.java)

        return bound.asType(generic)
            .asSpreader(0, Array<Any?>::class.java, argumentCount) // Object(args[], continuation)
            .asType(INVOKER_TYPE)
    }

    @TestOnly
    internal fun createInvokerClass(): ByteArray {
        return invokerClassBytes
    }
}