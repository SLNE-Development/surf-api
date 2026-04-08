package dev.slne.surf.api.core.server.messages

import dev.slne.surf.api.core.util.mutableObject2ObjectMapOf

object TranslationFlattener {
    fun flatten(map: Map<String, Any>): Map<String, Any> {
        val result = mutableObject2ObjectMapOf<String, Any>()
        flattenInto(map, "", result)
        return result
    }

    private fun flattenInto(
        map: Map<String, Any>,
        prefix: String,
        target: MutableMap<String, Any>
    ) {
        for ((key, value) in map) {
            val newKey = if (prefix.isEmpty()) key else "$prefix.$key"

            when (value) {
                is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    flattenInto(value as Map<String, Any>, newKey, target)
                }

                else -> target[newKey] = value
            }
        }
    }
}