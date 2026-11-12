package dev.slne.surf.redis.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializerOrNull

class KotlinSerializerCache<T>(
    private val module: SerializersModule,
    private val type: Class<T>
) : ClassValue<KSerializer<T>?>() {

    @Suppress("UNCHECKED_CAST")
    override fun computeValue(type: Class<*>): KSerializer<T>? {
        if (!this.type.isAssignableFrom(type)) return null
        return module.serializerOrNull(type) as? KSerializer<T>
    }

    companion object {
        inline operator fun <reified T> invoke(
            module: SerializersModule,
        ): KotlinSerializerCache<T> = KotlinSerializerCache(module, T::class.java)
    }
}