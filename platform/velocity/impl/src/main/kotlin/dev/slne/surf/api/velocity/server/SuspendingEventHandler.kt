package dev.slne.surf.api.velocity.server

import com.google.common.reflect.TypeToken
import com.velocitypowered.api.event.EventManager
import com.velocitypowered.api.event.EventTask
import com.velocitypowered.proxy.event.VelocityEventManager
import dev.slne.surf.api.common.invoker.InvokerMethods
import dev.slne.surf.api.reflection.reflectionProxy
import dev.slne.surf.api.velocity.server.reflection.CustomHandlerAdapterProxy
import dev.slne.surf.api.velocity.server.reflection.VelocityEventManagerProxy
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

    private fun registerValidationAdapter() {
        require(eventManager is VelocityEventManager) { "Only VelocityEventManager is supported" }

        val handler = reflectionProxy<CustomHandlerAdapterProxy>().create(
            "surf_api_suspending_event_handler",
            InvokerMethods::isSuspendFunction,
            { method, errors ->
                if (method.parameterCount != 2) {
                    val eventParameters = method.parameterCount - 1
                    errors += "Expected exactly one event parameter, got $eventParameters."
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
            lookup,
        )

        reflectionProxy<VelocityEventManagerProxy>().handlerAdapters(eventManager).add(handler)
    }

    private fun suspendingEventTask(handler: suspend () -> Unit): EventTask {
        return EventTask.withContinuation {
            handler.startCoroutine(
                it.asCoroutineContinuation(),
            )
        }
    }

    private fun EventContinuation.asCoroutineContinuation(): Continuation<Unit> {
        return Continuation(EmptyCoroutineContext) {
            if (it.isFailure) resumeWithException(it.exceptionOrNull()) else resume()
        }
    }

    companion object {
        private val lookup = MethodHandles.lookup()
    }
}
