package dev.slne.surf.surfapi.bukkit.server.config.serializers

import com.google.auto.service.AutoService
import dev.slne.surf.surfapi.core.api.config.serializer.SpongeConfigSerializers
import org.bukkit.inventory.ItemStack
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type
import java.util.*

@AutoService(SpongeConfigSerializers::class)
class PaperSpongeConfigSerializers : SpongeConfigSerializers() {
    init {
        registerClassSerializer<ItemStack>(ItemStackSerializer)
    }

    object ItemStackSerializer : TypeSerializer<ItemStack> {
        override fun deserialize(
            type: Type,
            node: ConfigurationNode
        ): ItemStack {
            val itemStackBase64 = node.string
                ?: throw SerializationException("Expected a Base64 string for ItemStack deserialization")
            val decoded = Base64.getDecoder().decode(itemStackBase64)

            return ItemStack.deserializeBytes(decoded)
        }

        override fun serialize(
            type: Type,
            obj: ItemStack?,
            node: ConfigurationNode
        ) {
            if (obj == null) {
                node.raw(null)
                return
            }

            val serialized = obj.serializeAsBytes()
            val encoded = Base64.getEncoder().encodeToString(serialized)

            node.set(encoded)
        }
    }
}