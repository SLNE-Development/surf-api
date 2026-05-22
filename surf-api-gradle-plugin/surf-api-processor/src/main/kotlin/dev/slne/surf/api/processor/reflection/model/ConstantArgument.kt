package dev.slne.surf.api.processor.reflection.model

import dev.slne.surf.api.processor.reflection.ReflectionSymbolProcessor

sealed interface ConstantArgument {
    val type: ReflectionSymbolProcessor.JavaType
    val javaExpression: String

    data class IntValue(val value: Int) : ConstantArgument {
        override val type = ReflectionSymbolProcessor.JavaType.Int
        override val javaExpression = value.toString()
    }

    data class LongValue(val value: Long) : ConstantArgument {
        override val type = ReflectionSymbolProcessor.JavaType.Long
        override val javaExpression = "${value}L"
    }

    data class BooleanValue(val value: Boolean) : ConstantArgument {
        override val type = ReflectionSymbolProcessor.JavaType.Boolean
        override val javaExpression = value.toString()
    }

    data class StringValue(val value: String) : ConstantArgument {
        override val type = ReflectionSymbolProcessor.JavaType.String
        override val javaExpression = buildString {
            append('"')
            value.forEach { char ->
                when (char) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(char)
                }
            }
            append('"')
        }
    }
}