package dev.slne.surf.surfapi.velocity.server

import com.google.common.reflect.TypeToken
import com.velocitypowered.api.event.EventManager
import com.velocitypowered.api.event.EventTask
import dev.slne.surf.surfapi.velocity.server.reflection.VelocityReflection
import java.util.function.BiConsumer
import java.util.function.BiFunction
import java.util.function.Function
import java.util.function.Predicate
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.reflect.jvm.kotlinFunction
import com.velocitypowered.api.event.Continuation as EventContinuation

class SuspendingEventHandler(private val eventManager: EventManager) {

    fun register() {
        VelocityReflection.EVENT_MANAGER_PROXY.registerHandlerAdapter(
            eventManager,
            "surf_api_suspending_event_handler",
            Predicate { method -> method.kotlinFunction?.isSuspend == true },
            BiConsumer { method, errors ->
                val function = method.kotlinFunction!!
                // parameters includes receiver, but excludes continuation
                if (function.parameters.size != 2) {
                    errors += "Expected 1 parameter, but got ${function.parameters.size - 1}. You only need the event parameter."
                }
                if (function.returnType.classifier != Unit::class) {
                    errors += "Expected return type of Unit, but got ${function.returnType}."
                }
            },
            object : TypeToken<suspend (Any, Any) -> Unit>() {},
            Function { function ->
                BiFunction { instance, event ->
                    suspendingEventTask {
                        function(instance, event)
                    }
                }
            }
        )
    }

    private fun suspendingEventTask(handler: suspend () -> Unit) =
        EventTask.withContinuation { handler.startCoroutine(it.asCoroutineContinuation()) }

    private fun EventContinuation.asCoroutineContinuation(): Continuation<Unit> =
        Continuation(EmptyCoroutineContext) { if (it.isFailure) resumeWithException(it.exceptionOrNull()) else resume() }
}