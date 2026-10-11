package dev.slne.surf.nats.event

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.client.NatsClientImpl
import dev.slne.surf.nats.util.isSuspend
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.reflect.jvm.kotlinFunction

@InternalNatsApi
internal class ListenerMethod(
    val method: Method,
    val descriptor: EventDescriptor,
    val loadBalanced: Boolean,
    val ignoreOwn: Boolean,
)

@InternalNatsApi
internal object ListenerMethods {

    private data class Signature(val name: String, val parameterTypes: List<Class<*>>)

    fun findListenerMethods(listener: Any, client: NatsClientImpl): List<ListenerMethod> {
        val methods = ObjectArrayList<ListenerMethod>()
        val problems = ObjectArrayList<String>()

        val seen = ObjectOpenHashSet<Signature>()

        var currentClass: Class<*>? = listener.javaClass
        while (currentClass != null) {
            for (method in currentClass.declaredMethods) {
                if (method.isBridge || method.isSynthetic) continue

                val modifiers = method.modifiers
                val overridable = !Modifier.isPrivate(modifiers) && !Modifier.isStatic(modifiers)
                if (overridable &&
                    !seen.add(Signature(method.name, method.parameterTypes.toList()))
                ) continue

                val annotation = method.getAnnotation(NatsListener::class.java) ?: continue

                val problem = validate(method)
                if (problem != null) {
                    problems.add("${method.name}: $problem")
                    continue
                }

                if (annotation.loadBalanced && annotation.ignoreOwn) {
                    problems.add("${method.name}: ignoreOwn cannot be combined with loadBalanced")
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
                    annotation.loadBalanced,
                    annotation.ignoreOwn
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
            suspend && method.kotlinFunction?.returnType?.classifier.let { it != null && it != Unit::class } -> "must return Unit"
            !parameterTypes[0].isAnnotationPresent(NatsEvent::class.java) -> "parameter ${parameterTypes[0].name} is not annotated with @NatsEvent"
            else -> null
        }
    }
}