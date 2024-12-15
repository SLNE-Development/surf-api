package dev.slne.surf.surfapi.core.api.util

import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import java.util.function.ToIntFunction

fun interface ById {
    fun id(): Int

    fun interface ByByteId {
        fun id(): Byte
    }

    companion object {
        fun <T> build(clazz: Class<T>): Int2ObjectMap<T> where T : Enum<T>, T : ById {
            return Util.byIdMap<T>(
                ToIntFunction { it.id() },
                clazz.getEnumConstants()
            )
        }
    }
}
