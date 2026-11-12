package dev.slne.surf.nats.bytecode

import dev.slne.surf.nats.InternalNatsApi
import org.jetbrains.annotations.TestOnly
import java.lang.constant.ClassDesc
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.Method
import kotlin.coroutines.Continuation

/**
 * Creates a [ServiceInvoker] per service method as a hidden class that calls the method through a constant
 * [MethodHandle].
 *
 * All invokers share one generated class template. Each hidden class gets its own handle as class data.
 */
@InternalNatsApi
internal object ServiceInvokerFactory {

    /**
     * @see ServiceInvoker
     */
    private val INVOKER_TYPE = MethodType.methodType(
        Any::class.java,
        Any::class.java,
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

    /**
     * Creates an invoker of the suspending [method] on [implementation].
     */
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
     * Turns [method] into a [MethodHandle] with the shape `(Object, Continuation) -> Object` bound to [impl].
     *
     * [method] takes either the request and a [Continuation] or only a [Continuation]. In the latter case the
     * handle ignores the request.
     */
    private fun adaptHandle(impl: Any, method: Method): MethodHandle {
        // Object([request,] continuation)
        val bound = MethodHandles.privateLookupIn(method.declaringClass, LOOKUP)
            .unreflect(method)
            .bindTo(impl)

        val withRequest = if (method.parameterCount == 1) {
            MethodHandles.dropArguments(bound, 0, Any::class.java)
        } else {
            bound
        }

        return withRequest.asType(INVOKER_TYPE)
    }

    @TestOnly
    internal fun createInvokerClass(): ByteArray {
        return invokerClassBytes
    }
}
