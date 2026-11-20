package dev.slne.surf.api.config.serializer

import dev.slne.surf.api.messages.minimessage.miniMessage
import net.kyori.adventure.text.Component
import org.spongepowered.configurate.serialize.ScalarSerializer
import java.lang.reflect.AnnotatedType
import java.util.function.Predicate

/**
 * Configurate scalar serializer for Adventure [net.kyori.adventure.text.Component] values.
 *
 * Components are serialized as MiniMessage strings and deserialized through the shared
 * Surf MiniMessage instance.
 */
internal object ComponentSerializer : ScalarSerializer.Annotated<Component>(Component::class.java) {

    /**
     * Deserializes a MiniMessage string into a [Component].
     */
    override fun deserialize(type: AnnotatedType, obj: Any): Component {
        return miniMessage.deserialize(obj.toString())
    }

    /**
     * Serializes [item] into a MiniMessage string.
     */
    override fun serialize(
        type: AnnotatedType,
        item: Component,
        typeSupported: Predicate<Class<*>>
    ): Any {
        return miniMessage.serialize(item)
    }
}