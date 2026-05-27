package dev.slne.surf.api.core.config.constraints

import dev.slne.surf.api.core.config.type.StringOrDefault

internal tailrec fun Any?.configSizeOrNull(): Int? = when (this) {
    null -> null
    is Collection<*> -> size
    is Map<*, *> -> size
    is Array<*> -> size
    is CharSequence -> length
    is StringOrDefault -> value?.configSizeOrNull()
    else -> null
}