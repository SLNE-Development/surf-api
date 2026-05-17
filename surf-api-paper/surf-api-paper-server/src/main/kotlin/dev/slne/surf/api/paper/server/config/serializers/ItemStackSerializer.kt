package dev.slne.surf.api.paper.server.config.serializers

import org.bukkit.inventory.ItemStack
import org.spongepowered.configurate.serialize.ScalarSerializer
import java.lang.reflect.AnnotatedType
import java.util.function.Predicate
import kotlin.io.encoding.Base64

object ItemStackSerializer : ScalarSerializer.Annotated<ItemStack>(ItemStack::class.java) {

    override fun deserialize(type: AnnotatedType, obj: Any): ItemStack {
        val base64 = obj.toString()
        val bytes = Base64.decode(base64)

        return ItemStack.deserializeBytes(bytes)
    }

    override fun serialize(type: AnnotatedType, item: ItemStack?, typeSupported: Predicate<Class<*>>): Any? {
        if (item == null) return null
        val serialized = item.serializeAsBytes()
        return Base64.encode(serialized)
    }
}