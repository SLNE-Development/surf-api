package dev.slne.surf.api.core.server.impl.event.invoker

import dev.slne.surf.api.common.invoker.InvokerFactory
import dev.slne.surf.api.common.invoker.InvokerMethods
import dev.slne.surf.api.event.SurfAsyncEvent
import dev.slne.surf.api.event.SurfSyncEvent
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.Method
import kotlin.coroutines.Continuation

/**
 * Creates the hidden-class invokers of `@SurfEventHandler` methods.
 *
 * An invoker ignores events that are not instances of the handler's event type.
 */
object SurfEventInvokerFactory {
    val lookup: MethodHandles.Lookup = MethodHandles.lookup()

    private val syncFactory = InvokerFactory(SurfSyncEventInvoker::class.java, lookup)
    private val asyncFactory = InvokerFactory(SurfAsyncEventInvoker::class.java, lookup)

    private val IS_INSTANCE = lookup.findVirtual(
        Class::class.java,
        "isInstance",
        MethodType.methodType(Boolean::class.javaPrimitiveType, Any::class.java),
    )

    /**
     * Creates an invoker of the non-suspending [method] on [listener].
     *
     * @param listener the bean declaring [method]
     * @param method the `@SurfEventHandler` method
     * @param eventType the event type [method] accepts
     * @return the invoker
     */
    fun createSync(
        listener: Any,
        method: Method,
        eventType: Class<out SurfSyncEvent>,
    ): SurfSyncEventInvoker {
        val type = syncFactory.invokerMethodType
        val handler = InvokerMethods.bind(method, listener, lookup).asType(type)

        return syncFactory.create(onlyFor(eventType, handler, MethodHandles.empty(type)))
    }

    /**
     * Creates an invoker of [method] on [listener].
     *
     * A non-suspending [method] completes with `Unit`.
     *
     * @param listener the bean declaring [method]
     * @param method the `@SurfEventHandler` method
     * @param eventType the event type [method] accepts
     * @return the invoker
     */
    fun createAsync(
        listener: Any,
        method: Method,
        eventType: Class<out SurfAsyncEvent>,
    ): SurfAsyncEventInvoker {
        val type = asyncFactory.invokerMethodType
        val handler = asContinuationCall(InvokerMethods.bind(method, listener, lookup), method)
            .asType(type)
        val skip = MethodHandles.dropArguments(
            MethodHandles.constant(Any::class.java, Unit),
            0,
            type.parameterList(),
        )

        return asyncFactory.create(onlyFor(eventType, handler, skip))
    }

    private fun asContinuationCall(handle: MethodHandle, method: Method): MethodHandle {
        if (InvokerMethods.isSuspendFunction(method)) return handle

        val discardingResult = handle.asType(handle.type().changeReturnType(Void.TYPE))
        val returningUnit = MethodHandles.filterReturnValue(
            discardingResult,
            MethodHandles.constant(Any::class.java, Unit),
        )

        return MethodHandles.dropArguments(returningUnit, 1, Continuation::class.java)
    }

    private fun onlyFor(
        eventType: Class<*>,
        handler: MethodHandle,
        skip: MethodHandle,
    ): MethodHandle {
        val eventParameter = handler.type().parameterType(0)
        val test = IS_INSTANCE.bindTo(eventType)
            .asType(MethodType.methodType(Boolean::class.javaPrimitiveType, eventParameter))

        return MethodHandles.guardWithTest(test, handler, skip)
    }
}
