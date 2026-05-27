package dev.slne.surf.api.core.config.constraints

import dev.slne.surf.api.core.config.type.StringOrDefault

internal fun Any?.toStringOrDefaultAware(): String {
    if (this is StringOrDefault) {
        return value.toString()
    }

    return toString()
}