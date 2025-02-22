package dev.slne.surf.surfapi.core.api.messages.adventure

import net.kyori.adventure.pointer.Pointer
import net.kyori.adventure.pointer.Pointered
import kotlin.jvm.optionals.getOrNull

/**
 * Retrieves the value associated with the given [pointer] from this [Pointered] object.
 *
 * @param pointer The [Pointer] representing the key for retrieving the value.
 * @return The value of type [T], or `null` if no value is present.
 *
 * **Example Usage:**
 * ```kotlin
 * val pointer: Pointer<String> = pointer(key("example:data"))
 * val value: String? = somePointeredInstance.getPointer(pointer)
 * ```
 */
fun <T : Any> Pointered.getPointer(pointer: Pointer<T>): T? = get(pointer).getOrNull()