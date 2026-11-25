package dev.slne.surf.nats.bytecode

import dev.slne.surf.api.common.invoker.InvokerFactory
import dev.slne.surf.api.common.invoker.InvokerMethods
import dev.slne.surf.nats.InternalNatsApi
import org.jetbrains.annotations.TestOnly
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.reflect.Method
import kotlin.coroutines.Continuation

/**
 * Creates a [ServiceInvoker] per service method as a hidden class that calls the method through a
 * constant [MethodHandle].
 *
 * All invokers share one generated class template. Each hidden class gets its own handle as class
 * data.
 */
@InternalNatsApi
internal object ServiceInvokerFactory {

    private val LOOKUP = MethodHandles.lookup()
    private val factory = InvokerFactory(ServiceInvoker::class.java, LOOKUP)

    /**
     * Creates an invoker of the suspending [method] on [implementation].
     */
    fun create(implementation: Any, method: Method): ServiceInvoker {
        return factory.create(adaptHandle(implementation, method))
    }

    /**
     * Turns [method] into a [MethodHandle] with the shape `(Object, Continuation) -> Object` bound
     * to [impl].
     *
     * [method] takes either the request and a [Continuation] or only a [Continuation]. In the
     * latter case the handle ignores the request.
     */
    private fun adaptHandle(impl: Any, method: Method): MethodHandle {
        // Object([request,] continuation)
        val bound = InvokerMethods.bind(method, impl, LOOKUP)

        return if (method.parameterCount == 1) {
            MethodHandles.dropArguments(bound, 0, Any::class.java)
        } else {
            bound
        }
    }

    @TestOnly
    internal fun createInvokerClass(): ByteArray {
        return factory.classBytes
    }
}
