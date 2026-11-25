package dev.slne.surf.api.common.invoker

import dev.slne.surf.api.shared.api.util.InternalSurfApi
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.coroutines.Continuation

/**
 * Inspects and unreflects the methods that invoker factories call.
 */
@InternalSurfApi
object InvokerMethods {

    /**
     * Returns whether [method] is a compiled Kotlin `suspend` function, that is whether its last
     * parameter is a [Continuation].
     *
     * @param method the method to inspect
     * @return `true` for a `suspend` function
     */
    fun isSuspendFunction(method: Method): Boolean {
        val lastParameter = method.parameterTypes.lastOrNull() ?: return false

        return Continuation::class.java.isAssignableFrom(lastParameter)
    }

    /**
     * Returns whether [lookup] can unreflect [method] through a private lookup in its declaring
     * class.
     *
     * @param method the method to call
     * @param lookup the lookup the private lookup is derived from
     * @return `true` if [bind] succeeds for [method]
     */
    fun canAccess(method: Method, lookup: MethodHandles.Lookup): Boolean {
        return try {
            unreflect(method, lookup)
            true
        } catch (_: IllegalAccessException) {
            false
        }
    }

    /**
     * Unreflects [method] through a private lookup in its declaring class and binds an instance
     * method to [target].
     *
     * @param method the method to call
     * @param target the receiver of an instance method; ignored for a static one
     * @param lookup the lookup the private lookup is derived from
     * @return the handle, without the receiver parameter for an instance method
     * @throws IllegalAccessException if [lookup] cannot access [method]
     * @throws IllegalArgumentException if [method] is an instance method and [target] is `null`
     */
    fun bind(method: Method, target: Any?, lookup: MethodHandles.Lookup): MethodHandle {
        val handle = unreflect(method, lookup)
        if (Modifier.isStatic(method.modifiers)) return handle

        return handle.bindTo(requireNotNull(target) { "Instance required for non-static $method" })
    }

    private fun unreflect(method: Method, lookup: MethodHandles.Lookup): MethodHandle {
        return MethodHandles.privateLookupIn(method.declaringClass, lookup).unreflect(method)
    }
}
