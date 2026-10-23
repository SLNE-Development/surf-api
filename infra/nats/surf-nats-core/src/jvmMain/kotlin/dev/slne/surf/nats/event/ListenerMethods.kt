package dev.slne.surf.nats.event

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.client.NatsClientImpl
import dev.slne.surf.nats.descriptor.NatsEvent
import dev.slne.surf.nats.rpc.Recipients
import dev.slne.surf.nats.runtime.ListenerRules
import dev.slne.surf.nats.util.isSuspend
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.WildcardType
import kotlin.coroutines.Continuation

@InternalNatsApi
internal class ListenerMethod(
    val method: Method,
    val descriptor: EventDescriptor,
    val recipients: Recipients,
    val ignoreFromThisClient: Boolean,
)

@InternalNatsApi
internal object ListenerMethods {

    private data class Signature(val name: String, val parameterTypes: List<Class<*>>)

    fun findListenerMethods(listener: Any, client: NatsClientImpl): List<ListenerMethod> {
        val methods = ObjectArrayList<ListenerMethod>()
        val problems = ObjectArrayList<String>()

        val seen = ObjectOpenHashSet<Signature>()
        val unannotated = ObjectOpenHashSet<Signature>()

        var currentClass: Class<*>? = listener.javaClass
        while (currentClass != null) {
            for (method in currentClass.declaredMethods) {
                if (method.isBridge || method.isSynthetic) continue

                val annotation = method.getAnnotation(NatsListener::class.java)

                val modifiers = method.modifiers
                if (!Modifier.isPrivate(modifiers) && !Modifier.isStatic(modifiers)) {
                    val signature = Signature(method.name, method.parameterTypes.toList())
                    if (!seen.add(signature)) {
                        if (annotation != null && unannotated.remove(signature)) {
                            problems.add(
                                "${method.name}: overrides the @NatsListener method of " +
                                        "${currentClass.name} without @NatsListener"
                            )
                        }
                        continue
                    }
                    if (annotation == null) unannotated.add(signature)
                }

                if (annotation == null) continue

                val problem = validate(method)
                if (problem != null) {
                    problems.add("${method.name}: $problem")
                    continue
                }

                val listenerProblem = ListenerRules.problem(annotation.recipients, annotation.ignoreFromThisClient)
                if (listenerProblem != null) {
                    problems.add("${method.name}: $listenerProblem")
                    continue
                }

                val eventType = method.parameterTypes[0]

                val descriptor = try {
                    client.eventDescriptor(eventType)
                } catch (e: Exception) {
                    problems += "${method.name}: ${e.message}"
                    continue
                }

                methods += ListenerMethod(
                    method,
                    descriptor,
                    annotation.recipients,
                    annotation.ignoreFromThisClient
                )
            }

            currentClass = currentClass.superclass
        }

        require(problems.isEmpty) {
            "Invalid @NatsListener methods in ${listener.javaClass.name}:\n" +
                    problems.joinToString("\n") { " - $it" }
        }

        require(methods.isNotEmpty()) {
            "${listener.javaClass.name} has no @NatsListener methods"
        }

        return methods
    }

    private fun validate(method: Method): String? {
        val parameterTypes = method.parameterTypes
        val suspend = method.isSuspend()
        val expectedParameterCount = if (suspend) 2 else 1

        return when {
            parameterTypes.size != expectedParameterCount -> "must take exactly one event parameter"
            !suspend && method.returnType != Void.TYPE -> "must return Unit"
            suspend && !returnsUnitWhenSuspending(method) -> "must return Unit"
            !NatsEvent::class.java.isAssignableFrom(parameterTypes[0]) -> "parameter ${parameterTypes[0].name} is not a generated surf-nats event"
            else -> null
        }
    }

    /**
     * Checks for `Continuation<? super Unit>`; a raw `Continuation` parameter carries no return type and passes.
     */
    private fun returnsUnitWhenSuspending(method: Method): Boolean {
        val continuation = method.genericParameterTypes.last() as? ParameterizedType ?: return true
        if (continuation.rawType != Continuation::class.java) return false

        val argument = continuation.actualTypeArguments.singleOrNull() as? WildcardType ?: return false
        return argument.lowerBounds.singleOrNull() == Unit::class.java
    }
}
