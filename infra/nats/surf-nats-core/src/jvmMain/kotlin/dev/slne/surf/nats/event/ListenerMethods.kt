package dev.slne.surf.nats.event

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.client.NatsClientImpl
import dev.slne.surf.nats.descriptor.NatsEvent
import dev.slne.surf.nats.rpc.Recipients
import dev.slne.surf.nats.runtime.ListenerRules
import dev.slne.surf.nats.util.isSuspend
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.WildcardType
import kotlin.coroutines.Continuation

/**
 * A validated `@NatsListener` method with the settings of its annotation.
 */
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

    /**
     * Returns the `@NatsListener` methods that [scanType] and its superclasses declare.
     *
     * A listener method takes exactly one generated event and returns `Unit`, and may suspend. An overridden method
     * counts once, by its most specific declaration. Overriding an annotated method without the annotation is an
     * error.
     *
     * [scanType] defaults to the class of [listener]. A proxied bean passes the class behind its proxy: the methods
     * are then invoked on the proxy, so private and final methods, which a proxy cannot intercept, are rejected.
     *
     * Throws [IllegalArgumentException] listing every invalid method, when [listener] is not an instance of
     * [scanType], or when [scanType] has no listener method.
     */
    fun findListenerMethods(
        listener: Any,
        client: NatsClientImpl,
        scanType: Class<*> = listener.javaClass,
    ): List<ListenerMethod> {
        require(scanType.isInstance(listener)) {
            "${listener.javaClass.name} is not an instance of ${scanType.name}"
        }
        val proxied = scanType !== listener.javaClass

        val methods = ArrayList<ListenerMethod>()
        val problems = ArrayList<String>()

        val seen = HashSet<Signature>()
        val unannotated = HashSet<Signature>()

        var currentClass: Class<*>? = scanType
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

                val problem = validate(method) ?: if (proxied) proxyProblem(method) else null
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

        require(problems.isEmpty()) {
            "Invalid @NatsListener methods in ${scanType.name}:\n" +
                    problems.joinToString("\n") { " - $it" }
        }

        require(methods.isNotEmpty()) {
            "${scanType.name} has no @NatsListener methods"
        }

        return methods
    }

    private fun proxyProblem(method: Method): String? {
        val modifiers = method.modifiers
        return when {
            Modifier.isStatic(modifiers) -> null
            Modifier.isPrivate(modifiers) -> "must not be private on a proxied bean"
            Modifier.isFinal(modifiers) -> "must not be final on a proxied bean"
            else -> null
        }
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
     * Checks for `Continuation<? super Unit>`. A raw `Continuation` parameter carries no return type and passes.
     */
    private fun returnsUnitWhenSuspending(method: Method): Boolean {
        val continuation = method.genericParameterTypes.last() as? ParameterizedType ?: return true
        if (continuation.rawType != Continuation::class.java) return false

        val argument = continuation.actualTypeArguments.singleOrNull() as? WildcardType ?: return false
        return argument.lowerBounds.singleOrNull() == Unit::class.java
    }
}
