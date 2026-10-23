package dev.slne.surf.nats.bytecode

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.descriptor.NatsParameter
import java.lang.reflect.Method
import java.lang.reflect.Modifier

@InternalNatsApi
internal class ValueClassParameter(val box: Method, val unbox: Method) {
    val type: Class<*> get() = unbox.declaringClass
    val underlying: Class<*> get() = unbox.returnType
}

@InternalNatsApi
internal object ValueClassParameters {

    // parameters: the descriptor parameters of method, without the trailing Continuation; a problem is reported to
    // problems, prefixed with label, and returns null
    fun of(
        method: Method,
        parameters: List<NatsParameter>,
        label: String,
        problems: MutableList<String>,
    ): List<ValueClassParameter?>? {
        val parameterTypes = method.parameterTypes
        if (parameters.size != parameterTypes.size - 1) {
            problems.add("$label: ${method.name} has ${parameterTypes.size - 1} parameters, expected ${parameters.size}")
            return null
        }

        val result = ArrayList<ValueClassParameter?>(parameters.size)
        for ((i, parameter) in parameters.withIndex()) {
            val type = parameter.type.java
            val underlying = parameterTypes[i]
            if (!type.isAnnotationPresent(JvmInline::class.java) || underlying == type) {
                result.add(null)
                continue
            }

            val declared = type.declaredMethods
            val box = declared.singleOrNull { candidate ->
                candidate.name == "box-impl" &&
                        Modifier.isStatic(candidate.modifiers) &&
                        candidate.parameterCount == 1 &&
                        candidate.parameterTypes[0] == underlying
            }
            val unbox = declared.singleOrNull { candidate ->
                candidate.name == "unbox-impl" && candidate.parameterCount == 0
            }

            if (box == null || unbox == null || unbox.returnType != underlying) {
                problems.add(
                    "$label: parameter '${parameter.name}' (${type.name}) has no box-impl/unbox-impl for ${underlying.name}"
                )
                return null
            }

            result.add(ValueClassParameter(box, unbox))
        }
        return result
    }
}
