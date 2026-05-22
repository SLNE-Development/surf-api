package dev.slne.surf.api.processor.reflection.model

import dev.slne.surf.api.processor.reflection.ReflectionSymbolProcessor

data class JavaParameter(
    val name: String,
    val type: ReflectionSymbolProcessor.JavaType,
    val nullability: JavaNullability,
)