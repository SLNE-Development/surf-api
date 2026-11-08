package dev.slne.surf.api.core.invoker

import dev.slne.surf.api.shared.api.util.InternalInvokerApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.lang.invoke.MethodHandle
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn

/**
 * Utility for invoking suspend-function-backed MethodHandles from hidden class templates.
 *
 * When a handler method is a Kotlin `suspend fun`, the compiled method has an extra
 * `Continuation` parameter and returns `Object` (possibly `COROUTINE_SUSPENDED`).
 *
 * This utility provides the bridge between the hidden class template (Java) and the
 * Kotlin coroutine machinery.
 */
@InternalInvokerApi
object SuspendInvokerSupport {

    /**
     * Invokes a suspend MethodHandle on the given CoroutineScope.
     *
     * The MethodHandle must have already been bound to the target instance and
     * type-erased to accept (PayloadType, Continuation) -> Object.
     *
     * @param scope         the CoroutineScope to launch the coroutine on
     * @param methodHandle  the bound MethodHandle for the suspend function
     * @param payload       the single argument to pass to the handler (the event/request/context)
     * @param onError       callback for errors (nullable, defaults to rethrowing)
     */
    @JvmStatic
    @JvmOverloads
    fun invokeSuspend(
        scope: CoroutineScope,
        methodHandle: MethodHandle,
        payload: Any,
        onError: ((Throwable) -> Unit)? = null
    ) {
        scope.launch {
            invokeSuspendDirect(methodHandle, payload, coroutineContext)
        }.invokeOnCompletion { throwable ->
            if (throwable != null) {
                onError?.invoke(throwable) ?: throw throwable
            }
        }
    }

    /**
     * Directly invokes a suspend MethodHandle from within an existing coroutine.
     * This is a suspend function itself, so it properly participates in structured concurrency.
     *
     * @param methodHandle the bound MethodHandle for the suspend function
     * @param payload      the argument to pass to the handler
     * @param context      the CoroutineContext (used for the Continuation)
     */
    @JvmStatic
    suspend fun invokeSuspendDirect(
        methodHandle: MethodHandle,
        payload: Any,
        context: CoroutineContext = EmptyCoroutineContext
    ) {
        return suspendCoroutineUninterceptedOrReturn { cont ->
            try {
                val result = methodHandle.invoke(payload, cont)
                if (result === COROUTINE_SUSPENDED) {
                    COROUTINE_SUSPENDED
                } else {
                    // Handler completed synchronously (didn't actually suspend)
                    result
                }
            } catch (t: Throwable) {
                cont.resumeWith(Result.failure(t))
                COROUTINE_SUSPENDED
            }
        }
    }
}