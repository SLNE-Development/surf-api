package dev.slne.surf.surfapi.core.api.messages.adventure

import net.kyori.adventure.identity.Identity
import net.kyori.adventure.key.Key
import net.kyori.adventure.pointer.Pointer
import java.util.*

/**
 * Creates a [Pointer] instance for the given key with a reified value type.
 *
 * @param key The key associated with the pointer.
 * @return A [Pointer] of type [V] linked to the provided key.
 *
 * **Example Usage:**
 * ```kotlin
 * val myKey = "example" defines "custom_data"
 * val myPointer = pointer<String>(myKey)
 * ```
 */
inline fun <reified V> pointer(key: Key) = Pointer.pointer(V::class.java, key)

/**
 * Creates an [Identity] instance using the provided [UUID].
 *
 * @param uuid The UUID representing the identity.
 * @return An [Identity] object associated with the given UUID.
 *
 * **Example Usage:**
 * ```kotlin
 * val playerUUID = UUID.randomUUID()
 * val playerIdentity = identity(playerUUID)
 * ```
 */
fun identity(uuid: UUID) = Identity.identity(uuid)