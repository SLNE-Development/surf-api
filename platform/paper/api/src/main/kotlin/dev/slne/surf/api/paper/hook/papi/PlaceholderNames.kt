package dev.slne.surf.api.paper.hook.papi

import dev.slne.surf.api.shared.api.util.InternalSurfApi

private const val RESERVED_CHARACTERS = "%{}_"

/**
 * Requires [name] to be a valid expansion identifier or placeholder name: not empty and free of
 * `%`, `{`, `}` and `_`.
 *
 * @param name the identifier or name to check
 * @param kind what [name] is, such as "Placeholder name", used in the failure message
 * @throws IllegalArgumentException if [name] is empty or contains a reserved character
 */
@InternalSurfApi
fun requirePlaceholderName(name: String, kind: String) {
    require(name.isNotEmpty()) { "$kind must not be empty" }
    require(name.none { it in RESERVED_CHARACTERS }) {
        "$kind '$name' contains one of $RESERVED_CHARACTERS"
    }
}
