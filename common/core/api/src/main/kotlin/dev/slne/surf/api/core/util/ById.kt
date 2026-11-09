package dev.slne.surf.api.core.util

import java.util.function.ToIntFunction

fun interface ById {
    fun id(): Int

    fun interface ByByteId {
        fun id(): Byte
    }

    companion object {
        fun <T> build(clazz: Class<T>): Map<Int, T> where T : Enum<T>, T : ById {
            return byIdMap<T>(
                ToIntFunction { it.id() },
                clazz.getEnumConstants()
            )
        }
    }
}
