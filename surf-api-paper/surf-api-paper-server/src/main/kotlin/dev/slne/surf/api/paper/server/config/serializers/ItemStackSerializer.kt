package dev.slne.surf.api.paper.server.config.serializers

import org.bukkit.inventory.ItemStack
import org.spongepowered.configurate.serialize.ScalarSerializer
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.AnnotatedType
import java.util.function.Predicate
import kotlin.io.encoding.Base64

object ItemStackSerializer : ScalarSerializer.Annotated<ItemStack>(ItemStack::class.java) {

    override fun deserialize(type: AnnotatedType, obj: Any): ItemStack {
        val base64 = obj.toString()
        return try {
            val bytes = Base64.decode(base64)
            ItemStack.deserializeBytes(bytes)
        } catch (e: IllegalArgumentException) {
            throw SerializationException(type.type, "Invalid Base64-encoded ItemStack: ${e.message}", e)
        } catch (e: Exception) {
            throw SerializationException(type.type, "Failed to deserialize ItemStack: ${e.message}", e)
        }
    }

    override fun serialize(type: AnnotatedType, item: ItemStack?, typeSupported: Predicate<Class<*>>): Any? {
        if (item == null) return null
        val serialized = item.serializeAsBytes()
        return Base64.encode(serialized)
    }
}