package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.InternalNatsApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import kotlin.reflect.KType
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.findAnnotation

@InternalNatsApi
internal object TypeSerializers {

    @Suppress("UNCHECKED_CAST")
    fun serializerFor(type: KType, module: SerializersModule): KSerializer<Any?> {
        val custom = type.findAnnotation<Serializable>()
            ?.with
            ?.takeIf { it != KSerializer::class }
            ?: return module.serializer(type)

        val serializer = (custom.objectInstance ?: custom.createInstance()) as KSerializer<Any>
        return (if (type.isMarkedNullable) serializer.nullable else serializer) as KSerializer<Any?>
    }
}