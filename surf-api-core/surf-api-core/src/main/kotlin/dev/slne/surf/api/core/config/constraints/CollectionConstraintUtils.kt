package dev.slne.surf.api.core.config.constraints

internal fun Any?.configSizeOrNull(): Int? = when (this) {
    null -> null
    is Collection<*> -> size
    is Map<*, *> -> size
    is Array<*> -> size
    is CharSequence -> length
    else -> null
}