package dev.slne.surf.api.velocity.server

import com.google.common.reflect.TypeToken
import com.velocitypowered.api.event.EventManager
import com.velocitypowered.api.event.EventTask
import com.velocitypowered.proxy.event.VelocityEventManager
import dev.slne.surf.api.core.invoker.HiddenInvokerUtil
import dev.slne.surf.api.shared.api.util.InternalInvokerApi
import dev.slne.surf.api.velocity.server.reflection.VelocityEventManagerReflection
import java.lang.invoke.MethodHandles
import java.util.function.BiFunction
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import com.velocitypowered.api.event.Continuation as EventContinuation


class SuspendingEventHandler(private val eventManager: EventManager) {

    fun register() {
        registerValidationAdapter()
    }

    @OptIn(InternalInvokerApi::class)
    private fun registerValidationAdapter() {
        require(eventManager is VelocityEventManager) { "Only VelocityEventManager is supported" }

        val handler = VelocityEventManagerReflection.createCustomHandlerAdapter(
            "surf_api_suspending_event_handler",
            HiddenInvokerUtil::isSuspendFunction,
            { method, errors ->
                if (method.parameterCount != 2) {
                    errors += "Expected exactly one event parameter, got ${method.parameterCount - 1}."
                }
            },
            object : TypeToken<suspend (Any, Any) -> Unit>() {},
            { function ->
                BiFunction { instance, event ->
                    suspendingEventTask {
                        function(instance, event)
                    }
                }
            },
            lookup
        )

        VelocityEventManagerReflection.getHandlerAdapters(eventManager).add(handler)
    }

    private fun suspendingEventTask(handler: suspend () -> Unit) =
        EventTask.withContinuation { handler.startCoroutine(it.asCoroutineContinuation()) }

    private fun EventContinuation.asCoroutineContinuation(): Continuation<Unit> =
        Continuation(EmptyCoroutineContext) { if (it.isFailure) resumeWithException(it.exceptionOrNull()) else resume() }

    companion object {
        private val lookup = MethodHandles.lookup();
    }
}