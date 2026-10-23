package dev.slne.surf.nats.bytecode

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.util.isSuspend
import org.jetbrains.annotations.TestOnly
import java.lang.constant.ClassDesc
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.coroutines.Continuation

@InternalNatsApi
internal object ListenerInvokerFactory {

    /**
     * @see ListenerInvoker
     */
    private val INVOKER_TYPE = MethodType.methodType(
        Any::class.java,
        Any::class.java,
        Continuation::class.java
    )

    private val LOOKUP = MethodHandles.lookup()

    private val invokerClassBytes by lazy {
        HandleInvokerClasses.generate(
            ClassDesc.of(ListenerInvoker::class.java.packageName, "ListenerInvokerImpl"),
            ClassDesc.of(ListenerInvoker::class.java.name),
            "invoke",
            INVOKER_TYPE.describeConstable().orElseThrow(),
        )
    }

    fun create(target: Any?, method: Method): ListenerInvoker {
        val handle = adaptHandle(target, method)
        val hidden = LOOKUP.defineHiddenClassWithClassData(invokerClassBytes, handle, true)
        return hidden
            .findConstructor(hidden.lookupClass(), MethodType.methodType(Void.TYPE))
            .invoke() as ListenerInvoker
    }

    /**
     * Turns [method] into a [MethodHandle] with the shape `(Object, Continuation) -> Object`
     */
    private fun adaptHandle(target: Any?, method: Method): MethodHandle {
        var handle = MethodHandles.privateLookupIn(method.declaringClass, LOOKUP)
            .unreflect(method)

        if (!Modifier.isStatic(method.modifiers)) {
            handle = handle.bindTo(requireNotNull(target) {
                "Listener instance required for non-static $method"
            })
        }

        // suspend methods are already of the shape Object(event, continuation)
        if (!method.isSuspend()) {
            // void(event) -> Object(event) (always returns Unit)
            handle = MethodHandles.filterReturnValue(
                handle,
                MethodHandles.constant(Any::class.java, Unit)
            )

            // Object(event) -> Object(event, continuation) (ignores continuation)
            handle = MethodHandles.dropArguments(handle, 1, Continuation::class.java)
        }

        return handle.asType(INVOKER_TYPE)
    }

    @TestOnly
    internal fun createInvokerClass(): ByteArray {
        return invokerClassBytes
    }
}