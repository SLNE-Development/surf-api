package dev.slne.surf.surfapi.core.api.messages.adventure

import net.kyori.adventure.identity.Identity
import net.kyori.adventure.key.Key
import net.kyori.adventure.pointer.Pointer
import java.util.*

inline fun <reified V> pointer(key: Key) = Pointer.pointer(V::class.java, key)
fun identity(uuid: UUID) = Identity.identity(uuid)