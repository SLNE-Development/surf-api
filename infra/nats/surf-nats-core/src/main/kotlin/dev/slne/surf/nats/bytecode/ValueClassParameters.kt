package dev.slne.surf.nats.bytecode

import dev.slne.surf.nats.InternalNatsApi
import java.lang.reflect.Method
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.jvm.kotlinFunction

@InternalNatsApi
internal class ValueClassParameter(val box: Method, val unbox: Method) {
    val type: Class<*> get() = unbox.declaringClass
    val underlying: Class<*> get() = unbox.returnType
}

@InternalNatsApi
internal object ValueClassParameters {

    fun of(method: Method, parameters: List<KParameter>): List<ValueClassParameter?> {
        val argumentTypes = method.parameterTypes
        return parameters.mapIndexed { i, parameter ->
            val type = (parameter.type.classifier as? KClass<*>)?.takeIf { it.isValue }?.java
            val underlying = argumentTypes[i]
            if (type == null || underlying == type) return@mapIndexed null // Already boxed

            val box = type.declaredMethods.singleOrNull { method ->
                method.name == "box-impl" && method.parameterTypes.contentEquals(arrayOf(underlying))
            }
            val unbox = type.declaredMethods.singleOrNull { method ->
                method.name == "unbox-impl" && method.parameterCount == 0
            }

            require(box != null && unbox != null && unbox.returnType == underlying) {
                "parameter '${parameter.name}' has the value class type ${parameter.type} without box-impl/unbox-impl " +
                        "for ${underlying.name}"
            }

            ValueClassParameter(box, unbox)
        }
    }

    fun of(method: Method): List<ValueClassParameter?> {
        val parameters = method.kotlinFunction
            ?.parameters
            ?.filter { it.kind == KParameter.Kind.VALUE }
            ?: return List(method.parameterCount - 1) { null }

        return of(method, parameters)
    }
}
