package dev.slne.surf.nats.bytecode

import dev.slne.surf.api.common.invoker.InvokerFactory
import dev.slne.surf.api.common.invoker.InvokerMethods
import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.util.isSuspend
import org.jetbrains.annotations.TestOnly
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.reflect.Method
import kotlin.coroutines.Continuation

/**
 * Creates a [ListenerInvoker] per listener method as a hidden class that calls the method through a
 * constant [MethodHandle].
 *
 * All invokers share one generated class template. Each hidden class gets its own handle as class
 * data.
 */
@InternalNatsApi
internal object ListenerInvokerFactory {

    private val LOOKUP = MethodHandles.lookup()
    private val factory = InvokerFactory(ListenerInvoker::class.java, LOOKUP)

    /**
     * Creates an invoker of [method] on [target]. [target] may be `null` only for a static method.
     *
     * A non-suspending [method] is adapted to return `Unit` and ignore the continuation.
     */
    fun create(target: Any?, method: Method): ListenerInvoker {
        return factory.create(adaptHandle(target, method))
    }

    /**
     * Turns [method] into a [MethodHandle] with the shape `(Object, Continuation) -> Object`.
     */
    private fun adaptHandle(target: Any?, method: Method): MethodHandle {
        val handle = InvokerMethods.bind(method, target, LOOKUP)

        // suspend methods are already of the shape Object(event, continuation)
        if (method.isSuspend()) return handle

        // void(event) -> Object(event) (always returns Unit)
        val returningUnit = MethodHandles.filterReturnValue(
            handle,
            MethodHandles.constant(Any::class.java, Unit),
        )

        // Object(event) -> Object(event, continuation) (ignores continuation)
        return MethodHandles.dropArguments(returningUnit, 1, Continuation::class.java)
    }

    @TestOnly
    internal fun createInvokerClass(): ByteArray {
        return factory.classBytes
    }
}
