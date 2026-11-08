package dev.slne.surf.api.core.config.serializer

import org.spongepowered.configurate.serialize.ScalarSerializer
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.AnnotatedType
import java.util.function.Predicate
import java.util.regex.PatternSyntaxException

internal object RegexSerializer : ScalarSerializer.Annotated<Regex>(Regex::class.java) {
    override fun deserialize(type: AnnotatedType, obj: Any): Regex {
        return try {
            obj.toString().toRegex()
        } catch (e: PatternSyntaxException) {
            throw SerializationException(Regex::class.java, "$obj($type) is not a valid regex", e)
        }
    }

    override fun serialize(type: AnnotatedType, item: Regex, typeSupported: Predicate<Class<*>>): Any {
        return item.pattern
    }
}