package dev.slne.surf.surfapi.core.api.messages.adventure

import net.kyori.adventure.pointer.Pointer
import net.kyori.adventure.pointer.Pointered
import kotlin.jvm.optionals.getOrNull

fun <T : Any> Pointered.getPointer(pointer: Pointer<T>): T? = get(pointer).getOrNull()