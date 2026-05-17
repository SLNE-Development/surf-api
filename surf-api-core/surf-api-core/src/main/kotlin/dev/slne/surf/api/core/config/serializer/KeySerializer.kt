package dev.slne.surf.api.core.config.serializer

import net.kyori.adventure.key.InvalidKeyException
import net.kyori.adventure.key.Key
import org.spongepowered.configurate.serialize.ScalarSerializer.Annotated
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.AnnotatedType
import java.util.function.Predicate

/**
 * Configurate scalar serializer for Adventure [Key] values.
 *
 * Values are read from and written to their canonical string representation.
 */
internal object KeySerializer : Annotated<Key>(Key::class.java) {

    /**
     * Parses a string value into an Adventure [Key].
     */
    override fun deserialize(type: AnnotatedType, obj: Any): Key {
        try {
            return Key.key(obj.toString())
        } catch (e: InvalidKeyException) {
            throw SerializationException(Key::class.java, e)
        }
    }

    /**
     * Serializes [item] as its canonical string representation.
     */
    override fun serialize(type: AnnotatedType, item: Key, typeSupported: Predicate<Class<*>>): Any {
        return item.asString()
    }
}